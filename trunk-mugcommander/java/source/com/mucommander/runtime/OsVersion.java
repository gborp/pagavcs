/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.runtime;

import com.mucommander.commons.CommonsLogger;

/**
 * This class represents a major version of an operating system, like <code>Mac OS X 10.5</code> or
 * <code>Windows XP</code>. The current runtime value is determined using the value of the <code>os.version</code>
 * system property and the current {@link OsFamily} instance.
 * Being a {@link com.mucommander.runtime.ComparableRuntimeProperty}, OS versions are ordered and can be compared
 * against each other.
 *
 * @see OsVersions
 * @see OsFamily
 * @author Maxence Bernard
 */
public class OsVersion extends ComparableRuntimeProperty implements OsVersions {

    /** Holds the OsVersion of the current runtime environment  */
    private static OsVersion currentValue;


    protected OsVersion(String stringRepresentation, int intValue) {
        super(stringRepresentation, intValue);
    }

    
    ////////////////////
    // Static methods //
    ////////////////////

    /**
     * Determines the current value by parsing the corresponding system property. This method is called automatically
     * by this class the first time the current value is accessed. However, this method has been made public to allow
     * to force the initialization if it needs to happen at a predictable time.
     */
    public static void init() {
        // Note: performing the initialization outside of the class static block avoids cyclic dependency problems.
        if(currentValue==null) {
            currentValue = parseSystemProperty(getRawSystemProperty(), OsFamily.getRawSystemProperty(), OsFamily.getCurrent());
            CommonsLogger.config("Current OS version: "+ currentValue);
        }
    }

    /**
     * Returns the OS version of the current runtime environment.
     *
     * @return the OS version of the current runtime environment
     */
    public static OsVersion getCurrent() {
        if(currentValue==null) {
            // init() is called only once
            init();
        }

        return currentValue;
    }

    /**
     * Returns the value of the system property which serves to detect the OS version at runtime.
     *
     * @return the value of the system property which serves to detect the OS version at runtime.
     */
    public static String getRawSystemProperty() {
        return System.getProperty("os.version");
    }

    /**
     * Returns an <code>OsVersion</code> instance corresponding to the specified system property's value.
     *
     * @param osVersionProp the value of the "os.version" system property
     * @param osNameProp the value of the "os.name" system property
     * @param osFamily the current OS family
     * @return an OsVersion instance corresponding to the specified system property's value
     */
    static OsVersion parseSystemProperty(String osVersionProp, String osNameProp, OsFamily osFamily) {
        OsVersion osVersion;

        // This website holds a collection of system property values under many OSes:
        // http://lopica.sourceforge.net/os.html

        if(osFamily==OsFamilies.WINDOWS) {
            if(osNameProp.equals("Windows 95")) {
                osVersion = WINDOWS_95;
            }
            else if(osNameProp.equals("Windows 98")) {
                osVersion = WINDOWS_98;
            }
            else if(osNameProp.equals("Windows Me")) {
                osVersion = WINDOWS_ME;
            }
            else if(osNameProp.equals("Windows NT")) {
                osVersion = WINDOWS_NT;
            }
            else if(osNameProp.equals("Windows 2000")) {
                osVersion = WINDOWS_2000;
            }
            else if(osNameProp.equals("Windows XP")) {
                osVersion = WINDOWS_XP;
            }
            else if(osNameProp.equals("Windows 2003")) {
                osVersion = WINDOWS_2003;
            }
            else if(osNameProp.equals("Windows Vista")) {
                osVersion = WINDOWS_VISTA;
            }
            else if(osNameProp.equals("Windows 7")) {
                osVersion = WINDOWS_7;
            }
            else {
                // Newer version we don't know of yet, assume latest supported OS version
                osVersion = WINDOWS_7;
            }
        }
        // Mac OS X versions
        else if(osFamily==OsFamilies.MAC_OS_X) {
            if(osVersionProp.startsWith("10.6")) {
                osVersion = MAC_OS_X_10_6;
            }
            else if(osVersionProp.startsWith("10.5")) {
                osVersion = MAC_OS_X_10_5;
            }
            else if(osVersionProp.startsWith("10.4")) {
                osVersion = MAC_OS_X_10_4;
            }
            else if(osVersionProp.startsWith("10.3")) {
                osVersion = MAC_OS_X_10_3;
            }
            else if(osVersionProp.startsWith("10.2")) {
                osVersion = MAC_OS_X_10_2;
            }
            else if(osVersionProp.startsWith("10.1")) {
                osVersion = MAC_OS_X_10_1;
            }
            else if(osVersionProp.startsWith("10.0")) {
                osVersion = MAC_OS_X_10_0;
            }
            else {
                // Newer version we don't know of yet, assume latest supported OS version
                osVersion = MAC_OS_X_10_6;
            }
        }
        else {
            osVersion = OsVersions.UNKNOWN_VERSION;
        }

        return osVersion;
    }

    
    //////////////////////////////////////////////
    // ComparableRuntimeProperty implementation //
    //////////////////////////////////////////////

    @Override
    protected RuntimeProperty getCurrentValue() {
        return getCurrent();
    }
}
