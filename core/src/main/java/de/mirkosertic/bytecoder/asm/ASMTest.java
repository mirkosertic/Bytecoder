/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm;

import de.mirkosertic.bytecoder.asm.interpreter.Interpreter;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import org.objectweb.asm.Type;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ASMTest {

    private static void renderMethod(final ResolvedMethod resolvedMethod) throws IOException {
        final Graph g = resolvedMethod.methodBody;
        g.writeDebugTo(Files.newOutputStream(Paths.get("debug.dot")));

        final Optimizer o = Optimizations.DEFAULT;
        while (o.optimize(g)) {
        }

        g.writeDebugTo(Files.newOutputStream(Paths.get("debug_optimized.dot")));

        final DominatorTree dt = new DominatorTree(g);
        dt.writeDebugTo(Files.newOutputStream(Paths.get("dominatortree.dot")));

        final Sequencer sequencer = new Sequencer(g, dt);

        final Interpreter interpreter = new Interpreter("Temp",  g);
    }

    public static void main(final String[] args) throws IOException {

        final AnalysisStack analysisStack = new AnalysisStack();

        final CompileUnit compileUnit = new CompileUnit(ASMTest.class.getClassLoader());
        final ResolvedClass resolvedClass = compileUnit.resolveClass(Type.getType(Testclass.class), analysisStack);
        final ResolvedMethod method = resolvedClass.resolveMethod("<init>", Type.getMethodType(Type.VOID_TYPE), analysisStack);
        renderMethod(method);
    }
}
