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

import de.mirkosertic.bytecoder.allocator.Allocator;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileBackend;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileResult;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

public class CompilerTest {

    private Kernel createKernel() {
        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        return new Kernel() {
            @Override public void processWorkItem() {
                final int id = get_global_id(0);
                final float a = theA[id];
                final float b = theB[id];

                final byte b1 = (byte) 1;
                final short s2 = (short) 2;
                final int s3 = 3;
                final long s4 = 4;
                final float s5 = 5f;
                final double s6 = 6d;

                final int d;
                if (a<b) {
                    d = 100;
                    final int j = 100;
                    final int k = 2300;
                    final int c = 300;
                    final int dsd = 10010101;
                } else {
                    d = 200;
                    final int j = d * d * d * d;
                    final int k = 2300;
                    final int c = 300;
                    final int dsd = 10010101;
                }

                for (int lala = 100; 200 > lala; lala++) {
                    final int k = 100;
                }

                theResult[id] = a + b + d;
            }
        };
    }

    @Test
    public void testSimpleKernel() throws IOException {

        final OpenCLCompileBackend backend = new OpenCLCompileBackend();
        final CompileOptions compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true, "opencl", 512, 512, false, false, Allocator.passthru, new String[0], new String[0]);

        final Kernel theKernel = createKernel();
        final Class theKernelClass = theKernel.getClass();
        System.out.println(theKernelClass);

        final Method[] theMethods = theKernelClass.getDeclaredMethods();
        if (1 != theMethods.length) {
            throw new IllegalArgumentException("A kernel must have exactly one declared method!");
        }

        final Method theMethod = theMethods[0];

        final BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, compileOptions.getLogger());
        final OpenCLCompileResult compiledKernel = backend.generateCodeFor(compileOptions, theLinkerContext, theKernelClass, theMethod.getName(), theSignature);
        final OpenCLCompileResult.OpenCLContent content = (OpenCLCompileResult.OpenCLContent) compiledKernel.getContent()[0];

        System.out.println(content.asString());
    }

    @Test
    public void testKernelWithComplexType() throws IOException {

        final OpenCLCompileBackend backend = new OpenCLCompileBackend();
        final CompileOptions compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true, "opencl", 512, 512, false, false, Allocator.passthru, new String[0], new String[0]);

        final Float2[] theIn = new Float2[10];
        final Float2[] theOut = new Float2[10];
        final Kernel theKernel = new Kernel() {
            @Override public void processWorkItem() {
                final int theIndex = get_global_id(0);
                final Float2 a = theIn[theIndex];
                final Float2 b = theOut[theIndex];
                b.s0 = a.s0;
                b.s1 = a.s1;
            }
        };
        final Class theKernelClass = theKernel.getClass();
        System.out.println(theKernelClass);

        final Method[] theMethods = theKernelClass.getDeclaredMethods();
        if (1 != theMethods.length) {
            throw new IllegalArgumentException("A kernel must have exactly one declared method!");
        }

        final Method theMethod = theMethods[0];

        final BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, compileOptions.getLogger());
        final OpenCLCompileResult compiledKernel = backend.generateCodeFor(compileOptions, theLinkerContext, theKernelClass, theMethod.getName(), theSignature);
        final OpenCLCompileResult.OpenCLContent content = (OpenCLCompileResult.OpenCLContent) compiledKernel.getContent()[0];

        System.out.println(content.asString());
    }
}
