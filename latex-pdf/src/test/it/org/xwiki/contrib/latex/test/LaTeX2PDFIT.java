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
package org.xwiki.contrib.latex.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@link org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter}. Note that this test will connect to
 * the internet and use dockerhub to download the docker image used to perform the LaTeX to PDF conversion.
 *
 * @version $Id$
 */
@AllComponents
@ComponentTest
class LaTeX2PDFIT
{
    @InjectComponentManager
    private ComponentManager componentManager;

    @XWikiTempDir
    private File tmpDir;

    @Test
    void convert() throws Exception
    {
        FileUtils.copyDirectory(new File("src/test/resources/latex"), this.tmpDir);

        LaTeX2PDFConverter converter = this.componentManager.getInstance(LaTeX2PDFConverter.class);
        File pdfFile = converter.convert(this.tmpDir);
        assertEquals(new File(this.tmpDir, "index.pdf"), pdfFile);
        assertTrue(pdfFile.exists());
    }
}
