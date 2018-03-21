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
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilter;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilterStreamFactory;
import org.xwiki.contrib.latex.output.LaTeXOutputProperties;
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
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyDescriptor;
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

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    private DocumentDisplayer documentDisplayer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
    private OutputFilterStreamFactory factory;

    @Inject
    private BeanManager beans;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

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

    private boolean isIterable(PropertyDescriptor propertyDescriptor)
    {
        Type type = propertyDescriptor.getPropertyType();

        if (TypeUtils.isArrayType(type)) {
            return true;
        }

        return TypeUtils.isAssignable(type, Iterable.class);
    }

    private Map<String, Object> extractParameters(XWikiRequest request, Map<String, Object> properties)
        throws ParseException
    {
        BeanDescriptor descriptor = this.beans.getBeanDescriptor(LaTeXOutputProperties.class);

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getKey().startsWith(FILTERPROPERTY_PREFIX) && entry.getValue() != null
                && entry.getValue().length > 0) {
                String parameterKey = entry.getKey().substring(FILTERPROPERTY_PREFIX.length());
                addValue(descriptor, parameterKey, properties, entry.getValue());
            }
        }

        return properties;
    }

    private void addValue(BeanDescriptor descriptor, String parameterKey, Map<String, Object> properties,
        String[] value) throws ParseException
    {
        PropertyDescriptor propertyDescriptor = descriptor.getProperty(parameterKey);
        if (propertyDescriptor != null) {
            if (TypeUtils.isAssignable(propertyDescriptor.getPropertyType(), Date.class)) {
                String dateString = value[0];
                if (!dateString.isEmpty()) {
                    properties.put(parameterKey, DATE_FORMAT.parse(dateString));
                }
            } else if (isIterable(propertyDescriptor)) {
                properties.put(parameterKey, value);
            } else {
                properties.put(parameterKey, value[0]);
            }
        }
    }

    private Map<String, Object> createProperties(XWikiContext xcontext, XWikiResponse response)
        throws IOException, ParseException
    {
        XWikiRequest request = xcontext.getRequest();

        Map<String, Object> properties = new HashMap<>();

        properties.put("target", new DefaultOutputStreamOutputTarget(response.getOutputStream(), true));

        return extractParameters(request, properties);
    }
}
