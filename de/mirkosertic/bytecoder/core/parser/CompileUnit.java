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
package de.mirkosertic.bytecoder.core.parser;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.BytecoderCharsetEncoder;
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.ReflectionConfiguration;
import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.AnnotationUtils;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
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

    private final ReflectionConfiguration reflectionConfiguration;

    private final Logger logger;

    public CompileUnit(final Loader loader, final Logger logger, final Intrinsic intrinsic) {
        this.loader = loader;
        this.resolvedClasses = new HashMap<>();
        this.intrinsic = intrinsic;
        this.exportedMethods = new HashMap<>();
        this.constantPool = new ConstantPool();
        this.reflectionConfiguration = new ReflectionConfiguration();
        this.logger = logger;
        try {
            final Enumeration<URL> reflectionConfigs = loader.getResources("xbytecoder-reflection.json");
            while(reflectionConfigs.hasMoreElements()) {
                final URL url = reflectionConfigs.nextElement();
                reflectionConfiguration.mergeWithConfigFrom(url, logger);
            }
        } catch (final IOException e) {
            logger.warn("Failed to load reflection configuration files : {}", e.getMessage());
        }
    }

    public Logger getLogger() {
        return logger;
    }

    protected Intrinsic getIntrinsic() {
        return intrinsic;
    }

    public ConstantPool getConstantPool() {
        return constantPool;
    }

    public ReflectionConfiguration getReflectionConfiguration() {
        return reflectionConfiguration;
    }

    public Loader getLoader() {
        return loader;
    }

    public ResolvedClass findClass(final Type type) {
        final String resourceName = type.getClassName().replace(".", "/") + ".class";
        return resolvedClasses.get(resourceName);
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
        boolean isEnumType = rs.allTypesOf().stream().anyMatch(parent -> parent.type.getClassName().equals(Enum.class.getName())) || rs.type.getClassName().equals(Enum.class.getName());
        if(isEnumType){
            //Why aren't all classes considered for lookups? Performance? It would simplify implementation a lot if Class.forName just looped over classes.#rt and checked that
            getReflectionConfiguration().resolve(rs.type.getClassName()).setSupportsClassForName(true);
            getConstantPool().resolveFromPool(rs.type.getClassName());//add of not already there
        }
        // If there are any methods annotated with Export, we resolve them, too
        for (final MethodNode mn : rs.classNode.methods) {
            if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/Export;", mn.visibleAnnotations)) {
                final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/Export;", mn.visibleAnnotations);
                String exportName = (String) values.get("value");
                if (StringUtils.isEmpty(exportName)) {
                    exportName = mn.name;
                }
                final ResolvedMethod m = rs.resolveMethod(mn.name, Type.getMethodType(mn.desc), analysisStack);
                exportedMethods.put(exportName, m);
            }
        }

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

        for (final ReflectionConfiguration.ReflectiveClass cl : reflectionConfiguration.configuredClasses()) {
            constantPool.resolveFromPool(cl.getName());
            final ResolvedClass rc = resolveClass(Type.getObjectType(cl.getName().replace('.', '/')), analysisStack);
            if (cl.supportsClassForName()) {
                rc.resolveMethod("<init>", Type.getMethodType(Type.VOID_TYPE), analysisStack);
            }
        }

        // Link some runtime code
        final ResolvedClass methodHandleCl = resolveClass(Type.getType(MethodHandle.class), analysisStack);
        for (final MethodNode method : methodHandleCl.classNode.methods) {
            if ("invokeExact".equals(method.name)) {
                methodHandleCl.resolveMethod(method.name, Type.getMethodType(method.desc), analysisStack);
            }
        }
        final ResolvedClass callsiteCl = resolveClass(Type.getType(CallSite.class), analysisStack);
        for (final MethodNode method : callsiteCl.classNode.methods) {
            if ("getTarget".equals(method.name)) {
                callsiteCl.resolveMethod(method.name, Type.getMethodType(method.desc), analysisStack);
            }
        }

        resolveClass(Type.getType(VM.class), analysisStack);

        final ResolvedClass resolvedClass = resolveClass(invokedType, analysisStack);
        final ResolvedMethod method = resolvedClass.resolveMethod(methodName, methodType, analysisStack);

        exportedMethods.put(MAIN_ENTRY_POINT_EXPORT, method);

        // We need the String class and this very specific constructor for code generation
        resolveClass(Type.getType(String.class), analysisStack).resolveMethod("<init>", Type.getMethodType(Type.VOID_TYPE), analysisStack);
        resolveClass(Type.getType(BytecoderCharsetEncoder.class), analysisStack).resolveMethod("<init>", Type.getMethodType(Type.VOID_TYPE, Type.getType(Charset.class)), analysisStack);

        resolveClass(Type.getType(Array.class), analysisStack);

        return method;
    }

    public void finalizeLinkingHierarchy() {

        final AnalysisStack analysisStack = new AnalysisStack();
        boolean modified = true;
        final Supplier<List<ResolvedClass>> currentList = () -> new ArrayList<>(resolvedClasses.values());
        while (modified) {
            final List<ResolvedClass> lst = currentList.get();
            for (final ResolvedClass cl : lst) {
                cl.computeOpaqueReferenceTypeAndCallbackStatus(analysisStack);
                cl.finalizeLinkingHierarchy(analysisStack);
            }
            modified = currentList.get().size() != lst.size();
        }
    }

    public void logStatistics() {
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

        logger.info("Linkage statistics:");
        logger.info("  Resolved classes in total : {}", numberOfClasses);
        logger.info("    # interfaces            : {}", numberOfInterfaces);
        logger.info("    # abstract classes      : {}", numberOfAbstractClasses);
        logger.info("    # final classes         : {}", numberOfFinalClasses);
        logger.info("  Resolved methods in total : {}", numberOfMethods);
        logger.info("    # native methods        : {}", numberOfNativeMethods);

        for (final ResolvedClass cl : resolvedClasses.values()) {
            for (final ResolvedMethod m : cl.resolvedMethods) {
                if (m.owner == cl) {
                    if (Modifier.isNative(m.methodNode.access)) {
                        logger.info("        {}.{}{}", cl.type.getClassName(), m.methodNode.name, m.methodNode.desc);
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
