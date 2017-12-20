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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.classlib.java.lang.TArray;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.ssa.ArrayEntryValue;
import de.mirkosertic.bytecoder.ssa.ArrayLengthValue;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.ByteValue;
import de.mirkosertic.bytecoder.ssa.CheckCastExpression;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.CommentExpression;
import de.mirkosertic.bytecoder.ssa.CompareValue;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationReadValue;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationWriteValue;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.CurrentExceptionValue;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodValue;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.FixedBinaryValue;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.FloorValue;
import de.mirkosertic.bytecoder.ssa.GetFieldValue;
import de.mirkosertic.bytecoder.ssa.GetStaticValue;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.GraphNode;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InitVariableExpression;
import de.mirkosertic.bytecoder.ssa.InlinedNodeExpression;
import de.mirkosertic.bytecoder.ssa.InstanceOfValue;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodValue;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodValue;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.LookupSwitchExpression;
import de.mirkosertic.bytecoder.ssa.MemorySizeValue;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupValue;
import de.mirkosertic.bytecoder.ssa.MethodRefValue;
import de.mirkosertic.bytecoder.ssa.MethodTypeValue;
import de.mirkosertic.bytecoder.ssa.NegatedValue;
import de.mirkosertic.bytecoder.ssa.NewArrayValue;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayValue;
import de.mirkosertic.bytecoder.ssa.NewObjectValue;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIFunction;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectValue;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeValue;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.StackTopValue;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TableSwitchExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionValue;
import de.mirkosertic.bytecoder.ssa.TypeOfValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;

public class WASMSSAWriter extends IndentSSAWriter {

    public static final int GENERATED_INSTANCEOF_METHOD_ID = -1;

    public interface IDResolver {

        int resolveVTableMethodByType(BytecodeObjectTypeRef aObjectType);

        String resolveStringPoolFunctionName(String aValue);

        String resolveCallsiteBootstrapFor(BytecodeClass aOwningClass, String aCallsiteId, Program aProgram, GraphNode aBootstrapMethod);

        int resolveMethodIDByName(String aMethodName);

        void registerGlobalType(BytecodeMethodSignature aSignature, boolean aStatic);
    }

    private final List<Variable> stackVariables;
    private final IDResolver idResolver;

