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

import java.io.PrintWriter;
import java.util.List;

import de.mirkosertic.bytecoder.api.opencl.Kernel;
import de.mirkosertic.bytecoder.api.opencl.OpenCLType;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ArrayEntryValue;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.GetFieldValue;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InitVariableExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodValue;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodValue;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public class OpenCLWriter extends IndentSSAWriter {

    private final OpenCLInputOutputs inputOutputs;
    private final BytecodeLinkedClass kernelClass;

    public OpenCLWriter(BytecodeLinkedClass aKernelClass, CompileOptions aOptions, Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext, OpenCLInputOutputs aInputOutputs) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        inputOutputs = aInputOutputs;
        kernelClass = aKernelClass;
    }

    public void printRelooped(Relooper.Block aBlock) {
        print("__kernel void BytecoderKernel(");

        List<OpenCLInputOutputs.KernelArgument> theArguments = inputOutputs.arguments();
        for (int i = 0; i<theArguments.size(); i++) {
            if (i>0) {
                print(", ");
            }
            OpenCLInputOutputs.KernelArgument theArgument = theArguments.get(i);
            TypeRef theTypeRef = TypeRef.toType(theArgument.getField().getField().getTypeRef());
            switch (theArgument.getType()) {
                case INPUT:
                    print("const ");
                    print(toType(theTypeRef));
                    print(" ");
                    print(theArgument.getField().getField().getName().stringValue());
                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    print(toType(theTypeRef));
                    print(" ");
                    print(theArgument.getField().getField().getName().stringValue());
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
        OpenCLWriter theWriter = this;
        if (aSimpleBlock.isLabelRequired()) {
            print("$");
            print(aSimpleBlock.label().name());
            println(" : {");
            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.writeExpressions(aSimpleBlock.internalLabel().getExpressions());

        if (aSimpleBlock.isLabelRequired()) {
            println("}");
        }
        print(aSimpleBlock.next());
    }

    private void print(Relooper.LoopBlock aLoopBlock) {
        OpenCLWriter theWriter = this;
        if (aLoopBlock.isLabelRequired()) {
            print("$");
            print(aLoopBlock.label().name());
            print(" : ");
            theWriter = theWriter.withDeeperIndent();

        }
        println("for (;;) {");

        theWriter.print(aLoopBlock.inner());

        if (aLoopBlock.isLabelRequired()) {
            println("}");
        }
        print(aLoopBlock.next());
    }

    private void print(Relooper.MultipleBlock aMultiple) {
        OpenCLWriter theWriter = this;
        if (aMultiple.isLabelRequired()) {
            print("$");
            print(aMultiple.label().name());
            print(" : ");
            theWriter = theWriter.withDeeperIndent();
        }

        println("for(;;) switch (__label__) {");

        for (Relooper.Block theHandler : aMultiple.handlers()) {
            for (GraphNode theEntry : theHandler.entries()) {
                theWriter.print("case ");
                theWriter.print(theEntry.getStartAddress().getAddress());
                theWriter.println(" : ");
            }

            OpenCLWriter theHandlerWriter = theWriter.withDeeperIndent();
            theHandlerWriter.print(theHandler);
        }

        if (aMultiple.isLabelRequired()) {
            println("}");
        }

        print(aMultiple.next());
    }

    private OpenCLWriter withDeeperIndent() {
        return new OpenCLWriter(kernelClass, options, program, indent + "    ", writer, linkerContext, inputOutputs);
    }

    private void printInstanceFieldReference(BytecodeFieldRefConstant aField) {
        print("->");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private void writeExpressions(ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            if (options.isDebugOutput()) {
                String theComment = theExpression.getComment();
                if (theComment != null && theComment.length() > 0) {
                    print("// ");
                    println(theComment);
                }
            }
            if (theExpression instanceof InitVariableExpression) {
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
            } else if (theExpression instanceof IFExpression) {
                IFExpression theE = (IFExpression) theExpression;
                print("if (");
                printValue(theE.getBooleanValue());
                println(") {");

                withDeeperIndent().writeExpressions(theE.getExpressions());

                println("}");
            } else if (theExpression instanceof BreakExpression) {
                BreakExpression theBreak = (BreakExpression) theExpression;
                print("$__label__ = ");
                print(theBreak.jumpTarget().getAddress());
                println(";");
                print("break $");
                print(theBreak.blockToBreak().name());
                println(";");

            } else if (theExpression instanceof ContinueExpression) {
                ContinueExpression theContinue = (ContinueExpression) theExpression;
                print("$__label__ = ");
                print(theContinue.jumpTarget().getAddress());
                println(";");
                print("continue $");
                print(theContinue.labelToReturnTo().name());
                println(";");

            } else if (theExpression instanceof ReturnExpression) {
                println("return;");
            } else if (theExpression instanceof PutFieldExpression) {
                PutFieldExpression thePutField = (PutFieldExpression) theExpression;
                Value theTarget = thePutField.getTarget();
                BytecodeFieldRefConstant theField = thePutField.getField();
                Value thevalue = thePutField.getValue();
                printValue(theTarget);
                printInstanceFieldReference(theField);
                print(" = ");
                printValue(thevalue);
                println(";");
            } else {
                throw new IllegalArgumentException("Not supported. " + theExpression);
            }
        }
    }

    private String toStructName(BytecodeObjectTypeRef aObjectType) {
        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(aObjectType);
        BytecodeClass theBytecodeClass = theLinkedClass.getBytecodeClass();
        BytecodeAnnotation theAnnotation = theBytecodeClass.getAttributes().getAnnotationByType(OpenCLType.class.getName());
        if (theAnnotation == null) {
            throw new IllegalArgumentException("No @OpenCLType found for " + aObjectType.name());
        }
        return theAnnotation.getElementValueByName("name").stringValue();
    }

    private String toType(TypeRef aType) {
        return toType(aType, true);
    }

    private String toType(TypeRef aType, boolean aMakePointer) {
        if (aType.isArray()) {
            TypeRef.ArrayTypeRef theArray = (TypeRef.ArrayTypeRef) aType;
            return "__global " + toType(TypeRef.toType(theArray.arrayType().getType())) + (aMakePointer ? "*" : '&');
        }
        if (aType instanceof TypeRef.ObjectTypeRef) {
            TypeRef.ObjectTypeRef theObject = (TypeRef.ObjectTypeRef) aType;
            return toStructName(theObject.objectType());
        }
        switch (aType.resolve()) {
            case INT:
                return "int";
            case FLOAT:
                return "float";
            case LONG:
                return "long";
            case DOUBLE:
                return "double";
            case REFERENCE:
                return "void*";
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
        } else if (aValue instanceof InvokeStaticMethodValue) {
            printInvokeStatic((InvokeStaticMethodValue) aValue);
        } else if (aValue instanceof GetFieldValue) {
            printGetFieldValue((GetFieldValue) aValue);
        } else if (aValue instanceof ArrayEntryValue) {
            printArrayEntryValue((ArrayEntryValue) aValue);
        } else if (aValue instanceof BinaryValue) {
            printBinaryValue((BinaryValue) aValue);
        } else if (aValue instanceof IntegerValue) {
            printIntegerValue((IntegerValue) aValue);
        } else if (aValue instanceof LongValue) {
            printLongValue((LongValue) aValue);
        } else if (aValue instanceof FloatValue) {
            printFloatValue((FloatValue) aValue);
        } else if (aValue instanceof DoubleValue) {
            printDoubleValue((DoubleValue) aValue);
        } else if (aValue instanceof TypeConversionValue) {
            printTypeConversionValue((TypeConversionValue) aValue);
        } else {
            throw new IllegalArgumentException("Not supported : " + aValue);
        }
    }

    private void printTypeConversionValue(TypeConversionValue aValue) {
        print("((");
        print(toType(aValue.resolveType()));
        print(") ");
        printValue(aValue.resolveFirstArgument());
        print(")");
    }

    private void printDoubleValue(DoubleValue aValue) {
        print(aValue.getDoubleValue());
    }

    private void printFloatValue(FloatValue aValue) {
        print(aValue.getFloatValue());
    }

    private void printLongValue(LongValue aValue) {
        print(aValue.getLongValue());
    }

    private void printIntegerValue(IntegerValue aValue) {
        print(aValue.getIntValue());
    }

    private void printBinaryValue(BinaryValue aValue) {
        Value theValue1 = aValue.resolveFirstArgument();
        printValue(theValue1);
        switch (aValue.getOperator()) {
        case ADD:
            print(" + ");
            break;
        case DIV:
            print(" / ");
            break;
        case MUL:
            print(" * ");
            break;
        case SUB:
            print(" - ");
            break;
        case EQUALS:
            print(" == ");
            break;
        case BINARYOR:
            print(" | ");
            break;
        case LESSTHAN:
            print(" < ");
            break;
        case BINARYAND:
            print(" & ");
            break;
        case BINARYXOR:
            print(" ^ ");
            break;
        case NOTEQUALS:
            print(" != ");
            break;
        case REMAINDER:
            print(" % ");
            break;
        case GREATERTHAN:
            print(" > ");
            break;
        case BINARYSHIFTLEFT:
            print(" << ");
            break;
        case GREATEROREQUALS:
            print(" >= ");
            break;
        case BINARYSHIFTRIGHT:
            print(" >> ");
            break;
        case LESSTHANOREQUALS:
            print(" <= ");
            break;
        case BINARYUNSIGNEDSHIFTRIGHT:
            print(" >>> ");
            break;
        default:
            throw new IllegalStateException("Unsupported operator : " + aValue.getOperator());
        }
        Value theValue2 = aValue.resolveSecondArgument();
        printValue(theValue2);
    }

    private void printGetFieldValue(GetFieldValue aValue) {
        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));
        if (theLinkedClass == kernelClass) {
            print(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
        } else {
            Value theValue = aValue.resolveFirstArgument();
            if (theValue instanceof Variable && ((Variable) theValue).isSynthetic()) {
                print(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
            } else {
                printValue(theValue);
                printInstanceFieldReference(aValue.getField());
            }
        }
    }

    private void printArrayEntryValue(ArrayEntryValue aValue) {
        Value theArray = aValue.resolveFirstArgument();
        Value theIndex = aValue.resolveSecondArgument();
        if (aValue.resolveType().isObject()) {
            print("&");
        }
        printValue(theArray);
        print("[");
        printValue(theIndex);
        print("]");
    }

    private void printInvokeVirtual(InvokeVirtualMethodValue aValue) {
        throw new IllegalArgumentException("Not supported method : " + aValue.getMethodName());
    }

    private void printInvokeStatic(InvokeStaticMethodValue aValue) {
        if (aValue.getMethodName().equals("get_global_id")) {
            print("get_global_id(0)");
        } else {
            throw new IllegalArgumentException("Not supported method : " + aValue.getMethodName());
        }
    }

}