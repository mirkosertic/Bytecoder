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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResolvedClass {

    public final Type type;

    public final ClassNode classNode;

    public final ResolvedClass superClass;

    public final ResolvedClass[] interfaces;

    public final CompileUnit compileUnit;

    public final Set<ResolvedClass> directSubclasses;

    public final List<ResolvedMethod> resolvedMethods;

    public final List<ResolvedField> resolvedFields;

    boolean needsInitialization;

    private ResolvedMethod classInitializer;

    public ResolvedClass(final CompileUnit compileUnit, final Type type, final ClassNode classNode, final ResolvedClass superClass, final ResolvedClass[] interfaces) {
        this.compileUnit = compileUnit;
        this.type = type;
        this.classNode = classNode;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.directSubclasses = new HashSet<>();
        this.resolvedMethods = new ArrayList<>();
        this.resolvedFields = new ArrayList<>();
        this.needsInitialization = true;
        this.classInitializer = null;
        if (superClass != null) {
            superClass.registerDirectSubclass(this);
        }
        for (final ResolvedClass interf : interfaces) {
            interf.registerDirectSubclass(this);
        }
    }

    public ResolvedClass requestInitialization(final AnalysisStack analysisStack) {
        if (needsInitialization) {
            needsInitialization = false;
            if (superClass != null) {
                superClass.requestInitialization(analysisStack);
            }
            for (final ResolvedClass interf : interfaces) {
                interf.requestInitialization(analysisStack);
            }
            for (final MethodNode m : classNode.methods) {
                if (Modifier.isStatic(m.access) && "<clinit>".equals(m.name)) {
                    this.classInitializer = resolveMethod(m.name, Type.getMethodType(m.desc), analysisStack);
                }
            }
        }
        return this;
    }

    public void registerDirectSubclass(final ResolvedClass cl) {
        directSubclasses.add(cl);
    }

    public ResolvedMethod resolveMethod(final String methodName, final Type methodType, final AnalysisStack analysisStack) {
        for (final ResolvedMethod m : resolvedMethods) {
            final MethodNode methodNode = m.methodNode;
            if (methodNode.name.equals(methodName) && methodNode.desc.equals(methodType.getDescriptor())) {
                return m;
            }
        }
        for (int i = 0; i < classNode.methods.size(); i++) {
            final MethodNode methodNode = classNode.methods.get(i);
            if (methodNode.name.equals(methodName) && methodNode.desc.equals(methodType.getDescriptor())) {
                final ResolvedMethod r = new ResolvedMethod(this, methodNode);
                resolvedMethods.add(r);
                r.parseBody(analysisStack);
                return r;
            }
        }
        if (superClass != null) {
            return superClass.resolveMethod(methodName, methodType, analysisStack);
        }
        throw new RuntimeException("No such method : " + methodName + methodType);
    }

    public ResolvedField resolveField(final String name, final Type t) {
        for (final ResolvedField f : resolvedFields) {
            if (f.name.equals(name)) {
                return f;
            }
        }
        for (final FieldNode f : classNode.fields) {
            if (f.name.equals(name)) {
                final ResolvedField rf = new ResolvedField(this, name, f.access);
                resolvedFields.add(rf);
                return rf;
            }
        }
        if (superClass != null) {
            return superClass.resolveField(name, t);
        }
        throw new IllegalStateException("No such field " + name + " in " + classNode.name);
    }
}
