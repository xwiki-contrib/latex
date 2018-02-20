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

}
