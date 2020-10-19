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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the LaTeX Export progress page.
 *
 * @version $Id$
 * @since 1.12
 */
public class LaTeXExportProgress extends BaseElement
{
    /**
     * Waits for the success message to appear or time out.
     *
     * @return the success message box DIV
     */
    public WebElement waitAndGetSuccessBoxContent()
    {
        By location = By.xpath("//div[@class = 'box successmessage']");
        getDriver().waitUntilElementIsVisible(location);
        return getDriver().findElement(location);
    }
}
