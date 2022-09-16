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

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FigureBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.macro.figure.FigureTypeRecognizer;

/**
 * Provides useful Figure-related tools for use in the LaTeX templates.
 *
 * @version $Id$
 * @since 1.10
 */
@Component
@Singleton
public class DefaultFigureTool implements FigureTool
{
    @Inject
    private FigureTypeRecognizer figureTypeRecognizer;

    @Override
    public boolean isTable(Block figureBlock)
    {
        boolean isTable;
        String figureTypeParameter = figureBlock.getParameter("data-xwiki-rendering-figure-type");
        if (figureTypeParameter != null) {
            isTable = Objects.equals(figureTypeParameter, "table");
        } else {
            isTable = this.figureTypeRecognizer.isTable((FigureBlock) figureBlock);
        }
        return isTable;
    }

    @Override
    public boolean isFigureCaptionLast(Block figureCaptionBlock)
    {
        // Several cases are possible:
        // - The figureCaptionBlock's parent is a MMB and the MMB next sibling doesn't exist
        // - The figureCaptionBlock's parent is not a MMB and the figureCaptionBlock's next sibling doesn't exist
        // - The figureCaptionBlock's parent could be a MetaDataBlock, in which case it should be ignored and its
        //   parent checked instead.
        //
        // These are the different cases to handle to make sure it works in all versions of XWiki.
        //
        // XWiki < 10.10RC1:
        //  MacroMarkerBlock (Figure)
        //  |_ FigureBlock
        //    |_ MacroMarkerBlock (FigureCaption)
        //      |_ FigureCaptionBlock
        //
        // XWiki >= 10.10RC1 (see XRENDERING-538)
        //  MacroMarkerBlock (Figure)
        //  |_ MetaDataBlock
        //    |_ FigureBlock
        //      |_ MacroMarkerBlock (FigureCaption)
        //        |_ MetaDataBlock
        //          |_ FigureCaptionBlock
        //
        // XWiki >= 11.4RC1 (see XWIKI-16389)
        //  MacroMarkerBlock (Figure)
        //  |_ MetaDataBlock
        //    |_ FigureBlock
        //        |_ MetaDataBlock
        //          |_ FigureCaptionBlock
        //
        // XWiki >= 14.1RC1 (see XRENDERING-629)
        //  MacroMarkerBlock (Figure)
        //  |_ FigureBlock
        //    |_ MetaDataBlock
        //      |_ MacroMarkerBlock (FigureCaption)
        //        |_ FigureCaptionBlock
        //          |_ MetaDataBlock
        //
        // XWiki >= 14.1RC1 (see XRENDERING-628)
        // FigureBlock
        // |_ FigureCaptionBlock
        //
        // Note: XWIKI-16916 is still open and when closed it may put back the stripped MacroMarkerBlock inside the
        // Figure Macro MacroMarkerBlock.
        Block parentBlock = figureCaptionBlock.getParent();
        if (parentBlock instanceof MetaDataBlock) {
            parentBlock = parentBlock.getParent();
        }
        return parentBlock instanceof MacroMarkerBlock && parentBlock.getNextSibling() == null
            || !(parentBlock instanceof MacroMarkerBlock) && figureCaptionBlock.getNextSibling() == null;
    }
}
