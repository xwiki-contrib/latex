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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.internal.TemplateRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;

/**
 * Create the index file.
 * 
 * @version $Id$
 */
@Component(roles = IndexSerializer.class)
@Singleton
public class IndexSerializer
{
    /**
     * Script Context binding used to tell the Index template the list and location of pages that are being exported.
     */
    private static final String SC_INCLUDES_KEY = "latexPageIncludes";

    /**
     * Script Context binding used to tell templates what type of LaTeX document is being generated. Valid values
     * are values that are valid in LaTeX in the {@code \documentclass{... value here...}} command. For example:
     * {@code article} or {@code book}.
     */
    private static final String SC_DOCTYPE_KEY = "latexDocumentType";

    @Inject
    private TemplateRenderer templateRenderer;

    @Inject
    private ScriptContextManager scriptContextManager;

    /**
     * @param includes the references of the documents to include
     * @param stream the stream to write to
     * @throws IOException when failing to create the index file
     */
    public void serialize(Set<String> includes, OutputStream stream) throws IOException
    {
        // Add an includes binding in the ScriptContext (SC) so that they are accessible from the index template.
        // To be clean we save and restore any pre-existing property with the same name in the SC.
        // For performance reasons we don't initialize new Contexts and only remove the property we added.
        Object[] originalValues = new Object[2];
        Integer[] originalValueScopes = new Integer[2];
        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();
        try {
            originalValues[0] = scriptContext.getAttribute(SC_INCLUDES_KEY);
            originalValueScopes[0] = scriptContext.getAttributesScope(SC_INCLUDES_KEY);
            scriptContext.setAttribute(SC_INCLUDES_KEY, includes, ScriptContext.ENGINE_SCOPE);

            originalValues[1] = scriptContext.getAttribute(SC_DOCTYPE_KEY);
            originalValueScopes[1] = scriptContext.getAttributesScope(SC_DOCTYPE_KEY);
            scriptContext.setAttribute(SC_DOCTYPE_KEY, "article", ScriptContext.ENGINE_SCOPE);

            // Get and execute the index template
            WikiPrinter wikiprinter = new DefaultWikiPrinter();
            this.templateRenderer.render("Index", wikiprinter);
            writeln(stream, wikiprinter.toString());
        } finally {
            for (int i = 0; i < 2; i++) {
                if (originalValues[i] != null && originalValueScopes[i] != null) {
                    scriptContext.setAttribute(SC_INCLUDES_KEY, originalValues[i], originalValueScopes[i]);
                } else {
                    scriptContext.removeAttribute(SC_INCLUDES_KEY, ScriptContext.ENGINE_SCOPE);
                }
            }
        }
    }

    private void writeln(OutputStream stream, String txt) throws IOException
    {
        write(stream, txt, "\n");
    }

    private void write(OutputStream stream, String... strs) throws IOException
    {
        for (String str : strs) {
            IOUtils.write(str, stream, StandardCharsets.UTF_8);
        }
    }

}
