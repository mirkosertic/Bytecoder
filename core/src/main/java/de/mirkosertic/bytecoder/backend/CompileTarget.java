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

import de.mirkosertic.bytecoder.backend.js.JSSSACompilerBackend;
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
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;

import java.lang.reflect.Method;
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
        };

        public abstract CompileBackend createBackend();
    }

    private final CompileBackend backend;
    private final BytecodeLoader bytecodeLoader;

    public CompileTarget(final ClassLoader aClassLoader, final BackendType aType) {
        backend = aType.createBackend();
        bytecodeLoader = new BytecodeLoader(aClassLoader);
    }

    public CompileResult compile(
            final CompileOptions aOptions, final Class aClass, final String aMethodName, final BytecodeMethodSignature aSignature) {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(bytecodeLoader, aOptions.getLogger());

        final BytecodeLinkedClass theClassLinkedCass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Class.class));
        theClassLinkedCass.resolveConstructorInvocation(new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {}));

        // Lambda handling
        final BytecodeLinkedClass theCallsite = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.ImplementingCallsite.class));
        theCallsite.resolveVirtualMethod("invokeExact", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), 1)}));

        final BytecodeObjectTypeRef theTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(aClass);

        final BytecodeLinkedClass theClass = theLinkerContext.resolveClass(theTypeRef);
        final BytecodeMethod theMethod = theClass.getBytecodeClass().methodByNameAndSignatureOrNull(aMethodName, aSignature);
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
            final List<BytecodeLinkedClass> theLinkedClasses = theLinkerContext.linkedClasses().map(t -> t.targetNode())
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

        return backend.generateCodeFor(aOptions, theLinkerContext, aClass, aMethodName, aSignature);
    }

    public BytecodeMethodSignature toMethodSignature(final Method aMethod) {
        return bytecodeLoader.getSignatureParser().toMethodSignature(aMethod);
    }
}