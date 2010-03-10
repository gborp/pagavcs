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
import java.util.Vector;

/**
 *
 */
public class ArrayValue implements InfoElement {
    public static final String ELEMENT_ARRAY     = "array";
    private Vector<InfoElement> keys;

    public ArrayValue() {keys = new Vector<InfoElement>();}

    public BooleanValue createBoolean() {
        BooleanValue value;

        keys.add(value = new BooleanValue());

        return value;
    }

    public StringValue createString() {
        StringValue value;

        keys.add(value = new StringValue());

        return value;
    }

    public DictValue createDict() {
        DictValue value;

        keys.add(value = new DictValue());

        return value;
    }

    public ArrayValue createArray() {
        ArrayValue value;

        keys.add(value = new ArrayValue());

        return value;
    }

    public IntegerValue createInteger() {
        IntegerValue value;

        keys.add(value = new IntegerValue());

        return value;
    }

    public RealValue createReal() {
        RealValue value;

        keys.add(value = new RealValue());

        return value;
    }

    public DateValue createDate() {
        DateValue value;

        keys.add(value = new DateValue());

        return value;
    }

    public DataValue createData() {
        DataValue value;

        keys.add(value = new DataValue());

        return value;
    }

    public void write(XmlWriter out) throws BuildException {
        try {
            out.startElement(ELEMENT_ARRAY);
            out.println();

            for(InfoElement el : keys)
                el.write(out);

            out.endElement(ELEMENT_ARRAY);
        }
        catch(IOException e) {throw new BuildException(e);}
    }
}
