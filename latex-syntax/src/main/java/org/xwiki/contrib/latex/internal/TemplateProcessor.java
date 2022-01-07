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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

import static java.util.Collections.singletonMap;

/**
 * Locate and evaluate the LaTeX templates.
 *
 * @version $Id$
 * @since 1.0
 */
public class TemplateProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateProcessor.class);

    private static final String BLOCK = "block";

    private TemplateManager templateManager;

    private VelocityMacroFilter filter;

    private Map<String, Object> latexBinding;

    private UIExtensionManager uiExtensionManager;

    private BlockRenderer blockRenderer;

    /**
     * @param templateManager the template manager used to locate, get and execute template content
     * @param latexBinding the script context "latex" binding into which we can inject new "bindings" for the template
     *        evaluation
     * @param filter the Velocity filter to apply to the template content if the source is written in Velocity
     * @param uiExtensionManager the UI extension manager to resolve the {@link UIExtension}s for the templates
     * @param blockRenderer the block renderer used to render the {@link UIExtension}s for the templates
     */
    public TemplateProcessor(TemplateManager templateManager, Map<String, Object> latexBinding,
        VelocityMacroFilter filter, UIExtensionManager uiExtensionManager, BlockRenderer blockRenderer)
    {
        this.templateManager = templateManager;
        this.latexBinding = latexBinding;
        this.filter = filter;
        this.uiExtensionManager = uiExtensionManager;
        this.blockRenderer = blockRenderer;
    }

    /**
     * Evaluate the passed Blocks by finding matching LaTeX templates and executing Velocity on them.
     *
     * @param blocks the rendering blocks to evaluate
     * @return the result of processing all blocks
     */
    public String process(Collection<Block> blocks)
    {
        StringWriter writer = new StringWriter();
        for (Block block : blocks) {
            Block currentBlock = (Block) this.latexBinding.get(BLOCK);
            try {
                this.latexBinding.put(BLOCK, block);
                String templateName = getTemplateName(block);
                renderUIXs(templateName, "before").ifPresent(writer::write);
                Template template = getTemplate(templateName);
                if (template != null) {
                    writer.write(render(template));
                } else {
                    // Ignore the template and render children
                    writer.write(process(block.getChildren()));
                }
                renderUIXs(templateName, "after").ifPresent(writer::write);
            } catch (Exception e) {
                LOGGER.warn("Failed to evaluate template for Block [{}]. Reason [{}]. Skipping template",
                    block.getClass().getName(), ExceptionUtils.getRootCauseMessage(e));
            } finally {
                // Put back the block from the latex binding context
                if (currentBlock == null) {
                    this.latexBinding.remove(BLOCK);
                } else {
                    this.latexBinding.put(BLOCK, currentBlock);
                }
            }
        }
        return writer.toString();
    }

    /**
     * @param relativeTemplateName the template to locate and load (relative to {@code latex/})
     * @return the corresponding template
     * @throws Exception if the template cannot be found
     */
    public Template getTemplate(String relativeTemplateName) throws Exception
    {
        LOGGER.debug("Loading template [{}]", relativeTemplateName);
        String fullTemplateName = String.format("latex/%s", relativeTemplateName);
        Template template = this.templateManager.getTemplate(fullTemplateName);
        if (template == null) {
            // Try to find a default template.
            fullTemplateName = String.format("latex/default/%s", relativeTemplateName);
            template = this.templateManager.getTemplate(fullTemplateName);
        }
        if (template != null) {
            template = new ModifiableTemplate(template, this.filter);
        }
        return template;
    }

    /**
     * @param template the template to render
     * @return the result of the Template execution
     * @throws Exception if the template fails to render
     */
    public String render(Template template) throws Exception
    {
        if (template != null) {
            StringWriter writer = new StringWriter();
            this.templateManager.render(template, writer);
            return writer.toString();
        } else {
            return "";
        }
    }

    /**
     * @param templateName the name of the template to render
     * @return the result of the Template execution
     * @throws Exception if the template fails to render
     */
    public String render(String templateName) throws Exception
    {
        return render(getTemplate(templateName));
    }

    private String getTemplateName(Block block)
    {
        // If there's a custom template defined in the Block parameter's, use it!
        String templateName = block.getParameter("latex-template");
        if (templateName == null) {
            templateName = block.getClass().getSimpleName();
        }
        return templateName;
    }

    private Optional<String> renderUIXs(String templateName, String suffix)
    {
        // The XDOM template case is particular, the after UIXP needs to be located before "\end{document}" which closes
        // the latex document. To do so, the UIXP of XDOM are directly integrated in the template in Velocity.
        Optional<String> result;
        if (!templateName.equals("XDOM")) {
            // Get the extensions sorted by their "order" parameter.
            List<UIExtension> extensions =
                this.uiExtensionManager.getExtensions(
                    String.format("org.xwiki.contrib.latex.%s.%s", templateName, suffix),
                    singletonMap("sortByParameter", "order"));
            if (extensions.isEmpty()) {
                result = Optional.empty();
            } else {
                DefaultWikiPrinter printer = new DefaultWikiPrinter();
                for (UIExtension extension : extensions) {
                    this.blockRenderer.render(extension.execute(), printer);
                    printer.println("");
                }
                result = Optional.of(printer.toString());
            }
        } else {
            result = Optional.empty();
        }
        return result;
    }
}
