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
import java.io.FileOutputStream;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Export a document in LaTeX.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultLaTeXExporter extends AbstractLaTeXExporter
{
    @Override
    protected File performExport(DocumentReference documentReference, Map<String, Object> exportOptions,
        XWikiContext xcontext) throws Exception
    {
        File outputDir = generateTemporaryDirectory();
        File latexZip = new File(outputDir, ZIPFILENAME);
        try (FileOutputStream fos = new FileOutputStream(latexZip)) {
            exportOptions.put(TARGET_PROPERTY, new DefaultOutputStreamOutputTarget(fos, true));
            performExportInternal(documentReference, exportOptions, xcontext);
        }
        return latexZip;
    }
}
