/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.cpp;

import de.mirkosertic.bytecoder.ssa.DebugPosition;
import de.mirkosertic.bytecoder.ssa.Label;

import java.io.IOException;
import java.io.Writer;

public class CPPPrintWriter {

    private final CPPMinifier minifier;
    private final Writer out;

    public CPPPrintWriter(final Writer out, final CPPMinifier minifier) {
        this.minifier = minifier;
        this.out = out;
    }

    public CPPPrintWriter print(final String aText) {
        try {
            out.write(aText);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public CPPPrintWriter tab() {
        print("\t");
        return this;
    }

    public CPPPrintWriter tab(final int num) {
        for (int i=0;i<num;i++) {
            tab();
        }
        return this;
    }

    public CPPPrintWriter space() {
        print(" ");
        return this;
    }

    public CPPPrintWriter text(final String aText) {
        print(aText);
        return this;
    }

    public CPPPrintWriter newLine() {
        print("\n");
        return this;
    }

    public CPPPrintWriter assign() {
        return space().text("=").space();
    }

    public CPPPrintWriter label(final Label label) {
        return text("$").text(minifier.toSymbol(label.name()));
    }

    public CPPPrintWriter colon() {
        return space().text(":").space();
    }

    public CPPPrintWriter symbol(final String aSymbol, final DebugPosition aPosition) {
        print(minifier.toSymbol(aSymbol));
        return this;
    }

    public void flush() {
        try {
            out.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
