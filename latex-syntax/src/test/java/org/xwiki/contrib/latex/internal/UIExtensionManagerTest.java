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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link UIExtensionManager}.
 *
 * @version $Id$
 * @since 1.16
 */
@ComponentTest
class UIExtensionManagerTest
{
    public static final String FAKE_UIXP_ID = "uixp.id";

    @InjectMockComponents
    private UIExtensionManager uiExtensionManager;

    @MockComponent
    private org.xwiki.uiextension.UIExtensionManager xWikiIiExtensionManager;

    @Mock
    private org.xwiki.uiextension.UIExtensionManager uixpIdUIUiExtensionManager;

    /**
     * We use the Context Component Manager to lookup UI Extensions registered as components. The Context Component
     * Manager allows Extensions to be registered for a specific user, for a specific wiki or for a whole farm.
     */
    @MockComponent
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Mock
    private ComponentManager componentManager;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        when(this.contextComponentManagerProvider.get()).thenReturn(this.componentManager);
    }

    @Test
    void getExtensions()
    {
        this.uiExtensionManager.getExtensions(FAKE_UIXP_ID);

        verify(this.componentManager).hasComponent(org.xwiki.uiextension.UIExtensionManager.class, FAKE_UIXP_ID);
        verify(this.xWikiIiExtensionManager).get(FAKE_UIXP_ID);
    }

    @Test
    void getExtensionsOverridden() throws Exception
    {
        doReturn(true).when(this.componentManager)
            .hasComponent(org.xwiki.uiextension.UIExtensionManager.class, FAKE_UIXP_ID);
        when(this.componentManager.getInstance(org.xwiki.uiextension.UIExtensionManager.class, FAKE_UIXP_ID))
            .thenReturn(this.uixpIdUIUiExtensionManager);
        this.uiExtensionManager.getExtensions(FAKE_UIXP_ID);

        verify(this.xWikiIiExtensionManager, never()).get(FAKE_UIXP_ID);
        verify(this.uixpIdUIUiExtensionManager).get(FAKE_UIXP_ID);
    }

    @Test
    void getExtensionsGetExtensionsFails() throws Exception
    {
        doReturn(true).when(this.componentManager)
            .hasComponent(org.xwiki.uiextension.UIExtensionManager.class, FAKE_UIXP_ID);
        when(this.componentManager.getInstance(org.xwiki.uiextension.UIExtensionManager.class, FAKE_UIXP_ID))
            .thenThrow(ComponentLookupException.class);
        this.uiExtensionManager.getExtensions(FAKE_UIXP_ID);

        verify(this.xWikiIiExtensionManager).get(FAKE_UIXP_ID);
        verify(this.uixpIdUIUiExtensionManager, never()).get(FAKE_UIXP_ID);
        
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to initialize UI extension manager with hint [uixp.id].", this.logCapture.getMessage(0));
    }

    @Test
    void getExtensionsWithFilter() throws Exception
    {
        UIExtension uiExtensionA = mock(UIExtension.class);
        UIExtension uiExtensionB = mock(UIExtension.class);
        UIExtensionFilter uiExtensionFilter = mock(UIExtensionFilter.class);
        
        when(this.xWikiIiExtensionManager.get(FAKE_UIXP_ID)).thenReturn(asList(uiExtensionA, uiExtensionB));
        when(this.componentManager.getInstance(UIExtensionFilter.class, "filterKey")).thenReturn(uiExtensionFilter);

        this.uiExtensionManager.getExtensions(FAKE_UIXP_ID, singletonMap("filterKey", "filterValue"));

        verify(uiExtensionFilter).filter(asList(uiExtensionA, uiExtensionB), "filterValue");
    }
}
