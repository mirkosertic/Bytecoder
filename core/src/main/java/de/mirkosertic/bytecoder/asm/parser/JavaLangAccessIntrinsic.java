/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.parser;

import de.mirkosertic.bytecoder.asm.ir.AnalysisStack;
import de.mirkosertic.bytecoder.asm.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.ir.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.StaticMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.ir.Value;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class JavaLangAccessIntrinsic implements Intrinsic {

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if ("jdk/internal/access/JavaLangAccess".equals(node.owner)) {
            switch (node.name) {
                case "decodeASCII":
                    final ResolvedClass cl = compileUnit.resolveClass(Type.getType(String.class), analysisStack);
                    final ResolvedMethod m = cl.resolveMethod(node.name, Type.getMethodType(node.desc), analysisStack);
                    final StaticMethodInvocationExpression invocation = graph.newStaticMethodInvocationExpression(m);
                    invocation.addIncomingData(incomingData);
                    return invocation;
                default:
                    throw new IllegalStateException("Not supported method " + node.name + " in JavaLangAccess!");
            }
        }
        return null;
    }

    @Override
    public ControlTokenConsumer intrinsifyMethodInvocation(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if ("jdk/internal/access/JavaLangAccess".equals(node.owner)) {
            switch (node.name) {
                case "inflateBytesToChars":
                    final ResolvedClass cl = compileUnit.resolveClass(Type.getObjectType("java/lang/StringUTF16"), analysisStack);
                    final ResolvedMethod m = cl.resolveMethod("inflate", Type.getMethodType("([BI[BII)V"), analysisStack);
                    final StaticMethodInvocation invocation = graph.newStaticMethodInvocation(m);
                    invocation.addIncomingData(incomingData);
                    return invocation;
                default:
                    throw new IllegalStateException("Not supported method " + node.name + " in JavaLangAccess!");
            }
        }
        return null;
    }
}
