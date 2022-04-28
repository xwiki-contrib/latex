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
package org.xwiki.contrib.latex.pdf;

import java.io.File;

import org.xwiki.stability.Unstable;

/**
 * Results of the PDF compilation.
 *
 * @version $Id$
 * @since 1.10.1
 */
@Unstable
public class LaTeX2PDFResult
{
    private File pdfFile;

    private String logs;

    /**
     * @param pdfFile see {@link #getPDFFile()}
     * @param logs see {@link #getLogs()}
     */
    public LaTeX2PDFResult(File pdfFile, String logs)
    {
        this(logs);
        this.pdfFile = pdfFile;
    }

    /**
     * @param logs see {@link #getLogs()}
     */
    public LaTeX2PDFResult(String logs)
    {
        this.logs = logs;
    }

    /**
     * @return the location of the generated PDF file
     */
    public File getPDFFile()
    {
        return this.pdfFile;
    }

    /**
     * @return the compilation logs
     */
    public String getLogs()
    {
        return this.logs;
    }
}
