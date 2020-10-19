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
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FileUtils;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.velocity.tools.EscapeTool;

import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * Various utility methods used for the LaTeX export.
 *
 * @version $Id$
 * @since 1.12
 */
public final class LaTeXExportUtils
{
    private static final String JOB_ID_QUERY_STRING_KEY = "jobId";

    private static final EscapeTool ESCAPE_TOOL = new EscapeTool();

    private LaTeXExportUtils()
    {
        // To prevent instantiation since it's a utility class.
    }

    /**
     * @return the unique job id
     */
    static List<String> generateJobId()
    {
        String suffix = new Date().getTime() + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        return Arrays.asList("export", "latex", suffix);
    }

    /**
     * @param request the servlet request
     * @return the job id extracted from query string parameters
     */
    static List<String> getJobIdFromRequest(XWikiRequest request)
    {
        return Arrays.asList(request.getParameterValues(JOB_ID_QUERY_STRING_KEY));
    }

    static String computeQueryString(EntityResourceReference reference)
    {
        return ESCAPE_TOOL.url(reference.getParameters());
    }

    static void copyFileToResponse(File resultFile, XWikiResponse response) throws ResourceReferenceHandlerException
    {
        try {
            FileUtils.copyFile(resultFile, response.getOutputStream());
        } catch (IOException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to send the exported file [%s]", resultFile), e);
        }
    }

    static List<String> executeJob(DocumentReference documentReference, boolean isPDF,
        Map<String, Object> exportOptions, JobExecutor jobExecutor) throws ResourceReferenceHandlerException
    {
        // Export the document by starting the LaTeX export job
        LaTeXExportJobRequest jobRequest =
            LaTeXExportJobRequest.createJobRequest(documentReference, isPDF, exportOptions);
        try {
            // Start the export but don't wait since we want to display a progress bar.
            jobExecutor.execute(LaTeXExportJob.JOB_TYPE, jobRequest);
        } catch (JobException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to export document [%s] to LaTeX", documentReference), e);
        }
        return jobRequest.getId();
    }
}
