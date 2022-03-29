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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.PlainTextBlockFilter;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;

/**
 * Specialized plain text block filter for the LaTeX plugin.
 *
 * Compared to the generic one, it replaces newlines by spaces and removes generated figure, table and heading numbers.
 * Further, it inserts spaces between content that was formerly in different blocks such that, e.g., two paragraphs
 * are separated by a space. This shouldn't be used as a filter directly but instead the provided method
 * {@code getPlainTextDescendants(Block block)} should be used which does additional post-processing of the filtered
 * results.
 *
 * @since 1.17
 * @version $Id$
 */
public class LaTeXPlainTextBlockFilter extends PlainTextBlockFilter
{
    private static final List<String> FORBIDDEN_CLASSES = Arrays.asList("wikigeneratedfigurenumber",
        "wikigeneratedtablenumber", "wikigeneratedheadingnumber");

    /**
     * List of blocks for which no space should be added after them when they are removed.
     */
    private static final List<Class<? extends Block>> INSERT_NO_SPACE_AFTER_BLOCKS = Arrays.asList(FormatBlock.class,
        IdBlock.class, ImageBlock.class, MacroMarkerBlock.class, CompositeBlock.class, VerbatimBlock.class,
        MetaDataBlock.class);

    /**
     * Internal placeholder used to avoid that children of removed blocks are added.
     */
    private static class PlaceholderBlock extends AbstractBlock
    {
    }

    /**
     * Block to mark a place where we might want to insert a space block if there is no adjacent space block.
     */
    private static class MaybeSpaceBlock extends AbstractBlock
    {
    }

    /**
     *
     * @param plainTextParser the plain text parser used to transform link labels into plain text
     * @param linkLabelGenerator the link label generator
     */
    public LaTeXPlainTextBlockFilter(Parser plainTextParser, LinkLabelGenerator linkLabelGenerator)
    {
        super(plainTextParser, linkLabelGenerator);
    }

    @Override
    public List<Block> filter(Block block)
    {
        if (block instanceof FormatBlock) {
            // Remove format blocks with forbidden classes including their children by returning a placeholder.
            String classParameter = block.getParameter("class");
            if (classParameter != null && FORBIDDEN_CLASSES.stream().anyMatch(classParameter::contains)) {
                return Collections.singletonList(new PlaceholderBlock());
            }
        } else if (block instanceof NewLineBlock) {
            // Replace newlines by (possible) spaces. The parent filter keeps them.
            return Collections.singletonList(new MaybeSpaceBlock());
        }

        List<Block> parentResult = super.filter(block);
        List<Block> result;

        if (parentResult.isEmpty() && !INSERT_NO_SPACE_AFTER_BLOCKS.contains(block.getClass())) {
            // Possibly insert a space after every block where this seems sensible.
            result = new ArrayList<>(block.getChildren());
            result.add(new MaybeSpaceBlock());
        } else {
            result = parentResult;
        }

        return result;
    }

    /**
     * @param block the block for which plain text descendants shall be obtained
     * @return the plain text descendants of the given block
     */
    public List<Block> getPlainTextDescendants(Block block)
    {
        Block filteredBlock = block.clone(this);
        List<Block> resultBlocks = new ArrayList<>();

        for (Block currentBlock : filteredBlock.getChildren()) {
            if (currentBlock instanceof MaybeSpaceBlock) {
                // Insert a space if the previous block is not a space block and the next block is neither a
                // potential nor an actual space block - but not at the beginning or the end of the output.
                if (!resultBlocks.isEmpty() && currentBlock.getNextSibling() != null) {
                    Block previousBlock = resultBlocks.get(resultBlocks.size() - 1);
                    Block nextBlock = currentBlock.getNextSibling();

                    boolean previousBlockNoSpace = !(previousBlock instanceof SpaceBlock);
                    boolean nextBlockNoSpace =
                        !(nextBlock instanceof SpaceBlock) && !(nextBlock instanceof MaybeSpaceBlock);

                    if (previousBlockNoSpace && nextBlockNoSpace) {
                        resultBlocks.add(new SpaceBlock());
                    }
                }
            } else if (!(currentBlock instanceof PlaceholderBlock)) {
                // Filter out placeholder blocks.
                resultBlocks.add(currentBlock);
            }
        }

        // Set the blocks as children such that they have correct sibling and parent relationships set.
        filteredBlock.setChildren(resultBlocks);

        return resultBlocks;
    }

}
