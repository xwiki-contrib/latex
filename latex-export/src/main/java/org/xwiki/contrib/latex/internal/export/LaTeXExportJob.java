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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;

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

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
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
