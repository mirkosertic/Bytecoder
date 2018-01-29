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

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.get_global_id;

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
            public void processWorkItem() {
                int id = get_global_id(0);
                float a = theA[id];
                float b = theB[id];

                byte b1 = (byte) 1;
                short s2 = (short) 2;
                int s3 = 3;
                long s4 = 4;
                float s5 = 5f;
                double s6 = 6d;

                int d;
                if (a<b) {
                    d = 100;
                    int j = 100;
                    int k = 2300;
                    int c = 300;
                    int dsd = 10010101;
                } else {
                    d = 200;
                    int j = d * d * d * d;
                    int k = 2300;
                    int c = 300;
                    int dsd = 10010101;
                }

                for (int lala = 100; lala < 200; lala++) {
                    int k = 100;
                }

                theResult[id] = a + b + d;
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
        OpenCLCompileResult theCompiledKernel = backend.generateCodeFor(compileOptions, theLinkerContext, theKernelClass, theMethod.getName(), theSignature);

        System.out.println(theCompiledKernel.getData());
    }

    @Test
    public void testKernelWithComplexType() {

        OpenCLCompileBackend backend = new OpenCLCompileBackend();
        CompileOptions compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true);

        Float2[] theIn = new Float2[10];
        Float2[] theOut = new Float2[10];
        Kernel theKernel = new Kernel() {
            public void processWorkItem() {
                int theIndex = get_global_id(0);
                Float2 a = theIn[theIndex];
                Float2 b = theOut[theIndex];
                b.s0 = a.s0;
                b.s1 = a.s1;
            }
        };
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
