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
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFException;
import org.xwiki.text.StringUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

/**
 * Help perform various operations on containers (copy files and directories from container to host and from host to
 * container, etc).
 *
 * @version $Id$
 */
public class ContainerManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManager.class);

    private static final String DATA_DIR = "data";

    private static final String ABSOLUTE_DATA_DIR = String.format("/%s", DATA_DIR);

    private DockerClient client;

    /**
     * @param client the docker client object on which to perform the container operations
     */
    public ContainerManager(DockerClient client)
    {
        this.client = client;
    }

    /**
     * Copy the passed host directory to the {@ink #ABSOLUTE_DATA_DIR} directory in the container.
     *
     * @param containerId the id of the container into which to copy
     * @param hostDirectory the directory to copy from the host
     */
    public void copyToContainer(String containerId, File hostDirectory)
    {
        this.client.copyArchiveToContainerCmd(containerId)
            .withHostResource(hostDirectory.getAbsolutePath())
            .withDirChildrenOnly(true)
            .withRemotePath(ABSOLUTE_DATA_DIR)
            .exec();
    }

    /**
     * Copy the {@link #ABSOLUTE_DATA_DIR} directory from the container into the passed directory on the host.
     *
     * @param containerId the id of the container from which to copy
     * @param hostDirectory the directory to copy to on the host
     * @throws LaTeX2PDFException if there's an error during the copy
     */
    public void copyToHost(String containerId, File hostDirectory) throws LaTeX2PDFException
    {
        unTar(containerId, hostDirectory);
    }

    /**
     * Get logs from the container.
     *
     * @param containerId the id of the container from which to get logs from
     * @return the logs as a string
     */
    public String getLogs(String containerId)
    {
        StringBuilder logs = new StringBuilder();
        LogContainerCmd logContainerCmd = this.client.logContainerCmd(containerId)
            .withStdErr(true)
            .withStdOut(true)
            .withFollowStream(true)
            .withTailAll()
            .withTimestamps(true);
        wait(logContainerCmd.exec(new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item)
            {
                logs.append(item.toString()).append('\n');
            }
        }));
        String result = logs.toString();
        LOGGER.debug(result);
        return result;
    }

    /**
     * Start the passed container and wait till it exits.
     *
     * @param containerId the id of the container to start
     */
    public void startContainer(String containerId)
    {
        // Start (and stop and remove automatically when the conversion is finished, thanks to the autoremove above).
        this.client.startContainerCmd(containerId).exec();

        // Wait for the container to have fully finished before continuing (to be sure that the PDF file is output
        // completely).
        WaitContainerResultCallback resultCallback = new WaitContainerResultCallback();
        this.client.waitContainerCmd(containerId).exec(resultCallback);
        wait(resultCallback);
    }

    /**
     * Docker-pull the passed image.
     *
     * @param imageName the image to pull
     */
    public void pullImage(String imageName)
    {
        PullImageResultCallback pullImageResultCallback = this.client.pullImageCmd(imageName)
            .exec(new PullImageResultCallback());
        wait(pullImageResultCallback);
    }

    private void wait(ResultCallbackTemplate template)
    {
        try {
            template.awaitCompletion();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted thread [{}]. Root cause: [{}]", Thread.currentThread().getName(),
                ExceptionUtils.getRootCauseMessage(e));
            // Restore interrupted state to be a good citizen...
            Thread.currentThread().interrupt();
        }
    }

    private void unTar(String containerId, File latexDirectory) throws LaTeX2PDFException
    {
        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
            this.client.copyArchiveFromContainerCmd(containerId, ABSOLUTE_DATA_DIR).exec()))
        {
            unTar(tarStream, latexDirectory, String.format("%s/", DATA_DIR));
        } catch (IOException e) {
            throw new LaTeX2PDFException(
                String.format("Failed to copy container directory [/data] to host at [%s]",
                    latexDirectory.getAbsolutePath()), e);
        }
    }

    private void unTar(TarArchiveInputStream tis, File targetDirectory, String prefixToRemove) throws IOException
    {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        TarArchiveEntry tarEntry;
        while ((tarEntry = tis.getNextTarEntry()) != null) {
            File file = new File(targetDirectory, StringUtils.substringAfter(tarEntry.getName(), prefixToRemove));
            if (tarEntry.isDirectory()) {
                handleTarDirectory(file);
            } else {
                FileOutputStream fos = new FileOutputStream(file);
                IOUtils.copy(tis, fos);
                fos.close();
            }
        }
        tis.close();
    }

    private void handleTarDirectory(File file) throws IOException
    {
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new IOException(String.format("Failed to create directory [%s]", file));
            }
        }
    }
}
