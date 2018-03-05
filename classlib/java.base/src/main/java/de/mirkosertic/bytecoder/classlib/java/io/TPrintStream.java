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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TPrintStream extends FilterOutputStream {

    public TPrintStream(OutputStream aTarget) {
        super(aTarget);
    }

    public void println(long aValue) throws IOException {
        print(Long.toString(aValue));
        println();
    }

    public void println(int aValue) throws IOException {
        print(Integer.toString(aValue));
        println();
    }

    public void print(char aChar) throws IOException {
        write((int) aChar);
    }

    public void println() throws IOException {
        print(VM.NEWLINE);
    }

    public void print(String aValue) throws IOException {
        write(aValue.getBytes());
    }

    public void println(String aValue) throws IOException {
        print(aValue);
        println();
    }
}