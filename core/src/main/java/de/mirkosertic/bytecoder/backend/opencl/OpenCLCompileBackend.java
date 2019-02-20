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
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.NaiveProgramGenerator;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.Value;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

public class OpenCLCompileBackend implements CompileBackend<OpenCLCompileResult> {

    private final BytecodeLoader loader;
    private final ProgramGeneratorFactory programGeneratorFactory;

    public OpenCLCompileBackend() {
        loader = new BytecodeLoader(getClass().getClassLoader());
        programGeneratorFactory = NaiveProgramGenerator.FACTORY;
    }

    public BytecodeMethodSignature signatureFrom(final Method aMethod) {
        return loader.getSignatureParser().toMethodSignature(aMethod);
    }

    @Override
    public OpenCLCompileResult generateCodeFor(
            final CompileOptions aOptions, final BytecodeLinkerContext aLinkerContext, final Class aEntryPointClass, final String aEntryPointMethodName, final BytecodeMethodSignature aEntryPointSignatue) {

        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(loader, aOptions.getLogger());
        final BytecodeLinkedClass theKernelClass = theLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(aEntryPointClass));

        theKernelClass.resolveVirtualMethod(aEntryPointMethodName, aEntryPointSignatue);

        final BytecodeResolvedMethods theMethodMap = theKernelClass.resolvedMethods();

        final StringWriter theStrWriter = new StringWriter();

        final OpenCLInputOutputs theInputOutputs;

        // First of all, we link the kernel method
        final BytecodeMethod theKernelMethod = theKernelClass.getBytecodeClass().methodByNameAndSignatureOrNull("processWorkItem", new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        final ProgramGenerator theGenerator = programGeneratorFactory.createFor(aLinkerContext);
        final Program theSSAProgram = theGenerator.generateFrom(theKernelClass.getBytecodeClass(), theKernelMethod);

        //Run optimizer
        aOptions.getOptimizer().optimize(theSSAProgram.getControlFlowGraph(), aLinkerContext);

        // Ok, at this point we have to map kernel arguments
        // Every member of the kernel class becomes a kernel function argument
        try {
            theInputOutputs = inputOutputsFor(theLinkerContext, theKernelClass, theSSAProgram);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // And then we ca pass it to the code generator to generate the kernel code
        final OpenCLWriter theSSAWriter = new OpenCLWriter(theKernelClass, aOptions, theSSAProgram, "", new PrintWriter(theStrWriter), aLinkerContext, theInputOutputs);

        // We use the relooper here
        final Relooper theRelooper = new Relooper(aOptions);

        theMethodMap.stream().forEach(aMethodMapEntry -> {
            final BytecodeMethod theMethod = aMethodMapEntry.getValue();

            if (theMethod.isConstructor()) {
                return;
            }

            if (theMethod != theKernelMethod) {
                final Program theSSAProgram1 = theGenerator.generateFrom(aMethodMapEntry.getProvidingClass().getBytecodeClass(), theMethod);

                //Run optimizer
                aOptions.getOptimizer().optimize(theSSAProgram1.getControlFlowGraph(), aLinkerContext);

                // Write the method to the output
                // Try to reloop it!
                try {
                    final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram1.getControlFlowGraph());

                    theSSAWriter.printReloopedInline(theMethod, theSSAProgram1, theReloopedBlock);
                } catch (final Exception e) {
                    throw new IllegalStateException("Error relooping cfg", e);
                }

            }
        });

        // Finally, we write the kernel method
        try {
            final Relooper.Block theReloopedBlock = theRelooper.reloop(theSSAProgram.getControlFlowGraph());

            theSSAWriter.printReloopedKernel(theSSAProgram, theReloopedBlock);
        } catch (final Exception e) {
            throw new IllegalStateException("Error relooping cfg", e);
        }

        return new OpenCLCompileResult(new OpenCLCompileResult.OpenCLContent(theInputOutputs, theStrWriter.toString()));
    }

    private OpenCLInputOutputs inputOutputsFor(final BytecodeLinkerContext aLinkerContext, final BytecodeLinkedClass aKernelClass, final Program aProgram) {
        final OpenCLInputOutputs theResult = new OpenCLInputOutputs();
        for (final RegionNode theNode : aProgram.getControlFlowGraph().getKnownNodes()) {
            fillInputOutputs(aLinkerContext, aKernelClass, theNode.getExpressions(), theResult);
        }
        return theResult;
    }

    private void fillInputOutputs(
            final BytecodeLinkerContext aContext, final BytecodeLinkedClass aKernelClass, final ExpressionList aExpressionList, final OpenCLInputOutputs aInputOutputs) {
        final BytecodeResolvedFields theInstanceFields = aKernelClass.resolvedFields();
        theInstanceFields.streamForInstanceFields().forEach(aEntry -> {
            aInputOutputs.registerReadFrom(aEntry);
            aInputOutputs.registerWriteTo(aEntry);
        });
    }

    private void registerInputs(final BytecodeLinkerContext aContext, final BytecodeLinkedClass aKernelClass, final Value aValue, final OpenCLInputOutputs aInputOutputs) {
        if (aValue instanceof GetFieldExpression) {
            final GetFieldExpression theGetField = (GetFieldExpression) aValue;

            final BytecodeLinkedClass theClass = aContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theGetField.getField().getClassIndex().getClassConstant().getConstant()));
            if (theClass == aKernelClass) {
                final BytecodeResolvedFields theInstanceFields = aKernelClass.resolvedFields();
                final BytecodeResolvedFields.FieldEntry theField = theInstanceFields.fieldByName(
                        theGetField.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
                aInputOutputs.registerReadFrom(theField);
            }
        }
    }
}