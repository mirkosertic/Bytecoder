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

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.asm.AnalysisStack;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompileUnit {

    private final ClassLoader classLoader;

    private final Map<Type, ResolvedClass> resolvedClasses;

    private final Intrinsic intrinsic;

    public CompileUnit(final ClassLoader classLoader, final Intrinsic intrinsic) {
        this.classLoader = classLoader;
        this.resolvedClasses = new HashMap<>();
        this.intrinsic = intrinsic;
    }

    protected Intrinsic getIntrinsic() {
        return intrinsic;
    }

    public ResolvedClass resolveClass(final Type type, final AnalysisStack analysisStack) {
        return resolvedClasses.computeIfAbsent(type, key -> {
            final String theResourceName = key.getClassName().replace(".", "/") + ".class";
            for (final ClassLibProvider theProvider : ClassLibProvider.availableProviders()) {
                final InputStream is = theProvider.getClass().getClassLoader().getResourceAsStream(theProvider.getResourceBase() + "/" + theResourceName);
                if (is != null) {
                    try {
                        return loadClass(key, is, analysisStack);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            final InputStream fromRoot = classLoader.getResourceAsStream(theResourceName);
            if (fromRoot != null) {
                try {
                    return loadClass(key, fromRoot, analysisStack);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
            throw new RuntimeException(new ClassNotFoundException(type.getClassName()));
        }).requestInitialization(analysisStack);
    }

    private ResolvedClass loadClass(final Type type, final InputStream is, final AnalysisStack analysisStack) throws IOException {
        final ClassReader reader = new ClassReader(is);
        final ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        final AnalysisStack importedStack = analysisStack.addAction(new AnalysisStack.Action("Resolving type " + type));

        ResolvedClass superClass = null;
        if (classNode.superName != null) {
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
    }
}
