/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ClassInitializationOrderTest {

    @Test
    public void testJavaLangSystemInitOrder() {
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(new BytecodeLoader(getClass().getClassLoader()), new Slf4JLogger());

        final Set<BytecodeLinkedClass> theClassesToConsider = new HashSet<>();
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(System.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Properties.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Hashtable.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Dictionary.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Object.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(FileInputStream.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(FileDescriptor.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(FileOutputStream.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(PrintStream.class)));
        theClassesToConsider.add(theLinkerContext.resolveClass(new BytecodeObjectTypeRef("jdk.internal.misc.Unsafe")));

        final ClassInitializationOrder theClassInitializationOrder = new ClassInitializationOrder();
        theClassesToConsider.stream().forEach(theClass -> {
            if (!theClass.getClassName().name().equals(Object.class.getName())) {
                theClassInitializationOrder.usedBy(theClass, theClass.getSuperClass());
            }
            final BytecodeResolvedMethods theMethods = theClass.resolvedMethods();
            theMethods.stream().forEach(theMethodEntry -> {
                if (theMethodEntry.getProvidingClass() == theClass && theMethodEntry.getValue().isClassInitializer()) {
                    final ProgramGenerator theGenerator = NaiveProgramGenerator.FACTORY.createFor(theLinkerContext, new JSIntrinsics());
                    final Program theProg = theGenerator.generateFrom(theClass.getBytecodeClass(), theMethodEntry.getValue());
                    KnownOptimizer.ALL.optimize(theProg.getControlFlowGraph(), theLinkerContext);
                    theClassInitializationOrder.registerCodeForDependencyAnalysis(theClass, theMethodEntry.getValue(), theProg);
                }
            });
        });

        final List<BytecodeLinkedClass> theInitOrder = theClassInitializationOrder.computeInitializationOrder();
        System.out.println(theInitOrder);
    }
}