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

import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.text.StringUtils;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.tools.EscapeTool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockSetup
{
    public static void setUp(MockitoComponentManager componentManager) throws Exception
    {
        VelocityManager velocityManager = componentManager.registerMockComponent(VelocityManager.class);
        VelocityContext vcontext = new VelocityContext();
        vcontext.put("escapetool", new EscapeTool());
        vcontext.put("stringtool", new StringUtils());
        when(velocityManager.getCurrentVelocityContext()).thenReturn(vcontext);

        org.apache.velocity.app.VelocityEngine velocityEngine = new org.apache.velocity.app.VelocityEngine();
        velocityEngine.init();
        VelocityEngine xwikiVelocityEngine = mock(VelocityEngine.class);
        when(velocityManager.getVelocityEngine()).thenReturn(xwikiVelocityEngine);
        when(xwikiVelocityEngine.evaluate(any(Context.class), any(Writer.class), any(String.class),
            any(String.class))).thenAnswer(new Answer<Object>()
        {
            @Override public Object answer(InvocationOnMock invocation)
            {
                Context context = invocation.getArgument(0);
                Writer writer = invocation.getArgument(1);
                String templateName = invocation.getArgument(2);
                String source = invocation.getArgument(3);
                velocityEngine.evaluate(context, writer, templateName, source);
                return null;
            }
        });
    }
}
