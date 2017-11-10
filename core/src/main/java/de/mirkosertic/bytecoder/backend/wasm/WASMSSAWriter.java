/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.wasm;

import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.*;

import java.io.PrintWriter;

public class WASMSSAWriter extends IndentSSAWriter {

    public WASMSSAWriter(Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext) {
        super(aProgram, aIndent, aWriter, aLinkerContext);
    }

    public WASMSSAWriter withDeeperIndent() {
        return new WASMSSAWriter(program, indent + "    ", writer, linkerContext);
    }

    public void writeNode(ControlFlowGraph.Node aNode) {
        if (aNode instanceof ControlFlowGraph.SimpleNode) {
            writeNode((ControlFlowGraph.SimpleNode) aNode);
            return;
        }
        throw new IllegalStateException("Not supported!");
    }

    private void writeNode(ControlFlowGraph.SimpleNode aNode) {
        for (Expression theExpression : aNode.getNode().getExpressions().toList()) {
            writeExpression(theExpression);
        }
    }

    private void writeExpression(Expression aExpression) {
        if (aExpression instanceof ReturnExpression) {
            writeExpression((ReturnExpression) aExpression);
            return;
        }
        if (aExpression instanceof CommentExpression) {
            writeExpression((CommentExpression) aExpression);
            return;
        }
        if (aExpression instanceof InitVariableExpression) {
            writeExpression((InitVariableExpression) aExpression);
            return;
        }
        if (aExpression instanceof DirectInvokeMethodExpression) {
            writeExpression((DirectInvokeMethodExpression) aExpression);
            return;
        }
        throw new IllegalStateException("Not supported : " + aExpression);
    }

    private void writeExpression(DirectInvokeMethodExpression aExpression) {
        println(";; invoke " + aExpression.getValue());
    }

    private void writeExpression(InitVariableExpression aExpression) {
        Variable theVariable = aExpression.getVariable();
        if (theVariable.getValue() instanceof PrimitiveValue) {
            // Primitives are always inlined!
            return;
        }
        print("(set_local $");
        print(theVariable.getName());
        println();

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.writeValue(theVariable.getValue());

        println(")");
    }

    private void writeValue(Value aValue) {
        if (aValue instanceof BinaryValue) {
            writeValue((BinaryValue) aValue);
            return;
        }
        if (aValue instanceof ByteValue) {
            writeValue((ByteValue) aValue);
            return;
        }
        if (aValue instanceof IntegerValue) {
            writeValue((IntegerValue) aValue);
            return;
        }
        throw new IllegalStateException("Not supported : " + aValue);
    }

    private void writeValue(ByteValue aValue) {
        print("(i32.const ");
        print(aValue.getByteValue());
        print(")");
    }

    private void writeValue(IntegerValue aValue) {
        print("(i32.const ");
        print(aValue.getIntValue());
        print(")");
    }

    private void writeValue(BinaryValue aValue) {
        switch (aValue.getOperator()) {
            case ADD: {
                println("(i32.add ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();

                println(")");
                break;
            }
            default:
                throw new IllegalStateException("Operator not supported : " + aValue.getOperator());
        }
    }

    private void writeExpression(CommentExpression aExpression) {
        print(";; ");
        println(aExpression.getValue());
    }

    private void writeExpression(ReturnExpression aExpression) {
        println("(return)");
    }

    private void printVariableNameOrValue(Variable aVariable) {
        if (aVariable.getValue() instanceof PrimitiveValue) {
            writeValue(aVariable.getValue());
        } else {
            print("(get_local ");
            print("$");
            print(aVariable.getName());
            print(")");
        }
    }
}
