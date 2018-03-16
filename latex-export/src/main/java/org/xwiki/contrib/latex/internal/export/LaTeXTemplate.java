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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * Export a document in LaTeX.
 * 
 * @version $Id$
 * @since 1.5
 */
@Component(roles = LaTeXTemplate.class)
@Singleton
public class LaTeXTemplate
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * @param currentDocument the current document being exported
     * @throws Exception when failing to render the UI
     */
    public void render(DocumentReference currentDocument) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument xdocument = xcontext.getWiki().getDocument(currentDocument, xcontext);
        xcontext.setDoc(xdocument);

        Utils.parseTemplate("latex/export", xcontext);
    }
}
