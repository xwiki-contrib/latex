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
package org.xwiki.contrib.latex.internal.export;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.latex.output.LaTeXOutputProperties;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyDescriptor;

import com.xpn.xwiki.web.XWikiRequest;

/**
 * Generate LaTeX Properties for the output Filter stream.
 *
 * @version $Id$
 */
@Component(roles = LaTeXPropertiesExtractor.class)
@Singleton
public class LaTeXPropertiesExtractor
{
    private static final String FILTERPROPERTY_PREFIX = "property_";

    // Not making static as it's not thread-safe.
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    private BeanManager beans;

    /**
     * Extracts LaTeX Properties from the request.
     *
     * @param request the incoming request
     * @return the parameters for the output filter stream
     * @throws ParseException if the date inside input parameters cannot be parsed correctly
     */
    public Map<String, Object> extract(XWikiRequest request) throws ParseException
    {
        Map<String, Object> properties = new HashMap<>();
        return extractParameters(request, properties);
    }

    private Map<String, Object> extractParameters(XWikiRequest request, Map<String, Object> properties)
        throws ParseException
    {
        BeanDescriptor descriptor = this.beans.getBeanDescriptor(LaTeXOutputProperties.class);

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getKey().startsWith(FILTERPROPERTY_PREFIX) && entry.getValue() != null
                && entry.getValue().length > 0)
            {
                String parameterKey = entry.getKey().substring(FILTERPROPERTY_PREFIX.length());
                addValue(descriptor, parameterKey, properties, entry.getValue());
            }
        }

        return properties;
    }

    private void addValue(BeanDescriptor descriptor, String parameterKey, Map<String, Object> properties,
        String[] value) throws ParseException
    {
        PropertyDescriptor propertyDescriptor = descriptor.getProperty(parameterKey);
        if (propertyDescriptor != null) {
            if (TypeUtils.isAssignable(propertyDescriptor.getPropertyType(), Date.class)) {
                String dateString = value[0];
                if (!dateString.isEmpty()) {
                    properties.put(parameterKey, dateFormat.parse(dateString));
                }
            } else if (isIterable(propertyDescriptor)) {
                properties.put(parameterKey, value);
            } else {
                properties.put(parameterKey, value[0]);
            }
        }
    }

    private boolean isIterable(PropertyDescriptor propertyDescriptor)
    {
        Type type = propertyDescriptor.getPropertyType();

        if (TypeUtils.isArrayType(type)) {
            return true;
        }

        return TypeUtils.isAssignable(type, Iterable.class);
    }
}
