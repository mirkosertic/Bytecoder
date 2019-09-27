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

import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

@SubstitutesInClass(completeReplace = true)
public class TSystem {

    public static final class ConsoleOutputStream extends OutputStream {

        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        @Import(module = "system", name = "writeCharArrayToConsole")
        public static native void writeCharArrayToConsole(char[] aBytes);

        @Override
        public void write(final int b) {
            if (b != '\n') {
                bos.write(b);
            } else {
                final byte[] toData = bos.toByteArray();
                final String theString = new String(toData);
                final char[] theChars = new char[theString.length()];
                for (int i=0;i<theString.length();i++) {
                    theChars[i] = theString.charAt(i);
                }
                writeCharArrayToConsole(theChars);
                bos.reset();
            }
        }

        @Override
        public void close() {
        }
    }

    public static final PrintStream out = new PrintStream(new ConsoleOutputStream());

    public static final PrintStream err = new PrintStream(new ConsoleOutputStream());

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
        return null;
    }

    public static String getProperty(final String aProperty, final String aDefault) {
        return aDefault;
    }

    public static String lineSeparator() {
        return "\n";
    }
}