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
package de.mirkosertic.bytecoder.core.backend.wasm.ast;


public class F64Const implements WasmValue {

    private final double value;

    F64Const(final double value) {
        this.value = value;
    }

    @Override
    public void writeTo(final TextWriter textWriter, final ExportContext context) {
        textWriter.opening();
        textWriter.write("f64.const");
        textWriter.space();
        if (Double.isNaN(value)) {
            textWriter.write("nan");
        } else if (Double.isInfinite(value)) {
            textWriter.write("inf");
        } else {
            textWriter.writeDouble(value);
        }
        textWriter.closing();
    }

    @Override
    public void writeTo(final BinaryWriter.Writer codeWriter, final ExportContext context) {
        codeWriter.writeByte((byte) 0x44);
        codeWriter.writeDouble64(value);
    }
}
