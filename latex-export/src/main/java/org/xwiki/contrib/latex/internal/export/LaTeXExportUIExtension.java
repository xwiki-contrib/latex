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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Inject a button in the standard export menu UI to export to LaTeX.
 * 
 * @version $Id$
 * @since 1.5
 */
@Component
@Singleton
@Named(LaTeXExportUIExtension.ID)
public class LaTeXExportUIExtension extends AbstractLaTeXExportUIExtension
{
    /**
     * The ID of this UI extension.
     */
    public static final String ID = "latexexport";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    protected String getLabelKey()
    {
        return "latex.export.format.label";
    }

    @Override
    protected String getHintKey()
    {
        return "latex.export.format.hint";
    }

    @Override
    protected String getIcon()
    {
        return "file-text";
    }

    @Override
    protected String getCategory()
    {
        return "other";
    }

    @Override
    protected String getQueryString()
    {
        return null;
    }

    @Override
    protected boolean isEnabled()
    {
        return true;
    }
}
