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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zip utility.
 *
 * @version $Id$
 * @since 1.11
 */
public final class ZipUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipUtils.class);

    private ZipUtils()
    {
        // Utility class
    }

    /**
     * @param path the path under with to add a zip entry in the zip
     * @param inputStream the content to add to the zip entry
     * @param zipStream the zip file to add to
     * @throws IOException in case of error
     */
    public static void store(String path, InputStream inputStream, ZipArchiveOutputStream zipStream) throws IOException
    {
        ZipArchiveEntry entry = new ZipArchiveEntry(path);
        try {
            zipStream.putArchiveEntry(entry);
            IOUtils.copy(inputStream, zipStream);
        } catch (IOException e) {
            LOGGER.error("Failed to store file at [{}]", path, e);
        } finally {
            zipStream.closeArchiveEntry();
        }
    }
}
