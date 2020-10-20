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
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobStatusStore;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
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
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

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

    private static final String JOB_ID_QUERY_STRING_KEY = "jobId";

    /**
     * The LaTeX export Action.
     */
    public static final EntityResourceAction ACTION = new EntityResourceAction(ACTION_STRING);

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private LaTeXTemplate template;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private LaTeXExportJobExecutor jobExecutor;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private LaTeXPropertiesExtractor propertiesExtractor;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Inject
    private JobStatusStore jobStatusStore;

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

        XWikiRequest request = this.xcontextProvider.get().getRequest();

        EntityResourceReference documentResourceReference = (EntityResourceReference) reference;
        DocumentReference documentReference = new DocumentReference(documentResourceReference.getEntityReference());

        // This handle method is called several times:
        // 1) A first time by the LaTeX UIX used for the export modal buttons. When this happens we need to render
        //    the export.vm to display the export configuration UI.
        // 2) A second time when the user clicks the "Export" button on the export configuration UI. When this happens
        //    the "confirm" query string parameter is set (it's set inside the export.vm template, by the FORM).
        //    When this happens we trigger the Export Job and send a redirect in the response with the job id. This is
        //    because we need the "jobId" in the export.vm to display the progress bar.
        // 3) A third time with the "jobId" query string param (this is the redirect from the previous step). When this
        //    happens we need to render the export.vm again so that it displays the progress bar and regularly calls
        //    XWiki's server side from javascript to update the status (by using the "jobId").
        // 4) A fourth time with the "jobId" query string param and the "getExport" query string param to get the
        //    exported file.

        if (request.getParameter("confirm") != null && request.getParameter(JOB_ID_QUERY_STRING_KEY) == null) {
            // We're in case 2)
            // Start the job and don't wait.
            List<String> jobId = this.jobExecutor.execute(documentReference, isPDF(), getExportOptions(request));
            // Redirect and pass the jobId query string parameter this time.
            reference.addParameter(JOB_ID_QUERY_STRING_KEY, jobId);
            redirect(reference);
        } else if (isGetExportAction(request)) {
            // We're in case 4)
            sendExportFile(documentReference, request);
        } else {
            // We're in cases 1) and 3)
            renderTemplate(documentReference);
        }
    }

    private boolean isGetExportAction(XWikiRequest request)
    {
        String actionValue = request.getParameter("action");
        return actionValue != null && actionValue.equals("getExport");
    }

    private void renderTemplate(DocumentReference documentReference) throws ResourceReferenceHandlerException
    {
        try {
            this.template.render(documentReference);
        } catch (Exception e) {
            // Be a good citizen
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResourceReferenceHandlerException("Failed to render the LaTeX export properties UI", e);
        }
    }

    private void sendExportFile(DocumentReference documentReference, XWikiRequest request)
        throws ResourceReferenceHandlerException
    {
        // Stream the file found in the job result to the servlet output stream
        LaTeXExportJobStatus jobStatus =
            (LaTeXExportJobStatus) this.jobStatusStore.getJobStatus(LaTeXExportUtils.getJobIdFromRequest(request));
        XWikiResponse response = this.xcontextProvider.get().getResponse();
        boolean isPDF = jobStatus.getResultFile().getName().endsWith(".pdf");
        response.setContentType(isPDF ? "application/pdf" : "application/zip");
        response.setHeader("Content-Disposition", getContentDisposition(documentReference, isPDF));
        LaTeXExportUtils.copyFileToResponse(jobStatus.getResultFile(), response);
    }

    private String getContentDisposition(DocumentReference documentReference, boolean isPDF)
    {
        String fileName = computeFileNamePrefix(documentReference);
        return isPDF ? String.format("inline; filename=%s.pdf", fileName)
            : String.format("attachment; filename=%s.zip", fileName);
    }

    private Map<String, Object> getExportOptions(XWikiRequest request) throws ResourceReferenceHandlerException
    {
        try {
            return this.propertiesExtractor.extract(request);
        } catch (ParseException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to extract export options from request [%s]", request), e);
        }
    }

    private void redirect(ResourceReference reference) throws ResourceReferenceHandlerException
    {
        String url = getRedirectURL(reference);
        try {
            this.xcontextProvider.get().getResponse().sendRedirect(url);
        } catch (IOException e) {
            throw new ResourceReferenceHandlerException(String.format("Failed to redirect to [%s]",
                url), e);
        }
    }

    private String getRedirectURL(ResourceReference reference)
    {
        // TODO: When a ResourceReferenceSerializer<EntityResourceReference, ExtendedURL> component is implemented
        // move to using it. In the meantime use the "old" way.
        EntityResourceReference err = (EntityResourceReference) reference;
        String queryString = LaTeXExportUtils.extractQueryString(err);
        return this.dab.getDocumentURL(err.getEntityReference(), ACTION_STRING, queryString, null);
    }

    private boolean isPDF()
    {
        String pdfQueryStringValue = this.xcontextProvider.get().getRequest().getParameter("pdf");
        return pdfQueryStringValue == null ? false : Boolean.valueOf(pdfQueryStringValue);
    }

    private String computeFileNamePrefix(DocumentReference documentReference)
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
