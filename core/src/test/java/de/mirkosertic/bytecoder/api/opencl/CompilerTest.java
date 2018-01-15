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
package de.mirkosertic.bytecoder.api.opencl;

import java.lang.reflect.Method;

import org.junit.Test;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileBackend;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileResult;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodePackageReplacer;
import de.mirkosertic.bytecoder.ssa.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

public class CompilerTest {

    private Kernel createKernel() {
        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        return new Kernel() {
            public void add() {
                int id = get_global_id(0);
                float a = theA[id];
                float b = theB[id];
                theResult[id] = a + b;
            }
        };
    }

    @Test
    public void testSimpleKernel() {

        OpenCLCompileBackend backend = new OpenCLCompileBackend();
        CompileOptions compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true);

        Kernel theKernel = createKernel();
        Class theKernelClass = theKernel.getClass();
        System.out.println(theKernelClass);

        Method[] theMethods = theKernelClass.getDeclaredMethods();
        if (theMethods.length != 1) {
            throw new IllegalArgumentException("A kernel must have exactly one declared method!");
        }

        Method theMethod = theMethods[0];

        BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, compileOptions.getLogger());
        OpenCLCompileResult theCompiedKernel = backend.generateCodeFor(compileOptions, theLinkerContext, theKernelClass, theMethod.getName(), theSignature);

        System.out.println(theCompiedKernel.getData());

    }
}
