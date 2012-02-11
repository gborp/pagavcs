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

/**
 * @author Nicolas Rinaudo
 * @ant.type name="nativelib" category="webstart"
 */
public class NativeLibElement extends Downloadable {
    private String href;
    private String version;
    private int    size;
    private String part;

    public String getHref() {return href;}
    public String getVersion() {return version;}
    public int getSize() {return size;}
    public String getPart() {return part;}
    public void setHref(String s) {href = s;}
    public void setVersion(String s) {version = s;}
    public void setSize(int i) {size = i;}
    public void setPart(String s) {part = s;}
}
