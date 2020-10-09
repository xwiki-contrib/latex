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
package org.xwiki.contrib.latex.output;

import java.util.Date;

import org.xwiki.filter.DefaultFilterStreamProperties;
import org.xwiki.filter.output.OutputTarget;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;

/**
 * LaTeX output properties.
 * 
 * @version $Id$
 */
public class LaTeXOutputProperties extends DefaultFilterStreamProperties
{
    /**
     * @see #getTarget()
     */
    private OutputTarget target;

    /**
     * @see #getEntities()
     */
    private EntityReferenceSet entities;

    private boolean toc = true;

    private boolean listOfFigures;

    private boolean listOfTables;

    private boolean pageNumbering = true;

    private String documentClass = "article";

    private boolean coverPage = true;

    private String coverPageImage;

    private String title;

    private String subtitle;

    private String author;

    /**
     * Date should be null if not defined to leave it to LaTeX to decide how to display it.
     */
    private Date date;

    /**
     * @return The path and name of the ZIP where to save the content
     */
    @PropertyName("Target")
    @PropertyDescription("The target where to save the content")
    @PropertyMandatory
    public OutputTarget getTarget()
    {
        return this.target;
    }

    /**
     * @param target see {@link #getTarget()}
     */
    public void setTarget(OutputTarget target)
    {
        this.target = target;
    }

    /**
     * @return The list of documents to export
     */
    @PropertyHidden
    public EntityReferenceSet getEntities()
    {
        return this.entities;
    }

    /**
     * @param entities see {@link #getEntities()}
     */
    public void setEntities(EntityReferenceSet entities)
    {
        this.entities = entities;
    }

    /**
     * @return the tocEnabled
     */
    @PropertyName("TOC")
    @PropertyDescription("Whether a Table of Contents should be inserted or not")
    public boolean isToc()
    {
        return this.toc;
    }

    /**
     * @param toc see {@link #isToc()}
     */
    public void setToc(boolean toc)
    {
        this.toc = toc;
    }

    /**
     * @return true if a list of figures should be inserted or false otherwise
     */
    @PropertyName("Figures")
    @PropertyDescription("Whether a list of figures should be inserted or not")
    public boolean isListOfFigures()
    {
        return this.listOfFigures;
    }

    /**
     * @param listOfFigures see {@link #isListOfFigures()}
     */
    public void setListOfFigures(boolean listOfFigures)
    {
        this.listOfFigures = listOfFigures;
    }

    /**
     * @return true if a list of tables should be inserted or false otherwise
     */
    @PropertyName("Tables")
    @PropertyDescription("Whether a list of tables should be inserted or not")
    public boolean isListOfTables()
    {
        return this.listOfTables;
    }

    /**
     * @param listOfTables see {@link #isListOfTables()}
     */
    public void setListOfTables(boolean listOfTables)
    {
        this.listOfTables = listOfTables;
    }

    /**
     * @return the pageNumberEnabled
     */
    @PropertyName("Page numbering")
    @PropertyDescription("Whether pages should be numbered or not")
    public boolean isPageNumbering()
    {
        return this.pageNumbering;
    }

    /**
     * @param pageNumbering see {@link #isPageNumbering()}
     */
    public void setPageNumbering(boolean pageNumbering)
    {
        this.pageNumbering = pageNumbering;
    }

    /**
     * @return the documentclass
     */
    @PropertyName("Document Class")
    @PropertyDescription("The type of document to produce ('article', 'book', etc)")
    public String getDocumentClass()
    {
        return this.documentClass;
    }

    /**
     * @param documentClass see {@link #getDocumentClass()}
     */
    public void setDocumentClass(String documentClass)
    {
        this.documentClass = documentClass;
    }

    /**
     * @return true if there should be a page cover generated or false otherwise
     */
    @PropertyName("Cover Page")
    @PropertyDescription("Whether a cover page (title, author, date, etc) should be generated or not")
    public boolean isCoverPage()
    {
        return this.coverPage;
    }

    /**
     * @param coverPage see {@link #isCoverPage()} ()}
     */
    public void setCoverPage(boolean coverPage)
    {
        this.coverPage = coverPage;
    }

    /**
     * @return the xwiki syntax 2.1 ({@code xwiki/2.1} image reference of an image that should be displayed on the
     *         cover page (i.e. the part after the {@code image:} prefix).
     *         For example: {@code attach:space1.space2.page1@image.png}
     * @since 1.11
     */
    @PropertyName("Cover Page Image")
    @PropertyDescription("The XWiki Syntax 2.1 image reference of an image that should be displayed on the "
        + "cover page. For example: 'attach:space1.space2.page1@image.png'")
    public String getCoverPageImage()
    {
        return this.coverPageImage;
    }

    /**
     * @param coverPageImage see {@link #getCoverPageImage()}
     * @since 1.11
     */
    public void setCoverPageImage(String coverPageImage)
    {
        this.coverPageImage = coverPageImage;
    }

    /**
     * @return the title of the exported document or set of documents
     */
    @PropertyName("Title")
    @PropertyDescription("The title of the exported document or set of documents")
    public String getTitle()
    {
        return this.title;
    }

    /**
     * @param title see {@link #getTitle()}
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the subtitle of the exported document or set of documents
     */
    @PropertyName("Subtitle")
    @PropertyDescription("The subtitle of the exported document or set of documents")
    public String getSubtitle()
    {
        return this.subtitle;
    }

    /**
     * @param subtitle see {@link #getSubtitle()}
     */
    public void setSubtitle(String subtitle)
    {
        this.subtitle = subtitle;
    }

    /**
     * @return the author of the exported document or set of documents
     */
    @PropertyName("Author")
    @PropertyDescription("The author of the exported document or set of documents")
    public String getAuthor()
    {
        return this.author;
    }

    /**
     * @param author see {@link #getAuthor()}
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * @return the date
     */
    @PropertyName("Date")
    @PropertyDescription(
        "The date of the exported document or set of documents (yyyy-MM-dd). If empty, no specific date is set")
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @param date see {@link #getDate()}
     */
    public void setDate(Date date)
    {
        this.date = date;
    }
}
