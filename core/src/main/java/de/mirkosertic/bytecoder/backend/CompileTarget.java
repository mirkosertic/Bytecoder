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
package de.mirkosertic.bytecoder.backend;

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.backend.js.JSSSACompilerBackend;
import de.mirkosertic.bytecoder.backend.llvm.LLVMCompilerBackend;
import de.mirkosertic.bytecoder.backend.wasm.WASMSSAASTCompilerBackend;
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;

import java.io.FileDescriptor;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompileTarget {

    public enum BackendType {
        js {
            @Override
            public CompileBackend createBackend() {
                return new JSSSACompilerBackend(NaiveProgramGenerator.FACTORY);
            }
        },
        wasm {
            @Override
            public CompileBackend createBackend() {
                return new WASMSSAASTCompilerBackend(NaiveProgramGenerator.FACTORY);
            }
        },
        wasm_llvm {
            @Override
            public CompileBackend createBackend() {
                return new LLVMCompilerBackend(NaiveProgramGenerator.FACTORY);
            }
        };

        public abstract CompileBackend createBackend();
    }

    private final CompileBackend backend;
    private final BytecodeLoader bytecodeLoader;
    private final ClassLoader classLoader;

    public CompileTarget(final ClassLoader aClassLoader, final BackendType aType) {
        backend = aType.createBackend();
        bytecodeLoader = new BytecodeLoader(aClassLoader);
        classLoader = aClassLoader;
    }

    public CompileResult compile(
            final CompileOptions aOptions, final Class aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(bytecodeLoader, aOptions.getLogger());

        final BytecodeLinkedClass theClassLinkedCass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
        theClassLinkedCass.resolveConstructorInvocation(new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {}));

        // Lambda handling
        final BytecodeLinkedClass theCallsite = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(CallSite.class));
        theCallsite.resolveVirtualMethod("invokeExact", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), 1)}));

        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.LambdaStaticImplCallsite.class))
            .resolveConstructorInvocation(new BytecodeMethodSignature(
                        BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[]{
                                BytecodeObjectTypeRef.fromRuntimeClass(String.class),
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodType.class),
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodHandle.class)
                        }
                ));
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.LambdaConstructorRefCallsite.class))
                .resolveConstructorInvocation(new BytecodeMethodSignature(
                        BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[]{
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodType.class),
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodHandle.class),
                        }
                ));
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.InvokeInterfaceCallsite.class))
                .resolveConstructorInvocation(new BytecodeMethodSignature(
                        BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[]{
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodType.class),
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodHandle.class),
                        }
                ));
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.InvokeVirtualCallsite.class))
                .resolveConstructorInvocation(new BytecodeMethodSignature(
                        BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[]{
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodType.class),
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodHandle.class),
                        }
                ));
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.InvokeSpecialCallsite.class))
                .resolveConstructorInvocation(new BytecodeMethodSignature(
                        BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[]{
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodType.class),
                                BytecodeObjectTypeRef.fromRuntimeClass(MethodHandle.class),
                        }
                ));

        // We have to link character set implementations
        // to make them available via reflection API
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("sun/nio/cs/UTF_8")))
                .resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("sun/nio/cs/UTF_16"))).resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("sun/nio/cs/ISO_8859_1"))).resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));
        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("sun/nio/cs/US_ASCII"))).resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(new BytecodeUtf8Constant("java/lang/CharacterDataLatin1"))).resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        // Additional classes
        if (aOptions.getAdditionalClassesToLink() != null) {
            for (final String theClassname : aOptions.getAdditionalClassesToLink()) {
                theLinkerContext.resolveClass(new BytecodeObjectTypeRef(theClassname)).resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));
            }
        }

        final BytecodeObjectTypeRef theTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(aClass);

        theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(FileDescriptor.class)).resolveStaticMethod("initDefaultFileHandles", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        final BytecodeLinkedClass theClass = theLinkerContext.resolveClass(theTypeRef);
        final BytecodeMethod theMethod = theClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod == null) {
            throw new IllegalStateException("No method named " + aMethodName + " with signature " + aSignature + "found in " + theClass.getClassName().name());
        }
        if (theMethod.getAccessFlags().isStatic()) {
            theClass.resolveStaticMethod(aMethodName, aSignature);
        } else {
            theClass.resolveVirtualMethod(aMethodName, aSignature);
        }

        // Before code generation we have to make sure that all abstract method implementations are linked correctly
        aOptions.getLogger().info("Resolving abstract method hierarchy");
        theLinkerContext.resolveAbstractMethodsInSubclasses();

        // Ugly hack for deeply nested promises and anonymous inner classes
        boolean somethingAdded = true;
        final Set<BytecodeLinkedClass> theAlreadySeen = new HashSet<>();
        while (somethingAdded) {
            somethingAdded = false;
            // We have to link all callback implementations. They are not part of the dependency yet as
            // they are not invoked by the bytecode, but from the outside world. By adding them to the
            // dependency tree, we make sure they are available for invocation.
            final List<BytecodeLinkedClass> theLinkedClasses = theLinkerContext.linkedClasses().map(Edge::targetNode)
                    .collect(Collectors.toList());
            for (final BytecodeLinkedClass theLinkedClass : theLinkedClasses) {
                if (theLinkedClass.isCallback()) {
                    if (theAlreadySeen.add(theLinkedClass)) {
                        somethingAdded = true;
                        final BytecodeClass theBytecodeClass = theLinkedClass.getBytecodeClass();
                        aOptions.getLogger()
                                .info("Resolving callback {}", theBytecodeClass.getThisInfo().getConstant().stringValue());
                        for (final BytecodeMethod theCallbackMethod : theBytecodeClass.getMethods()) {
                            if (!theCallbackMethod.isConstructor() && !theCallbackMethod.isClassInitializer()) {
                                aOptions.getLogger()
                                        .info("Resolving callback method {} {}", theCallbackMethod.getName().stringValue(),
                                                theCallbackMethod.getSignature().toString());
                                theLinkedClass.resolveVirtualMethod(theCallbackMethod.getName().stringValue(),
                                        theCallbackMethod.getSignature());
                            }
                        }
                    }
                }
            }

            aOptions.getLogger().info("Resolving abstract method hierarchy");
            theLinkerContext.resolveAbstractMethodsInSubclasses();
        }

        final CompileResult theResult = backend.generateCodeFor(aOptions, theLinkerContext, aClass, aMethodName, aSignature);

        // Write some statistics
        theLinkerContext.getStatistics().writeTo(aOptions.getLogger());

        // Include all required resources from included modules
        final List<String> resourcesToInclude = new ArrayList<>();
        for (final ClassLibProvider provider : ClassLibProvider.availableProviders()) {
            Collections.addAll(resourcesToInclude, provider.additionalResources());
        }
        // Don't forget user specific ressources
        Collections.addAll(resourcesToInclude, aOptions.getAdditionalResources());

        // Finally, we add the list of additional resources to the result
        for (final String theResource : resourcesToInclude) {
            final URL theUrl = classLoader.getResource(theResource);
            if (theUrl != null) {
                aOptions.getLogger().info("Including resource {}", theResource);
                theResult.add(new CompileResult.URLContent(theResource, theUrl));
            } else {
                aOptions.getLogger().warn("Cannot find resource {}", theResource);
            }
        }

        return theResult;
    }

    public BytecodeMethodSignature toMethodSignature(final Method aMethod) {
        return bytecodeLoader.getSignatureParser().toMethodSignature(aMethod);
    }
}