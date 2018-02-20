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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.output.LaTeXOutputProperties;
import org.xwiki.filter.output.AbstractBeanOutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.type.SystemType;

/**
 * Generate LaTeX package from FilterStream events.
 * 
 * @version $Id: 3fbae6cccad9025b8f9cc39a91d1c655e48109f9 $
 */
@Component
@Singleton
@Named(LaTeXOutputFilterStreamFactory.ROLEHINT)
public class LaTeXOutputFilterStreamFactory
    extends AbstractBeanOutputFilterStreamFactory<LaTeXOutputProperties, LaTeXOutputFilter>
{
    public static final String ROLEHINT = "latex";

    public static final SystemType SYSTEM_TYPE = new SystemType("latex");

    public static final FilterStreamType STREAM_TYPE = new FilterStreamType(SYSTEM_TYPE, null);

    public LaTeXOutputFilterStreamFactory()
    {
        super(FilterStreamType.XWIKI_XAR_CURRENT);

        setName("XAR output stream");
        setDescription("Write XAR package from wiki events.");
    }
}
