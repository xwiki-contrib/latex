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

import java.util.List;
import java.util.Stack;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FigureBlock;

/**
 * Provides useful tools for use in the LaTeX templates.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface LaTeXTool
{
    /**
     * Escapes special characters in LaTeX.
     *
     * @param input the string to escape
     * @return the escaped string
     */
    String escape(String input);

    /**
     * Remove forbidden characters when used in labels. Forbidden chars are:
     * {@code #}, {@code %}, {@code ~}, {@code |}, {@code {}, {@code $}, and {@code }}.
     *
     * @see <a href="https://tex.stackexchange.com/questions/18311/what-are-the-valid-names-as-labels">what-are-the-
     *      valid-names-as-labels</a>
     * @param input the label string to normalize
     * @return the valid string
     */
    String normalizeLabel(String input);

    /**
     * @return the LaTeX language name to use for the content
     */
    String getLanguage();

    /**
     * @param currentBlock the current block for which to find all following siblings
     * @return all the siblings of the current block
     */
    List<Block> getSiblings(Block currentBlock);

    /**
     * @param figureBlock the figure block that needs to be checked to verify if it contains only a table (should be a
     *        FigureBlock)
     * @return true if it contains only a table or false otherwise
     */
    boolean isTable(FigureBlock figureBlock);

    /**
     * Retrieve and create a Stack if it doesn't exist.
     *
     * @param id the name of the stack to create
     * @return the stack
     */
    Stack<?> getStack(String id);

    /**
     * @param block the block to check
     * @return true if the block is a table cell block (head cell or normal cell)
     */
    boolean isTableCell(Block block);

    /**
     * @param current the current block for which to find the parent
     * @return the parent of the passed block, ignoring MacroMarkerBlocks
     */
    Block getParentBlock(Block current);

    /**
     * @param block the block for which we want to check previous siblings
     * @return return true if previous siblings only contain Id macros
     * @since 1.7
     */
    boolean previousSiblingsContainsOnlyIdMacros(Block block);
}
