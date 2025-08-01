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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.latex.internal.LaTeXResourceConverter;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Converts resource references. See {@link #convert(ResourceReference, boolean)}.
 *
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultLaTeXResourceConverter implements LaTeXResourceConverter
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceResolver<ResourceReference> resolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentDocumentResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentStringDocumentResolver;

    @Inject
    @Named("latexpath")
    private EntityReferenceSerializer<String> latexPathSerializer;

    @Inject
    private DocumentAccessBridge bridge;

    private Set<String> stored = new HashSet<>();

    private ZipArchiveOutputStream zipStream;

    private EntityReference currentEntityReference;

    private DocumentReference currentDocumentReference;

    @Override
    public void initialize(EntityReference currentEntityReference, OutputStream outputStream)
    {
        this.currentEntityReference = currentEntityReference;
        this.currentDocumentReference = null;
        this.zipStream = (ZipArchiveOutputStream) outputStream;
    }

    @Override
    public ResourceReference convert(ResourceReference reference, boolean forceDownload)
    {
        return convert(reference, null, forceDownload);
    }

    @Override
    public ResourceReference convert(LinkBlock linkBlock)
    {
        return convert(linkBlock.getReference(), getBaseReference(linkBlock), false);
    }

    @Override
    public ResourceReference convert(ImageBlock imageBlock)
    {
        return convert(imageBlock.getReference(), getBaseReference(imageBlock), true);
    }

    @Override
    public ResourceReference convert(ResourceReference reference, String baseResourceReference,
        boolean forceDownload)
    {
        ResourceReference convertedReference = reference;
        EntityReference resolvedBaseReference = getResolvedBaseReference(baseResourceReference);
        try {
            // TODO: make all this extensible instead of if/else
            if (ResourceType.ATTACHMENT.equals(reference.getType())) {
                convertedReference = convertATTACHMENTReference(convertedReference, resolvedBaseReference);
            } else if (ResourceType.DATA.equals(reference.getType())) {
                convertedReference = convertDATAReference(convertedReference);
            } else if (ResourceType.URL.equals(reference.getType())) {
                convertedReference = convertURLReference(convertedReference, forceDownload);
            } else if (ResourceType.DOCUMENT.equals(reference.getType())) {
                convertedReference = convertDOCUMENTReference(convertedReference, resolvedBaseReference);
            } else if (ResourceType.PATH.equals(reference.getType())) {
                convertedReference = convertPATHReference(convertedReference);
            }
        } catch (Exception e) {
            this.logger.error(
                "Unexpected error when trying to convert resource reference [{}] using base reference [{}]",
                reference, resolvedBaseReference, e);
        }
        return convertedReference;
    }

    @Override
    public void store(String path, InputStream inputStream) throws IOException
    {
        ZipUtils.store(path, inputStream, this.zipStream);
        this.stored.add(path);
    }

    private ResourceReference convertPATHReference(ResourceReference reference)
    {
        // If the path doesn't start with "/" then consider that there's nothing to do: it's either already a URL
        // or it's something weird that we can't convert.
        String path = reference.getReference();
        if (!path.startsWith("/")) {
            return reference;
        }

        // Convert to an absolute URL.
        ResourceReference convertedResourceReference;
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            URL serverURL = xcontext.getWiki().getServerURL(
                this.currentEntityReference.extractReference(EntityType.WIKI).getName(), xcontext);
            convertedResourceReference = toURLReference(reference, new URL(serverURL, path).toString());
        } catch (MalformedURLException e) {
            // Error, log a warning and fall back to not doing any conversion
            this.logger.warn("Failed to convert path [{}] to a URL. Root error: [{}]", path,
                ExceptionUtils.getRootCauseMessage(e));
            convertedResourceReference = reference;
        }

        return convertedResourceReference;
    }

    private ResourceReference convertATTACHMENTReference(ResourceReference reference, EntityReference baseReference)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("files/attachments/");

        // Resolve the reference to have an absolute reference
        AttachmentReference attachmentReference =
            (AttachmentReference) this.resolver.resolve(reference, EntityType.ATTACHMENT, baseReference);
        // Get the matching attachment from the list of attachments in the target doc.
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiAttachment attachment = getAttachment(attachmentReference, xcontext);
        if (attachment != null) {
            // Create a new attachment reference since the user could have used an attachment reference file name
            // without a suffix and the XWikiAttachment object would have found the first attachment matching the
            // name and thus contains the full file name.
            attachmentReference = new AttachmentReference(attachment.getFilename(),
                attachmentReference.getDocumentReference());
        } else {
            this.logger.warn("Can't find attachment with reference [{}]", attachmentReference);
        }

        String normalizedPath = this.latexPathSerializer.serialize(attachmentReference);
        builder.append(normalizedPath);

        String path = builder.toString();

        ResourceReference convertedReference = toReference(reference, path);

        // Store attachment content
        if (attachment != null && !this.stored.contains(path)) {
            try (InputStream inputStream = attachment.getContentInputStream(xcontext)) {
                store(path, inputStream);
            } catch (Exception e) {
                this.logger.error("Failed to store attachment [{}]", attachmentReference, e);
            }
        }

        return convertedReference;
    }

    private XWikiAttachment getAttachment(AttachmentReference attachmentReference, XWikiContext xcontext)
    {
        XWikiAttachment result;
        try {
            XWikiDocument document =
                xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
            result = document.getAttachment(attachmentReference.getName());
        } catch (XWikiException e) {
            this.logger.error("Failed to find attachment [{}]", attachmentReference, e);
            result = null;
        }
        return result;
    }

    private ResourceReference convertDATAReference(ResourceReference reference)
    {
        // TODO

        return reference;
    }

    private ResourceReference convertURLReference(ResourceReference reference, boolean forceDownload)
    {
        ResourceReference convertedReference = reference;

        if (forceDownload) {
            // Create a local path for the URL resource

            // Download the file and store it in the zip
            try {
                URL url = new URL(reference.getReference());

                String filename = FilenameUtils.getName(url.toString());
                // Limit collision with same filename and domain but different URL
                String path =
                    "files/downloaded/" + url.getHost() + '/' + reference.getReference().hashCode() + '/' + filename;

                // Convert the reference
                convertedReference = toReference(reference, path);

                if (!this.stored.contains(path)) {
                    try (InputStream stream = url.openStream()) {
                        store(path, stream);
                    }
                }
            } catch (Exception e) {
                this.logger.error("Failed to download file with URL [{}]", reference.getReference(), e);
            }
        }

        return convertedReference;
    }

    private ResourceReference convertDOCUMENTReference(ResourceReference reference, EntityReference baseReference)
    {
        ResourceReference convertedReference = reference;

        if (StringUtils.isNotEmpty(reference.getReference())) {
            DocumentReference documentReference =
                (DocumentReference) this.resolver.resolve(reference, EntityType.DOCUMENT, baseReference);

            if (isCurrentDocument(documentReference)) {
                convertedReference = new DocumentResourceReference("");
                convertedReference.setParameters(reference.getParameters());
            } else {
                // External URL
                String url = this.bridge.getDocumentURL(new DocumentReference(documentReference), "view",
                    reference.getParameter(DocumentResourceReference.QUERY_STRING),
                    reference.getParameter(DocumentResourceReference.ANCHOR), true);

                convertedReference = new ResourceReference(url, ResourceType.URL);
            }
        }

        return convertedReference;
    }

    private DocumentReference getCurrentDocumentReference()
    {
        if (this.currentDocumentReference == null) {
            this.currentDocumentReference = this.currentDocumentResolver.resolve(this.currentEntityReference);
        }

        return this.currentDocumentReference;
    }

    private boolean isCurrentDocument(DocumentReference documentReference)
    {
        return getCurrentDocumentReference().equals(documentReference);
    }

    private ResourceReference toReference(ResourceReference reference, String path)
    {
        ResourceReference convertedReference = new ResourceReference(path, reference.getType());
        convertedReference.setParameters(reference.getParameters());
        return convertedReference;
    }

    private ResourceReference toURLReference(ResourceReference reference, String url)
    {
        ResourceReference convertedReference = new ResourceReference(url, ResourceType.URL);
        convertedReference.setParameters(reference.getParameters());
        return convertedReference;
    }

    private EntityReference getResolvedBaseReference(String baseReference)
    {
        EntityReference reference;
        if (!StringUtils.isEmpty(baseReference)) {
            reference = this.currentStringDocumentResolver.resolve(baseReference);
        } else {
            reference = this.currentEntityReference;
        }
        return reference;
    }

    private String getBaseReference(Block currentBlock)
    {
        String reference = "";
        MetaDataBlock metaDataBlock =
            currentBlock.getFirstBlock(new MetadataBlockMatcher(MetaData.BASE), Block.Axes.ANCESTOR);
        if (metaDataBlock != null) {
            reference = (String) metaDataBlock.getMetaData().getMetaData(MetaData.BASE);
        }
        return reference;
    }
}
