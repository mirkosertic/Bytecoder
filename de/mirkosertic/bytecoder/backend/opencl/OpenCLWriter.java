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

import de.mirkosertic.bytecoder.allocator.AbstractAllocator;
import de.mirkosertic.bytecoder.allocator.Register;
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
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.FloorExpression;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.IFElseExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeDirectMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.Label;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
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
import de.mirkosertic.bytecoder.stackifier.Block;
import de.mirkosertic.bytecoder.stackifier.Stackifier;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jocl.Pointer;
import org.jocl.Sizeof;

import static org.jocl.CL.clSetKernelArg;

public class OpenCLWriter extends IndentSSAWriter {

    private final OpenCLInputOutputs inputOutputs;
    private final BytecodeLinkedClass kernelClass;
    private final AtomicBoolean stackifierOutout;
    private final Set<Label> recordedNextLabels;
    private AbstractAllocator allocator;

    public OpenCLWriter(
            final BytecodeLinkedClass aKernelClass, final CompileOptions aOptions, final Program aProgram, final PrintWriter aWriter, final BytecodeLinkerContext aLinkerContext, final OpenCLInputOutputs aInputOutputs) {
        this(aKernelClass, aOptions, aProgram, "", aWriter, aLinkerContext, aInputOutputs, new AtomicBoolean(false), new HashSet<>(), null);
    }

