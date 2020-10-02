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
package org.xwiki.contrib.latex.internal.export;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFException;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFResult;
import org.xwiki.environment.Environment;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiResponse;

import static org.xwiki.contrib.latex.internal.export.Unzipper.unzip;

/**
 * Export a document in LaTeX.
 * 
 * @version $Id$
 */
@Component
@Named("pdf")
@Singleton
public class PDFLaTeXExporter extends AbstractLaTeXExporter
{
    @Inject
    private LaTeX2PDFConverter laTeX2PDFConverter;

    @Inject
    private Environment environment;

    @Override
    protected String getOutputFileName(String outputFileNamePrefix, XWikiContext xcontext)
    {
        return String.format("%s.pdf", outputFileNamePrefix);
    }

    @Override
    protected void setResponseContentType(String outputFileName, XWikiResponse response, XWikiContext xcontext)
    {
        String contentDisposition;
        response.setContentType("application/pdf");
        // Display inline
        contentDisposition = String.format("inline; filename=%s", outputFileName);
        response.setHeader("Content-Disposition", contentDisposition);
    }

    @Override
    protected void performExport(String name, DocumentReference documentReference, XDOM xdom,
        Map<String, Object> properties, XWikiResponse response, XWikiContext xcontext) throws Exception
    {
        // Step 1: Generate the latex zip
        File outputDir = generateTemporaryDirectory();
        File latexZip = new File(outputDir, getLaTeXZipFileName(name));
        try (FileOutputStream fos = new FileOutputStream(latexZip)) {
            properties.put(TARGET_PROPERTY, new DefaultOutputStreamOutputTarget(fos, true));
            performExport(documentReference, xdom, properties);
        }
        // Step 2: Unzip latex zip
        File unzippedLaTeXDirectory = new File(outputDir, "latex");
        unzippedLaTeXDirectory.mkdirs();
        unzip(latexZip, unzippedLaTeXDirectory);
        // Step 3: Convert from latex to pdf
        LaTeX2PDFResult result = this.laTeX2PDFConverter.convert(unzippedLaTeXDirectory);
        // Step 4: Read the generated PDF and stream it back to the response output stream
        if (result.getPDFFile().exists()) {
            FileUtils.copyFile(result.getPDFFile(), response.getOutputStream());
            response.getOutputStream().close();
            // Delete the temporary directory to not use too much space on disk, unless we're in debug mode.
            if (xcontext.getRequest().getParameter("debug") != null) {
                FileUtils.deleteDirectory(outputDir);
            }
        } else {
            throw new LaTeX2PDFException(String.format("Error when generating the PDF file in [%s–. Compilation "
                + "logs: [%s]", unzippedLaTeXDirectory, result.getLogs()));
        }
    }

    /**
     * @return the unique (thread-safe) temporary directory to be used to put the LaTeX files in
     */
    private File generateTemporaryDirectory()
    {
        File latex2pdfDir = new File(this.environment.getTemporaryDirectory(), "latex2pdf");
        File uniqueTmpDir = new File(latex2pdfDir, UUID.randomUUID().toString());
        uniqueTmpDir.mkdirs();
        return uniqueTmpDir;
    }
}
