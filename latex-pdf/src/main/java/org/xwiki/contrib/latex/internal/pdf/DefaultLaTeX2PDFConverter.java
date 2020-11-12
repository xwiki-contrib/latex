/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.latex.internal.pdf;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConfiguration;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFException;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFResult;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

/**
 * Convert a LaTeX zip file to a PDF file by starting/stopping a docker image that does the work.
 *
 * @version $Id$
 * @since 1.10
 */
@Component
@Singleton
public class DefaultLaTeX2PDFConverter implements LaTeX2PDFConverter
{
    private static final String DOCKER_SOCK = "/var/run/docker.sock";

    @Inject
    private Logger logger;

    @Inject
    private LaTeX2PDFConfiguration configuration;

    @Override
    public LaTeX2PDFResult convert(File latexDirectory) throws LaTeX2PDFException
    {
        try {
            return convertInternal(latexDirectory);
        } catch (Exception e) {
            // Note: docker-java only throws runtime exceptions so we convert them to checked exceptions so that the
            // XWiki converter API is more expressive for the caller and follows the XWiki best practices related to
            // exception handling.
            throw new LaTeX2PDFException(
                String.format("Failed to convert LaTeX sources inside the [%s] directory to PDF", latexDirectory), e);
        }
    }

    @Override
    public boolean isReady()
    {
        boolean isReady;
        DockerClient dockerClient = getDockerClient();
        try {
            dockerClient.pingCmd().exec();
            isReady = true;
        } catch (Exception e) {
            isReady = false;
        }
        return isReady;
    }

    private LaTeX2PDFResult convertInternal(File latexDirectory)
    {
        DockerClient dockerClient = getDockerClient();

        // If the image doesn't exist locally, pull it.
        List<Image> images =
            dockerClient.listImagesCmd().withImageNameFilter(this.configuration.getDockerImageName()).exec();
        if (images.isEmpty()) {
            PullImageResultCallback pullImageResultCallback =
                dockerClient.pullImageCmd(this.configuration.getDockerImageName())
                .exec(new PullImageResultCallback());
            wait(pullImageResultCallback);
        }

        // Example docker run command line that we're simulating:
        //   docker run -v <local dir>:/data blang/latex:ubuntu <cmd>
        CreateContainerResponse container = null;
        String logs;
        try {
            container = dockerClient.createContainerCmd(this.configuration.getDockerImageName())
                .withCmd(this.configuration.getDockerCommands())
                .withHostConfig(HostConfig.newHostConfig()
                    .withBinds(
                        // Make sure it also works when XWiki is running in Docker
                        new Bind(DOCKER_SOCK, new Volume(DOCKER_SOCK)),
                        // Make the LaTeX source visible to the container, in the right place
                        new Bind(latexDirectory.getAbsolutePath(), new Volume("/data"), AccessMode.rw))
                )
                .exec();
            // Perform the compilation
            startContainer(dockerClient, container.getId());
            // Get container logs & display them in debug mode
            logs = getLogs(container.getId(), dockerClient);
        } finally {
            // Remove the container. Note that we cannot use autoremove since we need to get the logs and that works
            // only if the container is still running.
            if (container != null) {
                dockerClient.removeContainerCmd(container.getId()).exec();
            }
        }

        // Prepare results. Express an error by setting a null PDF file.
        File pdfFile = new File(latexDirectory, "index.pdf");
        LaTeX2PDFResult result;
        if (pdfFile.exists()) {
            result = new LaTeX2PDFResult(pdfFile, logs);
        } else {
            result = new LaTeX2PDFResult(logs);
        }

        return result;
    }

    private DockerClient getDockerClient()
    {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    private void startContainer(DockerClient dockerClient, String containerId)
    {
        // Start (and stop and remove automatically when the conversion is finished, thanks to the autoremove above).
        dockerClient.startContainerCmd(containerId).exec();

        // Wait for the container to have fully finished before continuing (to be sure that the PDF file is output
        // completely).
        WaitContainerResultCallback resultCallback = new WaitContainerResultCallback();
        dockerClient.waitContainerCmd(containerId).exec(resultCallback);
        wait(resultCallback);
    }

    private String getLogs(String containerId, DockerClient dockerClient)
    {
        StringBuilder logs = new StringBuilder();
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId)
            .withStdErr(true)
            .withStdOut(true)
            .withFollowStream(true)
            .withTailAll()
            .withTimestamps(true);
        wait(logContainerCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame item) {
                    logs.append(item.toString()).append('\n');
                }
            }));
        String result = logs.toString();
        this.logger.debug(result);
        return result;
    }

    private void wait(ResultCallbackTemplate template)
    {
        try {
            template.awaitCompletion();
        } catch (InterruptedException e) {
            this.logger.warn("Interrupted thread [{}]. Root cause: [{}]", Thread.currentThread().getName(),
                ExceptionUtils.getRootCauseMessage(e));
            // Restore interrupted state to be a good citizen...
            Thread.currentThread().interrupt();
        }
    }
}
