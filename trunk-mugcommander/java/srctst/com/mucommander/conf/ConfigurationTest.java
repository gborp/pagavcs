/**
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

package com.mucommander.conf;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;
import java.util.Vector;

/**
 * A test case for the {@link Configuration} class.
 * @author Maxence Bernard, Nicolas Rinaudo
 */
public class ConfigurationTest extends TestCase implements ConfigurationListener, ConfigurationReader, ConfigurationReaderFactory,
                                                           ConfigurationWriter, ConfigurationWriterFactory, ConfigurationSource {
    // - Class variables -----------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /** Maximum depth at which to conduct tests in the configuration tree. */
    private static final int    MAX_DEPTH       = 4;
    /** Name of the section in which to store string variables. */
    private static final String STRING_SECTION  = "string.";
    /** Name of the section in which to store list variables. */
    private static final String LIST_SECTION    = "list.";
    /** Name of the section in which to store boolean variables. */
    private static final String BOOLEAN_SECTION = "boolean.";
    /** Name of the section in which to store integer variables. */
    private static final String INTEGER_SECTION = "integer.";
    /** Name of the section in which to store long variables. */
    private static final String LONG_SECTION    = "long.";
    /** Name of the section in which to store float variables. */
    private static final String FLOAT_SECTION   = "float.";
    /** Name of the section in which to store double variables. */
    private static final String DOUBLE_SECTION  = "double.";



    // - Instance fields -----------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /** The Configuration instance that is being tested. Intialized each time a test is performed */
    private       Configuration             conf;
    /** Stack of configuration events. */
    private       Stack<ConfigurationEvent> events;
    /** Identifier of the current instance. */
    private final String        identifier = Long.toString(System.currentTimeMillis());



    // - Initialisation ------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Creates a fresh {@link Configuration} instance each time a test is performed, and registers itself as a listener.
     */
    @Override
    protected void setUp() throws Exception {
        conf   = new Configuration();
        events = new Stack<ConfigurationEvent>();

        Configuration.addConfigurationListener(this);
    }



    // - Configuration listening ---------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Stores the specified configuration event.
     */
    public void configurationChanged(ConfigurationEvent event) {events.push(event);}

    /**
     * Returns the last received event.
     * @return the last received event.
     */
    private ConfigurationEvent popEvent() {return events.pop();}

    /**
     * Returns <code>true</code> if there are still some unhandled events in the stack.
     * @return <code>true</code> if there are still some unhandled events in the stack, <code>false</code> otherwise.
     */
    private boolean hasEvents() {return !events.empty();}

    /**
     * Makes sure that event listener registration works.
     */
    public void testListenerRegistration() {
        // Makes sure events are not received anymore after
        // removeConfigurationListener has been called.
        Configuration.removeConfigurationListener(this);
        conf.setVariable("event.test", "value");
        assertFalse(hasEvents());

        // Makes sure events are received after addConfigurationListener
        // has been called.
        Configuration.addConfigurationListener(this);
        conf.setVariable("event.test", "new-value");
        assertNotNull(popEvent());
    }



    // - Event helper methods ------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Makes sure the specified event is not <code>null</code> and has the expected name.
     * @param event event to check.
     * @param name expected event name.
     */
    private void assertEventName(ConfigurationEvent event, String name) {
        assertNotNull(event);
        assertEquals(event.getVariable(), name);
    }

    /**
     * Makes sure the specified event matches the specified values.
     * @param event event to check.
     * @param name  expected event name.
     * @param value expected event value.
     */
    private void assertEvent(ConfigurationEvent event, String name, String value) {
        assertEventName(event, name);
        assertEquals(event.getValue(), value);
    }

    /**
     * Makes sure the specified event matches the specified values.
     * @param event event to check.
     * @param name  expected event name.
     * @param value expected event value.
     */
    private void assertEvent(ConfigurationEvent event, String name, long value) {
        assertEventName(event, name);
        assertEquals(event.getLongValue(), value);
    }

    /**
     * Makes sure the specified event matches the specified values.
     * @param event event to check.
     * @param name  expected event name.
     * @param value expected event value.
     */
    private void assertEvent(ConfigurationEvent event, String name, int value) {
        assertEventName(event, name);
        assertEquals(event.getIntegerValue(), value);
    }

    /**
     * Makes sure the specified event matches the specified values.
     * @param event event to check.
     * @param name  expected event name.
     * @param value expected event value.
     */
    private void assertEvent(ConfigurationEvent event, String name, float value) {
        assertEventName(event, name);
        assertEquals(event.getFloatValue(), value, 0);
    }

    /**
     * Makes sure the specified event matches the specified values.
     * @param event event to check.
     * @param name  expected event name.
     * @param value expected event value.
     */
    private void assertEvent(ConfigurationEvent event, String name, double value) {
        assertEventName(event, name);
        assertEquals(event.getDoubleValue(), value, 0);
    }

    /**
     * Makes sure the specified event matches the specified values.
     * @param event event to check.
     * @param name  expected event name.
     * @param value expected event value.
     */
    private void assertEvent(ConfigurationEvent event, String name, boolean value) {
        assertEventName(event, name);
        assertEquals(event.getBooleanValue(), value);
    }

    /**
     * Makes sure the specified event matches the specified values.
     * @param event     event to check.
     * @param name      expected event name.
     * @param separator separator used to tokenise the value.
     * @param value     expected event value.
     */
    private void assertEvent(ConfigurationEvent event, String name, String separator, Vector<String> value) {
        assertEventName(event, name);
        assertEquals(event.getListValue(separator), value);
    }



    // - String variables test -----------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests string variable operations in the specified section.
     * @param section section in which to manipulate the string variables.
     */
    private void testStringVariables(String section) {
        String var1;   // First variable we're using for tests.
        String var2;   // Second variable we're using for tests.
        String value1; // First value for that variable.
        String value2; // Second value for that variable.

        // Initialises test variables.
        var1   = section + "str1";
        var2   = section + "str2";
        value1 = "val1";
        value2 = "val2";

        // Makes sure that null is returned when requesting a variable that hasn't been defined.
        assertNull(conf.getVariable(var1));

        // Makes sure that true is returned when creating a new variable, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the variable was actually set.
        assertTrue(conf.isVariableSet(var1));
        assertEquals(value1, conf.getVariable(var1));

        // Makes sure that true is returned when changing a variable's value, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value2));
        assertEvent(popEvent(), var1, value2);
        assertFalse(hasEvents());

        // Makes sure that the right string value is returned.
        assertEquals(value2, conf.getVariable(var1));

        // Makes sure that false is returned when setting a variable to its old value.
        assertFalse(conf.setVariable(var1, value2));
        assertFalse(hasEvents());

        // Makes sure that default values do not override existing values.
        assertEquals(value2, conf.getVariable(var1, value1));
        assertFalse(hasEvents());

        // Makes sure the right value is returned and the right event generated
        // by the remove method
        assertEquals(value2, conf.removeVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, null);
        assertFalse(hasEvents());

        // Makes sure removing a null variable returns null and doesn't generate an event.
        assertNull(conf.removeVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertFalse(hasEvents());

        // Makes sure that false is returned when an undefined variable is set to null.
        assertFalse(conf.setVariable(var1, null));
        assertFalse(hasEvents());

        // Makes sure default values are properly set.
        assertEquals(value1, conf.getVariable(var1, value1));
        assertEquals(value1, conf.getVariable(var1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the rename method works properly.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEquals(value1, conf.getVariable(var2));
        assertEvent(popEvent(), var2, value1);
        assertEvent(popEvent(), var1, null);
        assertFalse(hasEvents());

        // Makes sure that renaming a variable to another with the same value
        // only triggers the variable deletion event.
        conf.setVariable(var1, value1);
        popEvent();
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, null);
        assertFalse(hasEvents());

        // Makes sure that renaming a non-set variable results in deleting
        // both the source and target.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertEvent(popEvent(), var2, null);
        assertFalse(hasEvents());

        // Makes sure that meaningless renames leave the configuration untouched
        // and do not generate any event.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertFalse(hasEvents());
    }

    /**
     * Runs string variable tests at different depths in the configuration tree.
     */
    public void testStringVariables() {
        StringBuffer section; // Name of the section in which to test string variables.

        testStringVariables("");

        section = new StringBuffer(STRING_SECTION);
        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testStringVariables(section.toString());
            section.append(STRING_SECTION);
        }
    }



    // - List variables test -------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests list variable operations in the specified section.
     * @param section section in which to manipulate the string variables.
     */
    private void testListVariables(String section) {
        String var1;   // First variable we're using for tests.
        String var2;   // Second variable we're using for tests.
        Vector<String> value1; // First value for that variable.
        Vector<String> value2; // Second value for that variable.

        // Initialises test variables.
        var1   = section + "list1";
        var2   = section + "list2";
        value1 = new Vector<String>();
        value2 = new Vector<String>();

        for(int i = 0; i < 4; i++) {
            value1.add("val1-" + i);
            value2.add("val2-" + i);
        }

        // Makes sure that null is returned when requesting a variable that hasn't been defined.
        assertNull(conf.getListVariable(var1, ";"));

        // Makes sure that true is returned when creating a new variable, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value1, ";"));
        assertEvent(popEvent(), var1, ";", value1);
        assertFalse(hasEvents());

        // Makes sure the variable was actually set.
        assertTrue(conf.isVariableSet(var1));
        assertEquals(value1, conf.getListVariable(var1, ";"));

        // Makes sure that true is returned when changing a variable's value, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value2, ";"));
        assertEvent(popEvent(), var1, ";", value2);
        assertFalse(hasEvents());

        // Makes sure that the right string value is returned.
        assertEquals(value2, conf.getListVariable(var1, ";"));

        // Makes sure that false is returned when setting a variable to its old value.
        assertFalse(conf.setVariable(var1, value2, ";"));
        assertFalse(hasEvents());

        // Makes sure that default values do not override existing values.
        assertEquals(value2, conf.getVariable(var1, value1, ";"));
        assertFalse(hasEvents());

        // Makes sure the right value is returned and the right event generated
        // by the remove method
        assertEquals(value2, conf.removeListVariable(var1, ";"));
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, ";", null);
        assertFalse(hasEvents());

        // Makes sure removing a null variable returns null and doesn't generate an event.
        assertNull(conf.removeVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertFalse(hasEvents());

        // Makes sure that false is returned when an undefined variable is set to null.
        assertFalse(conf.setVariable(var1, null));
        assertFalse(hasEvents());

        // Makes sure default values are properly set.
        assertEquals(value1, conf.getVariable(var1, value1, ";"));
        assertEquals(value1, conf.getListVariable(var1, ";"));
        assertEvent(popEvent(), var1, ";", value1);
        assertFalse(hasEvents());

        // Makes sure the rename method works properly.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEquals(value1, conf.getListVariable(var2, ";"));
        assertEvent(popEvent(), var2, ";", value1);
        assertEvent(popEvent(), var1, ";", null);
        assertFalse(hasEvents());

        // Makes sure that renaming a variable to another with the same value
        // only triggers the variable deletion event.
        conf.setVariable(var1, value1, ";");
        popEvent();
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, null);
        assertFalse(hasEvents());

        // Makes sure that renaming a non-set variable results in deleting
        // both the source and target.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertEvent(popEvent(), var2, null);
        assertFalse(hasEvents());

        // Makes sure that meaningless renames leave the configuration untouched
        // and do not generate any event.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertFalse(hasEvents());
    }

    /**
     * Runs list variable tests at different depths in the configuration tree.
     */
    public void testListVariables() {
        StringBuffer section; // Name of the section in which to test string variables.

        testStringVariables("");

        section = new StringBuffer(LIST_SECTION);
        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testListVariables(section.toString());
            section.append(LIST_SECTION);
        }
    }



    // - Integer variables test ----------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests string variable operations in the specified section.
     * @param section section in which to manipulate the string variables.
     */
    private void testIntegerVariables(String section) {
        String  var1;            // First variable we're using for tests.
        String  var2;            // Second variable we're using for tests.
        int     value1;          // First value for that variable.
        int     value2;          // Second value for that variable.
        boolean caughtException; // Whether or not an expected exception was caught.

        // Initialises test variables.
        var1   = section + "int1";
        var2   = section + "int2";
        value1 = 10;
        value2 = 20;

        // Makes sure that 0 is returned when requesting a variable that hasn't been defined.
        assertEquals(0, conf.getIntegerVariable(var1));

        // Makes sure that true is returned when creating a new variable, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the variable was actually set.
        assertTrue(conf.isVariableSet(var1));
        assertEquals(value1, conf.getIntegerVariable(var1));

        // Makes sure that true is returned when changing a variable's value, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value2));
        assertEvent(popEvent(), var1, value2);
        assertFalse(hasEvents());

        // Makes sure that the right string value is returned.
        assertEquals(value2, conf.getIntegerVariable(var1));

        // Makes sure that false is returned when setting a variable to its old value.
        assertFalse(conf.setVariable(var1, value2));
        assertFalse(hasEvents());

        // Makes sure that default values do not override existing values.
        assertEquals(value2, conf.getVariable(var1, value1));
        assertFalse(hasEvents());

        // Makes sure the right value is returned and the right event generated
        // by the remove method
        assertEquals(value2, conf.removeIntegerVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure removing a null variable returns 0 and doesn't generate an event.
        assertEquals(0, conf.removeIntegerVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertFalse(hasEvents());

        // Makes sure default values are properly set.
        assertEquals(value1, conf.getVariable(var1, value1));
        assertEquals(value1, conf.getIntegerVariable(var1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the rename method works properly.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEquals(value1, conf.getIntegerVariable(var2));
        assertEvent(popEvent(), var2, value1);
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a variable to another with the same value
        // only triggers the variable deletion event.
        conf.setVariable(var1, value1);
        popEvent();
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a non-set variable results in deleting
        // both the source and target.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertEvent(popEvent(), var2, 0);
        assertFalse(hasEvents());

        // Makes sure that meaningless renames leave the configuration untouched
        // and do not generate any event.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertFalse(hasEvents());

        // Makes sure that non-integer variables cannot be retrieved using getIntegerVariable.
        conf.setVariable(var1, "abcde");
        popEvent();
        caughtException = false;
        try {conf.getIntegerVariable(var1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);

        // Makes sure that non-integer variables cannot be retrieved using getVariable(String,int).
        caughtException = false;
        try {conf.getVariable(var1, value1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);
    }

    /**
     * Runs integer variable tests at different depths in the configuration tree.
     */
    public void testIntegerVariables() {
        StringBuffer section; // Name of the section in which to test integer variables.

        testIntegerVariables("");

        section = new StringBuffer(INTEGER_SECTION);
        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testIntegerVariables(section.toString());
            section.append(INTEGER_SECTION);
        }
    }



    // - Long variables test -------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests long variable operations in the specified section.
     * @param section section in which to manipulate the long variables.
     */
    private void testLongVariables(String section) {
        String  var1;            // First variable we're using for tests.
        String  var2;            // Second variable we're using for tests.
        long    value1;          // First value for that variable.
        long    value2;          // Second value for that variable.
        boolean caughtException; // Whether or not an expected exception was caught.

        // Initialises test variables.
        var1   = section + "long1";
        var2   = section + "long2";
        value1 = 10;
        value2 = 20;

        // Makes sure that 0 is returned when requesting a variable that hasn't been defined.
        assertEquals(0, conf.getLongVariable(var1));

        // Makes sure that true is returned when creating a new variable, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the variable was actually set.
        assertTrue(conf.isVariableSet(var1));
        assertEquals(value1, conf.getLongVariable(var1));

        // Makes sure that true is returned when changing a variable's value, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value2));
        assertEvent(popEvent(), var1, value2);
        assertFalse(hasEvents());

        // Makes sure that the right string value is returned.
        assertEquals(value2, conf.getLongVariable(var1));

        // Makes sure that false is returned when setting a variable to its old value.
        assertFalse(conf.setVariable(var1, value2));
        assertFalse(hasEvents());

        // Makes sure that default values do not override existing values.
        assertEquals(value2, conf.getVariable(var1, value1));
        assertFalse(hasEvents());

        // Makes sure the right value is returned and the right event generated
        // by the remove method
        assertEquals(value2, conf.removeLongVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure removing a null variable returns 0 and doesn't generate an event.
        assertEquals(0, conf.removeLongVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertFalse(hasEvents());

        // Makes sure default values are properly set.
        assertEquals(value1, conf.getVariable(var1, value1));
        assertEquals(value1, conf.getLongVariable(var1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the rename method works properly.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEquals(value1, conf.getLongVariable(var2));
        assertEvent(popEvent(), var2, value1);
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a variable to another with the same value
        // only triggers the variable deletion event.
        conf.setVariable(var1, value1);
        popEvent();
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a non-set variable results in deleting
        // both the source and target.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertEvent(popEvent(), var2, 0);
        assertFalse(hasEvents());

        // Makes sure that meaningless renames leave the configuration untouched
        // and do not generate any event.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertFalse(hasEvents());

        // Makes sure that non-long variables cannot be retrieved using getLongVariable.
        conf.setVariable(var1, "abcde");
        popEvent();
        caughtException = false;
        try {conf.getLongVariable(var1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);

        // Makes sure that non-long variables cannot be retrieved using getVariable(String,long).
        caughtException = false;
        try {conf.getVariable(var1, value1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);
    }

    /**
     * Runs long variable tests at different depths in the configuration tree.
     */
    public void testLongVariables() {
        StringBuffer section; // Name of the section in which to test integer variables.

        testLongVariables("");

        section = new StringBuffer(LONG_SECTION);
        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testLongVariables(section.toString());
            section.append(LONG_SECTION);
        }
    }



    // - Float variables test ------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests float variable operations in the specified section.
     * @param section section in which to manipulate the float variables.
     */
    private void testFloatVariables(String section) {
        String  var1;            // First variable we're using for tests.
        String  var2;            // Second variable we're using for tests.
        float   value1;          // First value for that variable.
        float   value2;          // Second value for that variable.
        boolean caughtException; // Whether or not an expected exception was caught.

        // Initialises test variables.
        var1   = section + "float1";
        var2   = section + "float2";
        value1 = 10;
        value2 = 20;

        // Makes sure that 0 is returned when requesting a variable that hasn't been defined.
        assertEquals(0, conf.getFloatVariable(var1), 0);

        // Makes sure that true is returned when creating a new variable, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the variable was actually set.
        assertTrue(conf.isVariableSet(var1));
        assertEquals(value1, conf.getFloatVariable(var1), 0);

        // Makes sure that true is returned when changing a variable's value, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value2));
        assertEvent(popEvent(), var1, value2);
        assertFalse(hasEvents());

        // Makes sure that the right string value is returned.
        assertEquals(value2, conf.getFloatVariable(var1), 0);

        // Makes sure that false is returned when setting a variable to its old value.
        assertFalse(conf.setVariable(var1, value2));
        assertFalse(hasEvents());

        // Makes sure that default values do not override existing values.
        assertEquals(value2, conf.getVariable(var1, value1), 0);
        assertFalse(hasEvents());

        // Makes sure the right value is returned and the right event generated
        // by the remove method
        assertEquals(value2, conf.removeFloatVariable(var1), 0);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure removing a null variable returns 0 and doesn't generate an event.
        assertEquals(0, conf.removeFloatVariable(var1), 0);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(hasEvents());

        // Makes sure default values are properly set.
        assertEquals(value1, conf.getVariable(var1, value1), 0);
        assertEquals(value1, conf.getFloatVariable(var1), 0);
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the rename method works properly.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEquals(value1, conf.getFloatVariable(var2), 0);
        assertEvent(popEvent(), var2, value1);
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a variable to another with the same value
        // only triggers the variable deletion event.
        conf.setVariable(var1, value1);
        popEvent();
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a non-set variable results in deleting
        // both the source and target.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertEvent(popEvent(), var2, 0);
        assertFalse(hasEvents());

        // Makes sure that meaningless renames leave the configuration untouched
        // and do not generate any event.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertFalse(hasEvents());

        // Makes sure that non-float variables cannot be retrieved using getFloatVariable.
        conf.setVariable(var1, "abcde");
        popEvent();
        caughtException = false;
        try {conf.getFloatVariable(var1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);

        // Makes sure that non-float variables cannot be retrieved using getVariable(String,float).
        caughtException = false;
        try {conf.getVariable(var1, value1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);
    }

    /**
     * Runs float variable tests at different depths in the configuration tree.
     */
    public void testFloatVariables() {
        StringBuffer section; // Name of the section in which to test float variables.

        testFloatVariables("");

        section = new StringBuffer(FLOAT_SECTION);
        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testFloatVariables(section.toString());
            section.append(FLOAT_SECTION);
        }
    }



    // - Double variables test -----------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests double variable operations in the specified section.
     * @param section section in which to manipulate the double variables.
     */
    private void testDoubleVariables(String section) {
        String  var1;            // First variable we're using for tests.
        String  var2;            // Second variable we're using for tests.
        double  value1;          // First value for that variable.
        double  value2;          // Second value for that variable.
        boolean caughtException; // Whether or not an expected exception was caught.

        // Initialises test variables.
        var1   = section + "double1";
        var2   = section + "double2";
        value1 = 10;
        value2 = 20;

        // Makes sure that 0 is returned when requesting a variable that hasn't been defined.
        assertEquals(0, conf.getDoubleVariable(var1), 0);

        // Makes sure that true is returned when creating a new variable, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value1));
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the variable was actually set.
        assertTrue(conf.isVariableSet(var1));
        assertEquals(value1, conf.getDoubleVariable(var1), 0);

        // Makes sure that true is returned when changing a variable's value, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, value2));
        assertEvent(popEvent(), var1, value2);
        assertFalse(hasEvents());

        // Makes sure that the right string value is returned.
        assertEquals(value2, conf.getDoubleVariable(var1), 0);

        // Makes sure that false is returned when setting a variable to its old value.
        assertFalse(conf.setVariable(var1, value2));
        assertFalse(hasEvents());

        // Makes sure that default values do not override existing values.
        assertEquals(value2, conf.getVariable(var1, value1), 0);
        assertFalse(hasEvents());

        // Makes sure the right value is returned and the right event generated
        // by the remove method
        assertEquals(value2, conf.removeDoubleVariable(var1), 0);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure removing a null variable returns 0 and doesn't generate an event.
        assertEquals(0, conf.removeDoubleVariable(var1), 0);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(hasEvents());

        // Makes sure default values are properly set.
        assertEquals(value1, conf.getVariable(var1, value1), 0);
        assertEquals(value1, conf.getDoubleVariable(var1), 0);
        assertEvent(popEvent(), var1, value1);
        assertFalse(hasEvents());

        // Makes sure the rename method works properly.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEquals(value1, conf.getDoubleVariable(var2), 0);
        assertEvent(popEvent(), var2, value1);
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a variable to another with the same value
        // only triggers the variable deletion event.
        conf.setVariable(var1, value1);
        popEvent();
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, 0);
        assertFalse(hasEvents());

        // Makes sure that renaming a non-set variable results in deleting
        // both the source and target.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertEvent(popEvent(), var2, 0);
        assertFalse(hasEvents());

        // Makes sure that meaningless renames leave the configuration untouched
        // and do not generate any event.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertFalse(hasEvents());

        // Makes sure that non-double variables cannot be retrieved using getDoubleVariable.
        conf.setVariable(var1, "abcde");
        popEvent();
        caughtException = false;
        try {conf.getDoubleVariable(var1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);

        // Makes sure that non-double variables cannot be retrieved using getVariable(String,double).
        caughtException = false;
        try {conf.getVariable(var1, value1);}
        catch(NumberFormatException e) {caughtException = true;}
        assertTrue(caughtException);
    }

    /**
     * Runs double variable tests at different depths in the configuration tree.
     */
    public void testDoubleVariables() {
        StringBuffer section; // Name of the section in which to test double variables.

        testDoubleVariables("");

        section = new StringBuffer(DOUBLE_SECTION);
        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testDoubleVariables(section.toString());
            section.append(DOUBLE_SECTION);
        }
    }



    // - Boolean variables test ----------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests boolean variable operations in the specified section.
     * @param section section in which to manipulate the boolean variables.
     */
    private void testBooleanVariables(String section) {
        String var1;   // First variable we're using for tests.
        String var2;   // Second variable we're using for tests.

        // Initialises test variables.
        var1   = section + "bool1";
        var2   = section + "bool2";

        // Makes sure that false is returned when requesting a variable that hasn't been defined.
        assertFalse(conf.getBooleanVariable(var1));

        // Makes sure that true is returned when creating a new variable, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, true));
        assertEvent(popEvent(), var1, true);
        assertFalse(hasEvents());

        // Makes sure the variable was actually set.
        assertTrue(conf.isVariableSet(var1));
        assertTrue(conf.getBooleanVariable(var1));

        // Makes sure that true is returned when changing a variable's value, and checks on the
        // event generated as a result.
        assertTrue(conf.setVariable(var1, false));
        assertEvent(popEvent(), var1, false);
        assertFalse(hasEvents());

        // Makes sure that the right value is returned.
        assertFalse(conf.getBooleanVariable(var1));

        // Makes sure that false is returned when setting a variable to its old value.
        assertFalse(conf.setVariable(var1, false));
        assertFalse(hasEvents());

        // Makes sure that default values do not override existing values.
        assertFalse(conf.getVariable(var1, true));
        assertFalse(hasEvents());

        // Makes sure the right value is returned and the right event generated
        // by the remove method
        assertFalse(conf.removeBooleanVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertEvent(popEvent(), var1, false);
        assertFalse(hasEvents());

        // Makes sure removing a null variable returns 0 and doesn't generate an event.
        assertFalse(conf.removeBooleanVariable(var1));
        assertFalse(conf.isVariableSet(var1));
        assertFalse(hasEvents());

        // Makes sure default values are properly set.
        assertTrue(conf.getVariable(var1, true));
        assertTrue(conf.getBooleanVariable(var1));
        assertEvent(popEvent(), var1, true);
        assertFalse(hasEvents());

        // Makes sure the rename method works properly.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertTrue(conf.getBooleanVariable(var2));
        assertEvent(popEvent(), var2, true);
        assertEvent(popEvent(), var1, null);
        assertFalse(hasEvents());

        // Makes sure that renaming a variable to another with the same value
        // only triggers the variable deletion event.
        conf.setVariable(var1, true);
        popEvent();
        conf.renameVariable(var1, var2);
        assertEvent(popEvent(), var1, null);
        assertFalse(hasEvents());

        // Makes sure that renaming a non-set variable results in deleting
        // both the source and target.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertEvent(popEvent(), var2, null);
        assertFalse(hasEvents());

        // Makes sure that meaningless renames leave the configuration untouched
        // and do not generate any event.
        conf.renameVariable(var1, var2);
        assertFalse(conf.isVariableSet(var1));
        assertFalse(conf.isVariableSet(var2));
        assertFalse(hasEvents());
    }

    /**
     * Runs boolean variable tests at different depths in the configuration tree.
     */
    public void testBooleanVariables() {
        StringBuffer section; // Name of the section in which to test boolean variables.

        testBooleanVariables("");

        section = new StringBuffer(BOOLEAN_SECTION);
        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testBooleanVariables(section.toString());
            section.append(BOOLEAN_SECTION);
        }
    }



    // - Pruning -------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Tests pruning of a whole branch, all the way down to the root, at various depths.
     */
    private void testCompletePrune(String section) {
        String variableName;
        String variableValue;

        variableName  = section + "variable";
        variableValue = "value";

        assertTrue(conf.setVariable(variableName, variableValue));
        assertEquals(variableValue, conf.removeVariable(variableName));
        assertTrue(conf.getRoot().isEmpty());
    }

    /**
     * Makes sure no pruning occur when a the section from which a variable has been removed is not empty.
     */
    private void testNoPrune(String section) {
        String variable1Name;
        String variable1Value;
        String variable2Name;
        String variable2Value;

        variable1Name  = section + "variable1";
        variable1Value = "value1";
        variable2Name  = section + "variable2";
        variable2Value = "value2";

        assertTrue(conf.setVariable(variable1Name, variable1Value));
        assertTrue(conf.setVariable(variable2Name, variable2Value));
        assertEquals(variable1Value, conf.removeVariable(variable1Name));
        assertEquals(variable2Value, conf.removeVariable(variable2Name));
    }

    /**
     * Makes sure pruning stops at the first non empty section.
     */
    private void testPartialPrune(String section) {
        String variable1Name;
        String variable1Value;
        String variable2Name;
        String variable2Value;

        variable1Name  = section + "section1.section2.variable1";
        variable1Value = "value1";
        variable2Name  = section + "variable2";
        variable2Value = "value2";

        assertTrue(conf.setVariable(variable1Name, variable1Value));
        assertTrue(conf.setVariable(variable2Name, variable2Value));
        assertEquals(variable1Value, conf.removeVariable(variable1Name));
        assertEquals(variable2Value, conf.removeVariable(variable2Name));
    }

    /**
     * Tests configuration tree pruning.
     */
    public void testPrune() {
        StringBuffer section;

        // Makes sure that 
        section = new StringBuffer();

        testCompletePrune("");
        section.append(0);

        for(int i = 0; i < MAX_DEPTH; i++) {
            section.append('.');
            testCompletePrune(section.toString());
            testPartialPrune(section.toString());
            testNoPrune(section.toString());
            section.append(i);
        }
    }

    // - ConfigurationReader -------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Ignored.
     */
    public void read(InputStream in, ConfigurationBuilder builder) {}

    /**
     * Returns the current instance.
     */
    public ConfigurationReader getReaderInstance() {return this;}

    /**
     * Makes sure configuration reader factory registration works as expected.
     */
    public void testReaderFactory() {
        // Makes sure that setting a custom reader factory will result in the right
        // instances being generated.
        conf.setReaderFactory(this);
        assertEquals(conf.getReaderFactory().toString(), identifier);
        try {assertEquals(conf.getReader().toString(), identifier);}
        catch(ReaderConfigurationException e) {fail();}

        // Makes sure that setting the reader factory to null restores default behaviour.
        conf.setReaderFactory(null);
        assertTrue(conf.getReaderFactory() instanceof XmlConfigurationReaderFactory);
        try {assertTrue(conf.getReader() instanceof XmlConfigurationReader);}
        catch(ReaderConfigurationException e) {fail();}
    }



    // - ConfigurationSource -------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Ignored.
     */
    public InputStream getInputStream() {return null;}

    /**
     * Ignored.
     * @see #setOutputStream(OutputStream)
     */
    public OutputStream getOutputStream() {return null;}

    /**
     * Makes sure configuration source registration works as expected.
     */
    public void testConfigurationSource() {
        conf.setSource(this);
        assertEquals(conf.getSource().toString(), identifier);

        conf.setSource(null);
        assertNull(conf.getSource());
    }



    // - ConfigurationWriter -------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Ignored.
     */
    public void startConfiguration() {}

    /**
     * Ignored.
     */
    public void endConfiguration() {}

    /**
     * Ignored.
     */
    public void startSection(String name) {}

    /**
     * Ignored.
     */
    public void endSection(String name) {}

    /**
     * Ignored.
     */
    public void addVariable(String name, String value) {}

    /**
     * Ignored.
     * @see #getOutputStream()
     */
    public void setOutputStream(OutputStream out) {}


    /**
     * Returns the current instance.
     */
    public ConfigurationWriter getWriterInstance() {return this;}

    /**
     * Makes sure configuration writer factory registration works as expected.
     */
    public void testWriterFactory() {
        // Makes sure that setting a custom writer factory will result in the right
        // instances being generated.
        conf.setWriterFactory(this);
        assertEquals(conf.getWriterFactory().toString(), identifier);
        try {assertEquals(conf.getWriter().toString(), identifier);}
        catch(WriterConfigurationException e) {fail();}

        // Makes sure that setting the writer factory to null restores default behaviour.
        conf.setWriterFactory(null);
        assertTrue(conf.getWriterFactory() instanceof XmlConfigurationWriterFactory);
        try {assertTrue(conf.getWriter() instanceof XmlConfigurationWriter);}
        catch(WriterConfigurationException e) {fail();}
    }




    // - Misc. ---------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Returns this instance's identifier.
     * @return this instance's identifier.
     */
    public String toString() {return identifier;}
}
