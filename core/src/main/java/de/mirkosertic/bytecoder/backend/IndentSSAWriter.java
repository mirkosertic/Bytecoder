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
package de.mirkosertic.bytecoder.backend;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.Program;

import java.io.PrintWriter;

public class IndentSSAWriter {

    protected final Program program;
    protected final BytecodeLinkerContext linkerContext;
    protected final String indent;
    protected final PrintWriter writer;
    protected boolean newLine;

    public IndentSSAWriter(Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext) {
        writer = aWriter;
        indent = aIndent;
        program = aProgram;
        linkerContext = aLinkerContext;
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

    public void printlnComment(String aComment) {
        checkNewLine();
        writer.print("// ");
        println(aComment);
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
