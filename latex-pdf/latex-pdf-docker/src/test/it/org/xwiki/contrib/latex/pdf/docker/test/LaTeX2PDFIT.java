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
package org.xwiki.contrib.latex.pdf.docker.test;

import java.io.File;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFResult;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link org.xwiki.contrib.latex.internal.pdf.docker.DockerLaTeX2PDFConverter}. Note that this
 * test will connect to the internet and use dockerhub to download the docker image used to perform the LaTeX to PDF
 * conversion.
 *
 * @version $Id$
 */
@AllComponents
@ComponentTest
class LaTeX2PDFIT
{
    @InjectComponentManager
    private ComponentManager componentManager;

    @XWikiTempDir
    private File tmpDir;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @AfterEach
    void after()
    {
        // We only capture the DEBUG logs to debug an issue on the CI and display the debug logs in this case. Thus,
        // we don't care about asserting them
        logCapture.ignoreAllMessages();
    }

    @Test
    void convert() throws Exception
    {
        FileUtils.copyDirectory(new File("src/test/resources/latex"), this.tmpDir);

        LaTeX2PDFConverter converter = this.componentManager.getInstance(LaTeX2PDFConverter.class, "docker");
        LaTeX2PDFResult result = converter.convert(this.tmpDir);
        File fullPdfFile = new File(this.tmpDir, "index.pdf");
        assertEquals(fullPdfFile, result.getPDFFile(), String.format("PDF File not generated properly from LaTeX "
            + "files in [%s]. Debug logs:[\n%s\n]", this.tmpDir.toString(), getDebugLogs()));
        assertTrue(result.getPDFFile().exists());

        // Make sure that the generated PDF has the same owner as the current user. There used to be a bug where the
        // PDF file was created as root.
        assertEquals(System.getProperty("user.name"), Files.getOwner(fullPdfFile.toPath()).getName());

        // Assert the generated PDF
        assertTrue(getPDFContent(fullPdfFile).contains(
            "The sandbox is a part of your wiki that you can freely modify"));
    }

    @Test
    void convertWhenCompilationError() throws Exception
    {
        FileUtils.copyDirectory(new File("src/test/resources/latexInvalid"), this.tmpDir);

        LaTeX2PDFConverter converter = this.componentManager.getInstance(LaTeX2PDFConverter.class, "docker");
        LaTeX2PDFResult result = converter.convert(this.tmpDir);
        assertNull(result.getPDFFile());
        assertThat(result.getLogs(), containsString("==> Fatal error occurred, no output PDF file produced!"));
    }

    private String getPDFContent(File pdfFile) throws Exception
    {
        PDDocument pdd = PDDocument.load(pdfFile);
        String text;
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(pdd);
        } finally {
            if (pdd != null) {
                pdd.close();
            }
        }
        return text;
    }

    private String getDebugLogs()
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < logCapture.size(); i++) {
            builder.append(logCapture.getMessage(i)).append('\n');
        }
        return builder.toString();
    }
}
