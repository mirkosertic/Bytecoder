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

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileBackend;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileResult;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodePackageReplacer;
import de.mirkosertic.bytecoder.ssa.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

import java.lang.reflect.Method;

public class Context {

    private final OpenCLCompileBackend backend;
    private final CompileOptions compileOptions;

    public Context() {
        backend = new OpenCLCompileBackend();
        compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true);
    }

    public void compute(int aNumberOfStreams, Kernel aKernel) {
        Method[] theMethods = aKernel.getClass().getDeclaredMethods();
        if (theMethods.length != 1) {
            throw new IllegalArgumentException("A kernel must have exactly one declared method!");
        }

        Method theMethod = theMethods[0];

        BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, compileOptions.getLogger());
        OpenCLCompileResult theResult = backend.generateCodeFor(compileOptions, theLinkerContext, aKernel.getClass(), theMethod.getName(), theSignature);
    }
}
