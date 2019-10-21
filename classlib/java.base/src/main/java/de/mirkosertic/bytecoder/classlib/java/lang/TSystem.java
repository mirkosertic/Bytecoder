/*
 * Copyright 2018 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

@SubstitutesInClass(completeReplace = true)
public class TSystem {

    private static final Properties PROPERTIES;

    static {
        PROPERTIES = new Properties();
        PROPERTIES.setProperty("java.awt.graphicsenv", "de.mirkosertic.bytecoder.classlib.BytecoderGraphicsEnvironment");
    }

    public static final InputStream in = new FileInputStream(FileDescriptor.in);

    public static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));

    public static final PrintStream err = new PrintStream(new FileOutputStream(FileDescriptor.err));

    public static native long nanoTime();

    public static native long currentTimeMillis();

    public static void arraycopy(final Object aSource, final int aSourcePos, final Object aTarget, final int aTargetPos, final int aLength) {
        final Object[] theSource = (Object[]) aSource;
        final Object[] theTarget = (Object[]) aTarget;
        for (int i=0;i<aLength;i++) {
            theTarget[aTargetPos + i] = theSource[aSourcePos + i];
        }
    }

    public static int identityHashCode(final Object aValue) {
        return aValue.hashCode();
    }

    public static SecurityManager getSecurityManager() {
        return null;
    }

    public static String getProperty(final String aProperty) {
        return PROPERTIES.getProperty(aProperty);
    }

    public static String getProperty(final String aProperty, final String aDefault) {
        return PROPERTIES.getProperty(aProperty, aDefault);
    }

    public static String setProperty(final String aProperty, final String value) {
        return (String) PROPERTIES.setProperty(aProperty, value);
    }

    public static Properties getProperties() {
        return PROPERTIES;
    }

    public static String lineSeparator() {
        return "\n";
    }

    public static String getenv(final String name) {
        return null;
    }

    public static void loadLibrary(final String name) {
        // System libraries cannot be loaded by Bytecoder.
        // However, native parts are linked using the Bytecoder module imports
    }

    public static void exit(final int exitCode) {
    }

    public static void gc() {
    }
}