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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilter;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilterStreamFactory;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiResponse;

import static org.xwiki.contrib.latex.internal.export.Unzipper.unzip;

/**
 * Export a document in LaTeX.
 * 
 * @version $Id$
 */
@Component(roles = LaTeXExporter.class)
@Singleton
public class LaTeXExporter
{
    private static final String TARGET_PROPERTY = "target";

    @Inject
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
    private OutputFilterStreamFactory factory;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Inject
    private LaTeXPropertiesExtractor propertiesGenerator;

    @Inject
    private Environment environment;

    @Inject
    private LaTeX2PDFConverter laTeX2PDFConverter;

    /**
     * Export passed document.
     * 
     * @param documentReference the document reference
     * @throws Exception when failing to export
     */
    public void export(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Get document
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        xcontext.setDoc(document);

        // Display document
        DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
        displayParameters.setExecutionContextIsolated(true);
        displayParameters.setTransformationContextIsolated(true);
        displayParameters.setContentTranslated(true);
        displayParameters.setTargetSyntax(Syntax.PLAIN_1_0);

        XDOM xdom = this.documentDisplayer.display(document, displayParameters);

        XWikiResponse response = xcontext.getResponse();

        String name = computeExportFileName(documentReference);

        Map<String, Object> properties = this.propertiesGenerator.extract(xcontext.getRequest());

        String outputFileName = getOutputFileName(name, xcontext);
        setResponseContentType(outputFileName, response, xcontext);

        // If a PDF is supposed to be generated then generate the latex zip in the perm dir, perform conversion from
        // latex to pdf and stream back the pdf into the output.
        boolean isPDF = isPDF(xcontext);
        if (isPDF) {
            // Step 1: Generate the latex zip
            File outputDir = new File(this.environment.getPermanentDirectory(), "latex2pdf");
            outputDir.mkdirs();
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
            File pdfFile = this.laTeX2PDFConverter.convert(unzippedLaTeXDirectory);
            // Step 4: Read the generated PDF and stream it back to the response output stream
            FileUtils.copyFile(pdfFile, response.getOutputStream());
            response.getOutputStream().close();
        } else {
            properties.put(TARGET_PROPERTY, new DefaultOutputStreamOutputTarget(response.getOutputStream(), true));
            performExport(documentReference, xdom, properties);
        }
    }

    private String getLaTeXZipFileName(String outputFileNamePrefix)
    {
        return String.format("%s.zip", outputFileNamePrefix);
    }

    private String getOutputFileName(String outputFileNamePrefix, XWikiContext xcontext)
    {
        String outputFileName;
        if (isPDF(xcontext)) {
            outputFileName = String.format("%s.pdf", outputFileNamePrefix);
        } else {
            outputFileName = getLaTeXZipFileName(outputFileNamePrefix);
        }
        return outputFileName;
    }

    private void setResponseContentType(String outputFileName, XWikiResponse response, XWikiContext xcontext)
    {
        String contentDisposition;
        if (isPDF(xcontext)) {
            response.setContentType("application/pdf");
            // Display inline
            contentDisposition = String.format("inline; filename=%s", outputFileName);
        } else {
            response.setContentType("application/zip");
            // Force download since it's a zip (the browser cannot display it inline)
            contentDisposition = String.format("attachment; filename=%s", outputFileName);
        }
        response.setHeader("Content-Disposition", contentDisposition);
    }

    private boolean isPDF(XWikiContext xcontext)
    {
        String pdfQueryStringValue = xcontext.getRequest().getParameter("pdf");
        return pdfQueryStringValue == null ? false : Boolean.valueOf(pdfQueryStringValue);
    }

    private void performExport(DocumentReference documentReference, XDOM xdom, Map<String, Object> properties)
        throws Exception
    {
        try (OutputFilterStream streamFilter = this.factory.createOutputFilterStream(properties)) {
            LaTeXOutputFilter filter = (LaTeXOutputFilter) streamFilter.getFilter();

            List<SpaceReference> spaces = documentReference.getSpaceReferences();

            for (SpaceReference spaceElement : spaces) {
                filter.beginWikiSpace(spaceElement.getName(), FilterEventParameters.EMPTY);
            }
            filter.beginWikiDocument(documentReference.getName(), FilterEventParameters.EMPTY);
            xdom.traverse((Listener) filter);
            filter.endWikiDocument(documentReference.getName(), FilterEventParameters.EMPTY);
            for (SpaceReference spaceElement : spaces) {
                filter.endWikiSpace(spaceElement.getName(), FilterEventParameters.EMPTY);
            }
        }
    }

    private String computeExportFileName(DocumentReference documentReference)
    {
        String name = documentReference.getName();
        if (this.entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName().equals(name)
            && documentReference.getParent() != null)
        {
            name = documentReference.getParent().getName();
        }
        return name;
    }
}
