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
package org.xwiki.contrib.latex.internal.output;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.internal.reference.FSPathStringEntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;

/**
 * Serialize file paths for use in LaTeX commands (such as {@code \includegraphics{<path>}}
 * or {@code \include{<path>}}).
 *
 * @version $Id$
 * @since 1.7
 */
@Component
@Named("latexpath")
@Singleton
public class LaTeXPathEntityReferenceSerializer extends FSPathStringEntityReferenceSerializer
{
    private static final char[] RESERVED_CHARS = new char[] { '\\', '{', '}', '#', '$', '%', '&', '^', '_', '~' };

    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        String cleanedName = currentReference.getName();

        // Start by removing spaces since the would be URL-encoded into "+" which would get again encoded into "%xx"
        // which we don't want since % is a reserved LaTeX char
        cleanedName = StringUtils.remove(cleanedName, " ");

        // First pass to remove reserved LaTeX chars
        for (int i = 0; i < RESERVED_CHARS.length; i++) {
            cleanedName = StringUtils.remove(cleanedName, RESERVED_CHARS[i]);
        }

        // Encode the reference name since they can contain non-ASCII chars which will lead to introducing % characters
        // which are reserved chars for LaTeX and which will thus be removed in the next step below.
        cleanedName = encodeReferenceName(cleanedName, isLastReference);

        // Second pass to remove % characters
        cleanedName = StringUtils.remove(cleanedName, RESERVED_CHARS[5]);

        // Add a hash to handle the issue of having an already existing file with the same name as the path with the
        // special chars removed.
        if (isLastReference) {
            // Add the hashcode before any extension
            int pos = StringUtils.lastIndexOf(cleanedName, '.');
            String suffix = "-" + Math.abs(currentReference.getName().hashCode());
            if (pos > -1) {
                cleanedName = StringUtils.substring(cleanedName, 0, pos) + suffix
                    + StringUtils.substring(cleanedName, pos);
            } else {
                cleanedName = cleanedName + suffix;
            }
        }

        super.serializeEntityReference(
            new EntityReference(cleanedName, currentReference.getType(), currentReference.getParent()), representation,
            isLastReference, parameters);
    }
}
