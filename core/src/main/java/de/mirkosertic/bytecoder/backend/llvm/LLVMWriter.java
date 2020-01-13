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

import java.io.PrintWriter;
import java.util.List;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.graph.GraphDFSOrder;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

public class LLVMWriter {

    private final PrintWriter target;

    public LLVMWriter(final PrintWriter target) {
        this.target = target;
    }

    public void write(final Program aProgram) {
        final ControlFlowGraph theGraph = aProgram.getControlFlowGraph();
        final RegionNode theStart = theGraph.startNode();
        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(theStart,
                RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        final List<RegionNode> sorted = order.getNodesInOrder();
        for (final RegionNode theBlock : sorted) {
            final BytecodeOpcodeAddress theBlockStart = theBlock.getStartAddress();
            if (theBlockStart.getAddress() == 0) {
                target.println("entry:");
            } else {
                target.print("block");
                target.print(theBlockStart.getAddress());
                target.println(":");
            }
            write(theBlock);
        }
    }

    private void write(final RegionNode block) {
        write(block.getExpressions());
    }

    private void write(final ExpressionList list) {
        for (final Expression e : list.toList()) {
            if (e instanceof ReturnExpression) {
                write((ReturnExpression) e);
            } else if (e instanceof VariableAssignmentExpression) {
                write((VariableAssignmentExpression) e);
            } else if (e instanceof ReturnValueExpression) {
                write((ReturnValueExpression) e);
            } else if (e instanceof GotoExpression) {
                write((GotoExpression) e);
            } else {
                throw new IllegalStateException("Not implemented : " + e.getClass());
            }
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
        if (result instanceof Variable) {
            final Variable v = (Variable) result;
            target.print("%");
            target.print(v.getName());
        } else {
            write(result, true);
        }
        target.println();
    }

    private void write(final VariableAssignmentExpression expression) {
        target.print("    ");
        target.print("%");
        target.print(expression.getVariable().getName());
        target.print(" = ");
        write(expression.incomingDataFlows().get(0), false);
        target.println();
    }

    private void write(final Value aValue, final boolean useDirectVarRef) {
        if (aValue instanceof Variable) {
            final Variable v = (Variable) aValue;
            if (useDirectVarRef) {
                target.write("%");
            } else {
                target.write("add i32 0,%");
            }
            target.write(v.getName());
        } else if (aValue instanceof IntegerValue) {
            write((IntegerValue) aValue, useDirectVarRef);
        } else if (aValue instanceof InvokeStaticMethodExpression) {
            write((InvokeStaticMethodExpression) aValue);
        } else if (aValue instanceof BinaryExpression) {
            write((BinaryExpression) aValue);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue.getClass());
        }
    }

    private void write(final BinaryExpression aValue) {
        switch (aValue.getOperator()) {
            case ADD:
                target.write("add");
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
        target.println();
    }

    private void write(final IntegerValue aValue, final boolean useDirectVarRef) {
        if (useDirectVarRef) {
            target.print("i32 ");
            target.print(aValue.getIntValue());
        } else {
            target.print("add i32 ");
            target.print(aValue.getIntValue());
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
