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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
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
    private static final String FILTERPROPERTY_PREFIX = "property_";

    private static final String DATE_PROPERTY = "date";

    private final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    private DocumentDisplayer documentDisplayer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
    private OutputFilterStreamFactory factory;

    /**
     * Export passed document.
     * 
     * @param documentReference the document reference
     * @throws Exception when failing to export
     */
    public void export(DocumentReference documentReference) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        String name = documentReference.getName();
        if (name.equals("WebHome")) {
            name = documentReference.getName();
        }

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

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".zip");

        Map<String, Object> properties = createProperties(xcontext, response);

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

    private Map<String, Object> createProperties(XWikiContext xcontext, XWikiResponse response) throws IOException
    {
        XWikiRequest request = xcontext.getRequest();

        Map<String, Object> properties = new HashMap<>();
        properties.put("target", new DefaultOutputStreamOutputTarget(response.getOutputStream(), true));
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getKey().startsWith(FILTERPROPERTY_PREFIX) && entry.getValue() != null
                && entry.getValue().length > 0) {
                String propertyName = entry.getKey().substring(FILTERPROPERTY_PREFIX.length());
                if (propertyName.equals(DATE_PROPERTY)) {
                    String dateString = entry.getValue()[0];
                    if (!dateString.isEmpty()) {
                        try {
                            properties.put(DATE_PROPERTY, this.dateformat.parse(dateString));
                        } catch (ParseException e) {
                            // TODO: Should report something but not very nice to pollute the system log with that
                        }
                    }
                } else if (entry.getValue().length == 1) {
                    properties.put(propertyName, entry.getValue()[0]);
                } else {
                    properties.put(propertyName, entry.getValue());
                }
            }
        }

        return properties;
    }
}
