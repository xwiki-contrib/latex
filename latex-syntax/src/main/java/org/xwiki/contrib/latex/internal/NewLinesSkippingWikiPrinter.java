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
package org.xwiki.contrib.latex.internal;

import java.util.function.Consumer;

import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.WrappingWikiPrinter;
import org.xwiki.text.StringUtils;

/**
 * WikiPrinter that doesn't output leading new lines.
 *
 * @version $Id$
 * @since 1.15
 */
public class NewLinesSkippingWikiPrinter extends WrappingWikiPrinter
{
    private boolean hasContent;

    /**
     * @param printer the wrapped printer
     */
    public NewLinesSkippingWikiPrinter(WikiPrinter printer)
    {
        super(printer);
    }

    @Override
    public void print(String text)
    {
        internalPrint(text, s -> super.print(s));
    }

    @Override
    public void println(String text)
    {
        internalPrint(text, s -> super.println(s));
    }

    private void internalPrint(String text, Consumer<String> consumer)
    {
        // If no content has been output yet, remove all leading new lines.
        String normalizedText = this.hasContent ? text : StringUtils.stripStart(text, "\n");
        if (!StringUtils.isEmpty(normalizedText)) {
            consumer.accept(normalizedText);
            this.hasContent = true;
        }
    }
}
