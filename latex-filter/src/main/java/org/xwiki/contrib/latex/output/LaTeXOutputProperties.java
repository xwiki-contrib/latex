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

    private boolean tocEnabled = true;

    private boolean figuresEnabled = true;

    private boolean tablesEnabled = true;

    private boolean pageNumberEnabled = true;

    private String documentclass = "article";

    private boolean coverPageEnabled = true;

    private String title;

    private String subtitle;

    private String author;

    private Date date = new Date();

    /**
     * @return The target where to save the content
     */
    @PropertyName("Target")
    @PropertyDescription("The target where to save the content")
    @PropertyMandatory
    public OutputTarget getTarget()
    {
        return this.target;
    }

    /**
     * @param target The target where to save the content
     */
    public void setTarget(OutputTarget target)
    {
        this.target = target;
    }

    /**
     * @return The entities to generate events from
     */
    @PropertyHidden
    public EntityReferenceSet getEntities()
    {
        return this.entities;
    }

    /**
     * @param entities The entities to generate events from
     */
    public void setEntities(EntityReferenceSet entities)
    {
        this.entities = entities;
    }

    /**
     * @return the tocEnabled
     */
    @PropertyName("Toc")
    @PropertyDescription("Indicate if a toc should be generated")
    public boolean isTocEnabled()
    {
        return tocEnabled;
    }

    /**
     * @param tocEnabled the tocEnabled to set
     */
    public void setTocEnabled(boolean tocEnabled)
    {
        this.tocEnabled = tocEnabled;
    }

    /**
     * @return the figuresEnabled
     */
    @PropertyName("Figures")
    @PropertyDescription("Indicate if an index of the figures should be inserted")
    public boolean isFiguresEnabled()
    {
        return figuresEnabled;
    }

    /**
     * @param figuresEnabled the figuresEnabled to set
     */
    public void setFiguresEnabled(boolean figuresEnabled)
    {
        this.figuresEnabled = figuresEnabled;
    }

    /**
     * @return the tablesEnabled
     */
    @PropertyName("Tables")
    @PropertyDescription("Indicate if an index of the tables should be inserted")
    public boolean isTablesEnabled()
    {
        return tablesEnabled;
    }

    /**
     * @param tablesEnabled the tablesEnabled to set
     */
    public void setTablesEnabled(boolean tablesEnabled)
    {
        this.tablesEnabled = tablesEnabled;
    }

    /**
     * @return the pageNumberEnabled
     */
    @PropertyName("Page number")
    @PropertyDescription("Indicate if a number should be inserted for each page")
    public boolean isPageNumberEnabled()
    {
        return pageNumberEnabled;
    }

    /**
     * @param pageNumberEnabled the pageNumberEnabled to set
     */
    public void setPageNumberEnabled(boolean pageNumberEnabled)
    {
        this.pageNumberEnabled = pageNumberEnabled;
    }

    /**
     * @return the documentclass
     */
    @PropertyName("Document class")
    @PropertyDescription("The value to indicate in the LaTeX \\documentclass")
    public String getDocumentclass()
    {
        return documentclass;
    }

    /**
     * @param documentclass the documentclass to set
     */
    public void setDocumentclass(String documentclass)
    {
        this.documentclass = documentclass;
    }

    /**
     * @return the coverPageEnabled
     */
    @PropertyName("Page cover")
    @PropertyDescription("Indicate of a page cover should be exported")
    public boolean isCoverPageEnabled()
    {
        return coverPageEnabled;
    }

    /**
     * @param coverPageEnabled the coverPageEnabled to set
     */
    public void setCoverPageEnabled(boolean coverPageEnabled)
    {
        this.coverPageEnabled = coverPageEnabled;
    }

    /**
     * @return the title
     */
    @PropertyName("Title")
    @PropertyDescription("The title to display in the index")
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the subtitle
     */
    @PropertyName("Subtitle")
    @PropertyDescription("The subtitle to dispay in the index")
    public String getSubtitle()
    {
        return subtitle;
    }

    /**
     * @param subtitle the subtitle to set
     */
    public void setSubtitle(String subtitle)
    {
        this.subtitle = subtitle;
    }

    /**
     * @return the author
     */
    @PropertyName("Author")
    @PropertyDescription("The author document")
    public String getAuthor()
    {
        return author;
    }

    /**
     * @param author the author to set
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * @return the date
     */
    @PropertyName("Date")
    @PropertyDescription("The date of the document")
    public Date getDate()
    {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Date date)
    {
        this.date = date;
    }
}
