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
package de.mirkosertic.bytecoder.backend.js;

import java.io.PrintWriter;

public class JSWriter {

    private final String indent;
    private final PrintWriter writer;
    private boolean newLine;

    public JSWriter(String aIndent, PrintWriter aWriter) {
        indent = aIndent;
        writer = aWriter;
        newLine = true;
    }

    private void checkNewLine(){
        if (newLine) {
            writer.print(indent);
            newLine = false;
        }
    }

    public void print(String s) {
        checkNewLine();
        writer.print(s);
    }

    public void println() {
        checkNewLine();
        writer.println();
        newLine = true;
    }

    public void println(String s) {
        checkNewLine();
        writer.println(s);
        newLine = true;
    }

    public void print(byte b) {
        checkNewLine();
        writer.print(b);
    }

    public void print(short s) {
        checkNewLine();
        writer.print(s);
    }

    public void print(int i) {
        checkNewLine();
        writer.print(i);
    }

    public void print(long l) {
        checkNewLine();
        writer.print(l);
    }

    public void print(float f) {
        checkNewLine();
        writer.print(f);
    }

    public void print(double d) {
        checkNewLine();
        writer.print(d);
    }
}