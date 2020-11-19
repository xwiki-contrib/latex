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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceHandlerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Executes a LaTeX Export Job.
 *
 * @version $Id$
 * @since 1.12.1
 */
@Component(roles = LaTeXExportJobExecutor.class)
@Singleton
public class LaTeXExportJobExecutor
{
    @Inject
    private ContextStoreManager contextStoreManager;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    /**
     * @param documentReference the reference to the document to export
     * @param isPDF true if the export is a PDF export or false if it's a LaTeX zip export
     * @param exportOptions the user-selected export options
     * @return the job id
     * @throws ResourceReferenceHandlerException in case of error during the export
     */
    public List<String> execute(DocumentReference documentReference, boolean isPDF,
        Map<String, Object> exportOptions) throws ResourceReferenceHandlerException
    {
        LaTeXExportJobRequest jobRequest;
        try {
            jobRequest = createRequest(documentReference, isPDF, exportOptions);
            // Start the export but don't wait since we want to display a progress bar.
            this.jobExecutor.execute(LaTeXExportJob.JOB_TYPE, jobRequest);
        } catch (JobException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to export document [%s] to LaTeX", documentReference), e);
        }
        return jobRequest.getId();
    }

    private LaTeXExportJobRequest createRequest(DocumentReference documentReference, boolean isPDF,
        Map<String, Object> exportOptions) throws ResourceReferenceHandlerException
    {
        LaTeXExportJobRequest jobRequest;
        try {
            // Preserve all request data in the new xwiki context for the job, so that scripts in the exported page
            // can use them.
            // TODO: Remove "request.parameters" once https://jira.xwiki.org/browse/XWIKI-18082 is fixed and this
            //  extension depends on the version where it's fixed.
            Map<String, Serializable> contextEntries = this.contextStoreManager.save(
                Arrays.asList("request.*", "request.parameters"));
            jobRequest = new LaTeXExportJobRequest(documentReference, isPDF, exportOptions, contextEntries);
            List<String> jobId = LaTeXExportUtils.generateJobId();
            jobRequest.setId(jobId);
            jobRequest.setStatusLogIsolated(true);
        } catch (ComponentLookupException e) {
            throw new ResourceReferenceHandlerException("Failed to create LaTeX Job Request", e);
        }
        return jobRequest;
    }
}
