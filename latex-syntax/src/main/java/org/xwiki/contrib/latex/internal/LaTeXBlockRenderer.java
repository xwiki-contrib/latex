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
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

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
    private TemplateManager templateManager;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private Execution execution;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public void render(Block block, WikiPrinter printer)
    {
        render(Collections.singletonList(block), printer);
    }

    @Override
    public void render(Collection<Block> blocks, WikiPrinter printer)
    {
        try {
            // Push a new Execution Context for the template rendering.
            ExecutionContext context = new ExecutionContext();
            this.executionContextManager.initialize(context);

            // TODO: stream the template writer directly to the printer
            StringWriter writer = new StringWriter();
            ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();
            TemplateProcessor processor = new TemplateProcessor(this.templateManager, scriptContext, writer);
            scriptContext.setAttribute("processor", processor, ScriptContext.ENGINE_SCOPE);
            processor.process(blocks);
            printer.print(writer.toString());
        } catch (Exception e) {
            this.logger.warn("Failed to render LaTeX templates. Reason [{}].", ExceptionUtils.getRootCauseMessage(e));
        } finally {
            this.execution.popContext();
        }
    }
}
