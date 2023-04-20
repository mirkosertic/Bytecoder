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
package de.mirkosertic.bytecoder.core.backend.wasm;

import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

        final VTable resolved = new VTable();
        final List<ResolvedClass> classesToCheck = new ArrayList<>();
        if (rc.superClass != null) {
            classesToCheck.add(rc.superClass);
        }
        Collections.addAll(classesToCheck, rc.interfaces);

        for (int i = classesToCheck.size() - 1; i >= 0; i--) {
            resolved.mergeWith(resolveFor(classesToCheck.get(i)));
        }

        for (final ResolvedMethod m : rc.resolvedMethods) {
            if ((m.owner == rc) && !Modifier.isStatic(m.methodNode.access) && !("<init>".equals(m.methodNode.name))) {
                if (!Modifier.isAbstract(m.methodNode.access) || rc.isOpaqueReferenceType()) {
                    final int methodId = methodToIDMapper.resolveIdFor(m);
                    resolved.register(methodId, m);
                }
            }
        }

        vtables.put(rc, resolved);
        return resolved;
    }
}
