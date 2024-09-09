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
package org.xwiki.contrib.latex.internal.output;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.latex.internal.LaTeXBlockRenderer;
import org.xwiki.contrib.latex.internal.LaTeXResourceConverter;
import org.xwiki.contrib.latex.output.LaTeXOutputProperties;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.filter.output.OutputStreamOutputTarget;
import org.xwiki.filter.output.OutputTarget;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.XDOMGeneratorListener;
import org.xwiki.rendering.listener.WrappingListener;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.script.ScriptContextManager;

/**
 * @version $Id: 29443af498e773a330ccb0420285b030967f40c8 $
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
public class LaTeXOutputFilterStream extends AbstractBeanOutputFilterStream<LaTeXOutputProperties>
    implements LaTeXOutputFilter
{
    private static final String LATEX_BINDING = "latex";

    /**
     * Object put in the {@code latex} binding and used to tell the Index template the list and location of pages
     * that are being exported.
     */
    private static final String LATEX_BINDING_INCLUDES = "includes";

    /**
     * Object put in the {@code latex} binding and used to pass templates the filter properties.
     */
    private static final String LATEX_BINDING_PROPERTIES = "properties";

    private static final String LATEX_BINDING_RESOURCE_CONVERTER = "resourceConverter";

    @Inject
    private FilterDescriptorManager filterManager;

    @Inject
    @Named("latexpath")
    private EntityReferenceSerializer<String> latexPathSerializer;

    @Inject
    @Named(LaTeXBlockRenderer.ROLEHINT)
    private BlockRenderer renderer;

    @Inject
    private IndexGenerator indexGenerator;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Provider<LaTeXResourceConverter> resourceConverterProvider;

    @Inject
    private JobProgressManager progressManager;

    private boolean contextInitialized;

    private Object previousLatexBinding;

    private int previousLatexScope;

    private WrappingListener contentListener = new WrappingListener();

    // FIXME: refactor the latex renderer to support events instead of XDOM
    private XDOMGeneratorListener xdomGenerator;

    private EntityReference currentReference;

    private ZipArchiveOutputStream zipStream;

    private Set<String> includes = new LinkedHashSet<>();

    private void initializeZipStream() throws FilterException
    {
        OutputTarget target = this.properties.getTarget();
        if (target instanceof OutputStreamOutputTarget) {
            try {
                this.zipStream = new ZipArchiveOutputStream(((OutputStreamOutputTarget) target).getOutputStream());
            } catch (IOException e) {
                throw new FilterException("Failed to create zip output stream", e);
            }
        } else {
            throw new FilterException("Unsupported target [" + target.getClass() + "]");
        }
    }

    /**
     * Make sure the context contains the right information for the templates.
     */
    private void checkPushContext()
    {
        if (!this.contextInitialized) {
            ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();

            // Remember current "latex" binding
            this.previousLatexBinding = scriptContext.getAttribute(LATEX_BINDING);
            this.previousLatexScope = scriptContext.getAttributesScope(LATEX_BINDING);

            Map<String, Object> latex = new HashMap<>();

            // Provide filter properties
            latex.put(LATEX_BINDING_PROPERTIES, this.properties);

            // Provide list of documents
            latex.put(LATEX_BINDING_INCLUDES, this.includes);

            // Provide the resource reference converter
            LaTeXResourceConverter resourceConverter = this.resourceConverterProvider.get();
            resourceConverter.initialize(this.currentReference, this.zipStream);
            latex.put(LATEX_BINDING_RESOURCE_CONVERTER, resourceConverter);

            scriptContext.setAttribute(LATEX_BINDING, latex, ScriptContext.ENGINE_SCOPE);

            this.contextInitialized = true;
        }
    }

    /**
     * Restore to previous state of the context.
     */
    private void popContext()
    {
        if (this.contextInitialized) {
            ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();

            if (this.previousLatexScope != -1) {
                scriptContext.setAttribute(LATEX_BINDING, this.previousLatexBinding, this.previousLatexScope);
            } else {
                scriptContext.removeAttribute(LATEX_BINDING, ScriptContext.ENGINE_SCOPE);
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        this.zipStream.close();
        this.properties.getTarget().close();
    }

    private void generateIndex() throws IOException
    {
        this.progressManager.startStep(this, "Generate the LaTeX Index");

        String indexContent = this.indexGenerator.generate();
        try (InputStream inputStream = IOUtils.toInputStream(indexContent, "UTF-8")) {
            ZipUtils.store("index.tex", inputStream, this.zipStream);
        }

        popContext();

        this.progressManager.endStep(this);
    }

    @Override
    protected Object createFilter()
    {
        // Inject listener for the document content events
        return this.filterManager.createCompositeFilter(this.contentListener, this);
    }

    private void begin() throws FilterException
    {
        this.progressManager.pushLevelProgress(3, this);
        if (this.xdomGenerator == null) {
            this.xdomGenerator = new XDOMGeneratorListener();
            this.contentListener.setWrappedListener(this.xdomGenerator);
            initializeZipStream();
        }
        checkPushContext();
    }

    private void end() throws IOException
    {
        if (this.xdomGenerator != null) {
            this.progressManager.startStep(this, "Convert document to LaTeX");
            XDOM xdom = this.xdomGenerator.getXDOM();
            this.xdomGenerator = null;

            DefaultWikiPrinter printer = new DefaultWikiPrinter();
            this.renderer.render(xdom, printer);
            this.progressManager.endStep(this);

            this.progressManager.startStep(this, "Save LaTeX content in Zip");
            String latexContent = printer.toString();

            String path = "pages/" + this.latexPathSerializer.serialize(this.currentReference);

            ZipArchiveEntry entry = new ZipArchiveEntry(path + ".tex");

            try {
                this.zipStream.putArchiveEntry(entry);

                IOUtils.write(latexContent, this.zipStream, StandardCharsets.UTF_8);
            } finally {
                this.zipStream.closeArchiveEntry();
            }

            this.includes.add(path);

            this.progressManager.endStep(this);
        }
    }

    // events

    @Override
    public void beginWikiFarm(FilterEventParameters parameters)
    {
        this.currentReference = null;
    }

    @Override
    public void endWikiFarm(FilterEventParameters parameters)
    {
        this.currentReference = null;
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters)
    {
        this.currentReference = new WikiReference(name);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters)
    {
        this.currentReference = null;
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters)
    {
        this.currentReference = new EntityReference(name, EntityType.SPACE, this.currentReference);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters)
    {
        this.currentReference = this.currentReference.getParent();
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = new EntityReference(name, EntityType.DOCUMENT, this.currentReference);

        begin();
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        try {
            // Finish generating LaTeX content for the document.
            end();
            // Generate the index for the document.
            generateIndex();
        } catch (IOException e) {
            throw new FilterException(e);
        } finally {
            // Note: calling the pop here and not inside end() since generateIndex() will also add some steps.
            this.progressManager.popLevelProgress(this);
        }

        this.currentReference = this.currentReference.getParent();
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters)
    {
        // Not supported
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters)
    {
        // Not supported
    }

    @Override
    public void beginWikiDocumentRevision(String revision, FilterEventParameters parameters)
    {
        // Not supported
    }

    @Override
    public void endWikiDocumentRevision(String revision, FilterEventParameters parameters)
    {
        // Not supported
    }
}
