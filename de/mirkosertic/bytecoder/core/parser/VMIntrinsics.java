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
package de.mirkosertic.bytecoder.core.parser;

import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Value;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class VMIntrinsics implements Intrinsic {

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            final Type targetClass = Type.getObjectType(node.owner);
            if (VM.class.getName().equals(targetClass.getClassName())) {
                if ("bytePrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.BYTE_TYPE);
                }
                if ("charPrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.CHAR_TYPE);
                }
                if ("shortPrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.SHORT_TYPE);
                }
                if ("intPrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.INT_TYPE);
                }
                if ("floatPrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.FLOAT_TYPE);
                }
                if ("doublePrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.DOUBLE_TYPE);
                }
                if ("longPrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.LONG_TYPE);
                }
                if ("booleanPrimitiveClass".equals(node.name)) {
                    return graph.newPrimitiveClassReference(Type.BOOLEAN_TYPE);
                }
            }
        }
        return null;
    }
}
