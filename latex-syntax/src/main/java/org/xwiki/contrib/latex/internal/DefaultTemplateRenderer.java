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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

/**
 * Render LaTeX templates.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultTemplateRenderer implements TemplateRenderer
{
    @Inject
    private Logger logger;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private Execution execution;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private LaTeXTool latexTool;

    @Override
    public void render(Collection<Block> blocks, WikiPrinter printer)
    {
        try {
            // TODO: stream the template writer directly to the printer
            StringWriter writer = new StringWriter();
            TemplateProcessor processor = initializeProcessor(writer);
            processor.process(blocks);
            printer.print(writer.toString());
        } catch (Exception e) {
            this.logger.warn("Failed to render LaTeX templates. Reason [{}].", ExceptionUtils.getRootCauseMessage(e));
        } finally {
            this.execution.popContext();
        }
    }

    @Override
    public void render(String relativeTemplateName, WikiPrinter printer)
    {
        try {
            StringWriter writer = new StringWriter();
            TemplateProcessor processor = initializeProcessor(writer);
            processor.render(relativeTemplateName);
            printer.print(writer.toString());
        } catch (Exception e) {
            this.logger.warn("Failed to render LaTeX template [{}]. Reason [{}].", relativeTemplateName,
                ExceptionUtils.getRootCauseMessage(e));
        } finally {
            this.execution.popContext();
        }
    }

    private TemplateProcessor initializeProcessor(StringWriter writer) throws ExecutionContextException
    {
        // Push a new Execution Context for the template rendering.
        ExecutionContext context = new ExecutionContext();
        this.executionContextManager.initialize(context);
        this.execution.pushContext(context);

        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();
        TemplateProcessor processor = new TemplateProcessor(this.templateManager, scriptContext, writer);
        scriptContext.setAttribute("processor", processor, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("latex", this.latexTool, ScriptContext.ENGINE_SCOPE);
        return processor;
    }
}
