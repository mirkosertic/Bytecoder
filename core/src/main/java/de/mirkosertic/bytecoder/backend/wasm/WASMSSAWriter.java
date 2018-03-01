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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ArrayEntryExpression;
import de.mirkosertic.bytecoder.ssa.ArrayLengthExpression;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ByteValue;
import de.mirkosertic.bytecoder.ssa.CheckCastExpression;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.CompareExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationReadExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationWriteExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.CurrentExceptionExpression;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.FixedBinaryExpression;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.FloorExpression;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.GetStaticExpression;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InstanceOfExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.LookupSwitchExpression;
import de.mirkosertic.bytecoder.ssa.MemorySizeExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupExpression;
import de.mirkosertic.bytecoder.ssa.MethodRefExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.NegatedExpression;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeExpression;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.SqrtExpression;
import de.mirkosertic.bytecoder.ssa.StackTopExpression;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TableSwitchExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionExpression;
import de.mirkosertic.bytecoder.ssa.TypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

public class WASMSSAWriter extends IndentSSAWriter {

    public static final int GENERATED_INSTANCEOF_METHOD_ID = -1;

    public interface IDResolver {

        int resolveVTableMethodByType(BytecodeObjectTypeRef aObjectType);

        String resolveStringPoolFunctionName(StringValue aValue);

        String resolveCallsiteBootstrapFor(BytecodeClass aOwningClass, String aCallsiteId, Program aProgram, RegionNode aBootstrapMethod);

        int resolveMethodIDByName(String aMethodName);

        void registerGlobalType(BytecodeMethodSignature aSignature, boolean aStatic);
    }

    private final List<Variable> stackVariables;
    private final IDResolver idResolver;
    private final WASMMemoryLayouter memoryLayouter;

