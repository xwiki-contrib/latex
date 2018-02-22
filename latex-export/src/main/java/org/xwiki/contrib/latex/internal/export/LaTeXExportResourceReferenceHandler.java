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
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilter;
import org.xwiki.contrib.latex.internal.output.LaTeXOutputFilterStreamFactory;
import org.xwiki.contrib.latex.output.LaTeXOutputProperties;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.output.BeanOutputFilterStream;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Entry point to export a document as a LaTeX package.
 *
 * @version $Id: 2e98f0b413b4ae324afc083bad5ff79cc810d83e $
 */
@Component
@Named("latexexport")
@Singleton
public class LaTeXExportResourceReferenceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    /**
     * The LaTeX export Action.
     */
    public static final EntityResourceAction ACTION = new EntityResourceAction("latexexport");

    @Inject
    @Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
    private OutputFilterStreamFactory factory;

    @Inject
    private Container container;

    @Inject
    private DocumentDisplayer documentDisplayer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Arrays.asList(ACTION);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        Request request = this.container.getRequest();

        if (!(request instanceof ServletRequest)) {
            throw new ResourceReferenceHandlerException("Unsupported request type [" + request.getClass() + "]");
        }

        EntityResourceReference documentReference = (EntityResourceReference) reference;

        try {
            export(new DocumentReference(documentReference.getEntityReference()));
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                "Failed to export document [" + documentReference + "] in LaTeX", e);
        }
    }

    private void export(DocumentReference documentReference) throws XWikiException, IOException, FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        String name = documentReference.getName();
        if (name.equals("WebHome")) {
            name = documentReference.getName();
        }

        // Get document
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

        // Display document
        DocumentDisplayerParameters displayParameters = new DocumentDisplayerParameters();
        displayParameters.setExecutionContextIsolated(true);
        displayParameters.setTransformationContextIsolated(true);
        displayParameters.setContentTranslated(true);
        displayParameters.setTargetSyntax(Syntax.PLAIN_1_0);

        XDOM xdom = this.documentDisplayer.display(document, displayParameters);

        LaTeXOutputProperties latexproperties = new LaTeXOutputProperties();

        XWikiResponse response = xcontext.getResponse();

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".zip");

        latexproperties.setTarget(new DefaultOutputStreamOutputTarget(response.getOutputStream(), true));

        BeanOutputFilterStream<LaTeXOutputProperties> streamFilter =
            ((BeanOutputFilterStreamFactory<LaTeXOutputProperties>) this.factory)
                .createOutputFilterStream(latexproperties);
        LaTeXOutputFilter filter = (LaTeXOutputFilter) streamFilter.getFilter();

        // Export
        try {
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
        } finally {
            // Close
            streamFilter.close();
        }
    }
}
