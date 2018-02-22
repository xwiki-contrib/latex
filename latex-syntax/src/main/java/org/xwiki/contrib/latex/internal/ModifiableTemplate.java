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

import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;

/**
 * Allows modifying the Template's content.
 *
 * @version $Id$
 * @since 1.0
 */
public class ModifiableTemplate implements Template
{
    private Template wrappedTemplate;

    private TemplateContent content;

    /**
     * @param wrappedTemplate the original Template object we're wrapping
     * @throws Exception in case of an error getting the content
     */
    public ModifiableTemplate(Template wrappedTemplate) throws Exception
    {
        this.wrappedTemplate = wrappedTemplate;
        this.content = new ModifiableTemplateContent(wrappedTemplate.getContent());
    }

    @Override
    public String getId()
    {
        return this.wrappedTemplate.getId();
    }

    @Override
    public String getPath()
    {
        return this.wrappedTemplate.getPath();
    }

    @Override
    public TemplateContent getContent() throws Exception
    {
        return this.content;
    }
}
