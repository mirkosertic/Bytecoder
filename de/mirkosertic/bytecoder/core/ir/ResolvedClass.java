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

import de.mirkosertic.bytecoder.api.Callback;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
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

    private Boolean isOpaqueReferenceType;

    private Boolean isCallback;

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

    public boolean isOpaqueReferenceType() {
        return isOpaqueReferenceType;
    }

    public boolean isCallback() {
        return isCallback;
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
        final ResolvedMethod m = resolveMethodInternal(methodName, methodType, analysisStack, false);
        if (m == null) {
            throw new AnalysisException(
                    new IllegalStateException("No such method : " + classNode.name + "." + methodName + methodType),
                    analysisStack
            );
        }
        return m;
    }

    private String printStackTrace(AnalysisStack analysisStack) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream stringStream = new PrintStream(bos);
        analysisStack.dumpAnalysisStack(stringStream);
        return "\n" + bos;
    }

    private ResolvedMethod resolveMethodInternal(final String methodName, final Type methodType, final AnalysisStack analysisStack, final boolean onlyImplementations) {
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
                    if (onlyImplementations && (Modifier.isAbstract(methodNode.access) || Modifier.isNative(methodNode.access))) {
                        continue;
                    }
                    final ResolvedMethod r = new ResolvedMethod(this, methodNode, Type.getMethodType(methodNode.desc));
                    resolvedMethods.add(r);
                    r.parseBody(analysisStack);
                    return r;
                }
            }
        }

        final List<ResolvedClass> classesToCheck = new ArrayList<>();
        if (superClass != null) {
            classesToCheck.add(superClass);
        }
        Collections.addAll(classesToCheck, interfaces);

        for (final ResolvedClass classToCheck : classesToCheck) {
            final ResolvedMethod m = classToCheck.resolveMethodInternal(methodName, methodType, analysisStack, onlyImplementations);
            if (m != null) {
                if (onlyImplementations) {
                    if (!(Modifier.isAbstract(m.methodNode.access) || Modifier.isNative(m.methodNode.access))) {
                        return m;
                    }
                } else {
                    return m;
                }
            }
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
        final List<ResolvedMethod> methods = new ArrayList<>(resolvedMethods);
        for (final ResolvedClass leaf : leafSubclasses()) {
            for (final ResolvedMethod m : methods) {
                if (!Modifier.isStatic(m.methodNode.access)) {
                    leaf.resolveMethodInternal(m.methodNode.name, m.methodType, analysisStack, true);
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

    public Set<MethodNode> abstractMethods() {
        final Set<MethodNode> result = new HashSet<>();
        for (final MethodNode m : classNode.methods) {
            if (Modifier.isAbstract(m.access)) {
                result.add(m);
            }
        }
        for (final ResolvedClass interf : interfaces) {
            result.addAll(interf.abstractMethods());
        }
        if (superClass != null) {
            result.addAll(superClass.abstractMethods());
        }
        return result;
    }

    public void computeOpaqueReferenceTypeAndCallbackStatus(final AnalysisStack analysisStack) {
        if (isOpaqueReferenceType == null) {
            isOpaqueReferenceType = allTypesOf().stream().anyMatch(t -> t.type.getClassName().equals(OpaqueReferenceType.class.getName()));
        }
        if (isCallback == null) {
            isCallback = allTypesOf().stream().anyMatch(t -> t.type.getClassName().equals(Callback.class.getName()));
            if (isCallback) {
                for (final MethodNode m : classNode.methods) {
                    if (!Modifier.isStatic(m.access) && !("<init>".equals(m.name)) && !("<clinit>".equals(m.name))) {
                        final Type mt = Type.getMethodType(m.desc);
                        resolveMethod(m.name, mt, analysisStack);
                        for (final Type argument : mt.getArgumentTypes()) {
                            if (argument.getSort() == Type.OBJECT) {
                                compileUnit.resolveClass(argument, analysisStack);
                            }
                        }
                    }
                }
            }
        }
    }
}
