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

import java.util.Collections;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FigureBlock;
import org.xwiki.rendering.block.FigureCaptionBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.WordBlock;

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

    /**
     * @param figureBlock the figure block for which to get the environment name
     * @return the name of the environment (e.g., "figure", "block", or another value in case of customization by an
     *     extension)
     * @since 1.21
     */
    default String getFigureEnvironment(Block figureBlock)
    {
        return isTable(figureBlock) ? "table" : "figure";
    }

    /**
     * @param figureBlock the figure block for which to generate the parameters
     * @return the parameter of the figure (i.e., {@code "h"} by default, but can be overridden by extension)
     * @since 1.21
     */
    default Block getFigureParameter(FigureBlock figureBlock)
    {
        return new GroupBlock(Collections.singletonList(new WordBlock("h")));
    }

    /**
     * @param figureCaptionBlock the figure caption block to test
     * @return {@code true} when the figure caption can be displayed (the default implementation always return
     *     {@code true}, but this can be overridden by extension)
     * @since 1.21
     */
    default boolean displayFigureCaption(FigureCaptionBlock figureCaptionBlock)
    {
        return true;
    }
}
