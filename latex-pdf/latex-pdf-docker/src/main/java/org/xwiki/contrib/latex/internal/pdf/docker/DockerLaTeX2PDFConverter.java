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
package org.xwiki.contrib.latex.internal.pdf.docker;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.pdf.docker.LaTeX2PDFConfiguration;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFException;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFResult;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

/**
 * Convert a LaTeX zip file to a PDF file by starting/stopping a docker image that does the work.
 *
 * @version $Id$
 * @since 1.10
 */
@Component
@Named("docker")
@Singleton
public class DockerLaTeX2PDFConverter implements LaTeX2PDFConverter
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
            this.logger.debug("Docker is not ready!", e);
            isReady = false;
        }
        return isReady;
    }

    private LaTeX2PDFResult convertInternal(File latexDirectory) throws LaTeX2PDFException
    {
        DockerClient dockerClient = getDockerClient();
        ContainerManager manager = new ContainerManager(dockerClient);

        // If the image doesn't exist locally, pull it.
        if (!isLocalImagePresent(this.configuration.getDockerImageName(), dockerClient)) {
            manager.pullImage(this.configuration.getDockerImageName());
        }

        // Example docker run command line that we're simulating:
        //   docker run -v <local dir>:/data blang/latex:ubuntu <cmd>
        CreateContainerResponse container = null;
        String logs;
        try (CreateContainerCmd command = dockerClient.createContainerCmd(this.configuration.getDockerImageName())) {
            container = command
                .withCmd(this.configuration.getDockerCommands())
                .withHostConfig(HostConfig.newHostConfig()
                    .withBinds(
                        // Make sure it also works when XWiki is running in Docker
                        new Bind(DOCKER_SOCK, new Volume(DOCKER_SOCK))
                    )
                )
                .exec();
            // Copy the latex sources in the container so that they are visible to pdflatex.
            // Note: we don't use a volume binding such as:
            //   new Bind(latexDirectory.getAbsolutePath(), new Volume("/data"), AccessMode.rw)
            // The reason is that the generated files would be created as root on the host on linux OSes. One
            // solution would be to run docker with "--user" but that requires to create the user in the image and
            // since we want to use an existing image, we can't do this. Thus we are choosing the slower option to
            // copy files from the container to the host and back to get the generated PDF and other ancillary files.
            manager.copyToContainer(container.getId(), latexDirectory);
            // Perform the compilation
            manager.startContainer(container.getId());
            // Get container logs & display them in debug mode
            logs = manager.getLogs(container.getId());
            // Copy the generated files back to the host. For simplicity we copy all the files.
            // One option would be to control pdflatex and use the "--output-directory" option but that would force
            // users who want to tweak the configuration to understand they need to output the results to a specific
            // directory (and break backward compatibility). Thus we have chosen the transparent route FTM.
            manager.copyToHost(container.getId(), latexDirectory);
        } finally {
            // Remove the container. Note that we cannot use autoremove since we need to get the logs and that works
            // only if the container is still running.
            if (container != null) {
                try (RemoveContainerCmd command = dockerClient.removeContainerCmd(container.getId())) {
                    command.exec();
                }
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
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    private boolean isLocalImagePresent(String imageName, DockerClient dockerClient)
    {
        boolean exists = true;
        try {
            dockerClient.inspectImageCmd(imageName).exec();
        } catch (NotFoundException e) {
            exists = false;
        }
        return exists;
    }
}
