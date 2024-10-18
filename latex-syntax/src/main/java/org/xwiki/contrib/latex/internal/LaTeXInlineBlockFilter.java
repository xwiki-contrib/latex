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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BlockFilter;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;

/**
 * Specialized block filter for the LaTeX extension, that converts standalone blocks to inline content whenever
 * possible.It also replaces newlines by spaces and removes generated figure, table and heading numbers.
 * Further, it inserts spaces between content that was formerly in different blocks such that, e.g., two paragraphs
 * are separated by a space. This shouldn't be used as a filter directly but instead the provided method
 * {@code getInlineDescendants(Block block)} should be used which does additional post-processing of the filtered
 * results.
 * <p>
 * In addition, there's a special handling for RawBlocks with a LaTeX syntax: their content is considered inline. This
 * is to allow using the Raw macro to inject LaTeX content in captions.
 *
 *
 * @since 1.26
 * @version $Id$
 */
public class LaTeXInlineBlockFilter implements BlockFilter
{
    private static final String LATEX_SYNTAX_ID = "latex/1.0";

    private static final Set<Class<? extends Block>> VALID_BLOCKS = new HashSet<>();

    static {
        VALID_BLOCKS.add(WordBlock.class);
        VALID_BLOCKS.add(SpaceBlock.class);
        VALID_BLOCKS.add(SpecialSymbolBlock.class);
        VALID_BLOCKS.add(FormatBlock.class);
        VALID_BLOCKS.add(MacroMarkerBlock.class);
        VALID_BLOCKS.add(LinkBlock.class);
        VALID_BLOCKS.add(RawBlock.class);
    }

    private static final List<String> FORBIDDEN_CLASSES = Arrays.asList(
        "wikigeneratedfigurenumber",
        "wikigeneratedtablenumber",
        "wikigeneratedheadingnumber");

    /**
     * List of blocks for which no space should be added after them when they are removed.
     */
    private static final List<Class<? extends Block>> INSERT_NO_SPACE_AFTER_BLOCKS = Arrays.asList(
        IdBlock.class,
        ImageBlock.class,
        MacroMarkerBlock.class,
        CompositeBlock.class,
        VerbatimBlock.class,
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

    @Override
    public List<Block> filter(Block block)
    {
        List<Block> result = null;

        if (block instanceof FormatBlock) {
            // Remove format blocks with forbidden classes including their children by returning a placeholder, since
            // we don't want to see them in captions in list of tables/figures.
            String classParameter = block.getParameter("class");
            if (classParameter != null && FORBIDDEN_CLASSES.stream().anyMatch(classParameter::contains)) {
                result = Collections.singletonList(new PlaceholderBlock());
            }
        } else if (block instanceof RawBlock && !isLaTexSyntax((RawBlock) block)) {
            // Remove RawBlock with a non-LaTeX target syntax since it's too complex to convert them to inline
            result = Collections.singletonList(new PlaceholderBlock());
        } else if (block instanceof MacroMarkerBlock) {
            result = handleMacroMarkerBlock(block);
        }

        // Filter out not allowed blocks
        if (result == null) {
            if (VALID_BLOCKS.contains(block.getClass())) {
                result = Collections.singletonList(block);
            } else if (!INSERT_NO_SPACE_AFTER_BLOCKS.contains(block.getClass())) {
                // Possibly insert a space after every block where this seems sensible.
                result = new ArrayList<>(block.getChildren());
                result.add(new MaybeSpaceBlock());
            } else {
                result = Collections.emptyList();
            }
        }

        return result;
    }

    /**
     * @param block the block for which inline descendants shall be obtained
     * @return the inline text descendants of the given block
     */
    public List<Block> getInlineDescendants(Block block)
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

    private List<Block> handleMacroMarkerBlock(Block block)
    {
        List<Block> result = null;

        // Remove macros that are not inline since it's too complex to convert them to inline in a generic way.
        // Except for RawBlock with a LaTeX syntax, in which case we assume the user will have used proper content
        // that can be displayed when converted to inline content.
        if (isLaTeXRawBlock((MacroMarkerBlock) block)) {
            // The RawBlock template is checking if it's inside an inline MacroMarkerBlock to decide whether to
            // display it inline or not. As we want to convert it to an inline display, force the MacroMarkerBlock
            // to be inline.
            MacroMarkerBlock mmb = (MacroMarkerBlock) block;
            if (!mmb.isInline()) {
                MacroMarkerBlock mmbBlock = new MacroMarkerBlock(mmb.getId(), mmb.getParameters(),
                    mmb.getContent(), mmb.getChildren(), true);
                // Also add a space after the macro when it's standalone to convert it properly to inline.
                result = List.of(mmbBlock, new MaybeSpaceBlock());
            }
        } else if (!((MacroMarkerBlock) block).isInline()) {
            result = Collections.singletonList(new PlaceholderBlock());
        }

        return result;
    }

    private boolean isLaTexSyntax(RawBlock block)
    {
        return LATEX_SYNTAX_ID.equals(block.getSyntax().toIdString());
    }

    private boolean isLaTeXRawBlock(Block block)
    {
        return (block instanceof RawBlock) && isLaTexSyntax((RawBlock) block);
    }

    private boolean isLaTeXRawBlock(MacroMarkerBlock block)
    {
        return !block.getChildren().isEmpty() && isLaTeXRawBlock(block.getChildren().get(0));
    }
}
