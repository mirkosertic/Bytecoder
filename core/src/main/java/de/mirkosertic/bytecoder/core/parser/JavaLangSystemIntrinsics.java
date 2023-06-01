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

import de.mirkosertic.bytecoder.core.ir.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class JavaLangSystemIntrinsics implements Intrinsic {

    @Override
    public ControlTokenConsumer intrinsifyMethodInvocation(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if (node.getOpcode() == Opcodes.INVOKESTATIC) {
            final Type targetClass = Type.getObjectType(node.owner);
            if (System.class.getName().equals(targetClass.getClassName())) {
                if ("arraycopy".equals(node.name)) {
                    final Value source = incomingData[1];
                    final Type methodType;
                    final ResolvedClass systemClass = compileUnit.resolveClass(Type.getType(System.class), analysisStack);



                    if (source.type.getClassName().contentEquals(Object.class.getName())) {
                        // (Ljava/lang/Object;ILjava/lang/Object;II)V
                        methodType = Type.getMethodType(node.desc);
                    } else if (source.type.getSort() == Type.ARRAY) {
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
                            case Type.OBJECT: {
                                // (Ljava/lang/Object;ILjava/lang/Object;II)V
                                methodType = Type.getMethodType(node.desc);
                                break;
                            }
                            default: {
                                throw new IllegalArgumentException(source.type.getElementType().getSort()+" is not a valid type for an array in System.arraycopy() !");
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Expected array type or "+Object.class.getName()+" for arrayCopy, got " + source.type);
                    }



                    final ResolvedMethod rm = systemClass.resolveMethod(node.name,
                            methodType, analysisStack);
                    final ControlTokenConsumer n = graph.newMethodInvocation(InvocationType.STATIC, node, rm);
                    n.addIncomingData(incomingData);
                    return n;

                }
            }
        }
        return null;
    }
}
