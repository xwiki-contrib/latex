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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.latex.export.LaTeXExportConfiguration;
import org.xwiki.contrib.latex.pdf.LaTeX2PDFConverter;

/**
 * Provides the {@link LaTeX2PDFConverter} implementing matching the
 * {@link LaTeXExportConfiguration#getPDFExportHint()} set.
 *
 * @version $Id$
 */
@Component
@Singleton
public class LaTeX2PDFConverterProvider implements Provider<LaTeX2PDFConverter>
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private LaTeXExportConfiguration configuration;

    @Override
    public LaTeX2PDFConverter get()
    {
        try {
            return this.componentManager.getInstance(LaTeX2PDFConverter.class, this.configuration.getPDFExportHint());
        } catch (ComponentLookupException e) {
            // Failed to find an exporter for the specific eporter hint, fail the execution
            throw new RuntimeException(String.format("Cannot find LaTeX exporter for hint [%s]",
                this.configuration.getPDFExportHint()), e);
        }
    }
}
