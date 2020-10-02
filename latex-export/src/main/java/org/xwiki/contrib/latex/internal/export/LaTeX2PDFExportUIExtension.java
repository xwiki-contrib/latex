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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;

/**
 * Inject a button in the standard export menu UI to export to PDF (i.e. convert the LaTeX results to PDF
 * automatically), but only if Docker is available.
 * 
 * @version $Id$
 * @since 1.10
 */
@Component
@Singleton
@Named(LaTeX2PDFExportUIExtension.ID)
public class LaTeX2PDFExportUIExtension extends AbstractLaTeXExportUIExtension
{
    /**
     * The ID of this UI extension.
     */
    public static final String ID = "latex2pdfexport";

    @Inject
    private LaTeX2PDFConverter laTeX2PDFConverter;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    protected String getQueryString()
    {
        return "pdf=true";
    }

    @Override
    protected String getButtonLabelTranslationKey()
    {
        return "latex.exportToPDF.button.label";
    }

    @Override
    public Block execute()
    {
        Block result;
        if (this.laTeX2PDFConverter.isReady()) {
            result = super.execute();
        } else {
            // If the converter is not ready, don't display the export to PDF button!
            result = new CompositeBlock();
        }
        return result;
    }
}
