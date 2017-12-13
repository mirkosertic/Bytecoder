/*
 * Copyright 2017 Mirko Sertic
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

import de.mirkosertic.bytecoder.annotations.Import;
import de.mirkosertic.bytecoder.classlib.java.io.TOutputStream;
import de.mirkosertic.bytecoder.classlib.java.io.TPrintStream;

public class TSystem {

    public static final TPrintStream out = new TPrintStream(new TOutputStream() {

        @Import(module = "system", name = "logByteArrayAsString")
        public native void writeByteArrayToConsole(byte[] aBytes);

        private TStringBuilder currentLine = new TStringBuilder();

        @Override
        public void write(int aValue) throws IOException {
            if (aValue != TPrintStream.NEWLINE) {
                currentLine.append((char) aValue);
            } else {
                writeByteArrayToConsole(currentLine.getBytes());
                currentLine = new TStringBuilder();
            }
        }

        @Override
        public void close() throws IOException {
        }
    });

    @Import(module = "system", name = "nanoTime")
    public static native long nanoTime();

    @Import(module = "system", name = "currentTimeMillis")
    public static native long currentTimeMillis();

    @Import(module = "system", name = "logDebug")
    public static native void logDebug(long aValue);

    @Import(module = "system", name = "logDebug")
    public static native void logDebug(Object aValue);

    public static void arraycopy(Object aSource, int aSourcePos, Object aTarget, int aTargetPos, int aLength) {
        Object[] theSource = (Object[]) aSource;
        Object[] theTarget = (Object[]) aTarget;
        for (int i=0;i<aLength;i++) {
            theTarget[aTargetPos + i] = theSource[aSourcePos + i];
        }
    }
}