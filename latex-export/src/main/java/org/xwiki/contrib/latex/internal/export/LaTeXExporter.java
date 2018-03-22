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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilter;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilterStreamFactory;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
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

/**
 * Export a document in LaTeX.
 * 
 * @version $Id$
 */
@Component(roles = LaTeXExporter.class)
@Singleton
public class LaTeXExporter
{
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

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".zip");

        Map<String, Object> properties = this.propertiesGenerator.extract(xcontext.getRequest());
        properties.put("target", new DefaultOutputStreamOutputTarget(response.getOutputStream(), true));

        // Export
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
                filter.beginWikiSpace(spaceElement.getName(), FilterEventParameters.EMPTY);
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
