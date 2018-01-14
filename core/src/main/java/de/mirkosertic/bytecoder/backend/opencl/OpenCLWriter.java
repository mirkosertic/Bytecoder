/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.opencl;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ArrayEntryValue;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.CommentExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.GetFieldValue;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.InitVariableExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.io.PrintWriter;
import java.util.List;

public class OpenCLWriter extends IndentSSAWriter {

    private final OpenCLInputOutputs inputOutputs;

    public OpenCLWriter(CompileOptions aOptions, Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext, OpenCLInputOutputs aInputOutputs) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        inputOutputs = aInputOutputs;
    }

    public void printRelooped(Relooper.Block aBlock) {
        print("__kernel void BytecoderKernel(");

        List<OpenCLInputOutputs.KernelArgument> theArguments = inputOutputs.arguments();
        for (int i = 0; i<theArguments.size(); i++) {
            if (i>0) {
                print(", ");
            }
            OpenCLInputOutputs.KernelArgument theArgument = theArguments.get(i);
            switch (theArgument.getType()) {
                case INPUT:
                    print("__global const float* ");
                    print(theArgument.getField().getName());
                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    print("__global float* ");
                    print(theArgument.getField().getName());
                    break;
            }
        }

        println(") {");
        OpenCLWriter theDeeper = withDeeperIndent();
        theDeeper.println("int $__label__ = 0;");
        theDeeper.print(aBlock);

        println("}");
    }

    private void print(Relooper.Block aBlock) {
        if (aBlock == null) {
            return;
        }
        if (aBlock instanceof Relooper.SimpleBlock) {
            print((Relooper.SimpleBlock) aBlock);
            return;
        }
        if (aBlock instanceof Relooper.LoopBlock) {
            print((Relooper.LoopBlock) aBlock);
            return;
        }
        if (aBlock instanceof Relooper.MultipleBlock) {
            print((Relooper.MultipleBlock) aBlock);
            return;
        }
        throw new IllegalStateException("Not implemented : " + aBlock);
    }

    private void print(Relooper.SimpleBlock aSimpleBlock) {
        print("$");
        print(aSimpleBlock.label().name());
        println(" : {");

        OpenCLWriter theDeeper = withDeeperIndent();
        theDeeper.writeExpressions(aSimpleBlock.internalLabel().getExpressions());

        println("}");
        print(aSimpleBlock.next());
    }

    private void print(Relooper.LoopBlock aLoopBlock) {
        print("$");
        print(aLoopBlock.label().name());
        println(" : for (;;) {");

        OpenCLWriter theDeeper = withDeeperIndent();
        theDeeper.print(aLoopBlock.inner());

        println("}");
        print(aLoopBlock.next());
    }

    private void print(Relooper.MultipleBlock aMultiple) {

        print("$");
        print(aMultiple.label().name());
        println(" : for(;;) switch (__label__) {");

        OpenCLWriter theDeeper = withDeeperIndent();
        for (Relooper.Block theHandler : aMultiple.handlers()) {
            for (GraphNode theEntry : theHandler.entries()) {
                theDeeper.print("case ");
                theDeeper.print(theEntry.getStartAddress().getAddress());
                theDeeper.println(" : ");
            }

            OpenCLWriter theHandlerWriter = theDeeper.withDeeperIndent();
            theHandlerWriter.print(theHandler);
        }

        println("}");
        print(aMultiple.next());
    }

    private OpenCLWriter withDeeperIndent() {
        return new OpenCLWriter(options, program, indent + "    ", writer, linkerContext, inputOutputs);
    }

    private void writeExpressions(ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof CommentExpression) {
                if (options.isDebugOutput()) {
                    CommentExpression theComment = (CommentExpression) theExpression;
                    print("// ");
                    println(theComment.getValue());
                }
            } else if (theExpression instanceof InitVariableExpression) {
                InitVariableExpression theInit = (InitVariableExpression) theExpression;
                print(toType(theInit.getVariable().resolveType()));
                print(" ");
                print(theInit.getVariable().getName());
                print(" = ");
                printValue(theInit.getValue());
                println(";");
            } else if (theExpression instanceof ArrayStoreExpression) {
                ArrayStoreExpression theStore = (ArrayStoreExpression) theExpression;
                Value theArray = theStore.getArray();
                Value theIndex = theStore.getIndex();
                Value theValue = theStore.getValue();
                printValue(theArray);
                print("[");
                printValue(theIndex);
                print("] = ");
                printValue(theValue);
                println(";");
            } else if (theExpression instanceof ReturnExpression) {
                println("return;");
            } else {
                throw new IllegalArgumentException("Not supported. " + theExpression);
            }
        }
    }

    private String toType(TypeRef aType) {
        switch (aType.resolve()) {
            case INT:
                return "int";
            case FLOAT:
                return "float";
            case REFERENCE:
                return "float*";
            default:
                throw new IllegalArgumentException("Not supported : " + aType.resolve());
        }
    }

    private void printValue(Value aValue) {
        if (aValue instanceof Variable) {
            Variable theVariable = (Variable) aValue;
            print(theVariable.getName());
        } else if (aValue instanceof InvokeVirtualMethodValue) {
            printInvokeVirtual((InvokeVirtualMethodValue) aValue);
        } else if (aValue instanceof GetFieldValue) {
            printGetFieldValue((GetFieldValue) aValue);
        } else if (aValue instanceof ArrayEntryValue) {
            printArrayEntryValue((ArrayEntryValue) aValue);
        } else if (aValue instanceof BinaryValue) {
            printBinaryValue((BinaryValue) aValue);
        } else {
            throw new IllegalArgumentException("Not supported : " + aValue);
        }
    }

    private void printBinaryValue(BinaryValue aValue) {
        switch (aValue.getOperator()) {
            case ADD: {
                Value theValue1 = aValue.resolveFirstArgument();
                Value theValue2 = aValue.resolveSecondArgument();
                printValue(theValue1);
                print(" + ");
                printValue(theValue2);
                break;
            }
            default:
                throw new IllegalStateException("Not supported " + aValue.getOperator());
        }
    }

    private void printGetFieldValue(GetFieldValue aValue) {
        print(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private void printArrayEntryValue(ArrayEntryValue aValue) {
        Value theArray = aValue.resolveFirstArgument();
        Value theIndex = aValue.resolveSecondArgument();
        printValue(theArray);
        print("[");
        printValue(theIndex);
        print("]");
    }

    private void printInvokeVirtual(InvokeVirtualMethodValue aValue) {
        if (aValue.getMethodName().equals("get_global_id")) {
            print("get_global_id(0)");
        } else {
            throw new IllegalArgumentException("Not supported method : " + aValue.getMethodName());
        }
    }
}
