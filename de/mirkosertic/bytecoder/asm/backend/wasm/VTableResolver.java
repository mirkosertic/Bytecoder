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

import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class VTableResolver {

    private final MethodToIDMapper methodToIDMapper;
    private final Map<ResolvedClass, VTable> vtables;

    public VTableResolver(final MethodToIDMapper methodToIDMapper) {
        this.methodToIDMapper = methodToIDMapper;
        this.vtables = new HashMap<>();
    }

    public VTable resolveFor(final ResolvedClass rc) {
        final VTable known = vtables.get(rc);
        if (known != null) {
            return known;
        }
        final VTable resolved;
        if (rc.superClass != null) {
            resolved = new VTable(resolveFor(rc.superClass));
        } else {
            resolved = new VTable();
        }

        for (final ResolvedClass interf : rc.interfaces) {
            resolved.mergeWith(resolveFor(interf));
        }

        for (final ResolvedMethod m : rc.resolvedMethods) {
            if ((m.owner == rc) && !Modifier.isStatic(m.methodNode.access) && !Modifier.isAbstract(m.methodNode.access) && !("<init>".equals(m.methodNode.name))) {
                final int methodId = methodToIDMapper.resolveIdFor(m);
                resolved.register(methodId, m);
            }
        }

        vtables.put(rc, resolved);
        return resolved;
    }
}
