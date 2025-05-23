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

public class ClassInitialization extends ControlTokenConsumer {

    public final Type type;

    ClassInitialization(final Graph owner, final Type type) {
        super(owner, NodeType.ClassInitialization);
        this.type = type;
    }

    @Override
    public String additionalDebugInfo() {
        return ": " + type.getClassName();
    }

    public void deleteFromControlFlow() {
        owner.deleteFromControlFlowInternally(this);
    }

    @Override
    public ClassInitialization stampInto(final Graph target) {
        return target.newClassInitialization(type);
    }
}
