
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
package de.mirkosertic.bytecoder.core.ir;

public class MethodReference extends PrimitiveValue {

    public enum Kind {

        INVOKESTATIC(1), INVOKEVIRTUAL(2), INVOKEINTERFACE(3), INVOKECONSTRUCTOR(4), INVOKESPECIAL(5);

        private final int id;

        Kind(final int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public final ResolvedMethod resolvedMethod;

    public final Kind kind;

    public MethodReference(final ResolvedMethod resolvedMethod, final Kind kind) {
        super(resolvedMethod.methodType);
        this.resolvedMethod = resolvedMethod;
        this.kind = kind;
    }


    @Override
    public String additionalDebugInfo() {
        return ": " + resolvedMethod.owner.type +"." + resolvedMethod.methodNode.name + type ;
    }
}
