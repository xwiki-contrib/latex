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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
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
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
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
    static final String SC_LATEX = "latex";

    private static final String LATEX_BINDING_RESOURCE_CONVERTER = "resourceConverter";

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

    @Inject
    @Named("indent")
    private VelocityMacroFilter filter;

    @Inject
    private Provider<LaTeXResourceConverter> resourceConverterProvider;

    @Override
    public void render(Collection<Block> blocks, WikiPrinter printer)
    {
        try {
            TemplateProcessor processor = initializeProcessor();
            printer.print(processor.process(blocks));
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
            TemplateProcessor processor = initializeProcessor();
            printer.print(processor.render(relativeTemplateName));
        } catch (Exception e) {
            this.logger.warn("Failed to render LaTeX template [{}]. Reason [{}].", relativeTemplateName,
                ExceptionUtils.getRootCauseMessage(e));
        } finally {
            this.execution.popContext();
        }
    }

    private TemplateProcessor initializeProcessor() throws ExecutionContextException
    {
        // Push a new Execution Context for the template rendering. Note that we need to copy the "latex" binding if
        // it exists since this code can be called by the LaTeX exporter for example which sets config options in this
        // binding.
        ScriptContext currentScriptContext = this.scriptContextManager.getCurrentScriptContext();
        Map<String, Object> latexBinding = (Map<String, Object>) currentScriptContext.getAttribute(SC_LATEX);

        ExecutionContext context = new ExecutionContext();
        this.execution.pushContext(context);
        this.executionContextManager.initialize(context);

        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();

        // Create the "latex" binding if it doesn't exist so that we're sure to always have one (this can happen if
        // this code is called directly without going through the LaTeX exporter for example).
        if (latexBinding == null) {
            latexBinding = new HashMap<>();
        }
        scriptContext.setAttribute(SC_LATEX, latexBinding, ScriptContext.ENGINE_SCOPE);

        TemplateProcessor processor = new TemplateProcessor(this.templateManager, latexBinding, this.filter);
        latexBinding.put("processor", processor);
        latexBinding.put("tool", this.latexTool);

        // If there's no resource converter in the latex binding then set up a no op one so that there's always the
        // binding available (as it's used in the LaTeX templates).
        if (!latexBinding.containsKey(LATEX_BINDING_RESOURCE_CONVERTER)) {
            latexBinding.put(LATEX_BINDING_RESOURCE_CONVERTER, this.resourceConverterProvider.get());
        }

        // Note: we don't put the SP binding inside the latex binding since we want to keep the way we use it as short
        // as possible. For example in Velocity: $SP vs $latex.SP.
        scriptContext.setAttribute("SP", " ", ScriptContext.ENGINE_SCOPE);

        return processor;
    }
}
