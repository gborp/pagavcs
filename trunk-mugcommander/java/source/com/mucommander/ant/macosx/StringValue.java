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

package com.mucommander.ant.macosx;

import com.mucommander.xml.XmlWriter;
import org.apache.tools.ant.BuildException;

import java.io.IOException;

/**
 * @author Nicolas Rinaudo
 */
public class StringValue implements InfoElement {
    private static final String ELEMENT_STRING    = "string";
    private String value;

    public StringValue() {}
    public StringValue(String s) {setValue(s);}

    public void setValue(String s) {value = s;}

    public void write(XmlWriter out) throws BuildException {
        if(value == null)
            throw new BuildException("Uninitialised string key.");

        try {
            out.startElement(ELEMENT_STRING);
            out.writeCData(value);
            out.endElement(ELEMENT_STRING);
        }
        catch(IOException e) {throw new BuildException(e);}
    }
}
