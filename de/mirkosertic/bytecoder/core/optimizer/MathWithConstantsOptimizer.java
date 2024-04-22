/*
 * Copyright 2024 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.NumericalTest;
import de.mirkosertic.bytecoder.core.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.core.ir.PrimitiveLong;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.stream.Collectors;

public class MathWithConstantsOptimizer implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        final Graph g = method.methodBody;
        boolean changed = false;

        final List<Value> values = g.nodes().stream().filter(t -> t.nodeType == NodeType.Add || t.nodeType == NodeType.Sub || t.nodeType == NodeType.Mul || t.nodeType == NodeType.NumericalTest).map(t  -> (Value) t).collect(Collectors.toList());
        for (final Value value : values) {
            Value replacement = null;
            switch (value.nodeType) {
                case Add: {
                    final Node[] incomingData = value.incomingDataFlows;
                    if (Type.INT_TYPE == value.type) {
                        if (incomingData[0].nodeType == NodeType.PrimitiveInt && incomingData[1].nodeType == NodeType.PrimitiveInt) {
                            replacement = g.newInt(((PrimitiveInt) incomingData[0]).value + ((PrimitiveInt) incomingData[1]).value);
                        }
                    }
                    if (Type.LONG_TYPE == value.type) {
                        if (incomingData[0].nodeType == NodeType.PrimitiveLong && incomingData[1].nodeType == NodeType.PrimitiveLong) {
                            replacement = g.newLong(((PrimitiveLong) incomingData[0]).value + ((PrimitiveLong) incomingData[1]).value);
                        }
                    }
                    break;
                }
                case Sub: {
                    final Node[] incomingData = value.incomingDataFlows;
                    if (Type.INT_TYPE == value.type) {
                        if (incomingData[0].nodeType == NodeType.PrimitiveInt && incomingData[1].nodeType == NodeType.PrimitiveInt) {
                            replacement = g.newInt(((PrimitiveInt) incomingData[0]).value - ((PrimitiveInt) incomingData[1]).value);
                        }
                    }
                    if (Type.LONG_TYPE == value.type) {
                        if (incomingData[0].nodeType == NodeType.PrimitiveLong && incomingData[1].nodeType == NodeType.PrimitiveLong) {
                            replacement = g.newLong(((PrimitiveLong) incomingData[0]).value - ((PrimitiveLong) incomingData[1]).value);
                        }
                    }
                    break;
                }
                case Mul: {
                    final Node[] incomingData = value.incomingDataFlows;
                    if (Type.INT_TYPE == value.type) {
                        if (incomingData[0].nodeType == NodeType.PrimitiveInt && incomingData[1].nodeType == NodeType.PrimitiveInt) {
                            replacement = g.newInt(((PrimitiveInt) incomingData[0]).value * ((PrimitiveInt) incomingData[1]).value);
                        }
                    }
                    if (Type.LONG_TYPE == value.type) {
                        if (incomingData[0].nodeType == NodeType.PrimitiveLong && incomingData[1].nodeType == NodeType.PrimitiveLong) {
                            replacement = g.newLong(((PrimitiveLong) incomingData[0]).value * ((PrimitiveLong) incomingData[1]).value);
                        }
                    }
                    break;
                }
                case NumericalTest: {
                    final Node[] incomingData = value.incomingDataFlows;
                    if (incomingData[0].nodeType == NodeType.PrimitiveInt && incomingData[1].nodeType == NodeType.PrimitiveInt) {
                        final int left = ((PrimitiveInt) incomingData[0]).value;
                        final int right = ((PrimitiveInt) incomingData[1]).value;
                        switch (((NumericalTest) value).operation) {
                            case EQ: {
                                if (left == right) {
                                    replacement = g.newInt(1);
                                } else {
                                    replacement = g.newInt(0);
                                }
                                break;
                            }
                            case GE: {
                                if (left >= right) {
                                    replacement = g.newInt(1);
                                } else {
                                    replacement = g.newInt(0);
                                }
                                break;
                            }
                            case GT: {
                                if (left > right) {
                                    replacement = g.newInt(1);
                                } else {
                                    replacement = g.newInt(0);
                                }
                                break;
                            }
                            case LE: {
                                if (left <= right) {
                                    replacement = g.newInt(1);
                                } else {
                                    replacement = g.newInt(0);
                                }
                                break;
                            }
                            case LT: {
                                if (left < right) {
                                    replacement = g.newInt(1);
                                } else {
                                    replacement = g.newInt(0);
                                }
                                break;
                            }
                            case NE: {
                                if (left != right) {
                                    replacement = g.newInt(1);
                                } else {
                                    replacement = g.newInt(0);
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            if (replacement != null) {
                g.remapDataFlow(value, replacement);
                g.deleteNode(value);
                changed = true;
            }
        }

        return changed;
    }
}
