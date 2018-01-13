/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.opencl;

import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePackageReplacer;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class OpenCLCompileBackend implements CompileBackend<OpenCLCompileResult> {

    private final BytecodeLoader loader;
    private final ProgramGeneratorFactory programGeneratorFactory;

    public OpenCLCompileBackend() {
        loader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        programGeneratorFactory = NaiveProgramGenerator.FACTORY;
    }

    public BytecodeMethodSignature signatureFrom(Method aMethod) {
        return loader.getSignatureParser().toMethodSignature(aMethod);
    }

    @Override
    public OpenCLCompileResult generateCodeFor(CompileOptions aOptions, BytecodeLinkerContext aLinkerContext, Class aEntryPointClass, String aEntryPointMethodName, BytecodeMethodSignature aEntryPointSignatue) {

        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(loader, aOptions.getLogger());
        BytecodeLinkedClass theKernelClass = theLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(aEntryPointClass));

        theKernelClass.linkVirtualMethod(aEntryPointMethodName, aEntryPointSignatue);

        // Now we need to retrive the method
        Set<BytecodeMethod> theBytecodeMethods = new HashSet<>();
        theKernelClass.forEachMethod(theBytecodeMethods::add);

        if (theBytecodeMethods.size() != 1) {
            throw new IllegalArgumentException("Invalid number of loaded methods : " + theBytecodeMethods.size());
        }

        ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
        Program theSSAProgram = theGenerator.generateFrom(theKernelClass.getBytecodeClass(), theBytecodeMethods.iterator().next());

        //Run optimizer
        aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

        // Ok, at this point we have to map kernel arguments
        // Every member of the kernel class becomes a kernel function argument
        StringWriter theStrWriter = new StringWriter();
        OpenCLWriter theSSAWriter = new OpenCLWriter(aOptions, theSSAProgram, "", new PrintWriter(theStrWriter), aLinkerContext);

        // Try to reloop it!
        if (aOptions.isRelooper()) {
            try {
                Relooper theRelooper = new Relooper();
                Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

                theSSAWriter.writeRelooped(theReloopedBlock);
            } catch (Exception e) {
                throw new IllegalStateException("Error relooping cfg", e);
            }
        } else {
            // Fallback, as this generates slower and larger code.
            ControlFlowGraph.Node theNode = theSSAProgram.getControlFlowGraph().toRootNode();
            theSSAWriter.writeStartNode(theNode);
        }

        return new OpenCLCompileResult();
    }

    @Override
    public String generatedFileName() {
        return "unused";
    }
}
