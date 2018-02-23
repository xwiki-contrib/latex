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

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.internal.LaTeXTool;
import org.xwiki.contrib.latex.internal.TemplateRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Serialize the index file.
 * 
 * @version $Id$
 */
@Component(roles = IndexSerializer.class)
@Singleton
public class IndexSerializer
{
    @Inject
    private TemplateRenderer templateRenderer;

    @Inject
    private LaTeXTool latexTool;

    /**
     * @param includes the references of the documents to include
     * @param stream the stream to write to
     * @throws IOException when failing to write in the stream
     */
    public void serialize(Set<String> includes, OutputStream stream) throws IOException
    {
        writeln(stream, "\\documentclass{article}");
        writeln(stream, "\\usepackage[utf8]{inputenc}");
        writeln(stream, "\\usepackage{standalone}");

        WikiPrinter wikiprinter = new DefaultWikiPrinter();
        this.templateRenderer.render("Preamble", wikiprinter);
        writeln(stream, wikiprinter.toString());

        writeln(stream, "");
        writeln(stream, "\\begin{document}");

        if (!includes.isEmpty()) {
            for (String include : includes) {
                write(stream, "\\include{", this.latexTool.escape(include), "}");
            }
            writeln(stream, "");
        }

        writeln(stream, "\\end{document}");
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
