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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilter;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilterStreamFactory;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.environment.Environment;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Export a document in LaTeX.
 * 
 * @version $Id$
 */
public abstract class AbstractLaTeXExporter implements LaTeXExporter
{
    protected static final String TARGET_PROPERTY = "target";

    protected static final String ZIPFILENAME = "Index.zip";

    @Inject
    protected JobProgressManager progressManager;

    @Inject
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
    private OutputFilterStreamFactory factory;

    @Inject
    private Environment environment;

    @Override
    public File export(DocumentReference documentReference, Map<String, Object> exportOptions) throws Exception
    {
        // Set the Servlet URL factor that generates full external URLs since we generate standalone content
        // (LaTeX or PDF)
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiURLFactory currentURLFactory = xcontext.getURLFactory();
        try {
            xcontext.setURLFactory(new ExternalServletURLFactory(xcontext));
            return this.progressManager.call(()
                -> exportInternal(documentReference, exportOptions, xcontext), 3, this);
        } finally {
            xcontext.setURLFactory(currentURLFactory);
        }
    }

    private File exportInternal(DocumentReference documentReference, Map<String, Object> exportOptions,
        XWikiContext xcontext) throws Exception
    {
        this.progressManager.startStep(this, "Perform the export");
        File file = performExport(documentReference, exportOptions, xcontext);
        this.progressManager.endStep(this);

        return file;
    }

    protected abstract File performExport(DocumentReference documentReference,
        Map<String, Object> exportOptions, XWikiContext xcontext) throws Exception;

    protected void performExportInternal(DocumentReference documentReference, Map<String, Object> properties,
        XWikiContext xcontext) throws Exception
    {
        try (OutputFilterStream streamFilter = this.factory.createOutputFilterStream(properties)) {
            LaTeXOutputFilter filter = (LaTeXOutputFilter) streamFilter.getFilter();

            List<SpaceReference> spaces = documentReference.getSpaceReferences();

            filter.beginWiki(documentReference.getWikiReference().getName(), FilterEventParameters.EMPTY);
            for (SpaceReference spaceElement : spaces) {
                filter.beginWikiSpace(spaceElement.getName(), FilterEventParameters.EMPTY);
            }
            filter.beginWikiDocument(documentReference.getName(), FilterEventParameters.EMPTY);
            getXDOM(documentReference, xcontext).traverse((Listener) filter);
            filter.endWikiDocument(documentReference.getName(), FilterEventParameters.EMPTY);
            for (SpaceReference spaceElement : spaces) {
                filter.endWikiSpace(spaceElement.getName(), FilterEventParameters.EMPTY);
            }
            filter.endWiki(documentReference.getWikiReference().getName(), FilterEventParameters.EMPTY);
        }
    }

    private XDOM getXDOM(DocumentReference documentReference, XWikiContext xcontext) throws Exception
    {
        // Get document
        this.progressManager.startStep(this, "Get the document to export");
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        xcontext.setDoc(document);
        this.progressManager.endStep(this);

        // Display document
        this.progressManager.startStep(this, "Render the document to export");
        DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
        displayParameters.setExecutionContextIsolated(true);
        displayParameters.setTransformationContextIsolated(true);
        displayParameters.setContentTranslated(true);
        displayParameters.setTargetSyntax(Syntax.PLAIN_1_0);

        XDOM xdom = this.documentDisplayer.display(document, displayParameters);
        this.progressManager.endStep(this);

        return xdom;
    }

    /**
     * @return the unique (thread-safe) temporary directory to be used to put the exported files in
     */
    protected File generateTemporaryDirectory()
    {
        File latexDir = new File(this.environment.getTemporaryDirectory(), "latex");
        File uniqueTmpDir = new File(latexDir, UUID.randomUUID().toString());
        uniqueTmpDir.mkdirs();
        return uniqueTmpDir;
    }
}
