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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Entry point to export a document as a LaTeX package.
 *
 * @version $Id: 2e98f0b413b4ae324afc083bad5ff79cc810d83e $
 */
@Component
@Named(LaTeXExportResourceReferenceHandler.ACTION_STRING)
@Singleton
public class LaTeXExportResourceReferenceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    /**
     * The LaTeX export Action as {@link String}.
     */
    public static final String ACTION_STRING = "latexexport";

    /**
     * The LaTeX export Action.
     */
    public static final EntityResourceAction ACTION = new EntityResourceAction(ACTION_STRING);

    @Inject
    private LaTeXExporter defaultExporter;

    @Inject
    @Named("pdf")
    private LaTeXExporter pdfExporter;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private LaTeXTemplate template;

    @Inject
    private Container container;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Collections.singletonList(ACTION);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        // Make sure the current user is allowed to export the document
        try {
            this.authorization.checkAccess(Right.VIEW);
        } catch (AccessDeniedException e) {
            throw new ResourceReferenceHandlerException("Not allowed to export this document", e);
        }

        Request request = this.container.getRequest();

        EntityResourceReference documentResourceReference = (EntityResourceReference) reference;
        DocumentReference documentReference = new DocumentReference(documentResourceReference.getEntityReference());

        if (request.getProperty("confirm") != null) {
            // Export the document
            try {
                if (isPDF()) {
                    this.pdfExporter.export(documentReference);
                } else {
                    this.defaultExporter.export(documentReference);
                }
            } catch (Exception e) {
                throw new ResourceReferenceHandlerException(
                    String.format("Failed to export document [%s] to LaTeX", documentReference), e);
            }
        } else {
            // Display the export option
            try {
                this.template.render(documentReference);
            } catch (Exception e) {
                throw new ResourceReferenceHandlerException("Failed to render the LaTeX export properties UI", e);
            }
        }
    }

    private boolean isPDF()
    {
        String pdfQueryStringValue = this.xcontextProvider.get().getRequest().getParameter("pdf");
        return pdfQueryStringValue == null ? false : Boolean.valueOf(pdfQueryStringValue);
    }
}