    private OpenCLWriter(
            final BytecodeLinkedClass aKernelClass, final CompileOptions aOptions, final Program aProgram, final String aIndent, final PrintWriter aWriter, final BytecodeLinkerContext aLinkerContext, final OpenCLInputOutputs aInputOutputs, final AtomicBoolean aStackifierOutout, final Set<Label> aRecordedNextLabels,
            final AbstractAllocator aRegisterAllocator) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        inputOutputs = aInputOutputs;
        kernelClass = aKernelClass;
        stackifierOutout = aStackifierOutout;
        recordedNextLabels = aRecordedNextLabels;
        allocator = aRegisterAllocator;
    }

    public void printReloopedKernel(final Relooper.Block aBlock, final AbstractAllocator registerAllocator) {
        stackifierOutout.set(false);
        allocator = registerAllocator;

        print("__kernel void BytecoderKernel(");

        printInputOutputArgs(inputOutputs.arguments());

        println(") {");
        final OpenCLWriter theDeeper = withDeeperIndent();
        theDeeper.println("int $__label__ = 0;");

        printProgramVariablesDeclaration(program);

        theDeeper.print(aBlock);

        println("}");
    }

    public void printReloopedInline(final BytecodeMethod aMethod, final Program aProgram, final Relooper.Block aBlock, final AbstractAllocator registerAllocator) {

        stackifierOutout.set(false);
        allocator = registerAllocator;

        final BytecodeMethodSignature theSignature = aMethod.getSignature();

        print("__inline ");
        print(toType(TypeRef.toType(theSignature.getReturnType())));
        print(" ");
        print(aMethod.getName().stringValue());
        print("(");

        printInputOutputArgs(inputOutputs.arguments());
        boolean theFirst = inputOutputs.arguments().isEmpty();

        final List<Variable> theProgramArguments = aProgram.getArguments();
        for (int i=1;i<theProgramArguments.size();i++) {
            final Variable theVariable = theProgramArguments.get(i);
            if (theFirst) {
                theFirst = false;
            } else {
                print(", ");
            }
            print(toType(theVariable.resolveType()));
            print(" ");
            print(theVariable.getName());
        }

        println(") {");

        final OpenCLWriter theDeeper = withDeeperIndent();
        theDeeper.println("int $__label__ = 0;");

        printProgramVariablesDeclaration(program);

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
        return new OpenCLWriter(kernelClass, options, program, indent + "    ", writer, linkerContext, inputOutputs, stackifierOutout, recordedNextLabels, allocator);
    }

    private void printInstanceFieldReference(final BytecodeFieldRefConstant aField) {
        print(".");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private void writeExpression(final Expression expression) {
        if (options.isDebugOutput()) {
            final String theComment = expression.getComment();
            if (theComment != null && !theComment.isEmpty()) {
                print("// ");
                println(theComment);
            }
        }
        if (expression instanceof VariableAssignmentExpression) {
            final VariableAssignmentExpression theInit = (VariableAssignmentExpression) expression;

            final Variable theVariable = theInit.getVariable();
            final Register r = allocator.registerAssignmentFor(theVariable);
            final Value theValue = theInit.incomingDataFlows().get(0);
            print(registerName(r));
            print(" = ");
            printValue(theValue);
            println(";");
        } else if (expression instanceof ArrayStoreExpression) {
            final ArrayStoreExpression theStore = (ArrayStoreExpression) expression;
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
        } else if (expression instanceof IFExpression) {
            final IFExpression theE = (IFExpression) expression;
            print("if ");
            printValue(theE.incomingDataFlows().get(0));
            println(" {");

            withDeeperIndent().writeExpressions(theE.getExpressions());

            println("}");
        } else if (expression instanceof BreakExpression) {
            final BreakExpression theBreak = (BreakExpression) expression;
            if (theBreak.isSetLabelRequired() && !stackifierOutout.get()) {
                print("$__label__ = ");
                print(theBreak.jumpTarget().getAddress());
                println(";");
            }
            if (!theBreak.isSilent()) {
                print("goto $");
                print(theBreak.blockToBreak().name());
                println("_next;");

                recordedNextLabels.add(theBreak.blockToBreak());
            }
        } else if (expression instanceof ContinueExpression) {
            final ContinueExpression theContinue = (ContinueExpression) expression;
            if (!stackifierOutout.get()) {
                print("$__label__ = ");
                print(theContinue.jumpTarget().getAddress());
                println(";");
            }
            print("goto $");
            print(theContinue.labelToReturnTo().name());
            println(";");

        } else if (expression instanceof ReturnExpression) {
            println("return;");
        } else if (expression instanceof PutFieldExpression) {
            final PutFieldExpression thePutField = (PutFieldExpression) expression;

            final List<Value> theIncomingData = thePutField.incomingDataFlows();

            final Value theTarget = theIncomingData.get(0);
            final BytecodeFieldRefConstant theField = thePutField.getField();

            final Value thevalue = theIncomingData.get(1);
            printValue(theTarget);
            printInstanceFieldReference(theField);
            print(" = ");
            printValue(thevalue);
            println(";");
        } else if (expression instanceof ReturnValueExpression) {
            final ReturnValueExpression theReturn = (ReturnValueExpression) expression;

            final List<Value> theIncomingData = theReturn.incomingDataFlows();
            print("return ");
            printValue(theIncomingData.get(0));
            println(";");
        } else if (expression instanceof IFElseExpression) {
            final IFElseExpression theE = (IFElseExpression) expression;
            final IFExpression wrapped = theE.getCondition();
            print("if ");
            printValue(wrapped.incomingDataFlows().get(0));
            println(" {");

            withDeeperIndent().writeExpressions(wrapped.getExpressions());

            println("} else {");

            withDeeperIndent().writeExpressions(theE.getElsePart());

            println("}");
        } else {
            throw new IllegalArgumentException("Not supported. " + expression);
        }
    }

    private void writeExpressions(final ExpressionList aList) {
        for (final Expression theExpression : aList.toList()) {
            writeExpression(theExpression);
        }
    }

    private String toStructName(final BytecodeObjectTypeRef aObjectType) {
        if (kernelClass.getClassName().equals(aObjectType)) {
            // Kernel classes are mapped to type int,
            // which is not used
            return "int";
        }
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(aObjectType);
        final BytecodeClass theBytecodeClass = theLinkedClass.getBytecodeClass();
        final BytecodeAnnotation theAnnotation = theBytecodeClass.getAttributes().getAnnotationByType(OpenCLType.class.getName());
        if (theAnnotation == null) {
            throw new IllegalArgumentException("No @OpenCLType found for " + aObjectType.name());
        }
        return theAnnotation.getElementValueByName("name").stringValue();
    }

    private String toType(final TypeRef aType) {
        if (aType.isArray()) {
            final TypeRef.ArrayTypeRef theArray = (TypeRef.ArrayTypeRef) aType;
            return toType(TypeRef.toType(theArray.arrayType().getType()));
        }
        if (aType instanceof TypeRef.ObjectTypeRef) {
            final TypeRef.ObjectTypeRef theObject = (TypeRef.ObjectTypeRef) aType;
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
            case SHORT:
                return "short";
            case BYTE:
                return "byte";
            case REFERENCE:
                return "void*";
            default:
                throw new IllegalArgumentException("Not supported : " + aType.resolve());
        }
    }

    private void printValue(final Value aValue) {
        if (aValue instanceof Variable) {
            final Variable theVariable = (Variable) aValue;
            if (!theVariable.isSynthetic()) {
                final Register r = allocator.registerAssignmentFor(theVariable);
                print(registerName(r));
            } else {
                if (Variable.THISREF_NAME.equalsIgnoreCase(theVariable.getName())) {
                    print(0);
                } else {
                    print(theVariable.getName());
                }
            }
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
        } else if (aValue instanceof InvokeDirectMethodExpression) {
            printDirectInvokeMethodExpression((InvokeDirectMethodExpression) aValue);
        } else if (aValue instanceof FloorExpression) {
            printFloorExpression((FloorExpression) aValue);
        } else if (aValue instanceof PHIValue) {
            final Variable v = allocator.variableAssignmentFor((PHIValue) aValue);
            if (v.isSynthetic()) {
                print(v.getName());
            } else {
                final Register r = allocator.registerAssignmentFor(v);
                print(registerName(r));
            }
        } else {
            throw new IllegalArgumentException("Not supported : " + aValue);
        }
    }

    private void printFloorExpression(final FloorExpression e) {
        print("floor((float)(");
        final Value v = e.incomingDataFlows().get(0);
        printValue(v);
        print("))");
    }

    private void printDirectInvokeMethodExpression(final InvokeDirectMethodExpression aExpression) {
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

        final BytecodeMethodSignature theSignature = aExpression.getSignature();
        final List<Value> theMethodArguments = aExpression.incomingDataFlows();
        for (int i=1;i<theMethodArguments.size();i++) {
            final Value theValue = theMethodArguments.get(i);
            if (theFirst) {
                theFirst = false;
            } else {
                print(",");
            }
            if (!theSignature.getArguments()[i - 1].isPrimitive()) {
                // Everything except primitives is passed by reference
                print("&");
            }

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
        if (kernelClass.getClassName().equals(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()))) {
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
        printValue(theArray);
        print("[");
        printValue(theIndex);
        print("]");
    }

    private void printInvokeVirtual(final InvokeVirtualMethodExpression aExpression) {
        if (kernelClass.getClassName().equals(aExpression.getInvokedClass())) {
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

            final BytecodeMethodSignature theSignature = aExpression.getSignature();
            final List<Value> theMethodArguments = aExpression.incomingDataFlows();
            for (int i=1;i<theMethodArguments.size();i++) {
                final Value theValue = theMethodArguments.get(i);
                if (theFirst) {
                    theFirst = false;
                } else {
                    print(",");
                }
                if (!theSignature.getArguments()[i - 1].isPrimitive()) {
                    // Everything except primitives is passed by reference
                    print("&");
                }

                printValue(theValue);
            }

            print(")");
            return;
        }
        throw new IllegalArgumentException("Not supported virtual method invocation : " + aExpression.getMethodName());
    }

    private void printInvokeStatic(final InvokeStaticMethodExpression aValue) {
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(aValue.getInvokedClass());
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
                final BytecodeAnnotation.ElementValue literalValue = theAnnotation.getElementValueByName("literal");
                if (literalValue!= null && "true".equals(literalValue.stringValue())) {
                    print("(");
                    print(theMethodName);
                    print(")");
                } else {
                    print(theMethodName);
                }
                print("(");
                final List<Value> theArguments = aValue.incomingDataFlows();
                for (int i=0;i<theArguments.size();i++) {
                    if (i>0) {
                        print(",");
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

    private void printInputOutputArgs(final List<OpenCLInputOutputs.KernelArgument> arguments) {
        for (int i = 0; i<arguments.size(); i++) {
            if (i>0) {
                print(", ");
            }
            final OpenCLInputOutputs.KernelArgument theArgument = arguments.get(i);
            final TypeRef theTypeRef = TypeRef.toType(theArgument.getField().getValue().getTypeRef());
            if (theTypeRef.isObject() || theTypeRef.isArray()) {
                print("__global ");
            }
            switch (theArgument.getType()) {
                case INPUT:
                    print("const ");
                    print(toType(theTypeRef));
                    if (theTypeRef.isArray()) {
                        print("*");
                    }
                    print(" ");
                    print(theArgument.getField().getValue().getName().stringValue());
                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    print(toType(theTypeRef));
                    if (theTypeRef.isArray()) {
                        print("*");
                    }
                    print(" ");
                    print(theArgument.getField().getValue().getName().stringValue());
                    break;
            }
        }
    }

    private String registerName(final Register r) {
        if (r == null) {
            throw new IllegalStateException();
        }
        return "r" + r.getNumber();
    }

    private void printProgramVariablesDeclaration(final Program program) {
        final List<Register> theRegisters = allocator.assignedRegister();
        for (final Register r : theRegisters) {
            final TypeRef theVarType = r.getType();
            if (theVarType.isArray()) {
                print("__global ");
                print(toType(theVarType));
                print("* ");
                print(registerName(r));
                println(";");
            } else {
                print(toType(theVarType));
                print(" ");
                print(registerName(r));
                println(";");
            }
        }
    }

    public void writeStackifiedKernel(final Program program, final Stackifier stackifier, final AbstractAllocator registerAllocator) {

        stackifierOutout.set(true);
        recordedNextLabels.clear();
        allocator = registerAllocator;

        print("__kernel void BytecoderKernel(");

        printInputOutputArgs(inputOutputs.arguments());

        println(") {");
        final OpenCLWriter theDeeper = withDeeperIndent();

        theDeeper.printProgramVariablesDeclaration(program);

        final Stack<OpenCLWriter> writerStack = new Stack<>();
        final Stack<Label> labels = new Stack<>();
        writerStack.push(theDeeper);

        stackifier.writeStructuredControlFlow(new Stackifier.StackifierStructuredControlFlowWriter(stackifier) {

            @Override
            public void beginLoopFor(final Block<RegionNode> block) {
                super.beginLoopFor(block);
                final OpenCLWriter current = writerStack.peek();

                current.print("$");
                current.print(block.getLabel().name());
                current.println(":");

                writerStack.push(current.withDeeperIndent());
                labels.push(block.getLabel());
            }

            @Override
            public void beginBlockFor(final Block<RegionNode> block) {
                super.beginBlockFor(block);

                final OpenCLWriter current = writerStack.peek();

                current.print("$");
                current.print(block.getLabel().name());
                current.println(":");

                writerStack.push(current.withDeeperIndent());
                labels.push(block.getLabel());
            }

            @Override
            public void writeExpression(final RegionNode currentNode, final Expression e) {
                writerStack.peek().writeExpression(e);
            }

            @Override
            public void closeBlock() {
                writerStack.pop();
                final OpenCLWriter current = writerStack.peek();
                final Label currentLabel = labels.pop();

                if (recordedNextLabels.contains(currentLabel)) {
                    current.print("$");
                    current.print(currentLabel.name());
                    current.println("_next:");
                }

                super.closeBlock();
            }
        });

        println("}");
    }

    public void writeStackifiedInline(final BytecodeMethod method, final Program program, final Stackifier stackifier, final AbstractAllocator registerAllocator) {
        stackifierOutout.set(true);
        recordedNextLabels.clear();
        allocator = registerAllocator;

        final BytecodeMethodSignature theSignature = method.getSignature();

        print("__inline ");
        print(toType(TypeRef.toType(theSignature.getReturnType())));
        print(" ");
        print(method.getName().stringValue());
        print("(");

        printInputOutputArgs(inputOutputs.arguments());
        boolean theFirst = inputOutputs.arguments().isEmpty();

        final List<Variable> theProgramArguments = program.getArguments();
        for (int i=1;i<theProgramArguments.size();i++) {
            final Variable theVariable = theProgramArguments.get(i);
            if (theFirst) {
                theFirst = false;
            } else {
                print(", ");
            }
            print(toType(theVariable.resolveType()));
            print(" ");
            print(theVariable.getName());
        }

        println(") {");

        final OpenCLWriter theDeeper = withDeeperIndent();

        printProgramVariablesDeclaration(program);

        final Stack<OpenCLWriter> writerStack = new Stack<>();
        final Stack<Label> labels = new Stack<>();
        writerStack.push(theDeeper);

        stackifier.writeStructuredControlFlow(new Stackifier.StackifierStructuredControlFlowWriter(stackifier) {

            @Override
            public void beginLoopFor(final Block<RegionNode> block) {
                super.beginLoopFor(block);
                final OpenCLWriter current = writerStack.peek();

                current.print("$");
                current.print(block.getLabel().name());
                current.println(":");

                writerStack.push(current.withDeeperIndent());
                labels.push(block.getLabel());
            }

            @Override
            public void beginBlockFor(final Block<RegionNode> block) {
                super.beginBlockFor(block);

                final OpenCLWriter current = writerStack.peek();

                current.print("$");
                current.print(block.getLabel().name());
                current.println(":");

                writerStack.push(current.withDeeperIndent());
                labels.push(block.getLabel());
            }

            @Override
            public void writeExpression(final RegionNode currentNode, final Expression e) {
                writerStack.peek().writeExpression(e);
            }

            @Override
            public void closeBlock() {
                writerStack.pop();
                final OpenCLWriter current = writerStack.peek();
                final Label currentLabel = labels.pop();

                if (recordedNextLabels.contains(currentLabel)) {
                    current.print("$");
                    current.print(currentLabel.name());
                    current.println("_next:");
                }

                super.closeBlock();
            }
        });

        println("}");
    }
}