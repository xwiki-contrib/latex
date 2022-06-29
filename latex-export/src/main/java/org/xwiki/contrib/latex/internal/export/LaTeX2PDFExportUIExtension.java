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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
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
public class LaTeX2PDFExportUIExtension extends AbstractLaTeXExportUIExtension implements Initializable
{
    /**
     * The ID of this UI extension.
     */
    public static final String ID = "latex2pdfexport";

    /**
     * Cache the status of whether docker is available or not for performance reasons. Indeed this UIExtension is
     * called at each page load and needs to be ultra fast. This means that if docker is not available when XWiki is
     * stared and then made available later on, XWiki will need to be restarted to be able to use it.
     */
    private boolean isDockerAvailable;

    @Inject
    private Provider<LaTeX2PDFConverter> converterProvider;

    @Override
    public void initialize()
    {
        this.isDockerAvailable = this.converterProvider.get().isReady();
    }

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
        if (this.isDockerAvailable) {
            result = super.execute();
        } else {
            // If the converter is not ready, don't display the export to PDF button!
            result = new CompositeBlock();
        }
        return result;
    }
}
