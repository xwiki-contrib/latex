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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

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

/**
 * @version $Id: 29443af498e773a330ccb0420285b030967f40c8 $
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
public class LaTeXOutputFilterStream extends AbstractBeanOutputFilterStream<LaTeXOutputProperties>
    implements LaTeXOutputFilter
{
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

    @Override
    public void close() throws IOException
    {
        ZipArchiveEntry entry = new ZipArchiveEntry("index.tex");

        try {
            this.zipStream.putArchiveEntry(entry);

            writeln("\\documentclass{article}");
            writeln("\\usepackage[utf8]{inputenc}");
            writeln("");
            writeln("\\begin{document}");
            for (String include : this.includes) {
                // TODO: escape include
                write("\\include{", include, "}");
            }
            writeln("\\end{document}");
        } finally {
            this.zipStream.closeArchiveEntry();
        }

        this.properties.getTarget().close();
    }

    private void writeln(String txt) throws IOException
    {
        write(txt, "\n");
    }

    private void write(String... strs) throws IOException
    {
        for (String str : strs) {
            IOUtils.write(str, this.zipStream, StandardCharsets.UTF_8);
        }
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
    }

    private void end() throws IOException
    {
        if (this.xdomGenerator != null) {
            XDOM xdom = this.xdomGenerator.getXDOM();
            this.xdomGenerator = null;

            DefaultWikiPrinter printer = new DefaultWikiPrinter();
            this.renderer.render(xdom, printer);

            String latexContent = printer.toString();

            String path = this.fsPathSerializer.serialize(this.currentReference);

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
