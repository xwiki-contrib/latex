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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.script.ScriptContext;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.latex.internal.LaTeXBlockRenderer;
import org.xwiki.contrib.latex.output.LaTeXOutputProperties;
import org.xwiki.filter.FilterDescriptorManager;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.output.AbstractBeanOutputFilterStream;
import org.xwiki.filter.output.OutputStreamOutputTarget;
import org.xwiki.filter.output.OutputTarget;
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

    /**
     * Object put in the {@code latex} binding and representing the current document (of type {@code Document} class)
     */
    private static final String LATEX_BINDING_DOC = "doc";

    /**
     * Binding in the Script Context under which to find the current document.
     */
    private static final String SC_BINDING_DOC = LATEX_BINDING_DOC;

    @Inject
    private FilterDescriptorManager filterManager;

    @Inject
    private ConverterListener converterListener;

    @Inject
    @Named("fspath")
    private EntityReferenceSerializer<String> fsPathSerializer;

    @Inject
    @Named(LaTeXBlockRenderer.ROLEHINT)
    private BlockRenderer renderer;

    @Inject
    private IndexSerializer indexSerializer;

    @Inject
    private ScriptContextManager scriptContextManager;

    private boolean contextInitialized;

    private Object previousLatexBinding;

    private int previousLatexScope;

    private WrappingListener contentListener = new WrappingListener();

    // FIXME: refactor the latex renderer to support events instead of XDOM
    private XDOMGeneratorListener xdomGenerator;

    private EntityReference currentReference;

    private ZipArchiveOutputStream zipStream;

    private Set<String> includes = new LinkedHashSet<>();

    private ZipArchiveOutputStream getZipStream() throws FilterException
    {
        if (this.zipStream == null) {
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

        return this.zipStream;
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
        checkPushContext();

        ZipArchiveEntry entry = new ZipArchiveEntry("index.tex");

        try {
            this.zipStream.putArchiveEntry(entry);

            this.indexSerializer.serialize(this.zipStream);
        } finally {
            this.zipStream.closeArchiveEntry();
        }

        this.zipStream.close();

        this.properties.getTarget().close();

        popContext();
    }

    @Override
    protected Object createFilter()
    {
        // Inject listener for the document content events
        return this.filterManager.createCompositeFilter(this.contentListener, this);
    }

    private void begin() throws FilterException
    {
        if (this.xdomGenerator == null) {
            this.xdomGenerator = new XDOMGeneratorListener();

            this.converterListener.initialize(this.properties, getZipStream());
            this.converterListener.setWrappedListener(this.xdomGenerator);

            this.contentListener.setWrappedListener(this.converterListener);
        }

        this.converterListener.setCurrentReference(this.currentReference);
    }

    private void end() throws IOException
    {
        if (this.xdomGenerator != null) {
            checkPushContext();

            // Provide current document in the latex binding in the Script Context so that it's available to templates
            ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();
            Map<String, Object> latex = (Map<String, Object>) scriptContext.getAttribute(LATEX_BINDING);
            Object currentDoc = scriptContext.getAttribute(SC_BINDING_DOC);
            if (latex != null && currentDoc != null) {
                latex.put(LATEX_BINDING_DOC, currentDoc);
            }

            XDOM xdom = this.xdomGenerator.getXDOM();
            this.xdomGenerator = null;

            DefaultWikiPrinter printer = new DefaultWikiPrinter();
            this.renderer.render(xdom, printer);

            String latexContent = printer.toString();

            String path = "pages/" + this.fsPathSerializer.serialize(this.currentReference);

            ZipArchiveEntry entry = new ZipArchiveEntry(path + ".tex");

            try {
                this.zipStream.putArchiveEntry(entry);

                IOUtils.write(latexContent, this.zipStream, StandardCharsets.UTF_8);
            } finally {
                this.zipStream.closeArchiveEntry();
            }

            this.includes.add(path);
        }
    }

    // events

    @Override
    public void beginWikiFarm(FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = null;
    }

    @Override
    public void endWikiFarm(FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = null;
    }

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = new WikiReference(name);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = null;
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws FilterException
    {
        this.currentReference = new EntityReference(name, EntityType.SPACE, this.currentReference);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws FilterException
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
            end();
        } catch (IOException e) {
            throw new FilterException(e);
        }

        this.currentReference = this.currentReference.getParent();
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        // Not supported
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws FilterException
    {
        // Not supported
    }

    @Override
    public void beginWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {
        // Not supported
    }

    @Override
    public void endWikiDocumentRevision(String revision, FilterEventParameters parameters) throws FilterException
    {
        // Not supported
    }
}
