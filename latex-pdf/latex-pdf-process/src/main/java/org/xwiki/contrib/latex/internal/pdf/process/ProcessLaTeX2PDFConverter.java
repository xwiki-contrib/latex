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
package org.xwiki.contrib.latex.internal.pdf.process;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.pdf.process.LaTeX2PDFConfiguration;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFException;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFResult;

/**
 * Convert a LaTeX zip file to a PDF file by starting a local process.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Named("process")
@Singleton
public class ProcessLaTeX2PDFConverter implements LaTeX2PDFConverter
{
    @Inject
    private Logger logger;

    @Inject
    private LaTeX2PDFConfiguration configuration;

    @Override
    public LaTeX2PDFResult convert(File latexDirectory) throws LaTeX2PDFException
    {
        for (String command : this.configuration.getCommands()) {
            executeCommand(command, latexDirectory);
        }

        // Check if there's a PDF file to return.
        // Prepare results. Express an error by setting a null PDF file.
        File pdfFile = new File(latexDirectory, "index.pdf");
        LaTeX2PDFResult result;
        if (pdfFile.exists()) {
            result = new LaTeX2PDFResult(pdfFile, null);
        } else {
            result = new LaTeX2PDFResult(null);
        }

        return result;
    }

    @Override
    public boolean isReady()
    {
        return true;
    }

    private void executeCommand(String commandLine, File workingDirectory) throws LaTeX2PDFException
    {
        // The command line to execute
        CommandLine command = CommandLine.parse(commandLine);

        // Send Process output and error streams to our logger.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        // Make sure we end the process when the JVM exits
        ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();

        // Prevent the process from running indefinitely and kill it after 1 hour...
        ExecuteWatchdog watchDog = new ExecuteWatchdog(60L * 60L * 1000L);

        // The executor to execute the command
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDirectory);
        executor.setStreamHandler(streamHandler);
        executor.setProcessDestroyer(processDestroyer);
        executor.setWatchdog(watchDog);

        try {
            // Inherit the current process's environment variables
            @SuppressWarnings("unchecked")
            Map<String, String> newEnvironment = EnvironmentUtils.getProcEnvironment();

            executor.execute(command, newEnvironment);
        } catch (Exception e) {
            // Since we don't force an exit value with executor.setExitValue() the call to execute() will throw an
            // Exception if the exit code is non-zero. We catch the exception here to provide additional information
            // and pass it upstream.
            throw new LaTeX2PDFException(String.format("Failed to execute command [%s] in [%s]. Logs: [%n%s%n]",
                command, workingDirectory, outputStream), e);
        }

        // For debugging information, log the output of the command's execution.
        this.logger.info(outputStream.toString());
    }
}