    public WASMSSAWriter(CompileOptions aOptions, Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext, IDResolver aIDResolver) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        stackVariables = new ArrayList<>();
        idResolver = aIDResolver;
        for (Variable theVariable : aProgram.getVariables()) {
            if (theVariable.resolveType().resolve() == TypeRef.Native.REFERENCE) {
                stackVariables.add(theVariable);
            }
        }
    }

    private int stackSize() {
        return stackVariables.size() * 4;
    }

    public boolean isStackVariable(Variable aVariable) {
        for (Variable theVariable : stackVariables) {
            if (theVariable.getName().equals(aVariable.getName())) {
                return true;
            }
        }
        return false;
    }

    private int stackOffsetFor(Variable aVariable) {
        int theStart = 0;
        for (Variable theVariable : stackVariables) {
            if (theVariable.getName().equals(aVariable.getName())) {
                return theStart;
            }
            theStart += 4;
        }
        throw new IllegalStateException("Unknown variable : " + aVariable);
    }

    public WASMSSAWriter withDeeperIndent() {
        return new WASMSSAWriter(options, program, indent + "    ", writer, linkerContext, idResolver);
    }

    public void writeStartNode(ControlFlowGraph.Node aNode, int aMethodId) {
        writeNode(aNode, true, aMethodId);
    }

    private void writeNode(ControlFlowGraph.Node aNode, boolean aStart, int aMethodId) {
        if (aNode instanceof ControlFlowGraph.SimpleNode) {

            if (aStart) {
                printStackEnter(aMethodId);
            }

            writeExpressionList(((ControlFlowGraph.SimpleNode) aNode).getNode().getExpressions());
            return;
        }
        if (aNode instanceof ControlFlowGraph.SequenceOfSimpleNodes) {
            ControlFlowGraph.SequenceOfSimpleNodes theSequence = (ControlFlowGraph.SequenceOfSimpleNodes) aNode;

            println("(local $currentLabel i32)");

            if (aStart) {
                printStackEnter(aMethodId);
            }

            println("(set_local $currentLabel (i32.const 0))");
            println("(loop $controlflowloop");

            WASMSSAWriter theChild = withDeeperIndent();
            for (ControlFlowGraph.SimpleNode theJumpTarget : theSequence.getNodes()) {
                theChild.print("(block $");
                theChild.print(theJumpTarget.getNode().getStartAddress().getAddress());
                theChild.println();

                WASMSSAWriter theChild2 = theChild.withDeeperIndent();
                theChild2.print("(br_if $");
                theChild2.print(theJumpTarget.getNode().getStartAddress().getAddress());
                theChild2.println();

                WASMSSAWriter theChild3 = theChild2.withDeeperIndent();
                theChild3.print("(i32.ne (get_local $currentLabel) (i32.const ");
                theChild3.print(theJumpTarget.getNode().getStartAddress().getAddress());
                theChild3.println("))");

                theChild2.println(")");

                theChild2.writeNode(theJumpTarget, false, aMethodId);

                theChild.println("(br $controlflowloop)");
                theChild.println(")");
            }

            theChild.println("(br $controlflowloop)");

            println(")");
            println("(unreachable)");
            return;
        }
        throw new IllegalStateException("Not supported!" +  aNode);
    }

    public void writeExpressionList(ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            writeExpression(theExpression);
        }
    }

    private void writeExpression(Expression aExpression) {
        if (aExpression instanceof CheckCastExpression) {
            return;
        }
        if (aExpression instanceof ReturnExpression) {
            writeReturnExpression((ReturnExpression) aExpression);
            return;
        }
        if (aExpression instanceof CommentExpression) {
            writeCommentExpression((CommentExpression) aExpression);
            return;
        }
        if (aExpression instanceof InitVariableExpression) {
            writeInitVariableExpression((InitVariableExpression) aExpression);
            return;
        }
        if (aExpression instanceof DirectInvokeMethodExpression) {
            writeDirectMethodInvokeExpression((DirectInvokeMethodExpression) aExpression);
            return;
        }
        if (aExpression instanceof IFExpression) {
            writeIFExpression((IFExpression) aExpression);
            return;
        }
        if (aExpression instanceof GotoExpression) {
            writeGotoExpression((GotoExpression) aExpression);
            return;
        }
        if (aExpression instanceof InlinedNodeExpression) {
            writeInlineExpression((InlinedNodeExpression) aExpression);
            return;
        }
        if (aExpression instanceof ReturnValueExpression) {
            writeReturnExpression((ReturnValueExpression) aExpression);
            return;
        }
        if (aExpression instanceof PutFieldExpression) {
            writePutFieldExpression((PutFieldExpression) aExpression);
            return;
        }
        if (aExpression instanceof SetMemoryLocationExpression) {
            writeSetMemoryLocationExpression((SetMemoryLocationExpression) aExpression);
            return;
        }
        if (aExpression instanceof PutStaticExpression) {
            writePutStaticExpression((PutStaticExpression) aExpression);
            return;
        }
        if (aExpression instanceof InvokeStaticMethodExpression) {
            writeInvokeStaticExpression((InvokeStaticMethodExpression) aExpression);
            return;
        }
        if (aExpression instanceof ThrowExpression) {
            writeThrowExpression((ThrowExpression) aExpression);
            return;
        }
        if (aExpression instanceof ArrayStoreExpression) {
            writeArrayStoreExpression((ArrayStoreExpression) aExpression);
            return;
        }
        if (aExpression instanceof InvokeVirtualMethodExpression) {
            writeInvokeVirtualExpression((InvokeVirtualMethodExpression) aExpression);
            return;
        }
        if (aExpression instanceof TableSwitchExpression) {
            writeTableSwitchExpression((TableSwitchExpression) aExpression);
            return;
        }
        if (aExpression instanceof LookupSwitchExpression) {
            writeLookupSwitchExpression((LookupSwitchExpression) aExpression);
            return;
        }
        if (aExpression instanceof UnreachableExpression) {
            writeUnreachable((UnreachableExpression) aExpression);
            return;
        }

        throw new IllegalStateException("Not supported : " + aExpression);
    }

    private void writeUnreachable(UnreachableExpression aExpression) {
        println("(unreachable)");
    }

    private void writeLookupSwitchExpression(LookupSwitchExpression aExpression) {
        println("(block $outer");

        Value theValue = aExpression.getValue();

        WASMSSAWriter theChild2 = withDeeperIndent();

        // For each statement
        for (Map.Entry<Long, ExpressionList> theEntry : aExpression.getPairs().entrySet()) {
            theChild2.print ("(block $switch_");
            theChild2.print(theEntry.getKey());
            theChild2.println();

            WASMSSAWriter theChild3 = theChild2.withDeeperIndent();
            theChild3.print("(br_if $switch_");
            theChild3.print(theEntry.getKey());
            theChild3.print(" (i32.ne (i32.const ");
            theChild3.print(theEntry.getKey());
            theChild3.print(") ");

            theChild3.writeValue(theValue);
            theChild3.print("))");
            theChild3.println();

            theChild3.writeExpressionList(theEntry.getValue());
            theChild3.println();

            theChild3.writeExpressionList(theEntry.getValue());

            theChild3.println("(br $outer)");

            theChild2.println(")");
        }

        writeExpressionList(aExpression.getDefaultExpressions());
        println(")");
    }

    private void writeTableSwitchExpression(TableSwitchExpression aExpression) {
        println("(block $tableswitch");

        Value theValue = aExpression.getValue();
        WASMSSAWriter theChild1 = withDeeperIndent();

        theChild1.println("(block $label$0");

        WASMSSAWriter theChild2 = theChild1.withDeeperIndent();
        theChild2.println("(block $label$1");

        WASMSSAWriter theChild3 = theChild2.withDeeperIndent();
        theChild3.print("(br_if $label$1 (i32.lt_s ");
        theChild3.writeValue(theValue);
        theChild3.print(" (i32.const ");
        theChild3.print(aExpression.getLowValue());
        theChild3.println(")))");

        theChild3.print("(br_if $label$0 (i32.le_s ");
        theChild3.writeValue(theValue);
        theChild3.print(" (i32.const ");
        theChild3.print(aExpression.getHighValue());
        theChild3.println(")))");

        theChild3.writeExpressionList(aExpression.getDefaultExpressions());
        theChild3.println();

        theChild3.println("(br $tableswitch)");

        theChild2.println(")");

        theChild1.println(")");

        WASMSSAWriter theChild4 = withDeeperIndent();
        // For each statement
        for (Map.Entry<Long, ExpressionList> theEntry : aExpression.getOffsets().entrySet()) {
            theChild4.print ("(block $switch_");
            theChild4.print(theEntry.getKey());
            theChild4.println();

            WASMSSAWriter theChild5 = theChild4.withDeeperIndent();
            theChild5.print("(br_if $switch_");
            theChild5.print(theEntry.getKey());
            theChild5.print(" (i32.ne (i32.const ");
            theChild5.print(theEntry.getKey());
            theChild5.print(") (i32.sub ");

            theChild5.writeValue(theValue);
            theChild5.print(" (i32.const ");
            theChild5.print(aExpression.getLowValue());
            theChild5.print("))))");
            theChild5.println();

            theChild5.writeExpressionList(theEntry.getValue());
            theChild5.println();

            theChild4.println(")");
        }

        println(")");
        println("(unreachable)");
    }

    private void writeInvokeVirtualExpression(InvokeVirtualMethodExpression aExpression) {
        writeInvokeVirtualValue(aExpression.getValue());
        println();
    }

    private void writeArrayStoreExpression(ArrayStoreExpression aExpression) {
        switch (aExpression.getArrayType().resolve()) {
            case DOUBLE:
            case FLOAT: {
                println("(f32.store offset=20 ");
                break;
            }
            default: {
                println("(i32.store offset=20 ");
                break;
            }
        }

        WASMSSAWriter theChild = withDeeperIndent();

        theChild.print("(i32.add ");
        theChild.writeValue(aExpression.getArray());
        theChild.print(" (i32.mul ");
        theChild.writeValue(aExpression.getIndex());
        theChild.print(" (i32.const 4)");
        theChild.println("))");

        theChild.writeValue(aExpression.getValue());
        theChild.println();

        println(")");
    }

    private void writeThrowExpression(ThrowExpression aExpression) {
        printStackExit();
        println("(unreachable)");
    }

    private void writeInvokeStaticExpression(InvokeStaticMethodExpression aExpression) {
        writeValue(aExpression.getValue());
        println();
    }

    private void writePutStaticExpression(PutStaticExpression aExpression) {

        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(aExpression.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = WASMWriterUtils.computeStaticFieldOffsetOf(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                theLinkedClass);

        String theClassName = WASMWriterUtils.toClassName(aExpression.getField().getClassIndex().getClassConstant());
        switch (aExpression.getValue().resolveType().resolve()) {
            case DOUBLE:
            case FLOAT: {
                print("(f32.store offset=");
                break;
            }
            default: {
                print("(i32.store offset=");
                break;
            }

        }
        print(theMemoryOffset);
        println();

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.print("(get_global $");
        theChild.print(theClassName);
        theChild.println("__runtimeClass)");

        theChild.writeValue(aExpression.getValue());

        println(")");
    }

    private void writeSetMemoryLocationExpression(SetMemoryLocationExpression aExpression) {
        println("(i32.store");

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.writeValue(aExpression.getAddress());
        theChild.println();
        theChild.writeValue(aExpression.getValue());
        theChild.println();
        println(")");
    }

    private void writePutFieldExpression(PutFieldExpression aExpression) {
        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(aExpression.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = WASMWriterUtils.computeFieldOffsetOf(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                theLinkedClass);

        BytecodeLinkedClass.LinkedField theField = theLinkedClass.memberFieldByName(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        switch (TypeRef.toType(theField.getField().getTypeRef()).resolve()) {
            case DOUBLE:
            case FLOAT:
                print("(f32.store offset=");
                break;
            default:
                print("(i32.store offset=");
                break;
        }
        print(theMemoryOffset);
        println();

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.writeValue(aExpression.getTarget());
        theChild.writeValue(aExpression.getValue());

        println(")");
    }

    private void writeInlineExpression(InlinedNodeExpression aExpression) {
        writeExpressionList(aExpression.getNode().getExpressions());
    }

    private void writeGotoExpression(GotoExpression aExpression) {
        print("(set_local $currentLabel (i32.const ");
        print(aExpression.getJumpTarget().getAddress());
        println("))");
        println("(br $controlflowloop)");
    }

    private void writeIFExpression(IFExpression aExpression) {
        print("(block $");
        print(aExpression.getAddress().getAddress());
        println();

        WASMSSAWriter theChild = withDeeperIndent();

        theChild.print("(br_if $");
        theChild.print(aExpression.getAddress().getAddress());
        theChild.println();

        WASMSSAWriter theChild3 = theChild.withDeeperIndent();
        theChild3.print("(i32.eq ");
        theChild3.writeValue(aExpression.getBooleanValue());
        theChild3.print(" (i32.const 0)");
        theChild3.println(")");

        theChild.println(")");

        theChild.writeExpressionList(aExpression.getExpressions());

        println(")");
    }

    private void writeDirectMethodInvokeExpression(DirectInvokeMethodExpression aExpression) {
        writeValue(aExpression.getValue());
        println();
    }

    private void writeInitVariableExpression(InitVariableExpression aExpression) {
        Variable theVariable = aExpression.getVariable();
        Value theNewValue = aExpression.getValue();

        if (theNewValue instanceof PHIFunction) {
            return;
        }

        if (isStackVariable(theVariable)) {
            switch (theVariable.resolveType().resolve()) {
                case DOUBLE:
                case FLOAT: {
                    print("(f32.store offset=");
                    break;
                }
                case UNKNOWN:
                    throw new IllegalStateException();
                default: {
                    print("(i32.store offset=");
                    break;
                }
            }
            print(stackOffsetFor(theVariable));
            println(" (get_local $SP)");

            WASMSSAWriter theChild = withDeeperIndent();
            theChild.writeValue(theNewValue);

            println();
            println(")");

        } else {
            println(";; setting local variable with type " + theVariable.resolveType().resolve() + " with value of type " + theNewValue.resolveType().resolve());
            print("(set_local $");
            print(theVariable.getName());
            println();

            WASMSSAWriter theChild = withDeeperIndent();
            theChild.writeValue(theNewValue);

            println();
            println(")");
        }
    }

    private void writeValue(Value aValue) {
        if (aValue instanceof Variable) {
            printVariableName((Variable) aValue);
            return;
        }
        if (aValue instanceof BinaryValue) {
            writeBinaryValue((BinaryValue) aValue);
            return;
        }
        if (aValue instanceof ByteValue) {
            writeByteValue((ByteValue) aValue);
            return;
        }
        if (aValue instanceof IntegerValue) {
            writeIntegerValue((IntegerValue) aValue);
            return;
        }
        if (aValue instanceof DirectInvokeMethodValue) {
            writeDirectMethodInvokeValue((DirectInvokeMethodValue) aValue);
            return;
        }
        if (aValue instanceof InvokeStaticMethodValue) {
            writeInvokeStaticValue((InvokeStaticMethodValue) aValue);
            return;
        }
        if (aValue instanceof GetFieldValue) {
            writeGetFieldValue((GetFieldValue) aValue);
            return;
        }
        if (aValue instanceof NewObjectValue) {
            writeNewObjectValue((NewObjectValue) aValue);
            return;
        }
        if (aValue instanceof GetStaticValue) {
            writeGetStaticValue((GetStaticValue) aValue);
            return;
        }
        if (aValue instanceof LongValue) {
            writeLongValue((LongValue) aValue);
            return;
        }
        if (aValue instanceof FixedBinaryValue) {
            writeFixedBinaryValue((FixedBinaryValue) aValue);
            return;
        }
        if (aValue instanceof ComputedMemoryLocationReadValue) {
            writeComputedMemoryLocationValue((ComputedMemoryLocationReadValue) aValue);
            return;
        }
        if (aValue instanceof ComputedMemoryLocationWriteValue) {
            writeComputedMemoryLocationValue((ComputedMemoryLocationWriteValue) aValue);
            return;
        }
        if (aValue instanceof TypeConversionValue) {
            writeTypeConversion((TypeConversionValue) aValue);
            return;
        }
        if (aValue instanceof NullValue) {
            writeNullValue((NullValue) aValue);
            return;
        }
        if (aValue instanceof StackTopValue) {
            writeStackTopValue((StackTopValue) aValue);
            return;
        }
        if (aValue instanceof MemorySizeValue) {
            writrMemorySizeValue((MemorySizeValue) aValue);
            return;
        }
        if (aValue instanceof ShortValue) {
            writeShortValue((ShortValue) aValue);
            return;
        }
        if (aValue instanceof FloatValue) {
            writeFloatValue((FloatValue) aValue);
            return;
        }
        if (aValue instanceof InvokeVirtualMethodValue) {
            writeInvokeVirtualValue((InvokeVirtualMethodValue) aValue);
            return;
        }
        if (aValue instanceof FloorValue) {
            writeFloorValue((FloorValue) aValue);
            return;
        }
        if (aValue instanceof NewArrayValue) {
            writeNewArrayValue((NewArrayValue) aValue);
            return;
        }
        if (aValue instanceof ArrayLengthValue) {
            writeArrayLengthValue((ArrayLengthValue) aValue);
            return;
        }
        if (aValue instanceof StringValue) {
            writeStringValue((StringValue) aValue);
            return;
        }
        if (aValue instanceof ArrayEntryValue) {
            writeArrayEntryValue((ArrayEntryValue) aValue);
            return;
        }
        if (aValue instanceof CompareValue) {
            writeCompareValue((CompareValue) aValue);
            return;
        }
        if (aValue instanceof NegatedValue) {
            writeNegateValue((NegatedValue) aValue);
            return;
        }
        if (aValue instanceof InstanceOfValue) {
            writeInstanceOfValue((InstanceOfValue) aValue);
            return;
        }
        if (aValue instanceof DoubleValue) {
            writeDoubleValue((DoubleValue) aValue);
            return;
        }
        if (aValue instanceof ResolveCallsiteObjectValue) {
            writeResolveCallSiteObjectValue((ResolveCallsiteObjectValue) aValue);
            return;
        }
        if (aValue instanceof MethodHandlesGeneratedLookupValue) {
            writeMethodHandlesGeneratedLookupValue((MethodHandlesGeneratedLookupValue) aValue);
            return;
        }
        if (aValue instanceof MethodTypeValue) {
            writeMethodTypeValue((MethodTypeValue) aValue);
            return;
        }
        if (aValue instanceof CurrentExceptionValue) {
            writeCurrentException((CurrentExceptionValue) aValue);
            return;
        }
        if (aValue instanceof ClassReferenceValue) {
            writeClassReferenceValue((ClassReferenceValue) aValue);
            return;
        }
        if (aValue instanceof TypeOfValue) {
            writeTypeOfValue((TypeOfValue) aValue);
            return;
        }
        if (aValue instanceof RuntimeGeneratedTypeValue) {
            writeRuntimeGeneratedTypeValue((RuntimeGeneratedTypeValue) aValue);
            return;
        }
        if (aValue instanceof MethodRefValue) {
            writeMethodRefValue((MethodRefValue) aValue);
            return;
        }
        if (aValue instanceof NewMultiArrayValue) {
            writeNewMultiArrayValue((NewMultiArrayValue) aValue);
            return;
        }
        throw new IllegalStateException("Not supported : " + aValue);
    }

    private void writeNewMultiArrayValue(NewMultiArrayValue aValue) {

        List<Value> theDimensions = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);

        BytecodeTypeRef theType = aValue.getType();
        String theMethodName;
        switch (theDimensions.size()) {
            case 1:
                theMethodName = WASMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "newArray",
                        new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
                break;
            case 2:
                theMethodName = WASMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "newArray",
                        new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
                break;
            default:
                throw new IllegalStateException("Unsupported number of dimensions : " + theDimensions.size());
        }

        print("(call $");
        print(theMethodName);
        print(" (i32.const 0) "); // UNUSED argument

        for (Value theDimension : theDimensions) {
            print(" ");
            writeValue(theDimension);
        }

        // We also need the runtime class
        print(" (get_global $TArray__runtimeClass)");
        // Plus the vtable index
        print(" (i32.const ");
        print(idResolver.resolveVTableMethodByType(BytecodeObjectTypeRef.fromRuntimeClass(TArray.class)));
        print(")");

        println(") ;; new array of type " + theType);
    }

    private void writeMethodRefValue(MethodRefValue aValue) {
        print("(i32.const ");
        
        String theMethodName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromUtf8Constant(aValue.getMethodRef().getClassIndex().getClassConstant().getConstant()),
                aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());
        
        print(idResolver.resolveMethodIDByName(theMethodName));
        
        print(")");
    }

    private void writeRuntimeGeneratedTypeValue(RuntimeGeneratedTypeValue aValue) {
        println("(call $newLambda ");
        WASMSSAWriter theChild = withDeeperIndent();
        theChild.writeValue(aValue.getType());
        theChild.println();
        theChild.writeValue(aValue.getMethodRef());
        theChild.println();
        println(")");
    }

    private void writeTypeOfValue(TypeOfValue aValue) {
        print("(i32.load ");
        writeValue(aValue.resolveFirstArgument());
        print(")");
    }

    private void writeClassReferenceValue(ClassReferenceValue aValue) {
        print("(get_global $");
        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(aValue.getType());
        print(WASMWriterUtils.toClassName(theLinkedClass.getClassName()));
        print("__runtimeClass)");
    }

    private void writeCurrentException(CurrentExceptionValue aValue) {
        print("(i32.const 0)");
    }

    private void writeMethodTypeValue(MethodTypeValue aValue) {
//        print("(i32.const ");
//        print(idResolver.resolveTypeIDForSignature(aValue.getSignature()));
//        print(")");
        println();
        println(";; " + WASMWriterUtils.toMethodSignature(aValue.getSignature(), false));
        print("(i32.const 0)");
    }

    private void writeMethodHandlesGeneratedLookupValue(MethodHandlesGeneratedLookupValue aValue) {
        print("(i32.const 0)");
    }

    private void writeResolveCallSiteObjectValue(ResolveCallsiteObjectValue aValue) {
        print("(call $");
        print(idResolver.resolveCallsiteBootstrapFor(
            aValue.getOwningClass(),
            aValue.getCallsiteId(),
            aValue.getProgram(),
            aValue.getBootstrapMethod()
        ));
        print(")");
    }

    private void writeDoubleValue(DoubleValue aValue) {
        print("(f32.const ");
        print(aValue.getDoubleValue());
        print(")");
    }

    private void writeInstanceOfValue(InstanceOfValue aValue) {

        BytecodeLinkedClass theClass = linkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getType().getConstant()));

        print("(call $INSTANCEOF_CHECK ");
        writeValue(aValue.resolveFirstArgument());
        print(" (i32.const ");
        print(theClass.getUniqueId());
        println("))");
    }

    private void writeNegateValue(NegatedValue aValue) {
        Value theValue = aValue.resolveFirstArgument();
        switch (theValue.resolveType().resolve()) {
            case DOUBLE:
            case FLOAT: {
                    print("(f32.neg ");
                    writeValue(theValue);
                    print(")");
                }
                break;
            default:
                print("(i32.mul (i32.const -1) ");
                writeValue(theValue);
                print(")");
                break;
        }
    }

    private void writeCompareValue(CompareValue aValue) {
        Value theValue1 = aValue.resolveFirstArgument();
        Value theValue2 = aValue.resolveSecondArgument();

        TypeRef.Native theValue1Type = theValue1.resolveType().resolve();
        TypeRef.Native theValue2Type = theValue2.resolveType().resolve();
        if (theValue1Type != theValue2Type) {
            throw new IllegalStateException("Does not support mixed types : " + theValue1Type + " -> " + theValue2Type);
        }

        switch (theValue1Type) {
            case DOUBLE:
            case FLOAT:
                print("(call $compareValueF32 ");
                break;
            default:
                print("(call $compareValueI32 ");
                break;
        }
        writeValue(theValue1);
        print(" ");
        writeValue(theValue2);
        print(")");
    }

    private void writeArrayEntryValue(ArrayEntryValue aValue) {
        switch (aValue.resolveType().resolve()) {
            case DOUBLE:
            case FLOAT: {
                print("(f32.load offset=20 ");
                break;
            }
            default: {
                print("(i32.load offset=20 ");
                break;
            }
        }

        print("(i32.add ");
        writeValue(aValue.resolveFirstArgument());
        print(" (i32.mul ");
        writeValue(aValue.resolveSecondArgument());
        print(" (i32.const 4)");
        println("))");

        println(")");
    }

    private void writeStringValue(StringValue aValue) {
        print("(get_global $");
        print(idResolver.resolveStringPoolFunctionName(aValue.getStringValue()));
        print(")");
    }

    private void writeNewArrayValue(NewArrayValue aValue) {

        BytecodeTypeRef theType = aValue.getType();

        String theMethodName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "newArray",
                new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                        Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        print("(call $");
        print(theMethodName);
        print(" (i32.const 0) "); // UNUSED argument

        withDeeperIndent().writeValue(aValue.resolveFirstArgument());

        // We also need the runtime class
        print(" (get_global $TArray__runtimeClass)");
        // Plus the vtable index
        print(" (i32.const ");
        print(idResolver.resolveVTableMethodByType(BytecodeObjectTypeRef.fromRuntimeClass(TArray.class)));
        print(")");

        println(") ;; new array of type " + theType);
    }

    private void writeArrayLengthValue(ArrayLengthValue aValue) {
        println("(i32.load offset=16 ");
        withDeeperIndent().writeValue(aValue.resolveFirstArgument());
        println();
        println(")");
    }

    private void writeFloorValue(FloorValue aValue) {
        println("(i32.trunc_s/f32 (f32.floor ");
        withDeeperIndent().writeValue(aValue.resolveFirstArgument());
        println("))");
    }

    private void writeInvokeVirtualValue(InvokeVirtualMethodValue aValue) {

        idResolver.registerGlobalType(aValue.getSignature(), false);

        print("(call_indirect $t_");
        print(WASMWriterUtils.toMethodSignature(aValue.getSignature(), false));
        println();

        Value theTarget = aValue.consumedValues(Value.ConsumptionType.INVOCATIONTARGET).get(0);
        List<Value> theVariables = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);

        writeValue(theTarget);
        println();

        for (Value theValue : theVariables) {
            writeValue(theValue);
            println();
        }


        WASMSSAWriter theChild = withDeeperIndent();
        theChild.println("(call_indirect $t_RESOLVEMETHOD");

        WASMSSAWriter theChild2 = theChild.withDeeperIndent();

        theChild2.writeValue(theTarget);
        theChild2.println();

        // This is the method number
        theChild2.print("(i32.const ");

        BytecodeVirtualMethodIdentifier theMethodIdentifier = linkerContext.getMethodCollection().identifierFor(aValue.getMethodName(), aValue.getSignature());
        theChild2.print(theMethodIdentifier.getIdentifier());

        theChild2.println(")");

        // And this is the pointer into the function table
        theChild2.print("(i32.load offset=4 ");
        theChild2.writeValue(theTarget);
        theChild2.println(")");   // This is id of the virtual table method

        theChild.println(")");

        println(")");

    }

    private void writeFloatValue(FloatValue aValue) {
        if (aValue.getFloatValue() == Float.POSITIVE_INFINITY) {
            print("(f32.const 99999999999999999999999999999.99)");
        } else if (aValue.getFloatValue() == Float.NEGATIVE_INFINITY) {
            print("(f32.const -99999999999999999999999999999.99)");
        } else {
            print("(f32.const ");
            print(aValue.getFloatValue());
            print(")");
        }
    }

    private void writeShortValue(ShortValue aValue) {
        print("(i32.const ");
        print(aValue.getShortValue());
        print(")");
    }

    private void writeStackTopValue(StackTopValue aValue) {
        print("(get_global $STACKTOP)");
    }

    private void writrMemorySizeValue(MemorySizeValue aValue) {
        print("(i32.mul (current_memory) (i32.const 65536))");
    }

    private void writeNullValue(NullValue aValue) {
        print("(i32.const 0)");
    }

    private void writeTypeConversion(TypeConversionValue aValue) {
        TypeRef theTargetType = aValue.resolveType();
        Value theSource = aValue.resolveFirstArgument();
        if (theTargetType.resolve().equals(theSource.resolveType().resolve())) {
            // No conversion needed!
            writeValue(theSource);
            return;
        }
        switch (theSource.resolveType().resolve()) {
            case DOUBLE:
            case FLOAT: {
                // Convert floating point to something else
                switch (aValue.resolveType().resolve()) {
                    case DOUBLE:
                    case FLOAT: {
                        // No conversion needed
                        writeValue(theSource);
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
                    case LONG:
                    case CHAR: {
                        // Convert f32 to i32
                        print("(i32.trunc_s/f32 ");
                        writeValue(theSource);
                        print(")");
                        return;
                    }
                    default:
                        throw new IllegalStateException("target type " + aValue.resolveType() + " not supported!");
                }
            }
            case INT:
            case LONG:
            case BYTE:
            case SHORT:
            case CHAR: {
                // Convert integer type to something else
                // Convert floating point to something else
                switch (aValue.resolveType().resolve()) {
                case DOUBLE:
                case FLOAT: {
                    // Convert i32 to f32
                    print("(f32.convert_s/i32 ");
                    writeValue(theSource);
                    print(")");
                    return;
                }
                case INT:
                case SHORT:
                case BYTE:
                case LONG:
                case CHAR: {
                    // No conversion needed
                    writeValue(theSource);
                    return;
                }
                default:
                    throw new IllegalStateException("target type " + aValue.resolveType() + " not supported!");
                }
            }
            default:
                throw new IllegalStateException("Conversion of " + theSource.resolveType() + " not supported!");
        }
    }

    private void writeComputedMemoryLocationValue(ComputedMemoryLocationWriteValue aValue) {
        println("(i32.add ");

        withDeeperIndent().writeValue(aValue.resolveFirstArgument());
        println();
        withDeeperIndent().writeValue(aValue.resolveSecondArgument());
        println();

        println(")");
    }

    private void writeComputedMemoryLocationValue(ComputedMemoryLocationReadValue aValue) {
        println("(i32.load (i32.add ");

        withDeeperIndent().writeValue(aValue.resolveFirstArgument());
        println();
        withDeeperIndent().writeValue(aValue.resolveSecondArgument());
        println();

        println("))");
    }

    private void writeFixedBinaryValue(FixedBinaryValue aValue) {
        switch (aValue.getOperator()) {
            case ISNULL: {
                print("(i32.eq ");
                printVariableName(aValue.resolveFirstArgument());
                print(" (i32.const 0))");
                break;
            }
            case ISNONNULL: {
                print("(i32.ne ");
                printVariableName(aValue.resolveFirstArgument());
                print(" (i32.const 0))");
                break;
            }
            case ISZERO: {
                print("(i32.eq ");
                printVariableName(aValue.resolveFirstArgument());
                print(" (i32.const 0))");
                break;
            }
            default: {
                throw new IllegalStateException("Not supported");
            }
        }
    }

    private void writeLongValue(LongValue aValue) {
        print("(i32.const ");
        print(aValue.getLongValue());
        print(")");
    }

    private void writeGetStaticValue(GetStaticValue aValue) {
        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = WASMWriterUtils.computeStaticFieldOffsetOf(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                theLinkedClass);

        BytecodeLinkedClass.LinkedField theField = theLinkedClass.staticFieldByName(
                aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue()
        );

        String theClassName = WASMWriterUtils.toClassName(aValue.getField().getClassIndex().getClassConstant());
        switch (TypeRef.toType(theField.getField().getTypeRef()).resolve()) {
            case DOUBLE:
            case FLOAT: {
                print("(f32.load offset=");
                break;
            }
            default: {
                print("(i32.load offset=");
                break;
            }
        }
        print(theMemoryOffset);
        println();

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.print("(get_global $");
        theChild.print(theClassName);
        theChild.println("__runtimeClass)");

        println(")");
    }

    private void writeNewObjectValue(NewObjectValue aValue) {

        BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(aValue.getType().getConstant());

        String theMethodName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "newObject",
                new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                        Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(theType);
        print("(call $");
        print(theMethodName);
        print(" (i32.const 0) "); // UNUSED argument
        print(" (i32.const ");
        print(WASMWriterUtils.computeObjectSizeFor(theLinkedClass));
        print(") (get_global $");
        print(WASMWriterUtils.toClassName(theLinkedClass.getClassName()));
        print("__runtimeClass) (i32.const ");
        print(idResolver.resolveVTableMethodByType(theType));
        print(")) ;; object of type " + aValue.getType().getConstant().stringValue());
    }

    private void writeGetFieldValue(GetFieldValue aValue) {
        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = WASMWriterUtils.computeFieldOffsetOf(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                theLinkedClass);

        BytecodeLinkedClass.LinkedField theField = theLinkedClass.memberFieldByName(
                aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue()
        );

        switch (TypeRef.toType(theField.getField().getTypeRef()).resolve()) {
            case DOUBLE:
            case FLOAT:
                print("(f32.load offset=");
                break;
            default:
                print("(i32.load offset=");
                break;
        }
        print(theMemoryOffset);
        println();

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.printVariableName(aValue.resolveFirstArgument());

        println(")");
    }

    private void writeDirectMethodInvokeValue(DirectInvokeMethodValue aValue) {

        Value theTarget = aValue.consumedValues(Value.ConsumptionType.INVOCATIONTARGET).get(0);
        List<Value> theValues = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);

        print("(call $");
        print(WASMWriterUtils.toMethodName(aValue.getClazz(), aValue.getMethodName(), aValue.getSignature()));

        print(" ");
        writeValue(theTarget);

        for (Value theValue : theValues) {
            print(" ");
            writeValue(theValue);
        }

        print(")");
    }

    private void writeInvokeStaticValue(InvokeStaticMethodValue aValue) {
        print("(call $");
        print(WASMWriterUtils.toMethodName(aValue.getClassName(), aValue.getMethodName(), aValue.getSignature()));

        print(" (i32.const 0)"); // UNUSED Argument

        List<Value> theArguments = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);
        for (Value theValue : theArguments) {
            print(" ");
            writeValue(theValue);
        }

        print(")");
    }

    private void writeByteValue(ByteValue aValue) {
        print("(i32.const ");
        print(aValue.getByteValue());
        print(")");
    }

    private void writeIntegerValue(IntegerValue aValue) {
        print("(i32.const ");
        print(aValue.getIntValue());
        print(")");
    }

    private void writeBinaryValue(BinaryValue aValue) {
        Value theValue1 = aValue.resolveFirstArgument();
        Value theValue2 = aValue.resolveSecondArgument();
        String theType1 = WASMWriterUtils.toType(theValue1.resolveType());
        String theType2 = WASMWriterUtils.toType(theValue2.resolveType());
        switch (aValue.getOperator()) {
            case NOTEQUALS: {
                println("(" + theType1 + ".ne ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case EQUALS: {
                println("(" + theType1 + ".eq ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case LESSTHAN: {
                println("(" + theType1 + ".lt_s ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();
                println(")");
                break;
            }
            case LESSTHANOREQUALS: {
                if ("i32".equals(theType1)) {
                    println("(" + theType1 + ".le_s ");
                } else {
                    println("(" + theType1 + ".le ");
                }

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();
                println(")");
                break;
            }
            case GREATEROREQUALS: {
                if ("i32".equals(theType1)) {
                    println("(" + theType1 + ".ge_s ");
                } else {
                    println("(" + theType1 + ".ge ");
                }

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();
                println(")");
                break;
            }
            case GREATERTHAN: {
                if ("i32".equals(theType1)) {
                    println("(" + theType1 + ".gt_s ");
                } else {
                    println("(" + theType1 + ".gt ");
                }

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();
                println(")");
                break;
            }
            case ADD: {
                println("(" + theType1 + ".add ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case MUL: {
                println("(" + theType1 + ".mul");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case DIV: {
                println("(f32.div ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValueAsFloat(theValue1);
                theChild.println();
                theChild.printVariableNameOrValueAsFloat(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case REMAINDER: {
                if (theValue1.resolveType().resolve() == TypeRef.Native.INT) {
                    println("(i32.rem_s ");

                    WASMSSAWriter theChild = withDeeperIndent();
                    theChild.writeValue(theValue1);
                    theChild.println();
                    theChild.writeValue(theValue2);
                    theChild.println();

                    println(")");
                    break;
                }
                print("(call $float_remainder ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                print(")");
                break;
            }
            case SUB: {
                println("(" + theType1 + ".sub ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case BINARYXOR: {
                println("(" + theType1 + ".xor ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case BINARYOR: {
                println("(" + theType1 + ".or ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case BINARYAND: {
                println("(" + theType1 + ".and ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case BINARYSHIFTLEFT: {
                println("(" + theType1 + ".shl ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case BINARYSHIFTRIGHT: {
                println("(" + theType1 + ".shr_s ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            case BINARYUNSIGNEDSHIFTRIGHT: {
                println("(" + theType1 + ".shr_u ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.writeValue(theValue1);
                theChild.println();
                theChild.writeValue(theValue2);
                theChild.println();

                println(")");
                break;
            }
            default:
                throw new IllegalStateException("Operator not supported : " + aValue.getOperator());
        }
    }

    private void writeCommentExpression(CommentExpression aExpression) {
        if (options.isDebugOutput()) {
            print(";; ");
            println(aExpression.getValue());
        }
    }

    private void writeReturnExpression(ReturnExpression aExpression) {
        printStackExit();
        println("(return)");
    }

    private void writeReturnExpression(ReturnValueExpression aExpression) {
        printStackExit();
        print("(return ");
        writeValue(aExpression.getValue());
        writeValue(aExpression.getValue());
        println(")");
    }

    private void printVariableNameOrValueAsFloat(Value aValue) {
        switch (aValue.resolveType().resolve()) {
            case DOUBLE:
            case FLOAT:
                writeValue(aValue);
                break;
            default:
                print("(f32.convert_s/i32 ");
                writeValue(aValue);
                print(")");
                break;
        }
    }

    private void printVariableName(Variable aVariable) {
        if (isStackVariable(aVariable)) {
            switch (aVariable.resolveType().resolve()) {
                case DOUBLE:
                case FLOAT:
                    print("(f32.load offset=");
                    break;
                case UNKNOWN:
                    throw new IllegalStateException();
                default:
                    print("(i32.load offset=");
                    break;
            }
            print(stackOffsetFor(aVariable));
            print(" (get_local $SP)");
            print(")");
        } else {
            print("(get_local ");
            print("$");
            print(aVariable.getName());
            print(")");
        }
    }

    public void printStackEnter(int aMethodId) {

        int theStackSize = stackSize();
        if (theStackSize > 0) {
            println("(local $SP i32)");
            println("(local $OLD_SP i32)");
            println("(local $OLD_STACKNEST i32)");
            println("(set_local $OLD_SP (get_global $STACKTOP))");
            println("(set_local $OLD_STACKNEST (get_global $STACKNEST))");
            print("(set_global $STACKNEST (i32.add (get_global $STACKNEST) (i32.const 1)))");
            print("(set_global $STACKTOP (i32.sub (get_global $STACKTOP) (i32.const ");
            print(theStackSize);
            println(")))");
            println("(set_local $SP (get_global $STACKTOP))");
            println("(call $trace (get_local $SP) (i32.const " + aMethodId + "))");
            println("(block $check (br_if $check (i32.lt_s (get_global $STACKNEST) (i32.const 100))) (unreachable))");
        }
    }

    private void printStackExit() {

        int theStackSize = stackSize();
        if (theStackSize > 0) {
            println("(set_global $STACKTOP (get_local $OLD_SP))");
            println("(set_global $STACKNEST (get_local $OLD_STACKNEST))");
        }
    }
}