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
package de.mirkosertic.bytecoder.core.backend.opencl;

import de.mirkosertic.bytecoder.core.backend.CodeGenerationFailure;
import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.optimizer.Optimizer;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.util.List;

public class OpenCLWriter {

    private final OpenCLInputOutputs inputOutputs;
    private final ResolvedClass kernelClass;

    private final CompileUnit compileUnit;

    private final PrintWriter pw;

    private final Optimizer optimizer;

    public OpenCLWriter(
            final ResolvedClass kernelClass,
            final PrintWriter writer,
            final CompileUnit compileUnit,
            final OpenCLInputOutputs inputOutputs,
            final Optimizer optimizer) {

        this.compileUnit = compileUnit;
        this.pw = writer;
        this.inputOutputs = inputOutputs;
        this.kernelClass = kernelClass;
        this.optimizer = optimizer;
    }

    private void printInputOutputArgs(final List<OpenCLInputOutputs.KernelArgument> arguments) {
        for (int i = 0; i<arguments.size(); i++) {
            if (i>0) {
                pw.print(", ");
            }
            final OpenCLInputOutputs.KernelArgument theArgument = arguments.get(i);
            final Type theTypeRef = theArgument.getField().type;
            if (theTypeRef.getSort() == Type.OBJECT || theTypeRef.getSort() == Type.ARRAY) {
                pw.print("__global ");
            }
            switch (theArgument.getType()) {
                case INPUT:
                    pw.print("const ");
                    pw.print(OpenCLHelpers.toType(theTypeRef, compileUnit));
                    pw.print(" ");
                    pw.print(theArgument.getField().name);
                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    pw.print(OpenCLHelpers.toType(theTypeRef, compileUnit));
                    pw.print(" ");
                    pw.print(theArgument.getField().name);
                    break;
            }
        }
    }

    public void writeKernel(final ResolvedMethod method) {

        pw.print("__kernel void BytecoderKernel(");

        printInputOutputArgs(inputOutputs.arguments());

        pw.println(") {");

        final Graph g = method.methodBody;
        final DominatorTree dt = new DominatorTree(g);

        try {
            new Sequencer(g, dt, new OpenCLStructuredControlflowCodeGenerator(compileUnit, kernelClass, pw, inputOutputs));
        } catch (final RuntimeException e) {
            throw new CodeGenerationFailure(method, dt, e);
        }

        pw.println("}");
    }

    public void writeInline(final ResolvedMethod method) {

        pw.print("__inline ");
        pw.print(OpenCLHelpers.toType(method.methodType.getReturnType(), compileUnit));
        pw.print(" ");
        pw.print(OpenCLHelpers.generateMethodName(method.methodNode.name, method.methodType));
        pw.print("(");

        printInputOutputArgs(inputOutputs.arguments());

        for (int i = 0; i < method.methodType.getArgumentTypes().length; i++)  {
            if (i > 0 || !inputOutputs.arguments().isEmpty()) {
                pw.print(", ");
            }
            pw.print(OpenCLHelpers.toType(method.methodType.getArgumentTypes()[i], compileUnit));
            pw.print(" arg");
            pw.print(i);

        }

        pw.println(") {");

        final Graph g = method.methodBody;
        final DominatorTree dt = new DominatorTree(g);

        try {
            new Sequencer(g, dt, new OpenCLStructuredControlflowCodeGenerator(compileUnit, kernelClass, pw, inputOutputs));
        } catch (final RuntimeException e) {
            throw new CodeGenerationFailure(method, dt, e);
        }

        pw.println("}");
    }
}
