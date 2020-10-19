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

import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * Job status holding the exported file result so that the caller of the job can use it to send it back to the browser
 * for example.
 *
 * @version $Id$
 * @since 1.12
 */
public class LaTeXExportJobStatus extends DefaultJobStatus<LaTeXExportJobRequest>
{
    private File resultFile;

    LaTeXExportJobStatus(String jobType, LaTeXExportJobRequest request, JobStatus parentJobStatus,
        ObservationManager observationManager, LoggerManager loggerManager)
    {
        super(jobType, request, parentJobStatus, observationManager, loggerManager);
    }

    /**
     * @param resultFile see {@link #getResultFile()}
     */
    public void setResultFile(File resultFile)
    {
        this.resultFile = resultFile;
    }

    /**
     * @return the exported file
     */
    public File getResultFile()
    {
        return this.resultFile;
    }
}
