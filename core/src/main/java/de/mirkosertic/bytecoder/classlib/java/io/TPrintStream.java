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

import de.mirkosertic.bytecoder.annotations.Import;
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

    public void println(TString aValue) throws TIOException {
        for (int i=0;i<aValue.length();i++) {
            print((char) aValue.charAt(i));
        }
        print(NEWLINE);
    }

    public void print(char aChar) throws TIOException {
        target.write(aChar);
    }

    @Override
    public void write(int aValue) throws TIOException {
        target.write(aValue);
    }
}