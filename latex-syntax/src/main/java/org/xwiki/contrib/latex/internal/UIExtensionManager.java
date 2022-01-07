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
import java.util.Map;

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
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.script.UIExtensionScriptService;

/**
 * Supply {@link UIExtension}s from user interface extension point identifiers. This component duplicates the logic of
 * {@link UIExtensionScriptService#getExtensions(String)} and {@link UIExtensionScriptService#getExtensions(String,
 * Map)} outside of a {@link ScriptService}.
 *
 * @version $Id$
 * @since 1.16
 */
// TODO: Remove this class once XWIKI-19272 and XWIKI-19298 are fixed.
@Component(roles = UIExtensionManager.class)
@Singleton
public class UIExtensionManager
{
    /**
     * The default UIExtensionManager.
     */
    @Inject
    private org.xwiki.uiextension.UIExtensionManager xWikiUiExtensionManager;

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
     * {@code UIExtension} is by default provided by a default {@link org.xwiki.uiextension.UIExtensionManager}
     * component. The {@code UIExtensionManager} can be overridden by providing a {@code UIExtensionManager} with an
     * hint matching the provided UIXP id.
     *
     * @param extensionPointId an user interface extension point identifier (e.g., {@code
     *     org.xwiki.contrib.latex.XDOM.before})
     * @return a list of {@link UIExtension} for the given UIXP id
     */
    public List<UIExtension> getExtensions(String extensionPointId)
    {
        org.xwiki.uiextension.UIExtensionManager manager = this.xWikiUiExtensionManager;

        ComponentManager componentManager = this.contextComponentManagerProvider.get();
        if (componentManager.hasComponent(org.xwiki.uiextension.UIExtensionManager.class, extensionPointId)) {
            try {
                // Look for a specific UI extension manager for the given extension point.
                manager =
                    componentManager.getInstance(org.xwiki.uiextension.UIExtensionManager.class, extensionPointId);
            } catch (ComponentLookupException e) {
                // Note: this should never happen since we already checked for the presence of the component.
                this.logger.error("Failed to initialize UI extension manager with hint [{}].", extensionPointId, e);
            }
        }

        return manager.get(extensionPointId);
    }

    /**
     * Retrieves the list of {@link UIExtension} for a given Extension Point.
     * <p>
     * Examples:
     * <ul>
     * <li>Get only the {@link UIExtension}s with the given IDs for the Extension Point "platform.example"
     * <pre>$services.uix.getExtensions('platform.example', {'select' : 'id1, id2, id3'})</pre></li>
     * <li>Get all the {@link UIExtension}s for the Extension Point "platform.example" except the
     * {@link UIExtension}s with the IDs "id2" and "id3"
     * <pre>$services.uix.getExtensions('platform.example', {'exclude' : 'id2, id3'})</pre></li>
     * <li>Get all the {@link UIExtension}s for the Extension Point "platform.example" and order them by one of their
     * parameter
     * <pre>$services.uix.getExtensions('platform.example', {'sortByParameter' : 'parameterKey'})</pre></li>
     * <li>Get only the {@link UIExtension}s with the given IDs for the Extension Point "platform.example" and order
     * them by one of their parameter
     * <pre>$services.uix.getExtensions('platform.example',
     * {'select' : 'id1, id2, id3', 'sortByParameter' : 'parameterKey'})</pre></li>
     * </ul>
     *
     * @param extensionPointId The ID of the Extension Point to retrieve the {@link UIExtension}s for
     * @param filters Optional filters to apply before retrieving the list
     * @return the list of {@link UIExtension} for the given Extension Point
     */
    public List<UIExtension> getExtensions(String extensionPointId, Map<String, String> filters)
    {
        List<UIExtension> extensions = getExtensions(extensionPointId);

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String filterHint = entry.getKey();

            try {
                UIExtensionFilter filter =
                    this.contextComponentManagerProvider.get().getInstance(UIExtensionFilter.class, filterHint);
                extensions = filter.filter(extensions, this.parseFilterParameters(entry.getValue()));
            } catch (ComponentLookupException e) {
                this.logger.warn("Unable to find a UIExtensionFilter for hint [{}] "
                    + "while getting UIExtensions for extension point [{}]", filterHint, extensionPointId);
            }
        }

        return extensions;
    }

    /**
     * Utility method to split a list of extension names, for example {code}"Panels.Apps,Panels.QuickLinks"{code} to get
     * a List containing those names.
     *
     * @param nameList the list of extension names to split
     * @return a List containing all the names from the given String.
     */
    private String[] parseFilterParameters(String nameList)
    {
        return nameList.replaceAll(" ", "").split(",");
    }
}
