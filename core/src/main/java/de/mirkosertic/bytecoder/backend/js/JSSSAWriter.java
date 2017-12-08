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
package de.mirkosertic.bytecoder.backend.js;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.ssa.ArrayEntryValue;
import de.mirkosertic.bytecoder.ssa.ArrayLengthValue;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.BlockState;
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
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.MethodRefValue;
import de.mirkosertic.bytecoder.ssa.MethodTypeValue;
import de.mirkosertic.bytecoder.ssa.NegatedValue;
import de.mirkosertic.bytecoder.ssa.NewArrayValue;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayValue;
import de.mirkosertic.bytecoder.ssa.NewObjectValue;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIFunction;
import de.mirkosertic.bytecoder.ssa.PrimitiveValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectValue;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeValue;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.StackTopValue;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TableSwitchExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionValue;
import de.mirkosertic.bytecoder.ssa.TypeOfValue;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.UnknownValue;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableDescription;
import de.mirkosertic.bytecoder.ssa.ValueReferenceValue;

public class JSSSAWriter extends IndentSSAWriter {

    public JSSSAWriter(Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext) {
        super(aProgram, aIndent, aWriter, aLinkerContext);
    }

    public JSSSAWriter withDeeperIndent() {
        return new JSSSAWriter(program, indent + "    ", writer, linkerContext);
    }

