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

import de.mirkosertic.bytecoder.backend.wasm.WASMWriterUtils;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.GraphDFSOrder;
import de.mirkosertic.bytecoder.graph.Node;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationReadExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationWriteExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.MemorySizeExpression;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.StackTopExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;
import de.mirkosertic.bytecoder.ssa.VariableDescription;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LLVMWriter implements AutoCloseable {

    public static final String VTABLEFUNCTIONSUFFIX = "__resolvevtableindex";
    public static final String INSTANCEOFSUFFIX = "__instanceof";
    public static final int GENERATED_INSTANCEOF_METHOD_ID = -1;
    public static final String NEWINSTANCE_METHOD_NAME = "$newInstance";

    private final PrintWriter output;
    private final PrintWriter target;
    private final StringWriter buffer;
    private RegionNode currentNode;
    private final Map<Value, String> valueToSymbolMaping;
    private final Map<PHIValue, String> prereservedPHIValueNames;

    public LLVMWriter(final PrintWriter output) {
        this.output = output;
        this.buffer = new StringWriter();
        this.target = new PrintWriter(this.buffer);
        this.valueToSymbolMaping = new HashMap<>();
        this.prereservedPHIValueNames = new HashMap<>();
    }

    @Override
    public void close() {
        target.flush();
        String theUnencoded = buffer.toString();
        for (final Map.Entry<Value, String> theEntry : valueToSymbolMaping.entrySet()) {
            if (theEntry.getKey() instanceof Variable) {
                final Variable v = (Variable) theEntry.getKey();
                theUnencoded = theUnencoded.replaceAll("\\%" + v.getName() + "_", theEntry.getValue());
            }
        }
        output.print(theUnencoded);
    }

    public void write(final Program aProgram) {
        final ControlFlowGraph theGraph = aProgram.getControlFlowGraph();
        final RegionNode theStart = theGraph.startNode();
        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(theStart,
                RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        final List<RegionNode> sorted = order.getNodesInOrder();
        for (final RegionNode theBlock : sorted) {
            currentNode = theBlock;
            final BytecodeOpcodeAddress theBlockStart = theBlock.getStartAddress();
            if (theBlockStart.getAddress() == 0) {
                target.println("entry:");
            } else {
                target.print("block");
                target.print(theBlockStart.getAddress());
                target.println(":");
            }
            final BlockState theLiveIn = theBlock.liveIn();
            for (final Map.Entry<VariableDescription, Value> theEntry : theLiveIn.getPorts().entrySet()) {
                if (theEntry.getValue() instanceof PHIValue) {
                    final PHIValue phi = (PHIValue) theEntry.getValue();
                    if (!valueToSymbolMaping.containsKey(phi)) {
                        final String tempName;
                        if (prereservedPHIValueNames.containsKey(phi)) {
                            tempName = prereservedPHIValueNames.get(phi);
                        } else {
                            tempName ="%phitemp" + valueToSymbolMaping.size() + "_";
                        }
                        valueToSymbolMaping.put(phi, tempName);

                        target.write("    ");
                        target.write(tempName);
                        target.write(" = phi ");
                        target.write(WASMWriterUtils.toType(phi.resolveType()));
                        target.write(" ");
                        boolean first = true;
                        for (final RegionNode pred : currentNode.getPredecessors()) {
                            final Value theOut = pred.liveOut().getPorts().get(phi.getDescription());
                            String theReplacementSymbol = valueToSymbolMaping.get(theOut);
                            if (theReplacementSymbol == null && theOut instanceof PHIValue) {
                                theReplacementSymbol ="%futurephi_" + prereservedPHIValueNames.size() + "_";
                                prereservedPHIValueNames.put((PHIValue) theOut, theReplacementSymbol);
                            }
                            if (theReplacementSymbol != null) {
                                if (first) {
                                    first = false;
                                } else {
                                    target.write(",");
                                }
                                target.write("[");

                                target.write(theReplacementSymbol);
                                target.write(",");
                                if (pred.getStartAddress().getAddress() == 0) {
                                    target.write("%entry");
                                } else {
                                    target.write("%block");
                                    target.print(pred.getStartAddress().getAddress());
                                }

                                target.write("]");
                            } else if (theOut instanceof Variable) {
                                if (first) {
                                    first = false;
                                } else {
                                    target.write(",");
                                }
                                target.write("[");

                                if (valueToSymbolMaping.containsKey(theOut)) {
                                    target.write(valueToSymbolMaping.get(theOut));
                                } else {
                                    target.write("%");
                                    target.write(((Variable) theOut).getName());
                                    target.write("_");
                                }
                                target.write(",");
                                if (pred.getStartAddress().getAddress() == 0) {
                                    target.write("%entry");
                                } else {
                                    target.write("%block");
                                    target.print(pred.getStartAddress().getAddress());
                                }

                                target.write("]");
                            } else {
                                throw new RuntimeException("Unhandled type for PHI input : " + theOut.getClass());
                            }
                        }
                        target.println();
                    }
                }
            }
            write(theBlock);
        }
    }

    private void write(final RegionNode block) {
        write(block.getExpressions());
    }

    private void tempify(final Expression e) {
        for (final Value v : e.incomingDataFlows()) {
            if (v instanceof ComputedMemoryLocationReadExpression || v instanceof ComputedMemoryLocationWriteExpression) {
                if (!valueToSymbolMaping.containsKey(v)) {
                    final Value origin = v.incomingDataFlows().get(0);
                    final Value offset = v.incomingDataFlows().get(1);
                    final String theTempSymbol = "%temp_" + valueToSymbolMaping.size() + "_";
                    target.print("    ");
                    target.print(theTempSymbol);
                    target.print(" = ");
                    target.print("add i32 ");
                    write(origin, true);
                    target.print(", ");
                    write(offset, true);
                    target.println();

                    final String theTempSymbol2 = theTempSymbol + "_ptr";
                    target.print("    ");
                    target.print(theTempSymbol2);
                    target.print(" = inttoptr i32 ");
                    target.print(theTempSymbol);
                    target.println(" to i32*");

                    valueToSymbolMaping.put(v, theTempSymbol);
                }
            } else if (v instanceof MemorySizeExpression) {
                if (!valueToSymbolMaping.containsKey(v)) {

                    final String theTempSymbol = "%temp_" + valueToSymbolMaping.size() + "_raw_";
                    target.print("    ");
                    target.print(theTempSymbol);
                    target.print(" = ");
                    write(v, true);
                    target.println();

                    final String theTempSymbol2 = "%temp_" + valueToSymbolMaping.size() + "_";
                    target.print("    ");
                    target.print(theTempSymbol2);
                    target.print(" = ");
                    target.print("mul i32 ");
                    target.print(theTempSymbol);
                    target.print(", 65536");
                    target.println();

                    valueToSymbolMaping.put(v, theTempSymbol2);
                }
            } else if (v instanceof Expression) {
                if (!valueToSymbolMaping.containsKey(v)) {
                    final String theTempSymbol = "%temp_" + valueToSymbolMaping.size() + "_";

                    target.print("    ");
                    target.print(theTempSymbol);
                    target.print(" = ");
                    write(v, false);

                    target.println();

                    valueToSymbolMaping.put(v, theTempSymbol);
                }
            }
        }
    }

    private void write(final ExpressionList list) {
        for (final Expression e : list.toList()) {
            tempify(e);
            if (e instanceof ReturnExpression) {
                write((ReturnExpression) e);
            } else if (e instanceof VariableAssignmentExpression) {
                write((VariableAssignmentExpression) e);
            } else if (e instanceof ReturnValueExpression) {
                write((ReturnValueExpression) e);
            } else if (e instanceof GotoExpression) {
                write((GotoExpression) e);
            } else if (e instanceof IFExpression) {
                write((IFExpression) e);
            } else if (e instanceof InvokeStaticMethodExpression) {
                target.print("    ");
                write((InvokeStaticMethodExpression) e);
                target.println();
            } else if (e instanceof SetMemoryLocationExpression) {
                write((SetMemoryLocationExpression) e);
            } else if (e instanceof UnreachableExpression) {
                write((UnreachableExpression) e);
            } else {
                throw new IllegalStateException("Not implemented : " + e.getClass());
            }
        }
    }

    private void write(final UnreachableExpression expression) {
        target.println("    unreachable");
    }

    private void write(final SetMemoryLocationExpression expression) {
        final Value location = expression.incomingDataFlows().get(0);
        final Value value = expression.incomingDataFlows().get(1);
        target.print("    store i32 ");
        write(value, true);
        target.print(",");
        write(location, true);
        target.println();
    }

    private void write(final IFExpression expression) {
        target.print("    br i1 ");
        write(expression.incomingDataFlows().get(0), true);
        if (expression.getAddress().getAddress() == 0) {
            target.write(", label %entry");
        } else {
            target.write(", label %block");
            target.print(expression.getGotoAddress().getAddress());
        }

        final Set<BytecodeOpcodeAddress> forwardNodes = currentNode.outgoingEdges()
                .filter((Predicate<Edge<? extends EdgeType, ? extends Node>>) edge -> RegionNode.ALL_SUCCCESSORS_REGULAR_FLOW_ONLY
                        .test((Edge<EdgeType, RegionNode>) edge))
                .map(t -> t.targetNode().getStartAddress()).collect(Collectors.toSet());
        forwardNodes.remove(expression.getGotoAddress());

        if (forwardNodes.size() == 1) {
            final BytecodeOpcodeAddress theElse = forwardNodes.iterator().next();

            if (theElse.getAddress() == 0) {
                target.write(", label %entry");
            } else {
                target.write(", label %block");
                target.print(theElse.getAddress());
            }
            target.println();
        } else {
            throw new IllegalArgumentException("Expected one node for else branch of if statement, got " + forwardNodes);
        }
    }

    private void write(final GotoExpression expression) {
        target.print("    br label %");
        final BytecodeOpcodeAddress jumpTo = expression.jumpTarget();
        if (jumpTo.getAddress() == 0) {
            target.println("entry");
        } else {
            target.print("block");
            target.print(jumpTo.getAddress());
            target.println();
        }
    }

    private void write(final ReturnExpression expression) {
        target.print("    ");
        target.println("ret void");
    }

    private void write(final ReturnValueExpression expression) {
        final Value result = expression.incomingDataFlows().get(0);
        target.print("    ");
        target.print("ret ");
        target.print(LLVMWriterUtils.toType(result.resolveType()));
        target.print(" ");
        write(result, true);
        target.println();
    }

    private void write(final VariableAssignmentExpression expression) {
        final Value value = expression.incomingDataFlows().get(0);
        final String replacedBy = valueToSymbolMaping.get(value);
        if (replacedBy != null && (!(value instanceof ComputedMemoryLocationReadExpression))) {
            valueToSymbolMaping.put(expression.getVariable(), replacedBy);
            target.print("    ; ignore assignment of ");
            target.print(expression.getVariable().getName());
            target.print(" as identical to ");
            target.print(replacedBy);
            target.println();
        } else {
            // TODO: handle assignment of constants here
            target.print("    ");
            target.print("%");
            target.print(expression.getVariable().getName());
            target.print("_ = ");
            write(value, false);
            target.println();
        }
    }

    private void write(final Value aValue, final boolean useDirectVarRef) {
        final String replacedSymbol = valueToSymbolMaping.get(aValue);
        if (replacedSymbol != null && !(aValue instanceof ComputedMemoryLocationWriteExpression) && !(aValue instanceof ComputedMemoryLocationReadExpression)) {
            target.write(replacedSymbol);
            return;
        }

        if (aValue instanceof Variable) {
            final Variable v = (Variable) aValue;
            if (useDirectVarRef) {
                target.write("%");
            } else {
                target.write("add i32 0,%");
            }
            target.write(v.getName());
            target.write("_");
        } else if (aValue instanceof IntegerValue) {
            write((IntegerValue) aValue, useDirectVarRef);
        } else if (aValue instanceof LongValue) {
            write((LongValue) aValue, useDirectVarRef);
        } else if (aValue instanceof InvokeStaticMethodExpression) {
            write((InvokeStaticMethodExpression) aValue);
        } else if (aValue instanceof BinaryExpression) {
            write((BinaryExpression) aValue);
        } else if (aValue instanceof PHIValue) {
            write((PHIValue) aValue);
        } else if (aValue instanceof MemorySizeExpression) {
            write((MemorySizeExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationWriteExpression) {
            write((ComputedMemoryLocationWriteExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationReadExpression) {
            write((ComputedMemoryLocationReadExpression) aValue);
        } else if (aValue instanceof TypeConversionExpression) {
            write((TypeConversionExpression) aValue);
        } else if (aValue instanceof StackTopExpression) {
            write((StackTopExpression) aValue);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue.getClass());
        }
    }

    private void write(final StackTopExpression e) {
        target.write("load i32, i32* @stacktop");
    }

    private void write(final TypeConversionExpression e) {
        target.write("add i32 0,");
        write(e.incomingDataFlows().get(0), true);
    }

    private void write(final ComputedMemoryLocationReadExpression e) {
        final String theSymbol = valueToSymbolMaping.get(e);
        target.write("load i32, i32* ");
        target.write(theSymbol);
        target.write("_ptr");
    }

    private void write(final ComputedMemoryLocationWriteExpression e) {
        final String theSymbol = valueToSymbolMaping.get(e);
        target.write("i32* ");
        target.write(theSymbol);
        target.write("_ptr");
    }

    private void write(final MemorySizeExpression aValue) {
        target.write("call i32 @llvm.wasm.memory.size.i32(i32 0)");
    }

    private void write(final PHIValue aValue) {
        throw new IllegalArgumentException("This should never happen!");
    }

    private void write(final BinaryExpression aValue) {
        switch (aValue.getOperator()) {
            case ADD:
                target.write("add");
                break;
            case SUB:
                target.write("sub");
                break;
            case MUL:
                target.write("mul");
                break;
            case REMAINDER:
                target.write("srem");
                break;
            case GREATERTHAN:
                target.write("icmp sgt");
                break;
            case GREATEROREQUALS:
                target.write("icmp sge");
                break;
            case LESSTHAN:
                target.write("icmp slt");
                break;
            case LESSTHANOREQUALS:
                target.write("icmp sle");
                break;
            case EQUALS:
                target.write("icmp eq");
                break;
            case NOTEQUALS:
                target.write("icmp ne");
                break;
            default:
                throw new IllegalStateException("Not implemented : " + aValue.getOperator());
        }
        target.write(" ");
        target.write(LLVMWriterUtils.toType(aValue.resolveType()));
        target.write(" ");
        final List<Value> v = aValue.incomingDataFlows();
        for (int i=0;i<v.size();i++) {
            if (i>0) {
                target.write(",");
            }
            write(v.get(i), true);
        }
    }

    private void write(final IntegerValue aValue, final boolean useDirectVarRef) {
        if (useDirectVarRef) {
            target.print(aValue.getIntValue());
        } else {
            target.print("add i32 ");
            target.print(aValue.getIntValue());
            target.print(",0");
        }
    }

    private void write(final LongValue aValue, final boolean useDirectVarRef) {
        if (useDirectVarRef) {
            target.print(aValue.getLongValue());
        } else {
            target.print("add i32 ");
            target.print(aValue.getLongValue());
            target.print(",0");
        }
    }

    private void write(final InvokeStaticMethodExpression aValue) {
        target.print("call ");
        target.print(LLVMWriterUtils.toSignature(aValue.getSignature(), true));
        target.print(" @");
        target.print(LLVMWriterUtils.toMethodName(aValue.getClassName(), aValue.getMethodName(), aValue.getSignature()));
        target.print("(i32 undef");
        final List<Value> args = aValue.incomingDataFlows();
        for (int i=0;i<args.size();i++) {
            target.print(",");
            final Value theValue = args.get(i);
            if (theValue instanceof Variable) {
                target.print(LLVMWriterUtils.toType(TypeRef.toType(aValue.getSignature().getArguments()[i])));
                target.print(" ");
            }
            write(theValue, true);
        }
        target.print(")");
    }
}
