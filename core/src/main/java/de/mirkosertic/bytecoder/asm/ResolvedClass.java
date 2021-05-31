/*
 * Copyright 2021 Mirko Sertic
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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResolvedClass {

    public enum Flag {
        USED_BY_METHOD_SIGNATURE,
        USED_AS_STATIC_CALL_TARGET,
        HAS_CLASS_INITIALIZER
    }

    final CompilationUnit compilationUnit;
    final ClassNode classNode;
    final Set<ResolvedClass> superTypes;
    final Set<ResolvedClass> subTypes;
    final Set<Flag> flags;
    final Map<String, ResolvedMethod> resolvedMethods;

    public ResolvedClass(final CompilationUnit compilationUnit, final ClassNode classNode) {
        this.compilationUnit = compilationUnit;
        this.classNode = classNode;
        this.superTypes = new HashSet<>();
        this.subTypes = new HashSet<>();
        this.flags = new HashSet<>();
        this.resolvedMethods = new HashMap<>();
    }

    public void flagWith(final Flag flag) {
        flags.add(flag);
    }

    public ResolvedMethod resolveStaticMethod(final String methodName, final String methodDescriptor) {
        final String key = methodName + " " + methodDescriptor;
        final ResolvedMethod known = resolvedMethods.get(key);
        if (known != null) {
            return known;
        }

        for (final MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(methodName) && methodNode.desc.equals(methodDescriptor) && Modifier.isStatic(methodNode.access)) {
                final ResolvedMethod newResolvedMethod = new ResolvedMethod(methodNode);
                resolvedMethods.put(key, newResolvedMethod);

                if (!Modifier.isAbstract(methodNode.access)) {
                    compilationUnit.enqueue(new AnalyzeMethodCommand(this, newResolvedMethod));
                }

                return newResolvedMethod;
            }
        }
        throw new IllegalArgumentException("No such method : " + methodName + "(" + methodDescriptor + ") in class " + Type.getObjectType(classNode.name).getClassName());
    }
}
