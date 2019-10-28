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
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Locate and evaluate the LaTeX templates.
 *
 * @version $Id$
 * @since 1.0
 */
public class TemplateProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateProcessor.class);

    private TemplateManager templateManager;

    private VelocityMacroFilter filter;

    private Map<String, Object> latexBinding;

    /**
     * @param templateManager the template manager used to locate, get and execute template content
     * @param latexBinding the script context "latex" binding into which we can inject new "bindings" for the template
     *        evaluation
     * @param filter the Velocity filter to apply to the template content if the source is written in Velocity
     */
    public TemplateProcessor(TemplateManager templateManager, Map<String, Object> latexBinding,
        VelocityMacroFilter filter)
    {
        this.templateManager = templateManager;
        this.latexBinding = latexBinding;
        this.filter = filter;
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
            try {
                this.latexBinding.put("block", block);
                Template template = getTemplate(block);
                if (template != null) {
                    writer.write(render(template));
                } else {
                    // Ignore the template and render children
                    writer.write(process(block.getChildren()));
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to evaluate template for Block [{}]. Reason [{}]. Skipping template",
                    block.getClass().getName(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return writer.toString();
    }

    /**
     * @param block the block for which to find a template
     * @return the template for the passed block
     * @throws Exception if the template cannot be found
     */
    public Template getTemplate(Block block) throws Exception
    {
        // If there's a custom template defined in the Block parameter's, use it!
        String templateName = block.getParameter("latex-template");
        if (templateName == null) {
            templateName = block.getClass().getSimpleName();
        }
        return getTemplate(templateName);
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
}
