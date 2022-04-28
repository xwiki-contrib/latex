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
package org.xwiki.contrib.latex.internal.pdf.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.LogOutputStream;

/**
 * Logs the stdout and stderr streams of an external process started using Commons Exec. Store them in memory so that
 * they can be output in the export job logs.
 *
 * @version $Id: 662c6e99162544959c458389f71d8ef18709b08b $
 * @since 4.3.1
 */
public class XWikiLogOutputStream extends LogOutputStream
{
    /**
     * Represents the stdout stream.
     */
    private static final int STDOUT = 0;

    private List<String> logs = new ArrayList<>();

    /**
     * Always log at STDOUT level since we don't care as we'll log exactly the messages passed to us.
     */
    public XWikiLogOutputStream()
    {
        super(STDOUT);
    }

    @Override
    protected void processLine(String line, int level)
    {
        this.logs.add(line);
    }

    /**
     * @return the accumulated logs
     */
    public List<String> getLogs()
    {
        return this.logs;
    }
}
