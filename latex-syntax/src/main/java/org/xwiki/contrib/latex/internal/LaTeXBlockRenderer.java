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

import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Generates LaTeX syntax from a {@link org.xwiki.rendering.block.XDOM}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(LaTeXBlockRenderer.ROLEHINT)
@Singleton
public class LaTeXBlockRenderer implements BlockRenderer
{
    /**
     * The role hint of the component.
     */
    public static final String ROLEHINT = "latex/1.0";

    @Inject
    private Logger logger;

    @Inject
    private LaTeXConfiguration configuration;

    @Inject
    private VelocityManager velocityManager;

    @Override
    public void render(Block block, WikiPrinter printer)
    {
        render(Collections.singletonList(block), printer);
    }

    @Override
    public void render(Collection<Block> blocks, WikiPrinter printer)
    {
        // Prepare Velocity Context and Engine to execute the LaTeX templates.
        VelocityContext vcontext = this.velocityManager.getCurrentVelocityContext();

        // Convenience to explicitly set a space
        vcontext.put("SP", " ");

        try {
            VelocityEngine engine = this.velocityManager.getVelocityEngine();
            // TODO: stream the velocity writer directly to the printer
            StringWriter writer = new StringWriter();
            TemplateProcessor processor = new TemplateProcessor(engine, vcontext, writer, this.configuration);
            vcontext.put("processor", processor);
            processor.process(blocks);
            printer.print(writer.toString());
        } catch (XWikiVelocityException e) {
            this.logger.warn("Failed to render LaTeX templates. Reason [{}].", ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
