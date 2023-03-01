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
import de.mirkosertic.bytecoder.asm.ir.EnumValuesOf;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.Value;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class JavaLangClassIntrinsics implements Intrinsic {

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if (node.getOpcode() != Opcodes.INVOKESTATIC) {
            final Type targetClass = Type.getObjectType(node.owner);
            if (Class.class.getName().equals(targetClass.getClassName())) {
                if ("getEnumConstants".equals(node.name)) {
                    final Type methodType = Type.getMethodType(node.desc);
                    final EnumValuesOf exp = graph.newEnumValuesOf(methodType.getReturnType());
                    exp.addIncomingData(incomingData[0]);
                    return exp;
                }
            }
        }
        return null;
    }

    @Override
    public ControlTokenConsumer intrinsifyMethodInvocation(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        return null;
    }
}
