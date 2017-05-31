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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class NewArrayValue extends Value {

    private final BytecodeTypeRef type;
    private final Variable length;

    public NewArrayValue(BytecodeTypeRef aType, Variable aLength) {
        type = aType;
        length = aLength;
    }

    public BytecodeTypeRef getType() {
        return type;
    }

    public Variable getLength() {
        return length;
    }
}