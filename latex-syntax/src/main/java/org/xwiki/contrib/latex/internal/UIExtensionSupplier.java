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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.script.UIExtensionScriptService;

/**
 * Supply {@link UIExtension}s from user interface extension point identifiers. This component duplicates the logic of
 * {@link UIExtensionScriptService#getExtensions(String)} outside of a {@link ScriptService}.
 *
 * @version $Id$
 * @see <a href="https://jira.xwiki.org/browse/XWIKI-19272">XWIKI-19272</a>
 * @since 1.16
 */
@Component(roles = UIExtensionSupplier.class)
@Singleton
public class UIExtensionSupplier
{
    /**
     * The default UIExtensionManager.
     */
    @Inject
    private UIExtensionManager uiExtensionManager;

    /**
     * We use the Context Component Manager to lookup UI Extensions registered as components. The Context Component
     * Manager allows Extensions to be registered for a specific user, for a specific wiki or for a whole farm.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Return a list of {@link UIExtension} for a given user interface extension point identifier (UIXP id). The list of
     * {@code UIExtension} is by default provided by a default {@link UIExtensionManager} component. The {@code
     * UIExtensionManager} can be overridden by providing a {@code UIExtensionManager} with an hint matching the
     * provided UIXP id.
     *
     * @param extensionPointId an user interface extension point identifier (e.g., {@code
     *     org.xwiki.contrib.latex.XDOM.before})
     * @return a list of {@link UIExtension} for the given UIXP id
     */
    public List<UIExtension> getExtensions(String extensionPointId)
    {
        UIExtensionManager manager = this.uiExtensionManager;

        ComponentManager componentManager = this.contextComponentManagerProvider.get();
        if (componentManager.hasComponent(UIExtensionManager.class, extensionPointId)) {
            try {
                // Look for a specific UI extension manager for the given extension point.
                manager = componentManager.getInstance(UIExtensionManager.class, extensionPointId);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to initialize UI extension manager", e);
            }
        }

        return manager.get(extensionPointId);
    }
}
