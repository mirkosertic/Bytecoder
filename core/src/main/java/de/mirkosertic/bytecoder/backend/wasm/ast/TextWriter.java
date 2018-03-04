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
import java.util.List;

public class TextWriter {

    private final PrintWriter pw;
    private final String prefix;

    public TextWriter(PrintWriter aPw) {
        this(aPw, "");
    }

    public TextWriter(PrintWriter aPw, String aPrefix) {
        pw = aPw;
        prefix = aPrefix;
    }

    private TextWriter deeper() {
        return new TextWriter(pw, prefix + "    ");
    }

    public void write(SValue aValue) {
        if (aValue instanceof SExpression) {
            writeInternal((SExpression) aValue);
            return;
        }
        if (aValue instanceof SIntegerValue) {
            writeInternal((SIntegerValue) aValue);
            return;
        }
        throw new IllegalArgumentException("Not supported : " + aValue);
    }

    private void writeInternal(SIntegerValue aValue) {
        pw.print(aValue.getValue());
    }

    private void writeInternal(SExpression aExpression) {
        List<SValue> theValues = aExpression.getValues();

        if (theValues.isEmpty()) {
            pw.print(prefix);
            pw.print("(");
            pw.print(aExpression.getName());
            pw.println(")");
        } else {
            if (aExpression.isFlat()) {
                pw.print(prefix);
                pw.print("(");
                pw.print(aExpression.getName());

                for (SValue theValue : aExpression.getValues()) {
                    pw.print(" ");
                    write(theValue);
                }

                pw.println(")");
            } else {
                pw.print(prefix);
                pw.print("(");
                pw.println(aExpression.getName());

                TextWriter theDeeper = deeper();

                for (SValue theValue : aExpression.getValues()) {
                    theDeeper.write(theValue);
                }

                pw.print(prefix);
                pw.println(")");
            }
        }
    }
}
