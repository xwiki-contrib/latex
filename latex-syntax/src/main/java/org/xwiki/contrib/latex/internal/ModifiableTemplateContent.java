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

import java.lang.reflect.Type;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.TemplateContent;

/**
 * Allows modifying the TemplateContent's content.
 *
 * @version $Id$
 * @since 1.0
 */
public class ModifiableTemplateContent implements TemplateContent
{
    private TemplateContent wrappedTemplateContent;

    private String content;

    /**
     * @param wrappedTemplateContent the original TemplateContent object we're wrapping
     */
    public ModifiableTemplateContent(TemplateContent wrappedTemplateContent)
    {
        this.wrappedTemplateContent = wrappedTemplateContent;
    }

    @Override
    public String getContent()
    {
        if (this.content == null) {
            return this.wrappedTemplateContent.getContent();
        } else {
            return this.content;
        }
    }

    /**
     * @param content the modified content to set
     */
    public void setContent(String content)
    {
        this.content = content;
    }

    @Override
    public Syntax getSourceSyntax()
    {
        return this.wrappedTemplateContent.getSourceSyntax();
    }

    @Override
    public Syntax getRawSyntax()
    {
        return this.wrappedTemplateContent.getRawSyntax();
    }

    @Override
    public <T> T getProperty(String name, Type type)
    {
        return this.wrappedTemplateContent.getProperty(name, type);
    }

    @Override
    public <T> T getProperty(String name, T def)
    {
        return this.wrappedTemplateContent.getProperty(name, def);
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.wrappedTemplateContent.getAuthorReference();
    }

    @Override
    public boolean isAuthorProvided()
    {
        return this.wrappedTemplateContent.isAuthorProvided();
    }
}
