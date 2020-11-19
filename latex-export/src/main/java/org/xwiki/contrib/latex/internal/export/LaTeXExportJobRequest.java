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
import java.util.Map;

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;

/**
 * Holds request data for the Export job.
 *
 * @version $Id$
 */
public class LaTeXExportJobRequest extends AbstractRequest
{
    private static final String IS_PDF = "isPDF";

    private static final String REFERENCE = "reference";

    private static final String EXPORT_OPTIONS = "exportOptions";

    /**
     * @param reference see {@link #getReference()}
     * @param isPDF see {@link #isPDF()}
     * @param exportOptions see {@link #getExportOptions()}
     * @param contextEntries the list of special entries that need to be in the job context, see
     * <a href=
     * "https://extensions.xwiki.org/xwiki/bin/view/Extension/Rendering%20Module/Async/#HStandardcontextentries">
     * standard context entries
     * </a>
     */
    public LaTeXExportJobRequest(DocumentReference reference, boolean isPDF, Map<String, Object> exportOptions,
        Map<String, Serializable> contextEntries)
    {
        setReference(reference);
        setPDF(isPDF);
        setExportOptions(exportOptions);
        setContext(contextEntries);
    }

    /**
     * @param isPDF see {@link #isPDF()}
     */
    public void setPDF(boolean isPDF)
    {
        setProperty(IS_PDF, isPDF);
    }

    /**
     * @return true if the export should export to PDF and false if it should generate a LaTeX zip
     */
    public boolean isPDF()
    {
        return getProperty(IS_PDF);
    }

    /**
     * @param reference see {@link #getReference()}
     */
    public void setReference(DocumentReference reference)
    {
        setProperty(REFERENCE, reference);
    }

    /**
     * @return the reference of the document to export
     */
    public DocumentReference getReference()
    {
        return getProperty(REFERENCE);
    }

    /**
     * @param exportOptions see {@link #getExportOptions()}
     */
    public void setExportOptions(Map<String, Object> exportOptions)
    {
        setProperty(EXPORT_OPTIONS, exportOptions);
    }

    /**
     * @return the export options defined by the user
     */
    public Map<String, Object> getExportOptions()
    {
        return getProperty(EXPORT_OPTIONS);
    }
}
