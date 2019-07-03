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

import java.io.IOException;

public enum PrimitiveType {
    i32("i32", (byte) 0x7f),
    f32("f32", (byte) 0x7d),
    i64("i64", (byte) 0x7e),
    f64("f64", (byte) 0x7c),
    anyfunc("anyfunc", (byte) 0x70),
    func("func", (byte) 0x60),
    empty_pseudo_block("pseudo", (byte) 0x40),
    anyref("anyref", (byte) 0x6f),
    except_ref("except_ref", (byte) -0x18);

    private final String text;
    private final byte binaryType;

    PrimitiveType(final String text,final byte binaryType) {
        this.text = text;
        this.binaryType = binaryType;
    }

    public void writeTo(final TextWriter textWriter) {
        textWriter.write(text);
    }

    public void writeTo(final BinaryWriter.Writer binaryWriter) throws IOException {
        binaryWriter.writeByte(binaryType);
    }

    public byte getBinaryType() {
        return binaryType;
    }
}