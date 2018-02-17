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

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;

/**
 * Get LaTeX templates from the classloader.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultLaTeXConfiguration implements LaTeXConfiguration
{
    @Override
    public String getTemplate(Block block) throws IOException
    {
        String result = null;
        String location = String.format("templates/%s", block.getClass().getName());
        InputStream templateStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        if (templateStream != null) {
            result = IOUtils.toString(templateStream, "UTF-8");
        }
        return result;
    }
}
