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
package de.mirkosertic.bytecoder.asm.backend.wasm;

import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;

import java.util.HashMap;
import java.util.Map;

public class VTable {

    private final Map<Integer, ResolvedMethod> methods;

    public VTable() {
        methods = new HashMap<>();
    }

    public VTable(final VTable copySource) {
        methods = new HashMap<>(copySource.methods);
    }

    public void mergeWith(final VTable source) {
        methods.putAll(source.methods);
    }

    public void register(final int methodId, final ResolvedMethod method) {
        methods.put(methodId, method);
    }

    public Map<Integer, ResolvedMethod> getMethods() {
        return methods;
    }
}
