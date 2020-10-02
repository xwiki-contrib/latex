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

import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.output.DefaultOutputStreamOutputTarget;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiResponse;

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
    protected String getOutputFileName(String outputFileNamePrefix, XWikiContext xcontext)
    {
        return getLaTeXZipFileName(outputFileNamePrefix);
    }

    @Override
    protected void setResponseContentType(String outputFileName, XWikiResponse response, XWikiContext xcontext)
    {
        String contentDisposition;
        response.setContentType("application/zip");
        // Force download since it's a zip (the browser cannot display it inline)
        contentDisposition = String.format("attachment; filename=%s", outputFileName);
        response.setHeader("Content-Disposition", contentDisposition);
    }

    @Override
    protected void performExport(String name, DocumentReference documentReference, XDOM xdom,
        Map<String, Object> properties, XWikiResponse response, XWikiContext xcontext) throws Exception
    {
        properties.put(TARGET_PROPERTY, new DefaultOutputStreamOutputTarget(response.getOutputStream(), true));
        performExport(documentReference, xdom, properties);
    }
}
