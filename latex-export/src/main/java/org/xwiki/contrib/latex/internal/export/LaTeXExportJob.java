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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.classloader.xwiki.internal.ContextNamespaceURLClassLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Job to perform the LaTeX exports and be able to display a progress bar.
 *
 * @version $Id$
 * @since 1.12
 */
@Component
@Named("LaTeXExportJob")
public class LaTeXExportJob extends AbstractJob<LaTeXExportJobRequest, LaTeXExportJobStatus>
{
    static final String JOB_TYPE = "LaTeXExportJob";

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Execution execution;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ClassLoaderManager classLoaderManager;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        // Overwrite the Thread Context CL to work around the https://jira.xwiki.org/browse/XCOMMONS-2064 bug.
        // Remove this hack once it's fixed and the LaTeX extension starts depending on XWiki >= the version where
        // it's fixed.
        Thread.currentThread().setContextClassLoader(
            new ContextNamespaceURLClassLoader(this.wikiDescriptorManager, this.classLoaderManager));

        // TODO: Remove once https://jira.xwiki.org/browse/XCOMMONS-2069 is implemented and the LaTeX extension starts
        // depending on XWiki >= the version where it's fixed.
        ExecutionContext econtext = this.execution.getContext();
        XWikiContext xcontext = (XWikiContext) econtext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        xcontext.setAction(LaTeXExportResourceReferenceHandler.ACTION_STRING);

        File result = getExporter().export(this.request.getReference(), this.request.getExportOptions());

        // Set the result file in the Job status so that it can be accessed from the export.vm template.
        getStatus().setResultFile(result);
    }

    @Override
    protected LaTeXExportJobStatus createNewStatus(LaTeXExportJobRequest request)
    {
        return new LaTeXExportJobStatus(this.getType(), request, getCurrentJobStatus(), this.observationManager,
            this.loggerManager);
    }

    private JobStatus getCurrentJobStatus()
    {
        Job currentJob = this.jobContext.getCurrentJob();
        JobStatus currentJobStatus = currentJob != null ? currentJob.getStatus() : null;
        return currentJobStatus;
    }

    private LaTeXExporter getExporter()
    {
        LaTeXExporter exporter;
        try {
            if (this.request.isPDF()) {
                exporter = this.componentManager.getInstance(LaTeXExporter.class, "pdf");
            } else {
                exporter = this.componentManager.getInstance(LaTeXExporter.class);
            }
        } catch (Exception e) {
            // This should not happen since we bundle these 2 components in this module!
            throw new RuntimeException(String.format("Failed to get [%s] component instance for exporting to LaTeX",
                LaTeXExporter.class), e);
        }
        return exporter;
    }
}
