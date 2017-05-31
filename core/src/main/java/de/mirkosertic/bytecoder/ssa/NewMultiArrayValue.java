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

import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class NewMultiArrayValue extends Value {

    private final BytecodeTypeRef type;
    private final List<Variable> dimensions;

    public NewMultiArrayValue(BytecodeTypeRef aType, List<Variable> aDimensions) {
        type = aType;
        dimensions = aDimensions;
    }

    public BytecodeTypeRef getType() {
        return type;
    }

    public List<Variable> getDimensions() {
        return dimensions;
    }
}