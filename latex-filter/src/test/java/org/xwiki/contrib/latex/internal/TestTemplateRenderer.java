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

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Mock template renderer.
 * 
 * @version $Id$
 */
@Component
@Named("whatever")
@Singleton
public class TestTemplateRenderer implements TemplateRenderer
{
    @Inject
    @Named("event/1.0")
    private BlockRenderer eventRenderer;

    @Override
    public void render(Collection<Block> blocks, WikiPrinter printer)
    {
        printer.println("/********** latex result for the following events **********");

        this.eventRenderer.render(blocks, printer);

        printer.print("********** blocks " + blocks + " **********/");
    }

    @Override
    public void render(String relativeTemplateName, WikiPrinter printer)
    {
        printer.print("********** template " + relativeTemplateName + " **********");
    }
}
