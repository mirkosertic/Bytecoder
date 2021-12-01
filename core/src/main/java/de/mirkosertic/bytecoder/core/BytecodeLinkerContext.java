/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.intrinsics.Intrinsics;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BytecodeLinkerContext {

    private final BytecodeLoader loader;
    private final BytecodeMethodCollection methodCollection;
    private final Logger logger;
    private int classIdCounter;
    private final Statistics statistics;
    private final ReflectionConfiguration reflectionConfiguration;
    private final Map<BytecodeLinkedClass, BytecodeResolvedMethods> resolvedMethods;
    private final Map<BytecodeObjectTypeRef, BytecodeLinkedClass> linkedClasses;
    private final ProgramGeneratorFactory programGeneratorFactory;
    private final Intrinsics intrinsics;

    public BytecodeLinkerContext(final BytecodeLoader aLoader,
                                 final Logger aLogger,
                                 final ProgramGeneratorFactory aProgramGeneratorFactory,
                                 final Intrinsics aIntrinsics) {
        loader = aLoader;
        methodCollection = new BytecodeMethodCollection();
        logger = aLogger;
        classIdCounter = 0;
        statistics = new Statistics();
        reflectionConfiguration= new ReflectionConfiguration();
        resolvedMethods = new HashMap<>();
        linkedClasses = new HashMap<>();
        programGeneratorFactory = aProgramGeneratorFactory;
        intrinsics = aIntrinsics;
    }

    public void fillWithProgram(final BytecodeClass bytecodeClass,
                                final BytecodeMethod method,
                                final AnalysisStack analysisStack) {
        final ProgramGenerator generator = programGeneratorFactory.createFor(this, intrinsics);
        method.setProgram(generator.generateFrom(bytecodeClass, method, analysisStack));
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public Logger getLogger() {
        return logger;
    }

    public BytecodeSignatureParser getSignatureParser() {
        return loader.getSignatureParser();
    }

    public BytecodeMethodCollection getMethodCollection() {
        return methodCollection;
    }

    public BytecodeLinkedClass isLinkedOrNull(final BytecodeUtf8Constant aConstant) {
        final BytecodeObjectTypeRef theTypeRef = BytecodeObjectTypeRef.fromUtf8Constant(aConstant);
        return linkedClasses.get(theTypeRef);
    }

    public BytecodeLinkedClass resolveClass(final BytecodeObjectTypeRef aTypeRef, final AnalysisStack analysisStack) {

        final BytecodeLinkedClass existing = linkedClasses.get(aTypeRef);
        if (existing != null) {
            return existing;
        }

        final AnalysisStack.Frame analysisFrame = analysisStack.startClassInitialization(aTypeRef);
        try {
            final BytecodeClass theLoadedClass = loader.loadByteCode(aTypeRef);

            BytecodeLinkedClass theParentClass = null;
            final BytecodeClassinfoConstant theSuperClass = theLoadedClass.getSuperClass();
            if (theSuperClass != BytecodeClassinfoConstant.OBJECT_CLASS) {
                final BytecodeUtf8Constant theSuperClassName = theSuperClass.getConstant();
                theParentClass = resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theSuperClassName), analysisStack);
            }

            final BytecodeLinkedClass theLinkedClass = new BytecodeLinkedClass(theParentClass, classIdCounter++, this, aTypeRef, theLoadedClass);
            linkedClasses.put(aTypeRef, theLinkedClass);

            for (final BytecodeMethod theMethod : theLoadedClass.getMethods()) {
                final BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                if (theAnnotation != null) {
                    // The method should be exported
                    if (theMethod.getAccessFlags().isStatic()) {
                        theLinkedClass.resolveStaticMethod(theMethod.getName().stringValue(), theMethod.getSignature(), analysisStack);
                    } else {
                        theLinkedClass.resolveVirtualMethod(theMethod.getName().stringValue(), theMethod.getSignature(), analysisStack);
                    }
                }
            }

            for (final BytecodeInterface implementedInterface : theLoadedClass.getInterfaces()) {
                final BytecodeUtf8Constant interfaceClassName = implementedInterface.getClassinfoConstant().getConstant();
                final BytecodeLinkedClass implementedInterfaceClass = resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(interfaceClassName), analysisStack);

                theLinkedClass.addImplementedInterface(implementedInterfaceClass);
            }

            final BytecodeMethod classInitializerMethodOrNull = theLoadedClass.classInitializerOrNull();
            if (classInitializerMethodOrNull != null) {
                theLinkedClass.tagWith(BytecodeLinkedClass.Tag.HAS_CLASS_INITIALIZER);
                theLinkedClass.resolveClassInitializer(classInitializerMethodOrNull, analysisStack);
            }

            logger.info("Linked {}", theLinkedClass.getClassName().name());

            statistics.context("Linker context").counter("Loaded classes").increment();

            return theLinkedClass;
        } catch (final MissingLinkException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException("Error linking class " + aTypeRef.name() + ". Analysis stack is \n" + analysisStack.toDebugOutput(), e);
        } finally {
            analysisFrame.close();
        }
    }

    public Stream<BytecodeLinkedClass> linkedClasses() {
        return linkedClasses.values().stream();
    }

    public BytecodeLinkedClass resolveTypeRef(final BytecodeTypeRef aTypeRef, final AnalysisStack analysisStack) {
        if (aTypeRef.isVoid()) {
            return null;
        }
        if (aTypeRef.isPrimitive()) {
            return null;
        }
        if (aTypeRef.isArray()) {
            final BytecodeArrayTypeRef theArray = (BytecodeArrayTypeRef) aTypeRef;
            return resolveTypeRef(theArray.getType(), analysisStack);
        }
        final BytecodeObjectTypeRef theTypeRef = (BytecodeObjectTypeRef) aTypeRef;
        return resolveClass(theTypeRef, analysisStack);
    }

    public void resolveAbstractMethodsInSubclasses(final AnalysisStack analysisStack) {
        final List<BytecodeLinkedClass> theLinkedClasses = linkedClasses().collect(Collectors.toList());
        for (final BytecodeLinkedClass theLinked : theLinkedClasses) {
            theLinked.resolveInheritedOverriddenMethods(analysisStack);
        }
        if (linkedClasses().count() != theLinkedClasses.size()) {
            // New classes were added, we maybe have to resolve them as well
            resolveAbstractMethodsInSubclasses(analysisStack);
        }
    }

    public ReflectionConfiguration reflectionConfiguration() {
        return reflectionConfiguration;
    }

    public BytecodeResolvedMethods resolveMethods(final BytecodeLinkedClass linkedClass) {
        return resolvedMethods.computeIfAbsent(linkedClass, BytecodeLinkedClass::resolvedMethods);
    }

    public void finalizeLinkage() {
        linkedClasses()
                .filter(t -> t.hasTags() || t.getBytecodeClass().getAccessFlags().isInterface())
                .forEach(t -> {
                    final BytecodeResolvedMethods resolvedMethods = t.resolvedMethods();
                    resolvedMethods.stream()
                            .filter(entry -> !entry.getValue().getAccessFlags().isAbstract())
                            .forEach(entry -> {
                                if (entry.getProvidingClass().getBytecodeClass().getAccessFlags().isInterface()) {
                                    entry.getProvidingClass().tagWith(BytecodeLinkedClass.Tag.PROVIDES_DEFAULT_IMPLEMENTATION);
                                }
                                entry.getValue().tagWith(BytecodeMethod.Tag.IMPLEMENTATION_USED);
                            });
                });
    }
}