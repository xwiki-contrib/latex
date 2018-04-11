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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.latex.internal.output.LaTeXPathEntityReferenceSerializer;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link LaTeXPathEntityReferenceSerializer}.
 *
 * @version $Id$
 */
public class LaTeXPathEntityReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<LaTeXPathEntityReferenceSerializer> mocker =
        new MockitoComponentMockingRule<>(LaTeXPathEntityReferenceSerializer.class);

    @Test
    public void serializeAttachment() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        AttachmentReference attachmentReference = new AttachmentReference("test.txt", documentReference);
        assertEquals("wiki/space/page/test-1147906284.txt", this.mocker.getComponentUnderTest().serialize(attachmentReference));
    }

    @Test
    public void serializeDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        assertEquals("wiki/space/page-3433103", this.mocker.getComponentUnderTest().serialize(documentReference));
    }

    @Test
    public void serializeDocumentWithReservedChar() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "pa___ge");
        assertEquals("wiki/space/page-811115956", this.mocker.getComponentUnderTest().serialize(documentReference));
    }
}
