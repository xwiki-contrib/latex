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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DefaultIdBlockManager}.
 *
 * @version $Id$
 */
public class DefaultIdBlockManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultIdBlockManager> mocker =
        new MockitoComponentMockingRule<>(DefaultIdBlockManager.class);

    @Test
    public void isInlineWhenNoParent() throws Exception
    {
        IdBlock idBlock = new IdBlock("something");
        assertFalse(mocker.getComponentUnderTest().isInline(idBlock));
    }

    @Test
    public void isInlineWhenNextSiblingInline() throws Exception
    {
        IdBlock idBlock = new IdBlock("something");
        idBlock.setNextSiblingBlock(new WordBlock("whatever"));
        assertTrue(mocker.getComponentUnderTest().isInline(idBlock));
    }

    @Test
    public void isInlineWhenNextSiblingStandalone() throws Exception
    {
        IdBlock idBlock = new IdBlock("something");
        idBlock.setNextSiblingBlock(new BulletedListBlock(Collections.emptyList()));
        assertFalse(mocker.getComponentUnderTest().isInline(idBlock));
    }

    @Test
    public void isInlineWhenNoNextSiblingAndInsideParagraph() throws Exception
    {
        IdBlock idBlock = new IdBlock("something");
        idBlock.setParent(new ParagraphBlock(Collections.emptyList()));
        assertTrue(mocker.getComponentUnderTest().isInline(idBlock));
    }

    @Test
    public void isInlineWhenNextSiblingOfUndefinedType() throws Exception
    {
        IdBlock idBlock = new IdBlock("something");
        idBlock.setNextSiblingBlock(new MetaDataBlock(Collections.emptyList()));
        assertFalse(mocker.getComponentUnderTest().isInline(idBlock));
    }

    @Test
    public void isInlineWhenNoNextSiblingAndInlineMacroMarkerBlockParent() throws Exception
    {
        IdBlock idBlock = new IdBlock("something");
        idBlock.setParent(new MacroMarkerBlock("whatever", Collections.emptyMap(), Collections.emptyList(), true));
        assertTrue(mocker.getComponentUnderTest().isInline(idBlock));
    }

    @Test
    public void isInlineWhenNoNextSiblingAndStandaloneMacroMarkerBlockParent() throws Exception
    {
        IdBlock idBlock = new IdBlock("something");
        idBlock.setParent(new MacroMarkerBlock("whatever", Collections.emptyMap(), Collections.emptyList(), false));
        assertFalse(mocker.getComponentUnderTest().isInline(idBlock));
    }
}
