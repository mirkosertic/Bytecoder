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

import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedField;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.io.StringWriter;

public class OpenCLCompileBackend {

    public OpenCLCompileBackend() {
    }

    public OpenCLCompileResult generateCodeFor(
            final CompileUnit compileUnit,
            final Class entryPointClass,
            final String entryPointMethodName,
            final Type entryPointSignature,
            final AnalysisStack analysisStack) {

        final ResolvedClass kernelClass = compileUnit.resolveClass(Type.getType(entryPointClass), analysisStack);
        final ResolvedMethod kernelMethod = kernelClass.resolveMethod(entryPointMethodName, entryPointSignature, analysisStack);

        final StringWriter strWriter = new StringWriter();

        final OpenCLInputOutputs inputOutputs;

        compileUnit.finalizeLinkingHierarchy();

        // Ok, at this point we have to map kernel arguments
        // Every member of the kernel class becomes a kernel function argument
        try {
            inputOutputs = inputOutputsFor(kernelMethod);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // And then we ca pass it to the code generator to generate the kernel code
        final OpenCLWriter writer = new OpenCLWriter(kernelClass, new PrintWriter(strWriter), compileUnit, inputOutputs);

        for (final ResolvedMethod method : kernelClass.resolvedMethods) {

            if ("<init>".equals(method.methodNode.name)) {
                continue;
            }

            if (method.owner != kernelClass) {
                continue;
            }

            if (method != kernelMethod) {
                writer.writeInline(method);
            }
        }

        // Finally, we write the kernel method

        writer.writeKernel(kernelMethod);

        return new OpenCLCompileResult(new OpenCLCompileResult.OpenCLContent(inputOutputs, strWriter.toString()));
    }

    private OpenCLInputOutputs inputOutputsFor(final ResolvedMethod resolvedMethod) {
        final OpenCLInputOutputs theResult = new OpenCLInputOutputs();
        for (final ResolvedField f : resolvedMethod.owner.resolvedFields) {
            theResult.registerReadFrom(f);
            theResult.registerWriteTo(f);
        }
        return theResult;
    }
}
