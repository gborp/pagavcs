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

import java.util.Iterator;
import java.util.Vector;

/**
 * @author Nicolas Rinaudo
 * @ant.type name="resources" category="webstart"
 */
public class ResourcesElement {
    private String                   os;
    private String                   arch;
    private String                   locale;
    private Vector<J2seElement>      j2ses;
    private Vector<JarElement>       jars;
    private Vector<NativeLibElement> nativeLibs;
    private Vector<ExtensionElement> extensions;
    private Vector<PropertyElement>  properties;
    private Vector<PackageElement>   packages;

    public ResourcesElement() {
        j2ses      = new Vector<J2seElement>();
        jars       = new Vector<JarElement>();
        nativeLibs = new Vector<NativeLibElement>();
        extensions = new Vector<ExtensionElement>();
        properties = new Vector<PropertyElement>();
        packages   = new Vector<PackageElement>();
    }

    public String getOs() {return os;}
    public String getArch() {return arch;}
    public String getLocale() {return locale;}
    public Iterator<J2seElement> j2ses() {return j2ses.iterator();}
    public Iterator<JarElement> jars() {return jars.iterator();}
    public Iterator<NativeLibElement> nativeLibs() {return nativeLibs.iterator();}
    public Iterator<ExtensionElement> extensions() {return extensions.iterator();}
    public Iterator<PropertyElement> properties() {return properties.iterator();}
    public Iterator<PackageElement> packages() {return packages.iterator();}

    public void setOs(String s) {os = s;}
    public void setArch(String s) {arch = s;}
    public void setLocale(String s) {locale = s;}
    public J2seElement createJ2se() {
        J2seElement buffer;

        buffer = new J2seElement();
        j2ses.add(buffer);

        return buffer;
    }

    public JarElement createJar() {
        JarElement buffer;

        buffer = new JarElement();
        jars.add(buffer);

        return buffer;
    }

    public NativeLibElement createNativeLib() {
        NativeLibElement buffer;

        buffer = new NativeLibElement();
        nativeLibs.add(buffer);

        return buffer;
    }

    public ExtensionElement createExtension() {
        ExtensionElement buffer;

        buffer = new ExtensionElement();
        extensions.add(buffer);

        return buffer;
    }

    public PropertyElement createProperty() {
        PropertyElement buffer;

        buffer = new PropertyElement();
        properties.add(buffer);

        return buffer;
    }

    public PackageElement createPackage() {
        PackageElement buffer;

        buffer = new PackageElement();
        packages.add(buffer);

        return buffer;
    }
}
