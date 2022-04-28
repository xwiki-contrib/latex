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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFException;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFResult;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;

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
    private Provider<LaTeX2PDFConverter> converterProvider;

    @Override
    protected File performExport(DocumentReference documentReference, XDOM xdom,
        Map<String, Object> exportOptions, XWikiContext xcontext) throws Exception
    {
        return this.progressManager.call(
            () -> performExportInternal(documentReference, xdom, exportOptions), 3, this);
    }

    private File performExportInternal(DocumentReference documentReference, XDOM xdom,
        Map<String, Object> exportOptions) throws Exception
    {
        // Step 1: Generate the latex zip
        File outputDir = generateTemporaryDirectory();
        File latexZip = new File(outputDir, ZIPFILENAME);
        try (FileOutputStream fos = new FileOutputStream(latexZip)) {
            exportOptions.put(TARGET_PROPERTY, new DefaultOutputStreamOutputTarget(fos, true));
            performExport(documentReference, xdom, exportOptions);
        }
        // Step 2: Unzip latex zip
        this.progressManager.startStep(this, "Unzip the LaTex zip");
        File unzippedLaTeXDirectory = new File(outputDir, "files");
        unzippedLaTeXDirectory.mkdirs();
        unzip(latexZip, unzippedLaTeXDirectory);
        this.progressManager.endStep(this);
        // Step 3: Convert from latex to pdf
        this.progressManager.startStep(this, "Convert LaTeX to PDF");
        LaTeX2PDFResult result = this.converterProvider.get().convert(unzippedLaTeXDirectory);
        this.progressManager.endStep(this);
        // Step 4: Read the generated PDF and stream it back to the response output stream
        this.progressManager.startStep(this, "Copy PDF data to the output");
        if (result.getPDFFile() == null) {
            String message = String.format("Error when generating the PDF file in [%s].", unzippedLaTeXDirectory);
            if (result.getLogs() != null) {
                message = String.format("%s Compilation logs: [\n%s\n]", result.getLogs());
            }
            throw new LaTeX2PDFException(message);
        }
        this.progressManager.endStep(this);
        return result.getPDFFile();
    }
}
