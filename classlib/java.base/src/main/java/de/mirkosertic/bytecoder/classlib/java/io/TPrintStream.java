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
package de.mirkosertic.bytecoder.classlib.java.io;

import java.io.IOException;

import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;

public class TPrintStream extends TFilterOutputStream {

    public static final char NEWLINE = '\n';

    private final TOutputStream target;

    public TPrintStream(TOutputStream aTarget) {
        target = aTarget;
    }

    @Import(module = "system", name = "logDebug")
    public native void logDebug(long aValue);

    public void println(long aValue) {
        logDebug(aValue);
    }

    public void println(int aValue) {
        logDebug(aValue);
    }

    public void println() throws IOException {
        print(NEWLINE);
    }

    public void println(TString aValue) throws IOException {
        for (int i=0;i<aValue.length();i++) {
            print((char) aValue.charAt(i));
        }
        print(NEWLINE);
    }

    public void print(char aChar) throws IOException {
        target.write(aChar);
    }

    @Override
    public void write(int aValue) throws IOException {
        target.write(aValue);
    }

    @Override
    public void close() throws IOException {
    }
}