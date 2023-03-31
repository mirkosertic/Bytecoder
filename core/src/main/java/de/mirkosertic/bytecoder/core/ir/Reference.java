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

import org.objectweb.asm.Type;

public class Reference extends PrimitiveValue {

    public enum Kind {

        INVOKESTATIC(1), INVOKEVIRTUAL(2), INVOKEINTERFACE(3), INVOKECONSTRUCTOR(4), INVOKESPECIAL(5), GETINSTANCEFIELD(6);

        private final int id;

        Kind(final int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    public final FieldReference.Kind kind;

    public Reference(final Type type, final FieldReference.Kind kind) {
        super(type);
        this.kind = kind;
    }
}
