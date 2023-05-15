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
import org.xwiki.stability.Unstable;

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
     * Returns the environment to use for the current caption. By default, "table" or "figure" are returned but
     * extensions can allow for different environments. Note that different environments mights require different
     * parameters (see {@link #getFigureParameter(FigureBlock)}), or might not support captions in their content (see
     * {@link #displayFigureCaption(FigureCaptionBlock)}).
     *
     * @param figureBlock the figure block for which to get the environment name
     * @return the name of the environment (e.g., "figure", "block", or another value in case of customization by an
     *     extension)
     * @since 1.21
     */
    @Unstable
    default String getFigureEnvironment(Block figureBlock)
    {
        return isTable(figureBlock) ? "table" : "figure";
    }

    /**
     * Return the figure parameter for a figure block. The figure parameter must be synchronized with the returned
     * figure environment (see {@link #getFigureEnvironment(Block)}. For instance, for the figure environments "figure"
     * or "table", the default parameters is {@code "h"} (setting the figure location to "here"), resulting in the
     * following latex.
     * <pre>
     * \begin{figure}[h]
     * % Figure content.
     * \end{figure}
     * </pre>
     *
     * @param figureBlock the figure block for which to generate the parameters
     * @return the parameter of the figure (i.e., {@code "h"} by default, but can be overridden by extension)
     * @since 1.21
     */
    @Unstable
    default Block getFigureParameter(FigureBlock figureBlock)
    {
        return new GroupBlock(Collections.singletonList(new WordBlock("h")));
    }

    /**
     * Allow to control whether the current {@code FigureCaptionBlock} should be displayed. This is useful when the
     * parent {@code FigureBlock} environment (see {@link #getFigureEnvironment(Block)}) does not support the use of
     * {@code \caption{...}} in its contents. This can be the case if the parent environment is initialized with
     * {@code \newtheorem}. Note that by default, only "figure" and "table" environments are returned by
     * {@link #getFigureEnvironment(Block)}, both supporting the use of {@code \caption{...}} in their contents.
     *
     * @param figureCaptionBlock the figure caption block to test
     * @return {@code true} when the figure caption can be displayed (the default implementation always return
     *     {@code true}, but this can be overridden by extension)
     * @since 1.21
     */
    @Unstable
    default boolean displayFigureCaption(FigureCaptionBlock figureCaptionBlock)
    {
        return true;
    }
}
