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

import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
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
import de.mirkosertic.bytecoder.ssa.MemorySizeValue;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupValue;
import de.mirkosertic.bytecoder.ssa.MethodTypeValue;
import de.mirkosertic.bytecoder.ssa.NegatedValue;
import de.mirkosertic.bytecoder.ssa.NewArrayValue;
import de.mirkosertic.bytecoder.ssa.NewObjectValue;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIFunction;
import de.mirkosertic.bytecoder.ssa.PrimitiveValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectValue;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnVariableExpression;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.StackTopValue;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.Type;
import de.mirkosertic.bytecoder.ssa.TypeConversionValue;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableReferenceValue;

public class WASMSSAWriter extends IndentSSAWriter {

    public interface IDResolver {

        int resolveVTableMethodByType(BytecodeObjectTypeRef aObjectType);

        String resolveStringPoolFunctionName(String aValue);

        String resolveCallsiteBootstrapFor(BytecodeClass aOwningClass, String aCallsiteId, Program aProgram, GraphNode aBootstrapMethod);
    }

    private final List<Variable> stackVariables;
    private final IDResolver idResolver;

    public WASMSSAWriter(Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext, IDResolver aIDResolver) {
        super(aProgram, aIndent, aWriter, aLinkerContext);
        stackVariables = new ArrayList<>();
        idResolver = aIDResolver;
        for (Variable theVariable : aProgram.getVariables()) {
            if (theVariable.getType() == Type.REFERENCE) {
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
        return new WASMSSAWriter(program, indent + "    ", writer, linkerContext, idResolver);
    }

    public void writeStartNode(ControlFlowGraph.Node aNode) {
        writeNode(aNode, true);
    }

    private void writeNode(ControlFlowGraph.Node aNode, boolean aStart) {
        if (aNode instanceof ControlFlowGraph.SimpleNode) {

            if (aStart) {
                printStackEnter();
            }

            writeExpressionList(((ControlFlowGraph.SimpleNode) aNode).getNode().getExpressions());
            return;
        }
        if (aNode instanceof ControlFlowGraph.SequenceOfSimpleNodes) {
            ControlFlowGraph.SequenceOfSimpleNodes theSequence = (ControlFlowGraph.SequenceOfSimpleNodes) aNode;

            println("(local $currentLabel i32)");

            if (aStart) {
                printStackEnter();
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

                theChild2.writeNode(theJumpTarget, false);

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
        if (aExpression instanceof ReturnVariableExpression) {
            writeReturnExpression((ReturnVariableExpression) aExpression);
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
        throw new IllegalStateException("Not supported : " + aExpression);
    }

    private void writeInvokeVirtualExpression(InvokeVirtualMethodExpression aExpression) {
        writeInvokeVirtualValue(aExpression.getValue());
        println();
    }

    private void writeArrayStoreExpression(ArrayStoreExpression aExpression) {
        println("(i32.store offset=4 ");

        WASMSSAWriter theChild = withDeeperIndent();

        theChild.print("(i32.add ");
        theChild.printVariableNameOrValue(aExpression.getArray());
        theChild.print(" (i32.mul ");
        theChild.printVariableNameOrValue(aExpression.getIndex());
        theChild.print(" (i32.const 4)");
        theChild.println("))");

        theChild.printVariableNameOrValue(aExpression.getValue());
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
        switch (aExpression.getVariable().getType()) {
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
        theChild.println("__staticdata)");

        theChild.printVariableNameOrValue(aExpression.getVariable());;

        println(")");
    }

    private void writeSetMemoryLocationExpression(SetMemoryLocationExpression aExpression) {
        println("(i32.store");

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.printVariableNameOrValue(aExpression.getAddress());
        theChild.println();
        theChild.printVariableNameOrValue(aExpression.getValue());
        theChild.println();
        println(")");
    }

    private void writePutFieldExpression(PutFieldExpression aExpression) {
        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(BytecodeObjectTypeRef.fromUtf8Constant(aExpression.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = WASMWriterUtils.computeFieldOffsetOf(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                theLinkedClass);

        BytecodeLinkedClass.LinkedField theField = theLinkedClass.memberFieldByName(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        switch (Type.toType(theField.getField().getTypeRef())) {
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
        theChild.printVariableNameOrValue(aExpression.getTarget());
        theChild.printVariableNameOrValue(aExpression.getValue());

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
        aExpression.getBooleanExpression();
        print("(block $");
        print(aExpression.getAddress().getAddress());
        println();

        WASMSSAWriter theChild = withDeeperIndent();

        theChild.print("(br_if $");
        theChild.print(aExpression.getAddress().getAddress());
        theChild.println();

        WASMSSAWriter theChild3 = theChild.withDeeperIndent();
        theChild3.print("(i32.ne ");
        theChild3.printVariableNameOrValue(aExpression.getBooleanExpression());
        theChild3.print(" (i32.const 1)");
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
        if (theVariable.getValue() instanceof PrimitiveValue) {
            // Primitives are always inlined!
            return;
        }

        if (theVariable.getValue() instanceof PHIFunction) {
            return;
        }

        if (isStackVariable(theVariable)) {
            switch (theVariable.getType()) {
                case DOUBLE:
                case FLOAT: {
                    print("(f32.store offset=");
                }
                default: {
                    print("(i32.store offset=");
                    break;
                }
            }
            print(stackOffsetFor(theVariable));
            println(" (get_global $STACKTOP)");

            WASMSSAWriter theChild = withDeeperIndent();
            theChild.writeValue(theVariable.getValue());

            println();
            println(")");

        } else {
            print("(set_local $");
            print(theVariable.getName());
            println();

            WASMSSAWriter theChild = withDeeperIndent();
            theChild.writeValue(theVariable.getValue());

            println();
            println(")");
        }
    }

    private void writeValue(Value aValue) {
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
        if (aValue instanceof VariableReferenceValue) {
            writeVariableReferenceValue((VariableReferenceValue) aValue);
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
        throw new IllegalStateException("Not supported : " + aValue);
    }

    private void writeCurrentException(CurrentExceptionValue aValue) {
        print("(i32.const 0)");
    }

    private void writeMethodTypeValue(MethodTypeValue aValue) {
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
        print("(i32.const 0)");
    }

    private void writeNegateValue(NegatedValue aValue) {
        switch (aValue.getVariable().getType()) {
            case DOUBLE:
            case FLOAT: {
                    print("(f32.neg ");
                    printVariableNameOrValue(aValue.getVariable());
                    print(")");
                }
                break;
            default:
                print("(i32.mul (i32.const -1) ");
                printVariableNameOrValue(aValue.getVariable());
                print(")");
                break;
        }
    }

    private void writeCompareValue(CompareValue aValue) {
        if (aValue.getValue1().getType() == Type.DOUBLE && aValue.getValue2().getType() == Type.DOUBLE) {
            print("(call $compareValueF32 ");
        } else if (aValue.getValue1().getType() == Type.FLOAT && aValue.getValue2().getType() == Type.FLOAT) {
            print("(call $compareValueF32 ");
        } else {
            print("(call $compareValueI32 ");
        }
        printVariableNameOrValue(aValue.getValue1());
        print(" ");
        printVariableNameOrValue(aValue.getValue2());
        print(")");
    }

    private void writeArrayEntryValue(ArrayEntryValue aValue) {
        print("(i32.load offset=4 ");
        printVariableNameOrValue(aValue.getArray());

        print("(i32.add ");
        printVariableNameOrValue(aValue.getArray());
        print(" (i32.mul ");
        printVariableNameOrValue(aValue.getIndex());
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
        println("(call $newArray ");
        withDeeperIndent().printVariableNameOrValue(aValue.getLength());
        println();
        println(")");
    }

    private void writeArrayLengthValue(ArrayLengthValue aValue) {
        // First int is always length of array
        println("(i32.load ");
        withDeeperIndent().printVariableNameOrValue(aValue.getArray());
        println();
        println(")");
    }

    private void writeFloorValue(FloorValue aValue) {
        println("(i32.trunc_s/f32 (f32.floor ");
        withDeeperIndent().writeValue(aValue.getValue());
        println("))");
    }

    private void writeInvokeVirtualValue(InvokeVirtualMethodValue aValue) {
        print("(call_indirect $t_");
        print(WASMWriterUtils.toMethodSignature(aValue.getSignature()));
        println();

        printVariableNameOrValue(aValue.getTarget());

        for (Variable theVariable : aValue.getArguments()) {
            printVariableNameOrValue(theVariable);
            print(" ");
        }

        println();


        WASMSSAWriter theChild = withDeeperIndent();
        theChild.println("(call_indirect $RESOLVEMETHOD");

        WASMSSAWriter theChild2 = theChild.withDeeperIndent();

        // This is the method number
        theChild2.print("(i32.const ");

        BytecodeVirtualMethodIdentifier theMethodIdentifier = linkerContext.getMethodCollection().identifierFor(aValue.getMethodName(), aValue.getSignature());
        theChild2.print(theMethodIdentifier.getIdentifier());

        theChild2.println(")");

        // And this is the pointer into the function table
        theChild2.print("(i32.load offset=4 ");
        theChild2.printVariableNameOrValue(aValue.getTarget());
        theChild2.println(")");   // This is id of the virtual table method

        theChild.println(")");

        println(")");

    }

    private void writeFloatValue(FloatValue aValue) {
        print("(f32.const ");
        print(aValue.getFloatValue());
        print(")");
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
        Type theTargetType = aValue.getTargetType();
        if (theTargetType.equals(aValue.getVariable().getType())) {
            // No conversion needed!
            printVariableNameOrValue(aValue.getVariable());
            return;
        }
        switch (aValue.getVariable().getType()) {
            case DOUBLE:
            case FLOAT: {
                // Convert floating point to something else
                switch (aValue.getTargetType()) {
                    case DOUBLE:
                    case FLOAT: {
                        // No conversion needed
                        printVariableNameOrValue(aValue.getVariable());
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
                    case LONG:
                    case CHAR: {
                        // Convert f32 to i32
                        print("(i32.trunc_s/f32 ");
                        printVariableNameOrValue(aValue.getVariable());
                        print(")");
                        return;
                    }
                    default:
                        throw new IllegalStateException("target type " + aValue.getTargetType() + " not supported!");
                }
            }
            case INT:
            case LONG:
            case BYTE:
            case SHORT:
            case CHAR: {
                // Convert integer type to something else
                // Convert floating point to something else
                switch (aValue.getTargetType()) {
                case DOUBLE:
                case FLOAT: {
                    // Convert i32 to f32
                    print("(f32.convert_s/i32 ");
                    printVariableNameOrValue(aValue.getVariable());
                    print(")");
                    return;
                }
                case INT:
                case SHORT:
                case BYTE:
                case LONG:
                case CHAR: {
                    // No conversion needed
                    printVariableNameOrValue(aValue.getVariable());
                    return;
                }
                default:
                    throw new IllegalStateException("target type " + aValue.getTargetType() + " not supported!");
                }
            }
            default:
                throw new IllegalStateException("Conversion of " + aValue.getVariable().getType() + " not supported!");
        }
    }

    private void writeComputedMemoryLocationValue(ComputedMemoryLocationWriteValue aValue) {
        println("(i32.add ");

        withDeeperIndent().printVariableNameOrValue(aValue.getOrigin());
        println();
        withDeeperIndent().printVariableNameOrValue(aValue.getOffset());
        println();

        println(")");
    }

    private void writeComputedMemoryLocationValue(ComputedMemoryLocationReadValue aValue) {
        println("(i32.load (i32.add ");

        withDeeperIndent().printVariableNameOrValue(aValue.getOrigin());
        println();
        withDeeperIndent().printVariableNameOrValue(aValue.getOffset());
        println();

        println("))");
    }

    private void writeFixedBinaryValue(FixedBinaryValue aValue) {
        switch (aValue.getOperator()) {
            case ISNULL: {
                print("(i32.eq ");
                printVariableNameOrValue(aValue.getValue1());
                print(" (i32.const 0))");
                break;
            }
            case ISNONNULL: {
                print("(i32.ne ");
                printVariableNameOrValue(aValue.getValue1());
                print(" (i32.const 0))");
                break;
            }
            case ISZERO: {
                print("(i32.eq ");
                printVariableNameOrValue(aValue.getValue1());
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
        switch (Type.toType(theField.getField().getTypeRef())) {
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
        theChild.println("__staticdata)");

        println(")");
    }

    private void writeNewObjectValue(NewObjectValue aValue) {

        BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(aValue.getType().getConstant());

        String theMallocName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "newObject",
                new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                        Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        BytecodeLinkedClass theLinkedClass = linkerContext.linkClass(theType);
        print("(call $");
        print(theMallocName);
        print(" (i32.const ");
        print(WASMWriterUtils.computeObjectSizeFor(theLinkedClass));
        print(") (i32.const ");
        print(theLinkedClass.getUniqueId());
        print(") (i32.const ");
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

        switch (Type.toType(theField.getField().getTypeRef())) {
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
        theChild.printVariableNameOrValue(aValue.getTarget());

        println(")");
    }

    private void writeDirectMethodInvokeValue(DirectInvokeMethodValue aValue) {
        print("(call $");
        print(WASMWriterUtils.toMethodName(aValue.getClazz(), aValue.getMethodName(), aValue.getMethodSignature()));

        print(" ");
        printVariableNameOrValue(aValue.getTarget());

        for (Variable theVariable : aValue.getArguments()) {
            print(" ");
            printVariableNameOrValue(theVariable);
        }

        print(")");
    }

    private void writeInvokeStaticValue(InvokeStaticMethodValue aValue) {
        print("(call $");
        print(WASMWriterUtils.toMethodName(aValue.getClassName(), aValue.getMethodName(), aValue.getSignature()));

        for (Variable theVariable : aValue.getArguments()) {
            print(" ");
            printVariableNameOrValue(theVariable);
        }

        print(")");
    }

    private void writeVariableReferenceValue(VariableReferenceValue aValue) {
        printVariableNameOrValue(aValue.getVariable());
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
        String theType1 = WASMWriterUtils.toType(aValue.getValue1().getType());
        String theType2 = WASMWriterUtils.toType(aValue.getValue1().getType());
        switch (aValue.getOperator()) {
            case NOTEQUALS: {
                println("(" + theType1 + ".ne ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();

                println(")");
                break;
            }
            case EQUALS: {
                println("(" + theType1 + ".eq ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();

                println(")");
                break;
            }
            case LESSTHAN: {
                println("(" + theType1 + ".lt_s ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();
                println(")");
                break;
            }
            case LESSTHANOREQUALS: {
                println("(" + theType1 + ".le_s ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
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
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();

                println(")");
                break;
            }
            case GREATERTHAN: {
                println("(" + theType1 + ".gt_s ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();

                println(")");
                break;
            }
            case ADD: {
                println("(" + theType1 + ".add ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();

                println(")");
                break;
            }
            case MUL: {
                println("(" + theType1 + ".mul");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();

                println(")");
                break;
            }
            case DIV: {
                println("(f32.div ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValueAsFloat(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValueAsFloat(aValue.getValue2());
                theChild.println();

                println(")");
                break;
            }
            case REMAINDER: {
                if (aValue.getValue1().getType() == Type.INT) {
                    println("(i32.rem_s ");

                    WASMSSAWriter theChild = withDeeperIndent();
                    theChild.printVariableNameOrValue(aValue.getValue1());
                    theChild.println();
                    theChild.printVariableNameOrValue(aValue.getValue2());
                    theChild.println();

                    println(")");
                    break;
                }
                print("(call $float_remainder ");

                WASMSSAWriter theChild = withDeeperIndent();
                theChild.printVariableNameOrValue(aValue.getValue1());
                theChild.println();
                theChild.printVariableNameOrValue(aValue.getValue2());
                theChild.println();

                print(")");
                break;
            }
            case SUB: {
                println("(" + theType1 + ".sub ");

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

    private void writeCommentExpression(CommentExpression aExpression) {
        print(";; ");
        println(aExpression.getValue());
    }

    private void writeReturnExpression(ReturnExpression aExpression) {
        printStackExit();
        println("(return)");
    }

    private void writeReturnExpression(ReturnVariableExpression aExpression) {
        printStackExit();
        print("(return ");

        printVariableNameOrValue(aExpression.getVariable());

        println(")");
    }

    private void printVariableNameOrValueAsFloat(Variable aVariable) {
        switch (aVariable.getType()) {
            case DOUBLE:
            case FLOAT:
                printVariableNameOrValue(aVariable);
                break;
            default:
                print("(f32.convert_s/i32 ");
                printVariableNameOrValue(aVariable);
                print(")");
                break;
        }
    }

    private void printVariableNameOrValue(Variable aVariable) {
        if (aVariable.getValue() instanceof PrimitiveValue) {
            writeValue(aVariable.getValue());
        } else {
            if (isStackVariable(aVariable)) {
                print("(i32.load offset=");
                print(stackOffsetFor(aVariable));
                print(" (get_global $STACKTOP)");
                print(")");
            } else {
                print("(get_local ");
                print("$");
                print(aVariable.getName());
                print(")");
            }
        }
    }

    public void printStackEnter() {

        int theStackSize = stackSize();
        if (theStackSize > 0) {
            println("(local $OLD_SP i32)");
            println("(set_local $OLD_SP (get_global $STACKTOP))");
            print("(set_global $STACKTOP (i32.sub (get_global $STACKTOP) (i32.const ");
            print(theStackSize);
            println(")))");
        }

    }

    private void printStackExit() {

        int theStackSize = stackSize();
        if (theStackSize > 0) {
            println("(set_global $STACKTOP (get_local $OLD_SP))");
        }
    }
}