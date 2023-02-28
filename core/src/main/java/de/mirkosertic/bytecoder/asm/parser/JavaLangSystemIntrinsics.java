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
import de.mirkosertic.bytecoder.asm.ir.Value;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class JavaLangSystemIntrinsics implements Intrinsic {

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        return null;
    }

    @Override
    public ControlTokenConsumer intrinsifyMethodInvocation(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            final Type targetClass = Type.getObjectType(node.owner);
            if (System.class.getName().equals(targetClass.getClassName())) {
                if ("arraycopy".equals(node.name)) {
                    final Value source = incomingData[1];

                    if (source.type.getSort() != Type.ARRAY) {
                        throw new IllegalArgumentException("Expected array type for arrayCopy, got " + source.type);
                    }

                    final ResolvedClass systemClass = compileUnit.resolveClass(Type.getType(System.class), analysisStack);

                    final Type methodType;
                    switch (source.type.getElementType().getSort()) {
                        case Type.CHAR: {
                            methodType = Type.getMethodType("([CI[CII)V");
                            break;
                        }
                        case Type.BYTE: {
                            methodType = Type.getMethodType("([BI[BII)V");
                            break;
                        }
                        case Type.DOUBLE: {
                            methodType = Type.getMethodType("([DI[DII)V");
                            break;
                        }
                        case Type.FLOAT: {
                            methodType = Type.getMethodType("([FI[FII)V");
                            break;
                        }
                        case Type.INT: {
                            methodType = Type.getMethodType("([II[III)V");
                            break;
                        }
                        case Type.LONG: {
                            methodType = Type.getMethodType("([JI[JII)V");
                            break;
                        }
                        case Type.SHORT: {
                            methodType = Type.getMethodType("([SI[SII)V");
                            break;
                        }
                        case Type.BOOLEAN: {
                            methodType = Type.getMethodType("([ZI[ZII)V");
                            break;
                        }
                        default: {
                            // (Ljava/lang/Object;ILjava/lang/Object;II)V
                            methodType = Type.getMethodType(node.desc);
                            break;
                        }
                    }

                    final ResolvedMethod rm = systemClass.resolveMethod(node.name,
                            methodType, analysisStack);
                    final ControlTokenConsumer n = graph.newStaticMethodInvocation(rm);
                    n.addIncomingData(incomingData);
                    return n;

                }
            }
        }
        return null;
    }
}
