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

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;

/**
 * Provides useful Figure-related tools for use in the LaTeX templates.
 *
 * @version $Id$
 * @since 1.10
 */
@Role
public interface FigureTool
{
    /**
     * @param figureBlock the figure block that needs to be checked
     * @return true if it contains only a table inside the Figure Macro or false otherwise
     */
    boolean isTable(Block figureBlock);

    /**
     * @param figureCaptionBlock the figure caption block to test
     * @return true if the figure caption macro is the last content inside the figure macro content or false otherwise
     */
    boolean isFigureCaptionLast(Block figureCaptionBlock);
}
