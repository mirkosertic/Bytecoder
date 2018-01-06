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
package de.mirkosertic.bytecoder.backend.wasm;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.ssa.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

import javax.script.ScriptException;
import java.io.IOException;

public class CompilerTest {

    public static void main(String args[]) throws ClassNotFoundException, IOException, ScriptException {
        ClassLoader theLoader = CompilerTest.class.getClassLoader();
        Class theTargetClass = theLoader.loadClass(HelloWorld.class.getName());

        CompileTarget theCompileTarget = new CompileTarget(theLoader, CompileTarget.BackendType.js);

        BytecodeMethodSignature theSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[] { new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(TString.class), 1) });

        CompileResult theCode = theCompileTarget.compileToJS(new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.ALL), theTargetClass, "main", theSignature);

        System.out.println(theCode.getData());
    }
}
