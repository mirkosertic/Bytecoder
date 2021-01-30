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

public class Alignment {

    public static final Alignment ONE = new Alignment(1);

    public static final Alignment TWO = new Alignment(2);

    public static final Alignment FOUR = new Alignment(4);

    public final int value;

    Alignment(final int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public int log2Value() {
        return (int) (Math.log(value) / Math.log(2));
    }
}