    public void print(Value aValue) {
        if (aValue instanceof Variable) {
            print(((Variable) aValue).getName());
        } else if (aValue instanceof GetStaticValue) {
            print((GetStaticValue) aValue);
        } else if (aValue instanceof NullValue) {
            print((NullValue) aValue);
        } else if (aValue instanceof InvokeVirtualMethodValue) {
            print((InvokeVirtualMethodValue) aValue);
        } else if (aValue instanceof InvokeStaticMethodValue) {
            print((InvokeStaticMethodValue) aValue);
        } else if (aValue instanceof NewObjectValue) {
            print((NewObjectValue) aValue);
        } else if (aValue instanceof ValueReferenceValue) {
            print((ValueReferenceValue) aValue);
        } else if (aValue instanceof ByteValue) {
            print((ByteValue) aValue);
        } else if (aValue instanceof BinaryValue) {
            print((BinaryValue) aValue);
        } else if (aValue instanceof GetFieldValue) {
            print((GetFieldValue) aValue);
        } else if (aValue instanceof TypeConversionValue) {
            print((TypeConversionValue) aValue);
        } else if (aValue instanceof ArrayEntryValue) {
            print((ArrayEntryValue) aValue);
        } else if (aValue instanceof ArrayLengthValue) {
            print((ArrayLengthValue) aValue);
        } else if (aValue instanceof StringValue) {
            print((StringValue) aValue);
        } else if (aValue instanceof IntegerValue) {
            print((IntegerValue) aValue);
        } else if (aValue instanceof NewArrayValue) {
            print((NewArrayValue) aValue);
        } else if (aValue instanceof DirectInvokeMethodValue) {
            print((DirectInvokeMethodValue) aValue);
        } else if (aValue instanceof FloatValue) {
            print((FloatValue) aValue);
        } else if (aValue instanceof DoubleValue) {
            print((DoubleValue) aValue);
        } else if (aValue instanceof CompareValue) {
            print((CompareValue) aValue);
        } else if (aValue instanceof NegatedValue) {
            print((NegatedValue) aValue);
        } else if (aValue instanceof FixedBinaryValue) {
            print((FixedBinaryValue) aValue);
        } else if (aValue instanceof ShortValue) {
            print((ShortValue) aValue);
        } else if (aValue instanceof InstanceOfValue) {
            print((InstanceOfValue) aValue);
        } else if (aValue instanceof LongValue) {
            print((LongValue) aValue);
        } else if (aValue instanceof ClassReferenceValue) {
            print((ClassReferenceValue) aValue);
        } else if (aValue instanceof NewMultiArrayValue) {
            print((NewMultiArrayValue) aValue);
        } else if (aValue instanceof SelfReferenceParameterValue) {
            print((SelfReferenceParameterValue) aValue);
        } else if (aValue instanceof MethodParameterValue) {
            print((MethodParameterValue) aValue);
        } else if (aValue instanceof CurrentExceptionValue) {
            print((CurrentExceptionValue) aValue);
        } else if (aValue instanceof UnknownValue) {
            print((UnknownValue) aValue);
        } else if (aValue instanceof FloorValue) {
            print((FloorValue) aValue);
        } else if (aValue instanceof MethodRefValue) {
            print((MethodRefValue) aValue);
        } else if (aValue instanceof ComputedMemoryLocationReadValue) {
            print((ComputedMemoryLocationReadValue) aValue);
        } else if (aValue instanceof ComputedMemoryLocationWriteValue) {
            print((ComputedMemoryLocationWriteValue) aValue);
        } else if (aValue instanceof MethodHandlesGeneratedLookupValue) {
            print((MethodHandlesGeneratedLookupValue) aValue);
        } else if (aValue instanceof MethodTypeValue) {
            print((MethodTypeValue) aValue);
        } else if (aValue instanceof RuntimeGeneratedTypeValue) {
            print((RuntimeGeneratedTypeValue) aValue);
        } else if (aValue instanceof ResolveCallsiteObjectValue) {
            print((ResolveCallsiteObjectValue) aValue);
        } else if (aValue instanceof StackTopValue) {
            print((StackTopValue) aValue);
        } else if (aValue instanceof MemorySizeValue) {
            print((MemorySizeValue) aValue);
        } else if (aValue instanceof TypeOfValue) {
            print((TypeOfValue) aValue);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue);
        }
    }

    public void print(TypeOfValue aValue) {
        printVariableNameOrValue((Variable) aValue.resolveFirstArgument());
        print(".runtimeClass");
    }

    public void print(StackTopValue aValue) {
        print("0");
    }

    public void print(MemorySizeValue aValue) {
        print("0");
    }

    public void print(ResolveCallsiteObjectValue aValue) {

        print(JSWriterUtils.toClassName(aValue.getOwningClass().getThisInfo()));
        print(".resolveStaticCallSiteObject('");
        print(aValue.getCallsiteId());
        println("', function() {");

        Program theProgram = aValue.getProgram();
        GraphNode theBootstrapCode = aValue.getBootstrapMethod();

        JSSSAWriter theNested = withDeeperIndent();

        for (Variable theVariable : theProgram.globalVariables()) {
            theNested.print("var ");
            theNested.print(theVariable.getName());
            theNested.println(" = null;");
        }

        theNested.writeExpressions(theBootstrapCode.getExpressions());

        print("})");
    }

    public void print(RuntimeGeneratedTypeValue aValue) {
        print("{clazz: { resolveVirtualMethod : function(aIdentifier) {return function(inst, _p1, _p2, _p3, _p4, _p5, _p6, _p7, _p8, _p9) {return ");
        print(aValue.getMethodRef());
        print("(_p1, _p2, _p3, _p4, _p5, _p6, _p7, _p8, _p9);");
        print("}}}}");
    }

    public void print(MethodTypeValue aValue) {
        print("'");
        print(aValue.getSignature().toString());
        print("'");
    }

    public void print(MethodHandlesGeneratedLookupValue aValue) {
        print("null");
    }

    public void print(ComputedMemoryLocationWriteValue aValue) {
        print("bytecoderGlobalMemory[");
        print((Value) aValue.resolveFirstArgument());
        print(" + ");
        print((Value) aValue.resolveSecondArgument());
        print("]");
    }

    public void print(ComputedMemoryLocationReadValue aValue) {
        print("bytecoderGlobalMemory[");
        print((Value) aValue.resolveFirstArgument());
        print(" + ");
        print((Value) aValue.resolveSecondArgument());
        print("]");
    }

    public void print(MethodRefValue aValue) {
        String theMethodName = aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        BytecodeMethodSignature theSignature = aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        print(JSWriterUtils.toClassName(aValue.getMethodRef().getClassIndex().getClassConstant()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
    }

    public void print(FloorValue aValue) {
        print("Math.floor(");
        print((Value) aValue.resolveFirstArgument());
        print(")");
    }

    public void print(UnknownValue aValue) {
        print("undefined");
    }

    public void print(CurrentExceptionValue aValue) {
        //TODO: Fix this
        print("'current exception'");
    }

    public void print(MethodParameterValue aValue) {
        print("p" + (aValue.getParameterIndex() + 1));
    }

    public void print(SelfReferenceParameterValue aValue) {
        print("thisRef");
    }

    public void print(NewMultiArrayValue aValue) {
        BytecodeTypeRef theType = aValue.getType();
        Object theDefaultValue = theType.defaultValue();
        String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newMultiArray(");
        print("[");
        List<Value> theDimensions = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);
        for (int i=0;i<theDimensions.size();i++) {
            if (i>0) {
                print(",");
            }
            print(theDimensions.get(i));
        }
        print("]");
        print(",");
        print(theStrDefault);
        print(")");
    }

    public void print(ClassReferenceValue aValue) {
        print(JSWriterUtils.toClassName(aValue.getType()));
        print(".runtimeClass");
    }

    public void print(InstanceOfValue aValue) {
        Value theValue = aValue.resolveFirstArgument();
        print("(");
        print(theValue);
        print(" == null ? false : ");
        print(theValue);
        print(".clazz.instanceOfType(");

        BytecodeLinkedClass theLinkedClass = linkerContext.isLinkedOrNull(aValue.getType().getConstant());
        print(theLinkedClass.getUniqueId());

        print(")");
        print(")");
    }

    public void print(LongValue aValue) {
        print(aValue.getLongValue());
    }

    public void print(ShortValue aValue) {
        print(aValue.getShortValue());
    }

    public void print(NegatedValue aValue) {
        Value theValue = aValue.resolveFirstArgument();
        print("(-");
        print(theValue);
        print(")");
    }

    public void print(CompareValue aValue) {
        Value theVariable1 = aValue.resolveFirstArgument();
        Value theVariable2 = aValue.resolveSecondArgument();
        print("(");
        print(theVariable1);
        print(" > ");
        print(theVariable2);
        print(" ? 1 ");
        print(" : (");
        print(theVariable1);
        print(" < ");
        print(theVariable2);
        print(" ? -1 : 0))");
    }

    public void print(NewArrayValue aValue) {
        BytecodeTypeRef theType = aValue.getType();
        Value theLength = aValue.resolveFirstArgument();
        Object theDefaultValue = theType.defaultValue();
        String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newArray(");
        print(theLength);
        print(",");
        print(theStrDefault);
        print(")");
    }

    public void print(IntegerValue aValue) {
        print(aValue.getIntValue());
    }

    public void print(FloatValue aValue) {
        print(aValue.getFloatValue());
    }

    public void print(DoubleValue aValue) {
        print(aValue.getDoubleValue());
    }

    public void print(StringValue aValue) {
        String theData = JSWriterUtils.toArray(aValue.getStringValue().getBytes());
        print("bytecoder.newString(");
        print(theData);
        print(")");
    }

    public void print(ArrayLengthValue aValue) {
        print((Value) aValue.resolveFirstArgument());
        print(".data.length");
    }

    public void printArrayIndexReference(Value aValue) {
        print(".data[");
        print(aValue);
        print("]");
    }

    public void print(ArrayEntryValue aValue) {
        Value theArray = aValue.resolveFirstArgument();
        Value theIndex = aValue.resolveSecondArgument();
        print(theArray);
        printArrayIndexReference(theIndex);
    }

    public void print(TypeConversionValue aValue) {
        TypeRef theTargetType = aValue.resolveType();
        Value theValue = aValue.resolveFirstArgument();
        switch (theTargetType.resolve()) {
            case FLOAT:
                print(theValue);
                break;
            case DOUBLE:
                print(theValue);
                break;
            default:
                print("Math.floor(");
                print(theValue);
                print(")");
                break;
        }
    }

    public void print(GetFieldValue aValue) {
        Value theTarget = aValue.resolveFirstArgument();
        BytecodeFieldRefConstant theField = aValue.getField();
        print(theTarget);
        printInstanceFieldReference(theField);
    }

    public void print(BinaryValue aValue) {
        Value theValue1 = aValue.resolveFirstArgument();
        print(theValue1);
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
        print(theValue2);
    }

    public void print(FixedBinaryValue aValue) {
        Value theValue1 = aValue.resolveFirstArgument();
        print(theValue1);
        switch (aValue.getOperator()) {
            case ISNONNULL:
                print(" != null ");
                break;
            case ISZERO:
                print(" == 0 ");
                break;
            case ISNULL:
                print(" == null ");
                break;
            default:
                throw new IllegalStateException("Unsupported operator : " + aValue.getOperator());
        }
    }

    public void print(ByteValue aValue) {
        print(aValue.getByteValue());
    }

    public void print(ValueReferenceValue aValue) {
        Value theValue = aValue.resolveFirstArgument();
        print(theValue);
    }

    public void print(NewObjectValue aValue) {
        print(JSWriterUtils.toClassName(aValue.getType()));
        print(".emptyInstance()");
    }

    public void print(InvokeStaticMethodValue aValue) {
        String theMethodName = aValue.getMethodName();
        BytecodeMethodSignature theSignature = aValue.getSignature();

        List<Value> theVariables = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);

        print(JSWriterUtils.toClassName(aValue.getClassName()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
        print("(");

        for (int i = 0; i < theVariables.size(); i++) {
            if (i> 0) {
                print(",");
            }
            print(theVariables.get(i));
        }
        print(")");
    }

    public void print(DirectInvokeMethodValue aValue) {

        String theMethodName = aValue.getMethodName();
        BytecodeMethodSignature theSignature = aValue.getSignature();
        Value theTarget = aValue.consumedValues(Value.ConsumptionType.INVOCATIONTARGET).get(0);
        List<Value> theArguments = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);

        print(JSWriterUtils.toClassName(aValue.getClazz()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
        print("(");

        print(theTarget);

        for (Value theArgument : theArguments) {
            print(",");
            print(theArgument);
        }
        print(")");
    }

    public void print(InvokeVirtualMethodValue aValue) {
        String theMethodName = aValue.getMethodName();
        BytecodeMethodSignature theSignature = aValue.getSignature();

        Value theTarget = aValue.consumedValues(Value.ConsumptionType.INVOCATIONTARGET).get(0);
        List<Value> theArguments = aValue.consumedValues(Value.ConsumptionType.ARGUMENT);

        BytecodeVirtualMethodIdentifier theMethodIdentifier = linkerContext.getMethodCollection().identifierFor(theMethodName, theSignature);

        if (aValue.getMethodName().equals("invokeWithMagicBehindTheScenes")) {
            print("(");
        } else {
            print(theTarget);
            print(".clazz.resolveVirtualMethod(");
            print(theMethodIdentifier.getIdentifier());
            print(")(");
        }

        print(theTarget);
        for (Value theArgument : theArguments) {
            print(",");
            print(theArgument);
        }
        print(")");
    }

    public void print(NullValue aValue) {
        print("null");
    }

    public void print(GetStaticValue aValue) {
        printStaticFieldReference(aValue.getField());
    }

    public void printVariableNameOrValue(Variable aVariable) {
        if (aVariable.getValue() instanceof PrimitiveValue) {
            print(aVariable.getValue());
        } else {
            print(aVariable.getName());
        }
    }

    public void printStaticFieldReference(BytecodeFieldRefConstant aField) {
        print(JSWriterUtils.toClassName(aField.getClassIndex().getClassConstant()));
        print(".staticFields.");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    public void printInstanceFieldReference(BytecodeFieldRefConstant aField) {
        print(".");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    public String generateJumpCodeFor(BytecodeOpcodeAddress aTarget) {
        return "currentLabel = " + aTarget.getAddress()+";continue controlflowloop;";
    }

    private Value resolveRealValue(Value aValue) {
        if (aValue instanceof ValueReferenceValue) {
            ValueReferenceValue theRef = (ValueReferenceValue) aValue;
            return resolveRealValue(theRef.resolveFirstArgument());
        }
        return aValue;
    }

    private Value resolveRealValue(Variable aVariable) {
        return resolveRealValue(aVariable.getValue());
    }

    public void writeExpressions(ExpressionList aExpressions) {
        for (Expression theExpression : aExpressions.toList()) {
            if (theExpression instanceof ReturnExpression) {
                ReturnExpression theE = (ReturnExpression) theExpression;
                print("return");
                println(";");
            } else if (theExpression instanceof CommentExpression) {
                CommentExpression theE = (CommentExpression) theExpression;
                print("// ");
                println(theE.getValue());
            } else if (theExpression instanceof InitVariableExpression) {
                InitVariableExpression theE = (InitVariableExpression) theExpression;
                Variable theVariable = theE.getVariable();
                if (theVariable.getValue() instanceof PHIFunction) {
                    print("// ");
                    printVariableNameOrValue(theVariable);
                    println(" is PHI function and initialized from predecessor block in flow graph");
                } else {
                    if (theVariable.getValue() instanceof ComputedMemoryLocationWriteValue) {
                        continue;
                    }
                    if (theVariable.getValue() instanceof PrimitiveValue) {
                        continue;
                    }

                    if (!program.isGlobalVariable(theVariable)) {
                        print("var ");
                    }

                    print(theVariable.getName());
                    print(" = ");
                    print(theVariable.getValue());
                    print("; // type is ");
                    println(theVariable.resolveType().resolve().name() + " value type is " + theVariable.getValue().resolveType());
                }
            } else if (theExpression instanceof PutStaticExpression) {
                PutStaticExpression theE = (PutStaticExpression) theExpression;
                BytecodeFieldRefConstant theField = theE.getField();
                Value theValue = theE.getValue();
                printStaticFieldReference(theField);
                print(" = ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof ReturnValueExpression) {
                ReturnValueExpression theE = (ReturnValueExpression) theExpression;
                Value theValue = theE.getValue();
                print("return ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof ThrowExpression) {
                ThrowExpression theE = (ThrowExpression) theExpression;
                Value theValue = theE.getValue();
                print("throw ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof InvokeVirtualMethodExpression) {
                InvokeVirtualMethodExpression theE = (InvokeVirtualMethodExpression) theExpression;
                print(theE.getValue());
                println(";");
            } else if (theExpression instanceof DirectInvokeMethodExpression) {
                DirectInvokeMethodExpression theE = (DirectInvokeMethodExpression) theExpression;
                print(theE.getValue());
                println(";");
            } else if (theExpression instanceof InvokeStaticMethodExpression) {
                InvokeStaticMethodExpression theE = (InvokeStaticMethodExpression) theExpression;
                print(theE.getValue());
                println(";");
            } else if (theExpression instanceof PutFieldExpression) {
                PutFieldExpression theE = (PutFieldExpression) theExpression;
                Value theTarget = theE.getTarget();
                BytecodeFieldRefConstant theField = theE.getField();
                Value thevalue = theE.getValue();
                print(theTarget);
                printInstanceFieldReference(theField);
                print(" = ");
                print(thevalue);
                println(";");
            } else if (theExpression instanceof IFExpression) {
                IFExpression theE = (IFExpression) theExpression;
                print("if (");
                print(theE.getBooleanValue());
                println(") {");

                withDeeperIndent().writeExpressions(theE.getExpressions());

                println("}");

            } else if (theExpression instanceof GotoExpression) {
                GotoExpression theE = (GotoExpression) theExpression;
                BlockState theFinalState = theE.getSourceBlock().toFinalState();

/*                for (Map.Entry<VariableDescription, Variable> theEntry : theFinalState.getPorts().entrySet()) {
                    print(" // ");
                    printVariableNameOrValue(theEntry.getAddress());
                    print(" exported as ");
                    println(theEntry.getKey().toString());
                }*/

                println(generateJumpCodeFor(theE.getJumpTarget()));
            } else if (theExpression instanceof ArrayStoreExpression) {
                ArrayStoreExpression theE = (ArrayStoreExpression) theExpression;
                Value theArray = theE.getArray();
                Value theIndex = theE.getIndex();
                Value theValue = theE.getValue();
                print(theArray);
                printArrayIndexReference(theIndex);
                print(" = ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof CheckCastExpression) {
                CheckCastExpression theE = (CheckCastExpression) theExpression;
                // Completely ignored
            } else if (theExpression instanceof TableSwitchExpression) {
                TableSwitchExpression theE = (TableSwitchExpression) theExpression;
                Value theValue = theE.getValue();

                print("if (");
                print(theValue);
                print(" < ");
                print(theE.getLowValue());
                print(" || ");
                print(theValue);
                print(" > ");
                print(theE.getHighValue());
                println(") {");
                print(" ");

                writeExpressions(theE.getDefaultExpressions());

                println("}");
                print("switch(");
                print(theValue);
                print(" - ");
                print(theE.getLowValue());
                println(") {");

                for (Map.Entry<Long, ExpressionList> theEntry : theE.getOffsets().entrySet()) {
                    print(" case ");
                    print(theEntry.getKey());
                    println(":");
                    print("     ");
                    writeExpressions(theEntry.getValue());
                }

                println("}");
                println("throw 'Illegal jump target!';");
            } else if (theExpression instanceof LookupSwitchExpression) {
                LookupSwitchExpression theE = (LookupSwitchExpression) theExpression;
                print("switch(");
                print(theE.getValue());
                println(") {");

                for (Map.Entry<Long, ExpressionList> theEntry : theE.getPairs().entrySet()) {
                    print(" case ");
                    print(theEntry.getKey());
                    println(":");

                    print("     ");
                    writeExpressions(theEntry.getValue());
                }

                println("}");

                writeExpressions(theE.getDefaultExpressions());
            } else if (theExpression instanceof SetMemoryLocationExpression) {
                SetMemoryLocationExpression theE = (SetMemoryLocationExpression) theExpression;

                if (theE.getAddress() instanceof Variable) {
                    print(((Variable) theE.getAddress()).getValue());
                } else {
                    print(theE.getAddress());
                }

                print(" = ");

                print(theE.getValue());
                println(";");
            } else if (theExpression instanceof InlinedNodeExpression) {
                InlinedNodeExpression theInlined = (InlinedNodeExpression) theExpression;
                GraphNode theInlinedNode = theInlined.getNode();
                printlnComment("Inlined node " + theInlinedNode.getStartAddress().getAddress());
                writeExpressions(theInlinedNode.getExpressions());
            } else if (theExpression instanceof UnreachableExpression) {
                println("throw 'Unreachable';");
            } else {
                throw new IllegalStateException("Not implemented : " + theExpression);
            }
        }
    }

    public void print(ControlFlowGraph.Node aNode) {
        if (aNode instanceof ControlFlowGraph.SequenceOfSimpleNodes) {
            printSimpleSequenceNode((ControlFlowGraph.SequenceOfSimpleNodes) aNode);
            return;
        }
        if (aNode instanceof ControlFlowGraph.SimpleNode) {
            printSimpleNode((ControlFlowGraph.SimpleNode) aNode);
            return;
        }
        throw new IllegalArgumentException("Not supported node type : " + aNode.getClass());
    }

    public void printSimpleSequenceNode(ControlFlowGraph.SequenceOfSimpleNodes aSequence) {
        List<ControlFlowGraph.SimpleNode> theNodes = aSequence.getNodes();
        println();
        println(
                "var currentLabel = " + theNodes.get(0).getNode().getStartAddress().getAddress()
                        + ";");
        println("controlflowloop: while(true) {switch(currentLabel) {");

        for (ControlFlowGraph.SimpleNode theBlock : theNodes) {

            GraphNode theGraphNode = theBlock.getNode();

            println("    case " + theGraphNode.getStartAddress().getAddress() + ": {");

            JSSSAWriter theJSWriter = withDeeperIndent().withDeeperIndent();

            for (Map.Entry<VariableDescription, Value> theImported : theGraphNode.toStartState().getPorts()
                    .entrySet()) {
                theJSWriter.print("// ");
                theJSWriter.print(theImported.getValue());
                theJSWriter.print(" is imported as ");
                theJSWriter
                        .println(theImported.getKey().toString() + " and type " + theImported.getValue().resolveType());
            }

            for (GraphNode thePrececessor : theGraphNode.getPredecessors()) {
                theJSWriter.printlnComment(
                        "Predecessor of this block is " + thePrececessor.getStartAddress().getAddress());
            }
            for (GraphNode theSuccessor : theGraphNode.getSuccessors()) {
                theJSWriter
                        .printlnComment("Successor of this block is " + theSuccessor.getStartAddress().getAddress());
            }

            theJSWriter.printGraphNode(theGraphNode);

    /*                    for (Map.Entry<VariableDescription, Variable> theExported : theBlock.toFinalState().getPorts().entrySet()) {
                            theJSWriter.print("// ");
                            theJSWriter.printVariableNameOrValue(theExported.getAddress());
                            theJSWriter.print(" is exported as ");
                            theJSWriter.println(theExported.getKey().toString());
                        }*/

            println("    }");
        }
        println("    default: throw 'Illegal state exception ' + currentLabel;");
        println("}}");
    }

    public void printSimpleNode(ControlFlowGraph.SimpleNode aSimpleNode) {
        printGraphNode(aSimpleNode.getNode());
    }

    private void printGraphNode(GraphNode aNode) {
        switch (aNode.getType()) {
            case INFINITELOOP: {
                println("while (true) {");

                withDeeperIndent().writeExpressions(aNode.getExpressions());

                println("}");
                break;
            }
            default: {
                writeExpressions(aNode.getExpressions());
                break;
            }
        }
    }
}
