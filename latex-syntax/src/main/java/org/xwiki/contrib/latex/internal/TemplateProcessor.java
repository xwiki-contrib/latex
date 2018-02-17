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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.block.Block;
import org.xwiki.velocity.VelocityEngine;

public class TemplateProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateProcessor.class);

    private VelocityEngine engine;

    private VelocityContext vcontext;

    private Writer writer;

    private Map<String, String> templateCache = new HashMap<>();

    private LaTeXConfiguration configuration;

    public TemplateProcessor(VelocityEngine engine, VelocityContext vcontext, Writer writer,
        LaTeXConfiguration configuration)
    {
        this.engine = engine;
        this.vcontext = vcontext;
        this.writer = writer;
        this.configuration = configuration;
    }

    public void process(Collection<Block> blocks)
    {
        this.vcontext.put("blocks", blocks);
        for (Block block : blocks) {
            try {
                this.vcontext.put("block", block);
                String template = getTemplate(block);
                if (template != null) {
                    this.engine.evaluate(this.vcontext, this.writer, "LaTeX", template);
                } else {
                    // Ignore the template and render children
                    process(block.getChildren());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to evaluate template for Block [{}]. Reason [{}]. Skipping template",
                    block.getClass().getName(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private String getTemplate(Block block)
    {
        String key = block.getClass().getName();
        LOGGER.info("Loading template for block [{}]", key);
        String result = this.templateCache.get(key);
        if (result == null) {
            try {
                result = this.configuration.getTemplate(block);
            } catch (IOException e) {
                // Failed to load template, return null to tell to skip template evaluation
                LOGGER.warn("Failed to load LaTeX template for block [{}]. Skipping template. Root cause: [{}]",
                    key, ExceptionUtils.getRootCauseMessage(e));
                result = null;
            }
            this.templateCache.put(key, result);
        }
        return result;
    }
}
