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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Type;

public class ResolvedField {

    public final ResolvedClass owner;
    public final String name;
    public final Type type;
    public final int access;

    public ResolvedField(final ResolvedClass owner, final String name, final Type type, final int access) {
        this.owner = owner;
        this.name = name;
        this.type = type;
        this.access = access;
    }
}
