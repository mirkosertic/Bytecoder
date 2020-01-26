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
package de.mirkosertic.bytecoder.backend.llvm;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.mirkosertic.bytecoder.allocator.Allocator;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

public class LLVMCompilerBackendTest {

    private static class Superclass {
         int member;
    }

    private static class Subclass extends Superclass {

        private static int st;

        static {
            final int x = 10;
        }

        public Subclass(final int value) {
            member = value;
            directCallVoid();
            final int temp = directCall();
            Object o = null;
            st = 20;
            o = this;
        }

        public void callVirtual() {
        }

        private int directCall() {
            return 42;
        }

        private void directCallVoid() {
            final int help = st;
        }
    }

    private static int intConst(final int x) {
        int a = 0;
        for (int i = 0; i <10; i++) {
            a++;
        }
        return a;
    }

    public static int doSomething() {
        final Subclass y = new Subclass(10);
        final int z = y.member;
        final int x = intConst(20);
        y.callVirtual();
        return x;
    }

    @Test
    public void testGenerate() throws IOException {
        final CompileTarget theTarget = new CompileTarget(getClass().getClassLoader(), CompileTarget.BackendType.wasm_llvm);
        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), true, KnownOptimizer.NONE, false, "ks", 100, 100, false, true, Allocator.passthru, new String[0], new String[0]);
        final CompileResult theResult = theTarget.compile(
                theOptions,
                LLVMCompilerBackendTest.class,
                "doSomething",
                new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[0])
        );
        System.out.println(theResult.getContent()[0].asString());
        Assert.assertEquals(5, theResult.getContent().length);
    }
}