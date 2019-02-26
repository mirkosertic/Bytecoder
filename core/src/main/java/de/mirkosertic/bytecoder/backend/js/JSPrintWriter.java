/*
 * Copyright 2019 Mirko Sertic
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

import de.mirkosertic.bytecoder.backend.SourceMapWriter;
import de.mirkosertic.bytecoder.ssa.DebugPosition;

import java.io.IOException;
import java.io.Writer;

public class JSPrintWriter {

    private final JSMinifier minifier;
    private final Writer out;
    private int lineCounter;
    private int columnCounter;
    private final SourceMapWriter sourceMapWriter;

    public JSPrintWriter(final Writer out, final JSMinifier minifier, final SourceMapWriter sourceMapWriter) {
        this.out = out;
        this.minifier = minifier;
        this.lineCounter = 0;
        this.columnCounter = 0;
        this.sourceMapWriter = sourceMapWriter;
    }

    public JSPrintWriter print(final String aText) {
        try {
            out.write(aText);
            columnCounter += aText.length();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public JSPrintWriter tab() {
        print(minifier.tab());
        return this;
    }

    public JSPrintWriter tab(final int num) {
        for (int i=0;i<num;i++) {
            print(minifier.tab());
        }
        return this;
    }

    public JSPrintWriter space() {
        print(minifier.space());
        return this;
    }

    public JSPrintWriter text(final String aText) {
        print(aText);
        return this;
    }

    public JSPrintWriter symbol(final String aSymbol, final DebugPosition aPositionl) {
        sourceMapWriter.assignName(lineCounter, columnCounter, aSymbol, aPositionl);
        print(minifier.toSymbol(aSymbol));
        return this;
    }

    public void assignPositionToSourceFile(final DebugPosition aPosition) {
        sourceMapWriter.assignDebugPosition(lineCounter, columnCounter, aPosition);
    }

    public void assignSymbolToSourceFile(final String aSymbol, final DebugPosition aPosition) {
        sourceMapWriter.assignName(lineCounter, columnCounter, aSymbol, aPosition);
    }

    public JSPrintWriter newLine() {
        final String theNewLine = minifier.newLine();
        if (theNewLine.length() != 0) {
            print(theNewLine);
            lineCounter++;
            columnCounter=0;
        }
        return this;
    }

    public JSPrintWriter colon() {
        return space().text(":").space();
    }

    public JSPrintWriter assign() {
        return space().text("=").space();
    }

    public void flush() {
        try {
            out.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
