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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.contrib.latex.internal.output.IndexSerializer;
import org.xwiki.contrib.latex.output.LaTeXOutputProperties;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for the Index generation.
 *
 * @version $Id$
 */
@AllComponents
public class IndexGenerationTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @Before
    public void setUp() throws Exception
    {
        this.componentManager.registerMockComponent(ExecutionContextManager.class);
        MockSetup.setUp(this.componentManager);
    }

    @Test
    public void generateIndexWithTitleAndAuthor() throws Exception
    {
        LaTeXOutputProperties properties = new LaTeXOutputProperties();
        properties.setTitle("Title");
        properties.setAuthor("Author");

        String expected = "\\pagenumbering{Roman}\n"
            + "\\title{Title}\n"
            + "\\author{Author}\n"
            + "\\maketitle\n"
            + "\\setcounter{tocdepth}{3}\n"
            + "\\tableofcontents\n"
            + "\\clearpage\n"
            + "\\setcounter{page}{0}\n"
            + "\\pagenumbering{arabic}\n";

        assertIndex(properties, expected);
    }

    @Test
    public void generateIndexWithNoCoverPage() throws Exception
    {
        LaTeXOutputProperties properties = new LaTeXOutputProperties();
        properties.setCoverPage(false);

        String expected = "\\setcounter{tocdepth}{3}\n"
            + "\\tableofcontents\n"
            + "\\clearpage\n";

        assertIndex(properties, expected);
    }

    @Test
    public void generateIndexWithNoCoverPageAndNoTOC() throws Exception
    {
        LaTeXOutputProperties properties = new LaTeXOutputProperties();
        properties.setCoverPage(false);
        properties.setToc(false);

        String expected = "";

        assertIndex(properties, expected);
    }

    @Test
    public void generateIndexWithTitleAndSubtitle() throws Exception
    {
        LaTeXOutputProperties properties = new LaTeXOutputProperties();
        properties.setTitle("Title");
        properties.setSubtitle("Subtitle");

        String expected = "\\pagenumbering{Roman}\n"
            + "\\title{%\n"
            + "  Title\\\\\n"
            + "  \\large Subtitle}\n"
            + "\\maketitle\n"
            + "\\setcounter{tocdepth}{3}\n"
            + "\\tableofcontents\n"
            + "\\clearpage\n"
            + "\\setcounter{page}{0}\n"
            + "\\pagenumbering{arabic}\n";

        assertIndex(properties, expected);
    }

    @Test
    public void generateIndexWithListOfFiguresAndTables() throws Exception
    {
        LaTeXOutputProperties properties = new LaTeXOutputProperties();
        properties.setListOfFigures(true);
        properties.setListOfTables(true);

        String expected = "\\pagenumbering{Roman}\n"
            + "\\setcounter{tocdepth}{3}\n"
            + "\\tableofcontents\n"
            + "\\listoffigures\n"
            + "\\listoftables\n"
            + "\\clearpage\n"
            + "\\setcounter{page}{0}\n"
            + "\\pagenumbering{arabic}\n";

        assertIndex(properties, expected);
    }

    @Test
    public void generateIndexWithBookDocumentClass() throws Exception
    {
        LaTeXOutputProperties properties = new LaTeXOutputProperties();
        properties.setDocumentClass("book");

        String expected = "\\documentclass{book}\n"
            + "\\usepackage{standalone}\n"
            + "\n"
            + "********** Preamble **********\n"
            + "\n"
            + "\\begin{document}\n"
            + "\n"
            + "\\pagenumbering{Roman}\n"
            + "\\setcounter{tocdepth}{3}\n"
            + "\\tableofcontents\n"
            + "\\clearpage\n"
            + "\\setcounter{page}{0}\n"
            + "\\pagenumbering{arabic}\n"
            + "\n"
            + "\\end{document}\n";

        assertFullIndex(properties, expected);
    }

    @Test
    public void generateIndexWithTitleAndAuthorAndDate() throws Exception
    {
        LaTeXOutputProperties properties = new LaTeXOutputProperties();
        properties.setTitle("Title");
        properties.setAuthor("Author");
        properties.setDate(new DateTime(2018, 3, 14, 0, 0).toDate());

        String expected = "\\documentclass{article}\n"
            + "\\usepackage{standalone}\n"
            + "\n"
            + "********** Preamble **********\n"
            + "\n"
            + "% Used to format the date for the cover page\n"
            + "\\usepackage[useregional]{datetime2}\n"
            + "\n"
            + "\\begin{document}\n"
            + "\n"
            + "\\pagenumbering{Roman}\n"
            + "\\title{Title}\n"
            + "\\author{Author}\n"
            + "\\date{\\DTMdate{2018-03-14}}\n"
            + "\\maketitle\n"
            + "\\setcounter{tocdepth}{3}\n"
            + "\\tableofcontents\n"
            + "\\clearpage\n"
            + "\\setcounter{page}{0}\n"
            + "\\pagenumbering{arabic}\n"
            + "\n"
            + "\\end{document}\n";

        assertFullIndex(properties, expected);
    }

    private void assertIndex(LaTeXOutputProperties properties, String expected) throws Exception
    {
        String normalizedExpected = "\\documentclass{article}\n"
            + "\\usepackage{standalone}\n"
            + "\n"
            + "********** Preamble **********\n"
            + "\n"
            + "\\begin{document}\n"
            + "\n"
            + expected
            + "\n"
            + "\\end{document}\n";

        assertFullIndex(properties, normalizedExpected);
    }

    private void assertFullIndex(LaTeXOutputProperties properties, String expected) throws Exception
    {
        IndexSerializer serializer = this.componentManager.getInstance(IndexSerializer.class);
        ScriptContextManager scm = this.componentManager.getInstance(ScriptContextManager.class);
        ScriptContext scriptContext = scm.getCurrentScriptContext();
        Map<String, Object> latex = new HashMap<>();
        scriptContext.setAttribute("latex", latex, ScriptContext.ENGINE_SCOPE);
        latex.put("properties", properties);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(baos);

        assertEquals(expected, baos.toString());
    }
}
