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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.uiextension.UIExtension;

/**
 * Inject a button in the standard export menu to leading to the latex handler.
 * 
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
@Named(LaTeXExportUIExtension.ID)
public class LaTeXExportUIExtension implements UIExtension
{
    /**
     * The ID of this UI extension.
     */
    public static final String ID = "latexexport";

    @Inject
    private DocumentAccessBridge bridge;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.plaftorm.menu.export.buttons";
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Collections.emptyMap();
    }

    @Override
    public Block execute()
    {
        String path = this.bridge.getDocumentURL(this.bridge.getCurrentDocumentReference(),
            LaTeXExportResourceReferenceHandler.ACTION_STRING, null, null);
        ResourceReference linkReference = new ResourceReference(path, ResourceType.PATH);

        Map<String, String> linkParameters = new HashMap<>();
        linkParameters.put("class", "btn btn-primary");

        return new LinkBlock(Arrays.asList(new WordBlock("LaTeX")), linkReference, false, linkParameters);
    }
}
