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

import java.io.PrintWriter;
import java.io.Writer;

public class JSPrintWriter extends PrintWriter {

    private final JSMinifier minifier;

    public JSPrintWriter(final Writer out, final JSMinifier minifier) {
        super(out);
        this.minifier = minifier;
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

    public JSPrintWriter newLine() {
        print(minifier.newLine());
        return this;
    }

    public JSPrintWriter colon() {
        return space().text(":").space();
    }

    public JSPrintWriter assign() {
        return space().text("=").space();
    }
}
