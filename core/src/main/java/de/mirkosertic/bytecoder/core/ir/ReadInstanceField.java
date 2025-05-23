/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.ir;

import org.objectweb.asm.Type;

public class ReadInstanceField extends Value {

    public final ResolvedField resolvedField;

    ReadInstanceField(final Graph owner, final Type type, final ResolvedField resolvedField) {
        super(owner, type, NodeType.ReadInstanceField);
        this.resolvedField = resolvedField;
    }

    @Override
    public String additionalDebugInfo() {
        return ": " + resolvedField.name;
    }

    @Override
    public ReadInstanceField stampInto(final Graph target) {
        return target.newInstanceFieldExpression(type, resolvedField);
    }
}
