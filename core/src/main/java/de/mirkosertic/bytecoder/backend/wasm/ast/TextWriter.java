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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import java.io.PrintWriter;

public class TextWriter implements AutoCloseable {

    private final PrintWriter pw;
    private int indent;

    public TextWriter(final PrintWriter pw) {
        this.pw = pw;
        this.indent = 0;
    }

    public void opening() {
        pw.print("(");
        indent++;
    }

    public void write(final String value) {
        pw.print(value);
    }

    public void newLine() {
        pw.println();
        for (int i=0;i<indent;i++) {
            pw.print("    ");
        }
    }

    public void closing() {
        pw.print(")");
        indent--;
    }

    public void writeText(final String text) {
        pw.print("\"");
        pw.print(text);
        pw.print("\"");
    }

    public void writeInteger(final int value) {
        pw.print(value);
    }

    public void writeAttribute(final String name, final int value) {
        pw.print(name);
        pw.print("=");
        pw.print(value);
    }

    public void space() {
        pw.print(" ");
    }

    public void writeLabel(final String label) {
        pw.print("$");
        pw.print(label);
    }

    public void writeFloat(final float value) {
        pw.print(value);
    }

    @Override
    public void close() {
        pw.close();
    }
}
