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
package de.mirkosertic.bytecoder.asm.parser;

import de.mirkosertic.bytecoder.asm.AnalysisStack;
import de.mirkosertic.bytecoder.asm.AnnotationUtils;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class CompileUnit {

    public static final String MAIN_ENTRY_POINT_EXPORT = "main";

    private final Loader loader;

    private final Map<String, ResolvedClass> resolvedClasses;

    private final Intrinsic intrinsic;

    private final Map<String, ResolvedMethod> exportedMethods;

    private final ConstantPool constantPool;

    public CompileUnit(final Loader loader, final Intrinsic intrinsic) {
        this.loader = loader;
        this.resolvedClasses = new HashMap<>();
        this.intrinsic = intrinsic;
        this.exportedMethods = new HashMap<>();
        this.constantPool = new ConstantPool();
    }

    protected Intrinsic getIntrinsic() {
        return intrinsic;
    }

    public ConstantPool getConstantPool() {
        return constantPool;
    }

    public ResolvedClass resolveClass(final Type type, final AnalysisStack analysisStack) {
        final String resourceName = type.getClassName().replace(".", "/") + ".class";
        ResolvedClass rs = resolvedClasses.get(resourceName);
        if (rs != null) {
            return rs;
        }
        try {
            rs = loadClass(type, loader.loadClassFor(type), analysisStack);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        resolvedClasses.put(resourceName, rs);
        return rs.requestInitialization(analysisStack);
    }

    private ResolvedClass loadClass(final Type type, final ClassNode classNode, final AnalysisStack analysisStack) {

        final AnalysisStack importedStack = analysisStack.addAction(new AnalysisStack.Action("Resolving type " + type));

        ResolvedClass superClass = null;
        if (classNode.superName != null && !AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/IsObject;", classNode.visibleAnnotations)) {
            superClass = resolveClass(Type.getObjectType(classNode.superName), importedStack);
        }
        final List<ResolvedClass> interfaces = new ArrayList<>();
        for (final String interf : classNode.interfaces) {
            interfaces.add(resolveClass(Type.getObjectType(interf), importedStack));
        }

        return new ResolvedClass(this, type, classNode, superClass, interfaces.toArray(new ResolvedClass[0]));
    }

    private void computeSubtypesFor(final ResolvedClass cl, final int level, final Map<ResolvedClass, Integer> dependency) {
        final Integer l = dependency.get(cl);
        if (l == null || level > l) {
            dependency.put(cl, level);
        }
        for (final ResolvedClass sub : cl.directSubclasses) {
            computeSubtypesFor(sub, level + 1, dependency);
        }
    }

    public List<ResolvedClass> computeClassDependencies() {
        ResolvedClass objectClass = null;
        for (final ResolvedClass ent : resolvedClasses.values()) {
            if (ent.superClass == null) {
                objectClass = ent;
            }
        }
        if (objectClass == null) {
            throw new IllegalStateException("Cannot find object class");
        }
        final Map<ResolvedClass, Integer> dependency = new HashMap<>();
        computeSubtypesFor(objectClass, 0, dependency);

        final List<ResolvedClass> classDependencies = new ArrayList<>(dependency.keySet());
        classDependencies.sort(Comparator.comparingInt(dependency::get));

        return classDependencies;
    }

    public ResolvedMethod resolveMainMethod(final Type invokedType, final String methodName, final Type methodType) {
        final AnalysisStack analysisStack = new AnalysisStack();
        final ResolvedClass resolvedClass = resolveClass(invokedType, analysisStack);
        final ResolvedMethod method = resolvedClass.resolveMethod(methodName, methodType, analysisStack);

        exportedMethods.put(MAIN_ENTRY_POINT_EXPORT, method);

        return method;
    }

    public void finalizeLinkingHierarchy() {
        System.out.println("Finalizing linking hierarchy");
        final AnalysisStack analysisStack = new AnalysisStack();
        boolean modified = true;
        final Supplier<List<ResolvedClass>> currentList = () -> new ArrayList<>(resolvedClasses.values());
        while (modified) {
            final List<ResolvedClass> lst = currentList.get();
            for (final ResolvedClass cl : lst) {
                cl.finalizeLinkingHierarchy(analysisStack);
            }
            modified = currentList.get().size() != lst.size();
        }
    }

    public void printStatisticsTo(final PrintStream ps) {
        int numberOfClasses = 0;
        int numberOfInterfaces = 0;
        int numberOfAbstractClasses = 0;
        int numberOfFinalClasses = 0;
        int numberOfMethods = 0;
        int numberOfNativeMethods = 0;

        for (final ResolvedClass cl : resolvedClasses.values()) {
            numberOfClasses++;
            if (Modifier.isInterface(cl.classNode.access)) {
                numberOfInterfaces++;
            }
            if (Modifier.isAbstract(cl.classNode.access)) {
                numberOfAbstractClasses++;
            }
            if (Modifier.isFinal(cl.classNode.access)) {
                numberOfFinalClasses++;
            }
            for (final ResolvedMethod m : cl.resolvedMethods) {
                if (m.owner == cl) {
                    numberOfMethods++;
                    if (Modifier.isNative(m.methodNode.access)) {
                        numberOfNativeMethods++;
                    }
                }
            }
        }

        ps.println("Linkage statistics:");
        ps.print("  Resolved classes in total : ");
        ps.println(numberOfClasses);
        ps.print("    # interfaces            : ");
        ps.println(numberOfInterfaces);
        ps.print("    # abstract classes      : ");
        ps.println(numberOfAbstractClasses);
        ps.print("    # final classes         : ");
        ps.println(numberOfFinalClasses);
        ps.print("  Resolved methods in total : ");
        ps.println(numberOfMethods);
        ps.print("    # native methods        : ");
        ps.println(numberOfNativeMethods);

        for (final ResolvedClass cl : resolvedClasses.values()) {
            for (final ResolvedMethod m : cl.resolvedMethods) {
                if (m.owner == cl) {
                    if (Modifier.isNative(m.methodNode.access)) {
                        ps.print("        ");
                        ps.print(cl.type.getClassName());
                        ps.print(".");
                        ps.print(m.methodNode.name);
                        ps.println(m.methodNode.desc);
                    }
                }
            }
        }

    }

    public void processExportedMethods(final BiConsumer<String, ResolvedMethod> processor) {
        for (final Map.Entry<String, ResolvedMethod> entry : exportedMethods.entrySet()) {
            processor.accept(entry.getKey(), entry.getValue());
        }
    }
}
