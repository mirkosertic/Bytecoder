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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TSystem {

    public static final PrintStream out = new PrintStream(new OutputStream() {

        @Import(module = "system", name = "writeByteArrayToConsole")
        public native void writeByteArrayToConsole(byte[] aBytes);

        @Override
        public void write(final int b) throws IOException {
            final byte[] theData = new byte[1];
            theData[0] = (byte) b;
            write(theData);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            writeByteArrayToConsole(b);
        }

        @Override
        public void close() throws IOException {
        }
    });

    public static final PrintStream err = new PrintStream(new OutputStream() {

        @Import(module = "system", name = "writeByteArrayToConsole")
        public native void writeByteArrayToConsole(byte[] aBytes);

        @Override
        public void write(final int b) throws IOException {
            final byte[] theData = new byte[1];
            theData[0] = (byte) b;
            write(theData);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            writeByteArrayToConsole(b);
        }

        @Override
        public void close() throws IOException {
        }
    });

    public static native long nanoTime();

    public static native long currentTimeMillis();

    public static native void logDebug(long aValue);

    public static native void logDebug(Object aValue);

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

    public static String getProperty(String aProperty) {
        return null;
    }
}