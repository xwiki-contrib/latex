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
package org.xwiki.contrib.latex.test.ui;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;
import org.xwiki.contrib.latex.test.po.LaTeXExportOptions;
import org.xwiki.contrib.latex.test.po.LaTeXExportProgress;
import org.xwiki.contrib.latex.test.po.LaTeXFormatPane;
import org.xwiki.flamingo.skin.test.po.ExportModal;
import org.xwiki.test.docker.internal.junit5.FileTestUtils;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the ability to export to LaTeX and then transform into PDF.
 *
 * @version $Id: 4ec9fafbe8b7b043c145f37d2fa753eadec6e17b $
 */
@UITest
class ExportIT
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportIT.class);

    @AfterEach
    void validatationExcludes(LogCaptureConfiguration logCaptureConfiguration)
    {
        logCaptureConfiguration.registerExcludes(
            "New fonts found, font cache will be re-built",
            "Building on-disk font cache, this may take a while",
            "Using fallback font",
            "Can't find any begin event corresponding to",
            "Finished building on-disk font cache, found"
        );
    }

    @Test
    @Order(1)
    void exportToLaTeX(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        String content = "Hello **world** {{velocity}}$xcontext.action{{/velocity}}";
        ViewPage viewPage = setup.createPage("LaTeX", "WebHome", content, "Sample Page for LaTeX export");

        // Modal created before the opening to avoid fade effect. (see BaseModal)
        ExportModal exportModal = new ExportModal();

        viewPage.clickMoreActionsSubMenuEntry("tmExport");
        exportModal.openOtherFormatPane();
        LaTeXFormatPane laTeXFormatPane = new LaTeXFormatPane();
        assertTrue(laTeXFormatPane.isTreeAvailable());
        assertTrue(laTeXFormatPane.isExportAsLaTeXButtonAvailable());

        // Verify that the current page is selected
        TreeElement treeElement = laTeXFormatPane.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        LaTeXExportOptions leo = laTeXFormatPane.clickExportAsLaTeXButton();
        leo.clickExportButton();

        // We're now supposed to be on the progress bar template. We need to wait till we get the success message to
        // know if the export was successful. We also verify that the link is correct.
        LaTeXExportProgress exportProgress = new LaTeXExportProgress();
        WebElement successBox = exportProgress.waitAndGetSuccessBoxContent();
        assertEquals("You can now download your export.", successBox.getText());
        String expectedReportURLPrefix =
            "/xwiki/bin/latexexport/LaTeX/WebHome?action=getExport&jobId=export&jobId=latex&jobId=";
        String reportURL = successBox.findElement(By.tagName("a")).getAttribute("href");
        assertThat(reportURL, containsString(expectedReportURLPrefix));

        // Since it's difficult to click on the browser save dialog or to configure the browsers to do automatic saves
        // we take the approach of downloading the zip directly from the client side (ie from this test) and to save
        // it on the host in the target/ directory.
        // Note: we have to use localhost part since host.testcontainers.internal is only for internal calls.
        FileUtils.copyURLToFile(new URL(reportURL.replace("host.testcontainers.internal", "localhost")),
            new File("target/latex.zip"));

        // Unzip
        FileTestUtils.unzip(new File("target/latex.zip"), new File("target/latex"));

        // Convert the LaTeX results into PDF by using the Docker "blang/latex:ubuntu" image.
        // docker run --rm -i --user="$(id -u):$(id -g)" --net=none -v "$PWD":/data blang/latex:ubuntu
        try (GenericContainer container = new GenericContainer("blang/latex:ubuntu"))
        {
            // Note: we copy files instead of mounting volumes so that this can work when using DOOD
            // (Docker out of Docker).
            MountableFile mountableDirectory = MountableFile.forHostPath("target/latex");
            container.withCopyFileToContainer(mountableDirectory, "/data");
            container.withCommand("/bin/sh", "-c", "sleep infinity");
            container.withLogConsumer(new Slf4jLogConsumer(LOGGER));
            container.start();
            Container.ExecResult result = container.execInContainer("pdflatex", "-shell-escape", "index.tex");
            String stdout = result.getStdout();
            LOGGER.info(stdout);
            assertTrue(stdout.contains("Output written on index.pdf"));
            container.copyFileFromContainer("/data/index.pdf", "target/latex/index.pdf");
        }

        // Assert the generated PDF
        assertTrue(getPDFContent(new File("target/latex/index.pdf")).contains("Hello world latexexport"));
    }

    @Test
    @Order(2)
    void exportToPDF(TestUtils setup) throws Exception
    {
        ViewPage viewPage = setup.gotoPage("LaTeX", "WebHome");

        // Modal created before the opening to avoid fade effect. (see BaseModal)
        ExportModal exportModal = new ExportModal();

        viewPage.clickMoreActionsSubMenuEntry("tmExport");
        exportModal.openOtherFormatPane();
        LaTeXFormatPane laTeXFormatPane = new LaTeXFormatPane();
        assertTrue(laTeXFormatPane.isTreeAvailable());
        assertTrue(laTeXFormatPane.isExportAsPDFButtonAvailable());

        // Verify that the current page is selected
        TreeElement treeElement = laTeXFormatPane.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        LaTeXExportOptions leo = laTeXFormatPane.clickExportAsPDFButton();
        leo.clickExportButton();

        // We're now supposed to be on the progress bar template. We need to wait till we get the success message to
        // know if the export was successful. We also verify that the link is correct.
        LaTeXExportProgress exportProgress = new LaTeXExportProgress();
        WebElement successBox = exportProgress.waitAndGetSuccessBoxContent();
        assertEquals("You can now download your export.", successBox.getText());
        String expectedReportURLPrefix =
            "/xwiki/bin/latexexport/LaTeX/WebHome?action=getExport&jobId=export&jobId=latex&jobId=";
        String reportURL = successBox.findElement(By.tagName("a")).getAttribute("href");
        assertThat(reportURL, containsString(expectedReportURLPrefix));

        // Since it's difficult to click on the browser save dialog or to configure the browsers to do automatic saves
        // we take the approach of downloading the PDF directly from the client side (ie from this test) and to save
        // it on the host in the target/ directory.
        FileUtils.copyURLToFile(new URL(reportURL.replace("host.testcontainers.internal", "localhost")),
            new File("target/latex.pdf"));

        // Assert the generated PDF
        assertTrue(getPDFContent(new File("target/latex.pdf")).contains("Hello world latexexport"));
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
}
