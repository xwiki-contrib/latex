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
package org.xwiki.contrib.latex.export;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Configuration options for exporting to LaTeX and PDF.
 *
 * @version $Id$
 * @since 1.10
 */
@Role
@Unstable
public interface LaTeXExportConfiguration
{
    /**
     * @return the component hint for the {@link org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter} implementation to
     *      use for converting the LaTeX files to PDF. Default is {@code docker} (exporting using a docker image)
     */
    String getPDFExportHint();
}
