/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ant.jnlp;

import org.apache.tools.ant.BuildException;

/**
 * @author Nicolas Rinaudo
 * @ant.type name="description" category="webstart"
 */
public class DescriptionElement {
    public static final int KIND_UNSPECIFIED = 0;
    public static final int KIND_ONE_LINE    = 1;
    public static final int KIND_SHORT       = 2;
    public static final int KIND_TOOLTIP     = 3;
    private int kind;
    private StringBuffer description;

    public DescriptionElement() {description = new StringBuffer();}

    public void setKind(String s) {
        if(s.equals("one-line"))
            kind = KIND_ONE_LINE;
        else if(s.equals("short"))
            kind = KIND_SHORT;
        else if(s.equals("tooltip"))
            kind = KIND_TOOLTIP;
        else
            throw new BuildException("Unknown description kind: " + s);
    }
    public String getText() {return description.toString().trim();}
    public int getKind() {return kind;}
    public void addText(String s) {description.append(s);}
}
