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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.xwiki.extension.test.ExtensionPackager;
import org.xwiki.filter.test.integration.FilterTestSuite;
import org.xwiki.filter.test.integration.FilterTestSuite.Initialized;
import org.xwiki.filter.test.integration.FilterTestSuite.Scope;
import org.xwiki.filter.test.internal.FileAssert;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;

/**
 * Run all tests found in the classpath. These {@code *.test} files must follow the conventions described in
 * {@link org.xwiki.filter.test.integration.TestDataParser}.
 * 
 * @version $Id: e40aa21bca0d81646e776d761d5b692d3b7d8b3b $
 */
@RunWith(FilterTestSuite.class)
@AllComponents
@Scope(value = "latex"/* , pattern = "test1.test" */)
public class LaTexIntegrationTests
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        File folder = new File("target/test-" + new Date().getTime()).getAbsoluteFile();
        ExtensionPackager extensionPackager = new ExtensionPackager(null, folder);
        extensionPackager.generateExtensions();

        System.setProperty("extension.repository", folder.getAbsolutePath());

        // Make sure tex files are compared as text
        FileAssert.setStringComparator("tex");
    }

    public MockitoOldcore oldcore;

    @Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        MockSetup.setUp(componentManager);
        this.oldcore = new MockitoOldcore(componentManager);
    }

    @Before
    public void before() throws Exception
    {
        this.oldcore.before(this.getClass());

        XWikiDocument document =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "document"));

        document.setAttachment("attachment.txt", new ByteArrayInputStream(new byte[] { '1', '2', '3', '4' }),
            this.oldcore.getXWikiContext());
        document.setAttachment("image.png", new ByteArrayInputStream(new byte[] { '1', '2', '3', '4' }),
            this.oldcore.getXWikiContext());

        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        XWikiDocument otherdocument = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "otherspace", "otherdocument"));

        otherdocument.setAttachment("otherattachment.txt", new ByteArrayInputStream(new byte[] { '1', '2', '3', '4' }),
            this.oldcore.getXWikiContext());
        otherdocument.setAttachment("otherimage.png", new ByteArrayInputStream(new byte[] { '1', '2', '3', '4' }),
            this.oldcore.getXWikiContext());

        this.oldcore.getSpyXWiki().saveDocument(otherdocument, this.oldcore.getXWikiContext());

        this.oldcore.getMocker().registerMockComponent(WikiModel.class);

        this.oldcore.getXWikiContext().setURL(new URL("https", "test", ""));
        XWikiServletRequestStub initialRequest = new XWikiServletRequestStub();
        initialRequest.setHost("test");
        initialRequest.setScheme("http");
        XWikiServletRequest request = new XWikiServletRequest(initialRequest);
        this.oldcore.getXWikiContext().setRequest(request);
        XWikiServletURLFactory urlFactory = new XWikiServletURLFactory(this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setURLFactory(urlFactory);

        XWikiServletResponseStub initialResponse = new XWikiServletResponseStub();
        XWikiServletResponse response = new XWikiServletResponse(initialResponse);
        this.oldcore.getXWikiContext().setResponse(response);
    }

    @After
    public void after() throws Exception
    {
        this.oldcore.after();
    }
}
