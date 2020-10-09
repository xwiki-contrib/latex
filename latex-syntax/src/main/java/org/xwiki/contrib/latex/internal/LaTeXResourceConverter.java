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

import java.io.OutputStream;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Converts resource references. See {@link #convert(ResourceReference, boolean)}.
 *
 * @version $Id$
 * @since 1.11
 */
@Role
public interface LaTeXResourceConverter
{
    /**
     * @param currentReference the current document reference, used to resolve references to absolute references
     * @param outputStream the stream to which copied resources should be written to
     */
    void initialize(EntityReference currentReference, OutputStream outputStream);

    /**
     * Convert a {@link ResourceReference}'s reference so that it has the right reference when serialized into the
     * LaTeX output. For example this can mean converting the reference of an image attachment reference into a
     * filesystem file reference (by copying the image located into an attachment of a wiki page to the file system).
     *
     * @param reference the resource reference to convert (a link reference or an image reference)
     * @param baseResourceReference the base reference of the current XDOM block, used to resolve the resource
     *        reference into an absolute reference. Can be relative, in which case the current reference is used to
     *        make it absolute. Can be null in which was the current reference is used.
     * @param forceDownload if true, then convert external references (like URL references) into local references. For
     *        examples image reference by URLs can be downloaded locally and the reference swapped for a local
     *        filesystem reference.
     * @return the converted reference
     */
    ResourceReference convert(ResourceReference reference, String baseResourceReference, boolean forceDownload);

    /**
     * Convert a {@link ResourceReference}'s reference so that it has the right reference when serialized into the
     * LaTeX output. For example this can mean converting the reference of an image attachment reference into a
     * filesystem file reference (by copying the image located into an attachment of a wiki page to the file system).
     *
     * @param reference the resource reference to convert (a link reference or an image reference)
     * @param forceDownload if true, then convert external references (like URL references) into local references. For
     *        examples image reference by URLs can be downloaded locally and the reference swapped for a local
     *        filesystem reference.
     * @return the converted reference
     */
    ResourceReference convert(ResourceReference reference, boolean forceDownload);

    /**
     * @param linkBlock the link resource to convert. The closest base reference from the passed link block will be
     *        used to resolve local references
     * @return the converted reference
     */
    ResourceReference convert(LinkBlock linkBlock);

    /**
     * @param imageBlock the image resource to convert. The closest base reference from the passed image block will be
     *        used to resolve local references
     * @return the converted reference
     */
    ResourceReference convert(ImageBlock imageBlock);
}
