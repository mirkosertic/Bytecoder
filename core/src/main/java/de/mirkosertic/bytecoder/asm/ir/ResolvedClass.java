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
package de.mirkosertic.bytecoder.asm.ir;

import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

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

    public ResolvedMethod classInitializer;

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
        final ResolvedMethod m = resolveMethodInternal(methodName, methodType, analysisStack);
        if (m == null) {
            throw new IllegalStateException("No such method : " + classNode.name + "." + methodName + methodType);
        }
        return m;
    }

    private ResolvedMethod resolveMethodInternal(final String methodName, final Type methodType, final AnalysisStack analysisStack) {
        for (final ResolvedMethod m : resolvedMethods) {
            final MethodNode methodNode = m.methodNode;
            if (methodNode.name.equals(methodName) && methodNode.desc.equals(methodType.getDescriptor())) {
                return m;
            }
        }
        for (int i = 0; i < classNode.methods.size(); i++) {
            final MethodNode methodNode = classNode.methods.get(i);
            if (methodNode.name.equals(methodName)) {
                final boolean polymorphic = AnnotationUtils.hasAnnotation("Ljava/lang/invoke/MethodHandle$PolymorphicSignature;", methodNode.visibleAnnotations);
                if (polymorphic || methodNode.desc.equals(methodType.getDescriptor())) {
                    final ResolvedMethod r = new ResolvedMethod(this, methodNode);
                    resolvedMethods.add(r);
                    r.parseBody(analysisStack);
                    return r;
                }
            }
        }
        for (final ResolvedClass interf : interfaces) {
            final ResolvedMethod m = interf.resolveMethodInternal(methodName, methodType, analysisStack);
            if (m != null) {
                return m;
            }
        }
        if (superClass != null) {
            return superClass.resolveMethodInternal(methodName, methodType, analysisStack);
        }
        return null;
    }

    public ResolvedField resolveField(final String name, final Type t) {
        for (final ResolvedField f : resolvedFields) {
            if (f.name.equals(name)) {
                return f;
            }
        }
        for (final FieldNode f : classNode.fields) {
            if (f.name.equals(name)) {
                final ResolvedField rf = new ResolvedField(this, name, Type.getType(f.desc), f.value, f.access);
                resolvedFields.add(rf);
                return rf;
            }
        }
        if (superClass != null) {
            return superClass.resolveField(name, t);
        }
        throw new IllegalStateException("No such field " + name + " in " + classNode.name);
    }

    public boolean requiresClassInitializer() {
        boolean requiresClassInitializer = classInitializer != null;
        if (superClass != null) {
            requiresClassInitializer = requiresClassInitializer | superClass.requiresClassInitializer();
        }
        return requiresClassInitializer;
    }

    public void finalizeLinkingHierarchy(final AnalysisStack analysisStack) {
        for (final ResolvedClass leaf : leafSubclasses()) {
            final List<ResolvedMethod> methods = new ArrayList<>(resolvedMethods);
            for (final ResolvedMethod m : methods) {
                if (!Modifier.isStatic(m.methodNode.access)) {
                    leaf.resolveMethodInternal(m.methodNode.name, Type.getMethodType(m.methodNode.desc), analysisStack);
                }
            }
        }
    }

    public Set<ResolvedClass> leafSubclasses() {
        final Set<ResolvedClass> subClasses = new HashSet<>();
        final Stack<ResolvedClass> workingStack = new Stack<>();
        workingStack.push(this);
        while (!workingStack.isEmpty()) {
            final ResolvedClass entry = workingStack.pop();
            if (entry.directSubclasses.isEmpty()) {
                subClasses.add(entry);
            } else {
                for (final ResolvedClass subclass : entry.directSubclasses) {
                    workingStack.push(subclass);
                }
            }
        }
        return subClasses;
    }

    public Set<ResolvedClass> allTypesOf() {
       final Set<ResolvedClass> result = new HashSet<>();
       result.add(this);
       for (final ResolvedClass interf : interfaces) {
           result.addAll(interf.allTypesOf());
       }
       if (this.superClass != null) {
           result.addAll(this.superClass.allTypesOf());
       }
       return result;
    }

    public Set<ResolvedMethod> abstractResolvedMethods() {
        final Set<ResolvedMethod> result = new HashSet<>();
        for (final ResolvedMethod m : resolvedMethods) {
            if (m.owner == this && Modifier.isAbstract(m.methodNode.access)) {
                result.add(m);
            }
        }
        for (final ResolvedClass interf : interfaces) {
            result.addAll(interf.abstractResolvedMethods());
        }
        if (superClass != null) {
            result.addAll(superClass.abstractResolvedMethods());
        }
        return result;
    }
}
