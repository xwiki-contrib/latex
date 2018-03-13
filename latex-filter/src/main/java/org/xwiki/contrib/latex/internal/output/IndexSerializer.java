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
    private static final String SC_INCLUDES_KEY = "latexPageIncludes";

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
        Object originalValue = null;
        int originalValueScope = -1;
        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();
        try {
            originalValue = scriptContext.getAttribute(SC_INCLUDES_KEY);
            originalValueScope = scriptContext.getAttributesScope(SC_INCLUDES_KEY);
            scriptContext.setAttribute(SC_INCLUDES_KEY, includes, ScriptContext.ENGINE_SCOPE);

            // Get and execute the index template
            WikiPrinter wikiprinter = new DefaultWikiPrinter();
            this.templateRenderer.render("Index", wikiprinter);
            writeln(stream, wikiprinter.toString());
        } finally {
            if (originalValue != null && originalValueScope != -1) {
                scriptContext.setAttribute(SC_INCLUDES_KEY, originalValue, originalValueScope);
            } else {
                scriptContext.removeAttribute(SC_INCLUDES_KEY, ScriptContext.ENGINE_SCOPE);
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
