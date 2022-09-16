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

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.FigureBlock;
import org.xwiki.rendering.macro.figure.FigureTypeRecognizer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultFigureTool}.
 *
 * @version $Id$
 * @since 1.19
 */
@ComponentTest
class DefaultFigureToolTest
{
    @InjectMockComponents
    private DefaultFigureTool figureTool;

    @MockComponent
    private FigureTypeRecognizer figureTypeRecognizer;

    @Test
    void isTableWithExplicitTableType()
    {
        assertTrue(this.figureTool.isTable(new FigureBlock(emptyList(), singletonMap(
            "data-xwiki-rendering-figure-type", "table"
        ))));
        verifyNoInteractions(this.figureTypeRecognizer);
    }

    @Test
    void isTableWithExplicitFigureType()
    {
        assertFalse(this.figureTool.isTable(new FigureBlock(emptyList(), singletonMap(
            "data-xwiki-rendering-figure-type", "figure"
        ))));
        verifyNoInteractions(this.figureTypeRecognizer);
    }

    @Test
    void isTable()
    {
        FigureBlock figureBlock = new FigureBlock(emptyList());
        when(this.figureTypeRecognizer.isTable(figureBlock)).thenReturn(true);
        assertTrue(this.figureTool.isTable(figureBlock));
    }
}
