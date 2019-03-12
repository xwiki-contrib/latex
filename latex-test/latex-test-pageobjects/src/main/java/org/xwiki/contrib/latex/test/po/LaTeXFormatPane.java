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
package org.xwiki.contrib.latex.test.po;

import org.xwiki.flamingo.skin.test.po.OtherFormatPane;

/**
 * Represents the opened panel "Other Formats" in the modal export.
 *
 * @version $Id: 1c52a2a1065f442bd502c9baa4f0479122875a02 $
 */
public class LaTeXFormatPane extends OtherFormatPane
{
    private static final String LATEX_EXPORT_LINK_TEXT = "Export as LaTeX";

    /**
     * @return true if the export LaTeX button exists.
     */
    public boolean isExportAsLaTeXButtonAvailable()
    {
        return isExportButtonAvailable(LATEX_EXPORT_LINK_TEXT);
    }

    /**
     * @param buttonText the text of the button to assert
     * @return true if the export button exists.
     * @todo remove once we upgrade to XWiki 11.2RC1 since it's in there already
     */
    public boolean isExportButtonAvailable(String buttonText)
    {
        return getDriver().findElementByLinkText(buttonText) != null;
    }

    /**
     * Click on an export button.
     *
     * @param buttonText the text of the button to click
     * @todo remove once we upgrade to XWiki 11.2RC1 since it's in there already
     */
    public void clickExportButton(String buttonText)
    {
        getDriver().findElementByLinkText(buttonText).click();
    }

    /**
     * Click on the export LaTeX button.
     */
    public LaTeXExportOptions clickExportAsLaTeXButton()
    {
        clickExportButton(LATEX_EXPORT_LINK_TEXT);
        return new LaTeXExportOptions();
    }
}
