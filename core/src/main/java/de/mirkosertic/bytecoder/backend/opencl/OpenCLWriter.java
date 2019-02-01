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

import de.mirkosertic.bytecoder.api.opencl.OpenCLFunction;
import de.mirkosertic.bytecoder.api.opencl.OpenCLType;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ArrayEntryExpression;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.CompareExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvocationExpression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpenCLWriter extends IndentSSAWriter {

    private final OpenCLInputOutputs inputOutputs;
    private final BytecodeLinkedClass kernelClass;

    public OpenCLWriter(
            final BytecodeLinkedClass aKernelClass, final CompileOptions aOptions, final Program aProgram, final String aIndent, final PrintWriter aWriter, final BytecodeLinkerContext aLinkerContext, final OpenCLInputOutputs aInputOutputs) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        inputOutputs = aInputOutputs;
        kernelClass = aKernelClass;
    }

    public void printReloopedKernel(final Program aProgram, final Relooper.Block aBlock) {
        print("__kernel void BytecoderKernel(");

        final List<OpenCLInputOutputs.KernelArgument> theArguments = inputOutputs.arguments();
        for (int i = 0; i<theArguments.size(); i++) {
            if (i>0) {
                print(", ");
            }
            final OpenCLInputOutputs.KernelArgument theArgument = theArguments.get(i);
            final TypeRef theTypeRef = TypeRef.toType(theArgument.getField().getValue().getTypeRef());
            switch (theArgument.getType()) {
                case INPUT:
                    print("const ");
                    print(toType(theTypeRef));
                    print(" ");
                    print(theArgument.getField().getValue().getName().stringValue());
                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    print(toType(theTypeRef));
                    print(" ");
                    print(theArgument.getField().getValue().getName().stringValue());
                    break;
            }
        }

        println(") {");
        final OpenCLWriter theDeeper = withDeeperIndent();
        theDeeper.println("int $__label__ = 0;");

        for (final Variable theVariable : aProgram.getVariables()) {
            if (!theVariable.isSynthetic()) {
                theDeeper.print(toType(theVariable.resolveType(), false));
                theDeeper.print(" ");
                theDeeper.print(theVariable.getName());
                theDeeper.println(";");
            }
        }

        theDeeper.print(aBlock);

        println("}");
    }

    public void printReloopedInline(final BytecodeMethod aMethod, final Program aProgram, final Relooper.Block aBlock) {

        final BytecodeMethodSignature theSignature = aMethod.getSignature();

        print("__inline ");
        print(toType(TypeRef.toType(theSignature.getReturnType())));
        print(" ");
        print(aMethod.getName().stringValue());
        print("(");

        boolean theFirst = true;
        final List<OpenCLInputOutputs.KernelArgument> theArguments = inputOutputs.arguments();
        for (final OpenCLInputOutputs.KernelArgument theArgument1 : theArguments) {
            if (theFirst) {
                theFirst = false;
            } else {
                print(", ");
            }
            final TypeRef theTypeRef = TypeRef.toType(theArgument1.getField().getValue().getTypeRef());
            switch (theArgument1.getType()) {
                case INPUT:
                    print("const ");
                    print(toType(theTypeRef));
                    print(" ");
                    print(theArgument1.getField().getValue().getName().stringValue());
                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    print(toType(theTypeRef));
                    print(" ");
                    print(theArgument1.getField().getValue().getName().stringValue());
                    break;
            }
        }

        final List<Program.Argument> theProgramArguments = aProgram.getArguments();
        for (int i=1;i<theProgramArguments.size();i++) {
            final Program.Argument theArgument = theProgramArguments.get(i);
            if (theFirst) {
                theFirst = false;
            } else {
                print(", ");
            }
            print(toType(theArgument.getVariable().resolveType()));
            print(" ");
            print(theArgument.getVariable().getName());
        }

        println(") {");

        final OpenCLWriter theDeeper = withDeeperIndent();
        theDeeper.println("int $__label__ = 0;");

        for (final Variable theVariable : aProgram.getVariables()) {
            if (!theVariable.isSynthetic()) {
                theDeeper.print(toType(theVariable.resolveType(), false));
                theDeeper.print(" ");
                theDeeper.print(theVariable.getName());
                theDeeper.println(";");
            }
        }

        theDeeper.print(aBlock);

        println("}");
    }

    private void print(final Relooper.Block aBlock) {
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
        if (aBlock instanceof Relooper.IFThenElseBlock) {
            print((Relooper.IFThenElseBlock) aBlock);
            return;
        }
        throw new IllegalStateException("Not implemented : " + aBlock);
    }

    private void print(final Relooper.SimpleBlock aSimpleBlock) {
        OpenCLWriter theWriter = this;
        if (aSimpleBlock.isLabelRequired()) {
            print("$");
            print(aSimpleBlock.label().name());
            println(" :");
            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.writeExpressions(aSimpleBlock.expressions());

        if (aSimpleBlock.next() != null) {

            print("$");
            print(aSimpleBlock.label().name());
            println("_next:");

            print(aSimpleBlock.next());

        }
    }

    private void print(final Relooper.IFThenElseBlock aIfThenElseBlock) {
        OpenCLWriter theWriter = this;
        if (aIfThenElseBlock.isLabelRequired()) {
            print("$");
            print(aIfThenElseBlock.label().name());
            println(" :");
            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.writeExpressions(aIfThenElseBlock.getPrelude());

        theWriter.print("if (");
        theWriter.printValue(aIfThenElseBlock.getCondition());
        theWriter.println(") {");

        theWriter.withDeeperIndent().print(aIfThenElseBlock.getTrueBlock());

        theWriter.println("} else {");

        theWriter.withDeeperIndent().print(aIfThenElseBlock.getFalseBlock());

        theWriter.println("}");

        if (aIfThenElseBlock.next() != null) {

            print("$");
            print(aIfThenElseBlock.label().name());
            println("_next:");

            print(aIfThenElseBlock.next());
        }
    }

    private void print(final Relooper.LoopBlock aLoopBlock) {
        OpenCLWriter theWriter = this;
        if (aLoopBlock.isLabelRequired()) {
            print("$");
            print(aLoopBlock.label().name());
            println(" : ");
            theWriter = theWriter.withDeeperIndent();

        }

        theWriter.print(aLoopBlock.inner());

        if (aLoopBlock.next() != null) {
            print("$");
            print(aLoopBlock.label().name());
            println("_next:");

            print(aLoopBlock.next());
        }
    }

    private void print(final Relooper.MultipleBlock aMultiple) {
        OpenCLWriter theWriter = this;
        if (aMultiple.isLabelRequired()) {
            print("$");
            print(aMultiple.label().name());
            print(" : ");
            theWriter = theWriter.withDeeperIndent();
        }

        println("switch ($__label__) {");

        for (final Relooper.Block theHandler : aMultiple.handlers()) {
            for (final RegionNode theEntry : theHandler.entries()) {
                theWriter.print("case ");
                theWriter.print(theEntry.getStartAddress().getAddress());
                theWriter.println(" : ");
            }

            final OpenCLWriter theHandlerWriter = theWriter.withDeeperIndent();
            theHandlerWriter.print(theHandler);
        }

        println("}");

        if (aMultiple.next() != null) {
            print("$");
            print(aMultiple.label().name());
            println("_next:");

            print(aMultiple.next());
        }
    }

    private OpenCLWriter withDeeperIndent() {
        return new OpenCLWriter(kernelClass, options, program, indent + "    ", writer, linkerContext, inputOutputs);
    }

    private void printInstanceFieldReference(final BytecodeFieldRefConstant aField) {
        print("->");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private void writeExpressions(final ExpressionList aList) {
        for (final Expression theExpression : aList.toList()) {
            if (options.isDebugOutput()) {
                final String theComment = theExpression.getComment();
                if (theComment != null && !theComment.isEmpty()) {
                    print("// ");
                    println(theComment);
                }
            }
            if (theExpression instanceof VariableAssignmentExpression) {
                final VariableAssignmentExpression theInit = (VariableAssignmentExpression) theExpression;

                final Variable theVariable = theInit.getVariable();
                final Value theValue = theInit.getValue();
                if (theVariable.resolveType().isObject() && theValue instanceof InvocationExpression) {
                    print(toType(theVariable.resolveType(), false));
                    print(" ");
                    print(theVariable.getName());
                    print("_temp = ");
                    printValue(theValue);
                    println(";");

                    print(theVariable.getName());
                    print(" = &");
                    print(theVariable.getName());
                    println("_temp;");
                } else {
                    print(theVariable.getName());
                    print(" = ");
                    printValue(theValue);
                    println(";");
                }
            } else if (theExpression instanceof ArrayStoreExpression) {
                final ArrayStoreExpression theStore = (ArrayStoreExpression) theExpression;
                final List<Value> theIncomingData = theStore.incomingDataFlows();
                final Value theArray = theIncomingData.get(0);
                final Value theIndex = theIncomingData.get(1);
                final Value theValue = theIncomingData.get(2);
                printValue(theArray);
                print("[");
                printValue(theIndex);
                print("] = ");
                printValue(theValue);
                println(";");
            } else if (theExpression instanceof IFExpression) {
                final IFExpression theE = (IFExpression) theExpression;
                print("if ");
                printValue(theE.incomingDataFlows().get(0));
                println(" {");

                withDeeperIndent().writeExpressions(theE.getExpressions());

                println("}");
            } else if (theExpression instanceof BreakExpression) {
                final BreakExpression theBreak = (BreakExpression) theExpression;
                if (theBreak.isSetLabelRequired()) {
                    print("$__label__ = ");
                    print(theBreak.jumpTarget().getAddress());
                    println(";");
                }
                if (!theBreak.isSilent()) {
                    print("goto $");
                    print(theBreak.blockToBreak().name());
                    println("_next;");
                }
            } else if (theExpression instanceof ContinueExpression) {
                final ContinueExpression theContinue = (ContinueExpression) theExpression;
                print("$__label__ = ");
                print(theContinue.jumpTarget().getAddress());
                println(";");
                print("goto $");
                print(theContinue.labelToReturnTo().name());
                println(";");

            } else if (theExpression instanceof ReturnExpression) {
                println("return;");
            } else if (theExpression instanceof PutFieldExpression) {
                final PutFieldExpression thePutField = (PutFieldExpression) theExpression;

                final List<Value> theIncomingData = thePutField.incomingDataFlows();

                final Value theTarget = theIncomingData.get(0);
                final BytecodeFieldRefConstant theField = thePutField.getField();

                final Value thevalue = theIncomingData.get(1);
                printValue(theTarget);
                printInstanceFieldReference(theField);
                print(" = ");
                printValue(thevalue);
                println(";");
            } else if (theExpression instanceof ReturnValueExpression) {
                final ReturnValueExpression theReturn = (ReturnValueExpression) theExpression;

                final List<Value> theIncomingData = theReturn.incomingDataFlows();
                print("return ");
                printValue(theIncomingData.get(0));
                println(";");
            } else {
                throw new IllegalArgumentException("Not supported. " + theExpression);
            }
        }
    }

    private String toStructName(final BytecodeObjectTypeRef aObjectType) {
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(aObjectType);
        final BytecodeClass theBytecodeClass = theLinkedClass.getBytecodeClass();
        final BytecodeAnnotation theAnnotation = theBytecodeClass.getAttributes().getAnnotationByType(OpenCLType.class.getName());
        if (theAnnotation == null) {
            throw new IllegalArgumentException("No @OpenCLType found for " + aObjectType.name());
        }
        return theAnnotation.getElementValueByName("name").stringValue();
    }

    private String toType(final TypeRef aType) {
        return toType(aType, true);
    }

    private String toType(final TypeRef aType, final boolean aCreatePointer) {
        if (aType.isArray()) {
            final TypeRef.ArrayTypeRef theArray = (TypeRef.ArrayTypeRef) aType;
            return "__global " + toType(TypeRef.toType(theArray.arrayType().getType()), false) + "*";
        }
        if (aType instanceof TypeRef.ObjectTypeRef) {
            final TypeRef.ObjectTypeRef theObject = (TypeRef.ObjectTypeRef) aType;
            return toStructName(theObject.objectType()) + (aCreatePointer ? "*" : "");
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

    private void printValue(final Value aValue) {
        if (aValue instanceof Variable) {
            final Variable theVariable = (Variable) aValue;
            print(theVariable.getName());
        } else if (aValue instanceof InvokeVirtualMethodExpression) {
            printInvokeVirtual((InvokeVirtualMethodExpression) aValue);
        } else if (aValue instanceof InvokeStaticMethodExpression) {
            printInvokeStatic((InvokeStaticMethodExpression) aValue);
        } else if (aValue instanceof GetFieldExpression) {
            printGetFieldValue((GetFieldExpression) aValue);
        } else if (aValue instanceof ArrayEntryExpression) {
            printArrayEntryValue((ArrayEntryExpression) aValue);
        } else if (aValue instanceof BinaryExpression) {
            printBinaryValue((BinaryExpression) aValue);
        } else if (aValue instanceof IntegerValue) {
            printIntegerValue((IntegerValue) aValue);
        } else if (aValue instanceof LongValue) {
            printLongValue((LongValue) aValue);
        } else if (aValue instanceof FloatValue) {
            printFloatValue((FloatValue) aValue);
        } else if (aValue instanceof DoubleValue) {
            printDoubleValue((DoubleValue) aValue);
        } else if (aValue instanceof TypeConversionExpression) {
            printTypeConversionValue((TypeConversionExpression) aValue);
        } else if (aValue instanceof CompareExpression) {
            printCompareExpression((CompareExpression) aValue);
        } else if (aValue instanceof DirectInvokeMethodExpression) {
            printDirectInvokeMethodExpression((DirectInvokeMethodExpression) aValue);
        } else {
            throw new IllegalArgumentException("Not supported : " + aValue);
        }
    }

    private void printDirectInvokeMethodExpression(final DirectInvokeMethodExpression aExpression) {
        print(aExpression.getMethodName());
        print("(");

        final List<OpenCLInputOutputs.KernelArgument> theArguments = inputOutputs.arguments();
        boolean theFirst = true;
        for (int i = 0; i<theArguments.size(); i++) {
            theFirst = false;
            if (i>0) {
                print(", ");
            }
            final OpenCLInputOutputs.KernelArgument theArgument = theArguments.get(i);
            print(theArgument.getField().getValue().getName().stringValue());
        }

        final List<Value> theMethodArguments = aExpression.incomingDataFlows();
        for (int i=1;i<theMethodArguments.size();i++) {
            final Value theValue = theMethodArguments.get(i);
            if (theFirst) {
                theFirst = false;
            } else {
                print(",");
            }
            print("&");
            printValue(theValue);
        }

        print(")");
    }

    private void printCompareExpression(final CompareExpression aExpression) {
        final List<Value> theIncomingData = aExpression.incomingDataFlows();

        final Value theVariable1 = theIncomingData.get(0);
        final Value theVariable2 = theIncomingData.get(1);
        print("(");
        printValue(theVariable1);
        print(" > ");
        printValue(theVariable2);
        print(" ? 1 ");
        print(" : (");
        printValue(theVariable1);
        print(" < ");
        printValue(theVariable2);
        print(" ? -1 : 0))");
    }

    private void printTypeConversionValue(final TypeConversionExpression aValue) {
        print("((");
        print(toType(aValue.resolveType()));
        print(") ");
        printValue(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void printDoubleValue(final DoubleValue aValue) {
        print(aValue.getDoubleValue());
    }

    private void printFloatValue(final FloatValue aValue) {
        print(aValue.getFloatValue());
    }

    private void printLongValue(final LongValue aValue) {
        print(aValue.getLongValue());
    }

    private void printIntegerValue(final IntegerValue aValue) {
        print(aValue.getIntValue());
    }

    private void printBinaryValue(final BinaryExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        print("(");
        printValue(theIncomingData.get(0));
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
        printValue(theIncomingData.get(1));
        print(")");
    }

    private void printGetFieldValue(final GetFieldExpression aValue) {
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));
        if (theLinkedClass == kernelClass) {
            print(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
        } else {
            final Value theValue = aValue.incomingDataFlows().get(0);
            if (theValue instanceof Variable && ((Variable) theValue).isSynthetic()) {
                print(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
            } else {
                printValue(theValue);
                printInstanceFieldReference(aValue.getField());
            }
        }
    }

    private void printArrayEntryValue(final ArrayEntryExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        final Value theArray = theIncomingData.get(0);
        final Value theIndex = theIncomingData.get(1);
        if (aValue.resolveType().isObject()) {
            print("&");
        }
        printValue(theArray);
        print("[");
        printValue(theIndex);
        print("]");
    }

    private void printInvokeVirtual(final InvokeVirtualMethodExpression aValue) {
        throw new IllegalArgumentException("Not supported method : " + aValue.getMethodName());
    }

    private void printInvokeStatic(final InvokeStaticMethodExpression aValue) {
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(aValue.getClassName());
        final BytecodeResolvedMethods theMethods = theLinkedClass.resolvedMethods();
        final AtomicBoolean theFound = new AtomicBoolean(false);
        theMethods.stream().forEach(aMethodMapsEntry -> {
            final BytecodeMethod theMethod = aMethodMapsEntry.getValue();
            if (Objects.equals(theMethod.getName().stringValue(), aValue.getMethodName()) && theMethod.getSignature().matchesExactlyTo(aValue.getSignature())) {
                final BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(OpenCLFunction.class.getName());
                if (theAnnotation == null) {
                    throw new IllegalArgumentException("Annotation @OpenCLFunction required for static method " + aValue.getMethodName());
                }
                final String theMethodName = theAnnotation.getElementValueByName("value").stringValue();
                final BytecodeMethodSignature theSignature = aValue.getSignature();
                print(theMethodName);
                print("(");
                final List<Value> theArguments = aValue.incomingDataFlows();
                for (int i=0;i<theArguments.size();i++) {
                    if (i>0) {
                        print(",");
                    }
                    if (!theSignature.getArguments()[i].isPrimitive()) {
                        print("*");
                    }
                    printValue(theArguments.get(i));
                }
                print(")");

                theFound.set(true);
            }
        });
        if (!theFound.get()) {
            throw new IllegalArgumentException("Not supported method : " + aValue.getMethodName());
        }
    }
}