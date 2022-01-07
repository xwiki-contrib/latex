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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.uiextension.UIExtension;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link TemplateProcessor}.
 *
 * @version $Id$
 * @since 1.16
 */
@ComponentTest
class TemplateProcessorTest
{
    private TemplateProcessor templateProcessor;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private VelocityMacroFilter filter;

    @MockComponent
    private UIExtensionSupplier uiExtensionSupplier;

    @MockComponent
    private BlockRenderer blockRenderer;

    @Mock
    private UIExtension wordBlockBeforeUIX;

    @Mock
    private UIExtension wordBlockAfterUIX;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        this.templateProcessor =
            new TemplateProcessor(this.templateManager, new HashMap<>(), this.filter, this.uiExtensionSupplier,
                this.blockRenderer);
    }

    @Test
    void processUIXPsCalledOnBlock()
    {
        this.templateProcessor.process(singletonList(new WordBlock("Hello")));
        verify(this.uiExtensionSupplier).getExtensions("org.xwiki.contrib.latex.WordBlock.before");
        verify(this.uiExtensionSupplier).getExtensions("org.xwiki.contrib.latex.WordBlock.after");
    }

    @Test
    void processUIXPsRenderedWhenFound()
    {
        when(this.wordBlockBeforeUIX.execute()).thenReturn(new WordBlock("before"));
        when(this.wordBlockAfterUIX.execute()).thenReturn(new WordBlock("after"));

        doAnswer(invocation -> {
            invocation.<WikiPrinter>getArgument(1).print(invocation.<WordBlock>getArgument(0).getWord());
            return null;
        }).when(this.blockRenderer).render(any(WordBlock.class), any());

        doReturn(singletonList(this.wordBlockBeforeUIX))
            .when(this.uiExtensionSupplier)
            .getExtensions("org.xwiki.contrib.latex.WordBlock.before");
        doReturn(singletonList(this.wordBlockAfterUIX))
            .when(this.uiExtensionSupplier)
            .getExtensions("org.xwiki.contrib.latex.WordBlock.after");
        String processed = this.templateProcessor.process(singletonList(new WordBlock("Hello")));
        assertEquals("beforeafter", processed);
    }

    @Test
    void processUIXPsNotCalledOnXDOM()
    {
        this.templateProcessor.process(singletonList(new XDOM(emptyList())));
        verify(this.uiExtensionSupplier, never()).getExtensions(any());
        verify(this.uiExtensionSupplier, never()).getExtensions(any());
    }

    @Test
    void processUIXPInOrder()
    {
        UIExtension wordBlockBeforeUIX2 = mock(UIExtension.class);

        when(this.wordBlockBeforeUIX.getParameters()).thenReturn(singletonMap("order", "5"));
        when(wordBlockBeforeUIX2.getParameters()).thenReturn(singletonMap("order", "10"));

        WordBlock secondWordBlock = new WordBlock("SECOND");
        WordBlock firstWordBlock = new WordBlock("FIRST");

        when(this.wordBlockBeforeUIX.execute()).thenReturn(secondWordBlock);
        when(wordBlockBeforeUIX2.execute()).thenReturn(firstWordBlock);

        doReturn(asList(this.wordBlockBeforeUIX, wordBlockBeforeUIX2))
            .when(this.uiExtensionSupplier)
            .getExtensions("org.xwiki.contrib.latex.WordBlock.before");
        this.templateProcessor.process(singletonList(new WordBlock("Hello")));
        InOrder inOrderVerifier = inOrder(this.blockRenderer);
        // Verify that the extensions are called in the right order.
        inOrderVerifier.verify(this.blockRenderer).render(eq(firstWordBlock), any());
        inOrderVerifier.verify(this.blockRenderer).render(eq(secondWordBlock), any());
    }

    @Test
    void processUIXPInOrderMissingParameter()
    {
        UIExtension wordBlockBeforeUIX2 = mock(UIExtension.class);

        when(wordBlockBeforeUIX2.getParameters()).thenReturn(singletonMap("order", "10"));

        WordBlock secondWordBlock = new WordBlock("SECOND");
        WordBlock firstWordBlock = new WordBlock("FIRST");

        when(this.wordBlockBeforeUIX.execute()).thenReturn(secondWordBlock);
        when(wordBlockBeforeUIX2.execute()).thenReturn(firstWordBlock);

        doReturn(asList(this.wordBlockBeforeUIX, wordBlockBeforeUIX2))
            .when(this.uiExtensionSupplier)
            .getExtensions("org.xwiki.contrib.latex.WordBlock.before");
        this.templateProcessor.process(singletonList(new WordBlock("Hello")));
        InOrder inOrderVerifier = inOrder(this.blockRenderer);
        // Verify that the extensions are called in the right order.
        inOrderVerifier.verify(this.blockRenderer).render(eq(firstWordBlock), any());
        inOrderVerifier.verify(this.blockRenderer).render(eq(secondWordBlock), any());
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("The [order] parameter is missing for the [wordBlockBeforeUIX] UIExtension. "
            + "Using default value [0].", this.logCapture.getMessage(0));
    }

    @Test
    void processUIXPInOrderInvalidParameter()
    {
        UIExtension wordBlockBeforeUIX2 = mock(UIExtension.class);

        when(this.wordBlockBeforeUIX.getParameters()).thenReturn(singletonMap("order", "NOPE"));
        when(wordBlockBeforeUIX2.getParameters()).thenReturn(singletonMap("order", "10"));

        WordBlock secondWordBlock = new WordBlock("SECOND");
        WordBlock firstWordBlock = new WordBlock("FIRST");

        when(this.wordBlockBeforeUIX.execute()).thenReturn(secondWordBlock);
        when(wordBlockBeforeUIX2.execute()).thenReturn(firstWordBlock);

        doReturn(asList(this.wordBlockBeforeUIX, wordBlockBeforeUIX2))
            .when(this.uiExtensionSupplier)
            .getExtensions("org.xwiki.contrib.latex.WordBlock.before");
        this.templateProcessor.process(singletonList(new WordBlock("Hello")));
        InOrder inOrderVerifier = inOrder(this.blockRenderer);
        // Verify that the extensions are called in the right order.
        inOrderVerifier.verify(this.blockRenderer).render(eq(firstWordBlock), any());
        inOrderVerifier.verify(this.blockRenderer).render(eq(secondWordBlock), any());
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to parse the [order] parameter [NOPE] for the [wordBlockBeforeUIX] UIExtension. "
            + "Using default value [0].", this.logCapture.getMessage(0));
    }
}
