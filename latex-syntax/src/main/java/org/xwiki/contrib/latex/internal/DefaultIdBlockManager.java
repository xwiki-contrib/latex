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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.DefinitionDescriptionBlock;
import org.xwiki.rendering.block.DefinitionListBlock;
import org.xwiki.rendering.block.DefinitionTermBlock;
import org.xwiki.rendering.block.FigureBlock;
import org.xwiki.rendering.block.FigureCaptionBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.HorizontalLineBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.NumberedListBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.QuotationBlock;
import org.xwiki.rendering.block.QuotationLineBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.SpecialSymbolBlock;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.TableCellBlock;
import org.xwiki.rendering.block.TableHeadCellBlock;
import org.xwiki.rendering.block.TableRowBlock;
import org.xwiki.rendering.block.VerbatimBlock;
import org.xwiki.rendering.block.WordBlock;

import static org.xwiki.contrib.latex.internal.DefaultIdBlockManager.BlockType.inline;
import static org.xwiki.contrib.latex.internal.DefaultIdBlockManager.BlockType.standalone;

/**
 * Default implementation.
 *
 * @version $Id$
 * @since 1.9.4
 */
// TODO: Remove this class when it's implemented in the rendering module
@Component
@Singleton
public class DefaultIdBlockManager implements IdBlockManager, Initializable
{
    private static Map<Class<? extends Block>, BlockType> blockTypes = new HashMap<>();

    enum BlockType
    {
        standalone,
        inline
    }

    @Override
    public void initialize()
    {
        blockTypes.put(ParagraphBlock.class, standalone);
        blockTypes.put(BulletedListBlock.class, standalone);
        blockTypes.put(NumberedListBlock.class, standalone);
        blockTypes.put(QuotationBlock.class, standalone);
        blockTypes.put(QuotationLineBlock.class, standalone);
        blockTypes.put(DefinitionListBlock.class, standalone);
        blockTypes.put(DefinitionDescriptionBlock.class, standalone);
        blockTypes.put(DefinitionTermBlock.class, standalone);
        blockTypes.put(FigureBlock.class, standalone);
        blockTypes.put(FigureCaptionBlock.class, standalone);
        blockTypes.put(TableBlock.class, standalone);
        blockTypes.put(TableCellBlock.class, standalone);
        blockTypes.put(TableHeadCellBlock.class, standalone);
        blockTypes.put(TableRowBlock.class, standalone);
        blockTypes.put(HeaderBlock.class, standalone);
        blockTypes.put(GroupBlock.class, standalone);
        blockTypes.put(HorizontalLineBlock.class, standalone);
        blockTypes.put(RawBlock.class, standalone);
        blockTypes.put(SectionBlock.class, standalone);

        blockTypes.put(VerbatimBlock.class, inline);
        blockTypes.put(ImageBlock.class, inline);
        blockTypes.put(NewLineBlock.class, inline);
        blockTypes.put(ListItemBlock.class, inline);
        blockTypes.put(LinkBlock.class, inline);
        blockTypes.put(FormatBlock.class, inline);
        blockTypes.put(WordBlock.class, inline);
        blockTypes.put(SpaceBlock.class, inline);
        blockTypes.put(SpecialSymbolBlock.class, inline);
    }

    @Override
    public boolean isInline(IdBlock idBlock)
    {
        boolean isInline;

        // Check next sibling
        Block nextSibling = idBlock.getNextSibling();
        if (nextSibling != null) {
            BlockType type = blockTypes.get(nextSibling.getClass());
            if (type != null) {
                isInline = type == inline;
            } else {
                // TODO: Improve this. For example to handle the case when the next sibling is a MetaDataBlock.
                isInline = false;
            }
        } else {
            Block parentBlock = idBlock.getParent();
            if (parentBlock != null) {
                if (parentBlock instanceof MacroMarkerBlock) {
                    isInline = ((MacroMarkerBlock) parentBlock).isInline();
                } else if (parentBlock instanceof ParagraphBlock) {
                    isInline = true;
                } else {
                    isInline = false;
                }
            } else {
                isInline = false;
            }
        }

        return isInline;
    }
}