    public WASMSSAWriter(CompileOptions aOptions, Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext, IDResolver aIDResolver,
                         WASMMemoryLayouter aMemoryLayouter) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        stackVariables = new ArrayList<>();
        idResolver = aIDResolver;
        memoryLayouter = aMemoryLayouter;
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
            if (Objects.equals(theVariable.getName(), aVariable.getName())) {
                return true;
            }
        }
        return false;
    }

    private int stackOffsetFor(Variable aVariable) {
        int theStart = 0;
        for (Variable theVariable : stackVariables) {
            if (Objects.equals(theVariable.getName(), aVariable.getName())) {
                return theStart;
            }
            theStart += 4;
        }
        throw new IllegalStateException("Unknown variable : " + aVariable);
    }

    private WASMSSAWriter withDeeperIndent() {
        return new WASMSSAWriter(options, program, indent + "    ", writer, linkerContext, idResolver, memoryLayouter);
    }

    public void writeExpressionList(ExpressionList aList) {
        for (Expression theExpression : aList.toList()) {
            writeExpression(theExpression);
        }
    }

    private void writeExpression(Expression aExpression) {
        if (options.isDebugOutput()) {
            String theComment = aExpression.getComment();
            if (theComment != null && !theComment.isEmpty()) {
                print(";; ");
                println(theComment);
            }
        }

        if (aExpression instanceof CheckCastExpression) {
            return;
        }
        if (aExpression instanceof ReturnExpression) {
            writeReturnExpression((ReturnExpression) aExpression);
            return;
        }
        if (aExpression instanceof VariableAssignmentExpression) {
            writeInitVariableExpression((VariableAssignmentExpression) aExpression);
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
        if (aExpression instanceof BreakExpression) {
            BreakExpression theBreak = (BreakExpression) aExpression;
            if (theBreak.isSetLabelRequired()) {
                print("(set_local $__label__ (i32.const ");
                print(theBreak.jumpTarget().getAddress());
                println("))");
            }
            if (!theBreak.isSilent()) {
                print("(br $");
                print(theBreak.blockToBreak().name());
                println(")");
            }
            return;
        }
        if (aExpression instanceof ContinueExpression) {
            ContinueExpression theContinue = (ContinueExpression) aExpression;
            print("(set_local $__label__ (i32.const ");
            print(theContinue.jumpTarget().getAddress());
            println("))");
            print("(br $");
            print(theContinue.labelToReturnTo().name());
            println("_inner)");
            return;
        }

        throw new IllegalStateException("Not supported : " + aExpression);
    }

    private void writeUnreachable(UnreachableExpression aExpression) {
        println("(unreachable)");
    }

    private void writeLookupSwitchExpression(LookupSwitchExpression aExpression) {
        println("(block $outer");

        Value theValue = aExpression.incomingDataFlows().get(0);

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

        Value theValue = aExpression.incomingDataFlows().get(0);

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
        writeInvokeVirtualValue(aExpression);
        println();
    }

    private void writeArrayStoreExpression(ArrayStoreExpression aExpression) {

        List<Value> theIncomingData = aExpression.incomingDataFlows();
        Value theArray = theIncomingData.get(0);
        Value theIndex = theIncomingData.get(1);
        Value theValue = theIncomingData.get(2);

        // If the index is a constant, we can precompute the offset.
        if (theIndex instanceof IntegerValue) {
            int offset = 20 + ((IntegerValue)theIndex).getIntValue() * 4;

            switch (aExpression.getArrayType().resolve()) {
                case DOUBLE:
                case FLOAT: {
                    print("(f32.store ");
                    break;
                }
                default: {
                    print("(i32.store ");
                    break;
                }
            }

            print("offset="+offset+" ");

            writeValue(theArray);
            print(" ");
            writeValue(theValue);

            println(")");

            return;
        }

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
        theChild.writeValue(theArray);
        theChild.print(" (i32.mul ");
        theChild.writeValue(theIndex);
        theChild.print(" (i32.const 4)");
        theChild.println("))");

        theChild.writeValue(theValue);
        theChild.println();

        println(")");
    }

    private void writeThrowExpression(ThrowExpression aExpression) {
        printStackExit();
        println("(unreachable)");
    }

    private void writeInvokeStaticExpression(InvokeStaticMethodExpression aExpression) {
        writeValue(aExpression);
        println();
    }

    private void writePutStaticExpression(PutStaticExpression aExpression) {

        WASMMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(BytecodeObjectTypeRef.fromUtf8Constant(aExpression.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = theLayout.offsetForClassMember(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        List<Value> theIncomingData = aExpression.incomingDataFlows();

        String theClassName = WASMWriterUtils.toClassName(aExpression.getField().getClassIndex().getClassConstant());
        switch (theIncomingData.get(0).resolveType().resolve()) {
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

        theChild.writeValue(theIncomingData.get(0));

        println(")");
    }

    private void writeSetMemoryLocationExpression(SetMemoryLocationExpression aExpression) {
        println("(i32.store");

        List<Value> theIncomingData = aExpression.incomingDataFlows();

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.writeValue(theIncomingData.get(0));
        theChild.println();
        theChild.writeValue(theIncomingData.get(1));
        theChild.println();
        println(")");
    }

    private void writePutFieldExpression(PutFieldExpression aExpression) {

        WASMMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(BytecodeObjectTypeRef.fromUtf8Constant(aExpression.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = theLayout.offsetForInstanceMember(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(aExpression.getField().getClassIndex().getClassConstant().getConstant()));
        BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
        BytecodeResolvedFields.FieldEntry theField = theInstanceFields.fieldByName(aExpression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        switch (TypeRef.toType(theField.getValue().getTypeRef()).resolve()) {
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

        List<Value> theIncomingData = aExpression.incomingDataFlows();

        WASMSSAWriter theChild = withDeeperIndent();
        theChild.writeValue(theIncomingData.get(0));
        theChild.writeValue(theIncomingData.get(1));

        println(")");
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
        theChild3.writeValue(aExpression.incomingDataFlows().get(0));
        theChild3.print(" (i32.const 0)");
        theChild3.println(")");

        theChild.println(")");

        theChild.writeExpressionList(aExpression.getExpressions());

        println(")");
    }

    private void writeDirectMethodInvokeExpression(DirectInvokeMethodExpression aExpression) {
        writeValue(aExpression);
        println();
    }

    private void writeInitVariableExpression(VariableAssignmentExpression aExpression) {

        Variable theVariable = aExpression.getVariable();
        Value theNewValue = aExpression.getValue();

        if (theNewValue instanceof PHIExpression) {
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
        if (aValue instanceof BinaryExpression) {
            writeBinaryValue((BinaryExpression) aValue);
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
        if (aValue instanceof DirectInvokeMethodExpression) {
            writeDirectMethodInvokeValue((DirectInvokeMethodExpression) aValue);
            return;
        }
        if (aValue instanceof InvokeStaticMethodExpression) {
            writeInvokeStaticValue((InvokeStaticMethodExpression) aValue);
            return;
        }
        if (aValue instanceof GetFieldExpression) {
            writeGetFieldValue((GetFieldExpression) aValue);
            return;
        }
        if (aValue instanceof NewObjectExpression) {
            writeNewObjectValue((NewObjectExpression) aValue);
            return;
        }
        if (aValue instanceof GetStaticExpression) {
            writeGetStaticValue((GetStaticExpression) aValue);
            return;
        }
        if (aValue instanceof LongValue) {
            writeLongValue((LongValue) aValue);
            return;
        }
        if (aValue instanceof FixedBinaryExpression) {
            writeFixedBinaryValue((FixedBinaryExpression) aValue);
            return;
        }
        if (aValue instanceof ComputedMemoryLocationReadExpression) {
            writeComputedMemoryLocationValue((ComputedMemoryLocationReadExpression) aValue);
            return;
        }
        if (aValue instanceof ComputedMemoryLocationWriteExpression) {
            writeComputedMemoryLocationValue((ComputedMemoryLocationWriteExpression) aValue);
            return;
        }
        if (aValue instanceof TypeConversionExpression) {
            writeTypeConversion((TypeConversionExpression) aValue);
            return;
        }
        if (aValue instanceof NullValue) {
            writeNullValue((NullValue) aValue);
            return;
        }
        if (aValue instanceof StackTopExpression) {
            writeStackTopValue((StackTopExpression) aValue);
            return;
        }
        if (aValue instanceof MemorySizeExpression) {
            writrMemorySizeValue((MemorySizeExpression) aValue);
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
        if (aValue instanceof InvokeVirtualMethodExpression) {
            writeInvokeVirtualValue((InvokeVirtualMethodExpression) aValue);
            return;
        }
        if (aValue instanceof FloorExpression) {
            writeFloorValue((FloorExpression) aValue);
            return;
        }
        if (aValue instanceof NewArrayExpression) {
            writeNewArrayValue((NewArrayExpression) aValue);
            return;
        }
        if (aValue instanceof ArrayLengthExpression) {
            writeArrayLengthValue((ArrayLengthExpression) aValue);
            return;
        }
        if (aValue instanceof StringValue) {
            writeStringValue((StringValue) aValue);
            return;
        }
        if (aValue instanceof ArrayEntryExpression) {
            writeArrayEntryValue((ArrayEntryExpression) aValue);
            return;
        }
        if (aValue instanceof CompareExpression) {
            writeCompareValue((CompareExpression) aValue);
            return;
        }
        if (aValue instanceof NegatedExpression) {
            writeNegateValue((NegatedExpression) aValue);
            return;
        }
        if (aValue instanceof InstanceOfExpression) {
            writeInstanceOfValue((InstanceOfExpression) aValue);
            return;
        }
        if (aValue instanceof DoubleValue) {
            writeDoubleValue((DoubleValue) aValue);
            return;
        }
        if (aValue instanceof ResolveCallsiteObjectExpression) {
            writeResolveCallSiteObjectValue((ResolveCallsiteObjectExpression) aValue);
            return;
        }
        if (aValue instanceof MethodHandlesGeneratedLookupExpression) {
            writeMethodHandlesGeneratedLookupValue((MethodHandlesGeneratedLookupExpression) aValue);
            return;
        }
        if (aValue instanceof MethodTypeExpression) {
            writeMethodTypeValue((MethodTypeExpression) aValue);
            return;
        }
        if (aValue instanceof CurrentExceptionExpression) {
            writeCurrentException((CurrentExceptionExpression) aValue);
            return;
        }
        if (aValue instanceof ClassReferenceValue) {
            writeClassReferenceValue((ClassReferenceValue) aValue);
            return;
        }
        if (aValue instanceof TypeOfExpression) {
            writeTypeOfValue((TypeOfExpression) aValue);
            return;
        }
        if (aValue instanceof RuntimeGeneratedTypeExpression) {
            writeRuntimeGeneratedTypeValue((RuntimeGeneratedTypeExpression) aValue);
            return;
        }
        if (aValue instanceof MethodRefExpression) {
            writeMethodRefValue((MethodRefExpression) aValue);
            return;
        }
        if (aValue instanceof NewMultiArrayExpression) {
            writeNewMultiArrayValue((NewMultiArrayExpression) aValue);
            return;
        }
        if (aValue instanceof SqrtExpression) {
            writeSqrtValue((SqrtExpression) aValue);
            return;
        }
        throw new IllegalStateException("Not supported : " + aValue);
    }

    private void writeSqrtValue(SqrtExpression aValue) {
        print("(f32.sqrt ");
        writeValue(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void writeNewMultiArrayValue(NewMultiArrayExpression aValue) {

        List<Value> theDimensions = aValue.incomingDataFlows();

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
        print(" (get_global $jlrArray__runtimeClass)");
        // Plus the vtable index
        print(" (i32.const ");
        print(idResolver.resolveVTableMethodByType(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
        print(")");

        println(") ;; new array of type " + theType);
    }

    private void writeMethodRefValue(MethodRefExpression aValue) {
        print("(i32.const ");
        
        String theMethodName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromUtf8Constant(aValue.getMethodRef().getClassIndex().getClassConstant().getConstant()),
                aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());
        
        print(idResolver.resolveMethodIDByName(theMethodName));
        
        print(")");
    }

    private void writeRuntimeGeneratedTypeValue(RuntimeGeneratedTypeExpression aValue) {
        println("(call $newLambda ");
        WASMSSAWriter theChild = withDeeperIndent();
        theChild.writeValue(aValue.getType());
        theChild.println();
        theChild.writeValue(aValue.getMethodRef());
        theChild.println();
        println(")");
    }

    private void writeTypeOfValue(TypeOfExpression aValue) {
        print("(i32.load ");
        writeValue(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void writeClassReferenceValue(ClassReferenceValue aValue) {
        print("(get_global $");
        BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(aValue.getType());
        print(WASMWriterUtils.toClassName(theLinkedClass.getClassName()));
        print("__runtimeClass)");
    }

    private void writeCurrentException(CurrentExceptionExpression aValue) {
        print("(i32.const 0)");
    }

    private void writeMethodTypeValue(MethodTypeExpression aValue) {
//        print("(i32.const ");
//        print(idResolver.resolveTypeIDForSignature(aValue.getSignature()));
//        print(")");
        println();
        println(";; " + WASMWriterUtils.toMethodSignature(aValue.getSignature(), false));
        print("(i32.const 0)");
    }

    private void writeMethodHandlesGeneratedLookupValue(MethodHandlesGeneratedLookupExpression aValue) {
        print("(i32.const 0)");
    }

    private void writeResolveCallSiteObjectValue(ResolveCallsiteObjectExpression aValue) {
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

    private void writeInstanceOfValue(InstanceOfExpression aValue) {

        BytecodeLinkedClass theClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getType().getConstant()));

        print("(call $INSTANCEOF_CHECK ");
        writeValue(aValue.incomingDataFlows().get(0));
        print(" (i32.const ");
        print(theClass.getUniqueId());
        println("))");
    }

    private void writeNegateValue(NegatedExpression aValue) {
        Value theValue = aValue.incomingDataFlows().get(0);
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

    private void writeCompareValue(CompareExpression aValue) {
        List<Value> theIncomingFlows = aValue.incomingDataFlows();
        Value theValue1 = theIncomingFlows.get(0);
        Value theValue2 = theIncomingFlows.get(1);

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

    private void writeArrayEntryValue(ArrayEntryExpression aValue) {
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

        List<Value> theIncomingFlows = aValue.incomingDataFlows();

        print("(i32.add ");
        writeValue(theIncomingFlows.get(0));
        print(" (i32.mul ");
        writeValue(theIncomingFlows.get(1));
        print(" (i32.const 4)");
        println("))");

        println(")");
    }

    private void writeStringValue(StringValue aValue) {
        print("(get_global $");
        print(idResolver.resolveStringPoolFunctionName(aValue));
        print(")");
    }

    private void writeNewArray(Value aValue) {
        String theMethodName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "newArray",
                new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                        Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        print("(call $");
        print(theMethodName);
        print(" (i32.const 0) "); // UNUSED argument

        // Length
        withDeeperIndent().writeValue(aValue);

        // We also need the runtime class
        print(" (get_global $jlrArray__runtimeClass)");
        // Plus the vtable index
        print(" (i32.const ");
        print(idResolver.resolveVTableMethodByType(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
        print(")");

        println(")");
    }

    private void writeNewArrayValue(NewArrayExpression aValue) {
        writeNewArray(aValue.incomingDataFlows().get(0));
    }

    private void writeArrayLengthValue(ArrayLengthExpression aValue) {
        println("(i32.load offset=16 ");
        withDeeperIndent().writeValue(aValue.incomingDataFlows().get(0));
        println();
        println(")");
    }

    private void writeFloorValue(FloorExpression aValue) {
        println("(i32.trunc_s/f32 (f32.floor ");
        withDeeperIndent().writeValue(aValue.incomingDataFlows().get(0));
        println("))");
    }

    private void writeInvokeVirtualValue(InvokeVirtualMethodExpression aValue) {

        idResolver.registerGlobalType(aValue.getSignature(), false);

        print("(call_indirect $t_");
        print(WASMWriterUtils.toMethodSignature(aValue.getSignature(), false));
        println();

        List<Value> theFlows = aValue.incomingDataFlows();

        Value theTarget = theFlows.get(0);
        List<Value> theVariables = theFlows.subList(1, theFlows.size());

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

    private void writeStackTopValue(StackTopExpression aValue) {
        print("(get_global $STACKTOP)");
    }

    private void writrMemorySizeValue(MemorySizeExpression aValue) {
        print("(i32.mul (current_memory) (i32.const 65536))");
    }

    private void writeNullValue(NullValue aValue) {
        print("(i32.const 0)");
    }

    private void writeTypeConversion(TypeConversionExpression aValue) {
        TypeRef theTargetType = aValue.resolveType();
        Value theSource = aValue.incomingDataFlows().get(0);
        if (Objects.equals(theTargetType.resolve(), theSource.resolveType().resolve())) {
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

    private void writeComputedMemoryLocationValue(ComputedMemoryLocationWriteExpression aValue) {
        println("(i32.add ");

        List<Value> theIncomingData = aValue.incomingDataFlows();

        withDeeperIndent().writeValue(theIncomingData.get(0));
        println();
        withDeeperIndent().writeValue(theIncomingData.get(1));
        println();

        println(")");
    }

    private void writeComputedMemoryLocationValue(ComputedMemoryLocationReadExpression aValue) {
        println("(i32.load (i32.add ");

        List<Value> theIncomingData = aValue.incomingDataFlows();

        withDeeperIndent().writeValue(theIncomingData.get(0));
        println();
        withDeeperIndent().writeValue(theIncomingData.get(1));
        println();

        println("))");
    }

    private void writeFixedBinaryValue(FixedBinaryExpression aValue) {

        switch (aValue.getOperator()) {
            case ISNULL: {
                print("(i32.eq ");
                writeValue(aValue.incomingDataFlows().get(0));
                print(" (i32.const 0))");
                break;
            }
            case ISNONNULL: {
                print("(i32.ne ");
                writeValue(aValue.incomingDataFlows().get(0));
                print(" (i32.const 0))");
                break;
            }
            case ISZERO: {
                print("(i32.eq ");
                writeValue(aValue.incomingDataFlows().get(0));
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

    private void writeGetStaticValue(GetStaticExpression aValue) {
        BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));

        WASMMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = theLayout.offsetForClassMember(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
        String theFieldName = aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
        BytecodeResolvedFields.FieldEntry theField = theStaticFields.fieldByName(theFieldName);
        if (!theField.getValue().getAccessFlags().isStatic()) {
            throw new IllegalStateException("Field " + theFieldName + " is not static!");
        }

        String theClassName = WASMWriterUtils.toClassName(aValue.getField().getClassIndex().getClassConstant());
        switch (TypeRef.toType(theField.getValue().getTypeRef()).resolve()) {
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

    private void writeNewObjectValue(NewObjectExpression aValue) {

        BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(aValue.getType().getConstant());

        WASMMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theType);

        String theMethodName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "newObject",
                new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                        Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(theType);
        print("(call $");
        print(theMethodName);
        print(" (i32.const 0) "); // UNUSED argument
        print(" (i32.const ");
        print(theLayout.instanceSize());
        print(") (get_global $");
        print(WASMWriterUtils.toClassName(theLinkedClass.getClassName()));
        print("__runtimeClass) (i32.const ");
        print(idResolver.resolveVTableMethodByType(theType));
        print(")) ;; object of type " + aValue.getType().getConstant().stringValue());
    }

    private void writeGetFieldValue(GetFieldExpression aValue) {
        BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));

        WASMMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(BytecodeObjectTypeRef.fromUtf8Constant(aValue.getField().getClassIndex().getClassConstant().getConstant()));
        int theMemoryOffset = theLayout.offsetForInstanceMember(aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
        String theFieldName = aValue.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();

        BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
        BytecodeResolvedFields.FieldEntry theField = theInstanceFields.fieldByName(theFieldName);
        if (theField.getValue().getAccessFlags().isStatic()) {
            throw new IllegalStateException("Field " + theFieldName + " is static!");
        }

        switch (TypeRef.toType(theField.getValue().getTypeRef()).resolve()) {
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
        theChild.writeValue(aValue.incomingDataFlows().get(0));

        println(")");
    }

    private void writeDirectMethodInvokeValue(DirectInvokeMethodExpression aValue) {

        List<Value> theIncomingData = aValue.incomingDataFlows();

        Value theTarget = theIncomingData.get(0);
        List<Value> theValues = theIncomingData.subList(1, theIncomingData.size());

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

    private void writeInvokeStaticValue(InvokeStaticMethodExpression aValue) {
        print("(call $");
        print(WASMWriterUtils.toMethodName(aValue.getClassName(), aValue.getMethodName(), aValue.getSignature()));

        print(" (i32.const 0)"); // UNUSED Argument

        List<Value> theArguments = aValue.incomingDataFlows();
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

    private void writeBinaryValue(BinaryExpression aValue) {

        List<Value> theIncomingData = aValue.incomingDataFlows();

        Value theValue1 = theIncomingData.get(0);
        Value theValue2 = theIncomingData.get(1);

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
                if ("i32".equals(theType1)) {
                    println("(" + theType1 + ".lt_s ");
                } else {
                    println("(" + theType1 + ".lt ");
                }

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

    private void writeReturnExpression(ReturnExpression aExpression) {
        printStackExit();
        println("(return)");
    }

    private void writeReturnExpression(ReturnValueExpression aExpression) {
        printStackExit();
        print("(return ");
        writeValue(aExpression.incomingDataFlows().get(0));
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

    public void printStackEnter() {

        int theStackSize = stackSize();
        if (theStackSize > 0) {
            println("(local $SP i32)");
            println("(local $OLD_SP i32)");
            println("(set_local $OLD_SP (get_global $STACKTOP))");
            print("(set_global $STACKTOP (i32.sub (get_global $STACKTOP) (i32.const ");
            print(theStackSize);
            println(")))");
            println("(set_local $SP (get_global $STACKTOP))");
        }
    }

    private void printStackExit() {

        int theStackSize = stackSize();
        if (theStackSize > 0) {
            println("(set_global $STACKTOP (get_local $OLD_SP))");
        }
    }

    public void writeRelooped(Relooper.Block aBlock) {
        println("(local $__label__ i32)");
        printStackEnter();
        writeReloopedInternal(aBlock);
        println("(unreachable)");
    }

    private void writeReloopedInternal(Relooper.Block aBlock) {
        if (aBlock == null) {
            return;
        }
        if (aBlock instanceof Relooper.SimpleBlock) {
            writeSimpleBlock((Relooper.SimpleBlock) aBlock);
            return;
        }
        if (aBlock instanceof Relooper.LoopBlock) {
            writeLoopBlock((Relooper.LoopBlock) aBlock);
            return;
        }
        if (aBlock instanceof Relooper.MultipleBlock) {
            writeMultipleBlock((Relooper.MultipleBlock) aBlock);
            return;
        }
        throw new IllegalStateException("Don't know how to handle : " + aBlock);
    }

    private void writeSimpleBlock(Relooper.SimpleBlock aSimpleBlock) {
        WASMSSAWriter theWriter = this;
        if (aSimpleBlock.isLabelRequired()) {
            print("(block $");
            print(aSimpleBlock.label().name());
            println();

            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.writeExpressionList(aSimpleBlock.internalLabel().getExpressions());

        if (aSimpleBlock.isLabelRequired()) {
            println(")");
        }

        writeReloopedInternal(aSimpleBlock.next());
    }

    private void writeLoopBlock(Relooper.LoopBlock aLoopBlock) {
        WASMSSAWriter theWriter = this;
        if (aLoopBlock.isLabelRequired()) {
            print("(block $");
            print(aLoopBlock.label().name());
            println();
            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.print("(loop $");
        theWriter.print(aLoopBlock.label().name());
        theWriter.println("_inner");

        WASMSSAWriter theChild2 = theWriter.withDeeperIndent();
        theChild2.writeReloopedInternal(aLoopBlock.inner());

        theWriter.println(")");

        if (aLoopBlock.isLabelRequired()) {
            println(")");
        }

        writeReloopedInternal(aLoopBlock.next());
    }

    private void writeMultipleBlock(Relooper.MultipleBlock aMultipleBlock) {
        WASMSSAWriter theWriter = this;
        if (aMultipleBlock.isLabelRequired()) {
            print("(block $");
            print(aMultipleBlock.label().name());
            println();
            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.print("(loop $");
        theWriter.print(aMultipleBlock.label().name());
        theWriter.println("_inner");

        for (Relooper.Block theHandler : aMultipleBlock.handlers()) {
            for (RegionNode theEntry : theHandler.entries()) {
                int theEntryAddress = theEntry.getStartAddress().getAddress();

                WASMSSAWriter theChild2 = theWriter.withDeeperIndent();

                theChild2.print("(block $case_");
                theChild2.print(theEntryAddress);
                theChild2.println();

                WASMSSAWriter theChild3 = theChild2.withDeeperIndent();
                theChild3.print("(br_if $case_");
                theChild3.print(theEntryAddress);
                theChild3.print(" (i32.ne (get_local $__label__) (i32.const ");
                theChild3.print(theEntryAddress);
                theChild3.println(")))");

                theChild3.writeReloopedInternal(theHandler);

                theChild2.println(")");
            }
        }

        theWriter.println(")");

        if (aMultipleBlock.isLabelRequired()) {
            println(")");
        }

        writeReloopedInternal(aMultipleBlock.next());
    }
}