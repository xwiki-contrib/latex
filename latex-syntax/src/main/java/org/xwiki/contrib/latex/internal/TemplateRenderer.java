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

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Render LaTeX templates.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface TemplateRenderer
{
    /**
     * @param blocks the blocks to render as LaTeX templates
     * @param printer the result of the rendering
     */
    void render(Collection<Block> blocks, WikiPrinter printer);

    /**
     * @param relativeTemplateName the name of an explicit template to call (note that if the template requires some
     *        special bindings, you'll need to make sure they are available before calling this method, by putting
     *        them in the ScriptContext for example)
     * @param printer the result of the rendering
     */
    void render(String relativeTemplateName, WikiPrinter printer);
}
