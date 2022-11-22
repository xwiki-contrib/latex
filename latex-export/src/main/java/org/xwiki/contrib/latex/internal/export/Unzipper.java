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

import java.io.File;

import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

/**
 * Unzip a zip fil.
 *
 * @version $Id$
 * @since 1.10
 */
public final class Unzipper
{
    private Unzipper()
    {
        // Private voluntarily since this is a utility class.
    }

    /**
     * @param source the zip file to unzip
     * @param targetDirectory the directory in which to unzip
     * @throws Exception when an error occurs during the unzip
     */
    public static void unzip(File source, File targetDirectory) throws Exception
    {
        createDirectory(targetDirectory);
        try {
            // Note: starting with v4.4.0 Plexus Archiver moved to SLF4J and thus all unzipping logs will go through
            // XWiki SLF4J configuration. XWiki upgraded from v4.3.0 to 4.4.0 in XWiki 14.6RC1. This means that when
            // using this extension on XWiki < 14.6RC1 there'll be no logs by default. However, that's not a big problem
            // since an exception will be thrown with details.
            ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(targetDirectory);
            unArchiver.setOverwrite(true);
            unArchiver.extract();
        } catch (Exception e) {
            throw new Exception(
                String.format("Error unpacking file [%s] into [%s]", source, targetDirectory), e);
        }
    }

    /**
     * @param directory the directory to create. Works even if the directory already exists
     */
    public static void createDirectory(File directory)
    {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}
