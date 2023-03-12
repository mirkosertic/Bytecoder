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
package de.mirkosertic.bytecoder.core.backend.wasm;

import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Reinterpret;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.GraphParser;
import de.mirkosertic.bytecoder.core.parser.Intrinsic;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class JavaLangFloatIntrinsic implements Intrinsic {

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            final Type targetClass = Type.getObjectType(node.owner);
            if (Float.class.getName().equals(targetClass.getClassName())) {
                if ("floatToIntBits".equals(node.name)) {
                    final Reinterpret reinterpret = graph.newReinterpret(Type.INT_TYPE);
                    reinterpret.addIncomingData(incomingData[1]);
                    return reinterpret;
                }

                if ("floatToRawIntBits".equals(node.name)) {
                    final Reinterpret reinterpret = graph.newReinterpret(Type.INT_TYPE);
                    reinterpret.addIncomingData(incomingData[1]);
                    return reinterpret;
                }

                if ("intBitsToFloat".equals(node.name)) {
                    final Reinterpret reinterpret = graph.newReinterpret(Type.FLOAT_TYPE);
                    reinterpret.addIncomingData(incomingData[1]);
                    return reinterpret;
                }
            }
        }
        return null;
    }
}
