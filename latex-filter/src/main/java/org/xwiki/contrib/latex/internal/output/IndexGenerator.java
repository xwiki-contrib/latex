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
package org.xwiki.contrib.latex.internal.output;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.internal.TemplateRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Create the LaTeX index file for the wiki document.
 * 
 * @version $Id$
 */
@Component(roles = IndexGenerator.class)
@Singleton
public class IndexGenerator
{
    @Inject
    private TemplateRenderer templateRenderer;

    /**
     * @return the generated index LaTeX content
     */
    public String generate()
    {
        // Get and execute the index template
        WikiPrinter wikiprinter = new DefaultWikiPrinter();
        this.templateRenderer.render("Index", wikiprinter);
        return wikiprinter.toString();
    }
}
