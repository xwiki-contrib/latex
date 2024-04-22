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
package org.xwiki.contrib.latex.internal.export;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.uiextension.UIExtension;

import com.xpn.xwiki.XWikiContext;

/**
 * Inject a LaTeX export format in the standard export menu UI.
 * 
 * @version $Id$
 * @since 1.5
 */
public abstract class AbstractLaTeXExportUIExtension implements UIExtension
{
    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.template.exportFormats";
    }

    @Override
    public Map<String, String> getParameters()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("label", this.localizationManager.getTranslationPlain(getLabelKey()));
        parameters.put("hint", getHintKey());
        parameters.put("icon", getIcon());
        parameters.put("category", getCategory());
        parameters.put("multipage", Boolean.FALSE.toString());
        parameters.put("filterHiddenPages", Boolean.TRUE.toString());
        parameters.put("excludeNestedPagesByDefault", Boolean.TRUE.toString());
        parameters.put("enabled", Boolean.toString(isEnabled()));

        // Note: make sure to keep any existing query string since the user could need that to render its doc properly.
        String queryString = addToQueryString(this.xcontextProvider.get().getRequest().getQueryString(),
            getQueryString());
        String path = this.bridge.getDocumentURL(this.bridge.getCurrentDocumentReference(),
            LaTeXExportResourceReferenceHandler.ACTION_STRING, queryString, null);
        parameters.put("url", path);

        return parameters;
    }

    @Override
    public Block execute()
    {
        return new CompositeBlock();
    }

    protected abstract String getLabelKey();

    protected abstract String getHintKey();

    protected abstract String getIcon();

    protected abstract String getCategory();

    protected abstract String getQueryString();

    protected abstract boolean isEnabled();

    private String addToQueryString(String originalQueryString, String newPart)
    {
        String newQueryString;
        if (StringUtils.isEmpty(newPart)) {
            newQueryString = originalQueryString;
        } else if (StringUtils.isEmpty(originalQueryString)) {
            newQueryString = newPart;
        } else {
            newQueryString = String.format("%s&%s", originalQueryString, newPart);
        }
        return newQueryString;
    }
}
