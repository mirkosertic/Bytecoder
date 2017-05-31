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

import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.ssa.ArrayEntryValue;
import de.mirkosertic.bytecoder.ssa.ArrayLengthValue;
import de.mirkosertic.bytecoder.ssa.BinaryValue;
import de.mirkosertic.bytecoder.ssa.ByteValue;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.CompareValue;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodValue;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.ExternalReferenceValue;
import de.mirkosertic.bytecoder.ssa.FixedBinaryValue;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.GetFieldValue;
import de.mirkosertic.bytecoder.ssa.GetStaticValue;
import de.mirkosertic.bytecoder.ssa.InstanceOfValue;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodValue;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodValue;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.NegatedValue;
import de.mirkosertic.bytecoder.ssa.NewArrayValue;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayValue;
import de.mirkosertic.bytecoder.ssa.NewObjectValue;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TypeConversionValue;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableReferenceValue;

import java.io.PrintWriter;
import java.util.List;

public class JSSSAWriter extends JSWriter {

    private final BytecodeLinkerContext linkerContext;

    public JSSSAWriter(String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext) {
        super(aIndent, aWriter);
        linkerContext = aLinkerContext;
    }

    public void print(Value aValue) {
        if (aValue instanceof ExternalReferenceValue) {
            print((ExternalReferenceValue) aValue);
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
        } else if (aValue instanceof VariableReferenceValue) {
            print((VariableReferenceValue) aValue);
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
        } else {
            throw new IllegalStateException("Not implemented : " + aValue);
        }
    }

    public void print(NewMultiArrayValue aValue) {
        //TODO: replace this bad implementation
        BytecodeTypeRef theType = aValue.getType();
        Object theDefaultValue = theType.defaultValue();
        String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newArray(");
        print(1);
        print(",");
        print(theStrDefault);
        print(")");
    }

    public void print(ClassReferenceValue aValue) {
        print(JSWriterUtils.toClassName(aValue.getType()));
        print(".runtimeClass");
    }

    public void print(InstanceOfValue aValue) {
        Variable theVariable = aValue.getVariable();
        print("(");
        printVariableName(theVariable);
        print(" == null ? false : ");
        printVariableName(theVariable);
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
        Variable theVariable = aValue.getVariable();
        print("(-");
        printVariableName(theVariable);
        print(")");
    }

    public void print(CompareValue aValue) {
        Variable theVariable1 = aValue.getValue1();
        Variable theVariable2 = aValue.getValue2();
        print("(");
        printVariableName(theVariable1);
        print(" > ");
        printVariableName(theVariable2);
        print(" ? 1 ");
        print(" : (");
        printVariableName(theVariable1);
        print(" < ");
        printVariableName(theVariable2);
        print(" ? -1 : 0))");
    }

    public void print(NewArrayValue aValue) {
        BytecodeTypeRef theType = aValue.getType();
        Variable theLength = aValue.getLength();
        Object theDefaultValue = theType.defaultValue();
        String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newArray(");
        printVariableName(theLength);
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
        Variable theArray = aValue.getArray();
        printVariableName(theArray);
        print(".data.length");
    }

    public void printArrayIndexReference(Variable aVariable) {
        print(".data[");
        printVariableName(aVariable);
        print("]");
    }

    public void print(ArrayEntryValue aValue) {
        Variable theArray = aValue.getArray();
        Variable theIndex = aValue.getIndex();
        printVariableName(theArray);
        printArrayIndexReference(theIndex);
    }

    public void print(TypeConversionValue aValue) {
        BytecodePrimitiveTypeRef theTargetType = aValue.getTargetType();
        Variable theValue = aValue.getVariable();
        switch (theTargetType) {
            case FLOAT:
                printVariableName(theValue);
                break;
            case DOUBLE:
                printVariableName(theValue);
                break;
            default:
                print("Math.round(");
                printVariableName(theValue);
                print(")");
                break;
        }
    }

    public void print(GetFieldValue aValue) {
        Variable theTarget = aValue.getTarget();
        BytecodeFieldRefConstant theField = aValue.getField();
        printVariableName(theTarget);
        printInstanceFieldReference(theField);
    }

    public void print(BinaryValue aValue) {
        Variable theValue1 = aValue.getValue1();
        printVariableName(theValue1);
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
        Variable theValue2 = aValue.getValue2();
        printVariableName(theValue2);
    }

    public void print(FixedBinaryValue aValue) {
        Variable theValue1 = aValue.getValue1();
        printVariableName(theValue1);
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

    public void print(VariableReferenceValue aValue) {
        Variable theVariable = aValue.getVariable();
        printVariableName(theVariable);
    }

    public void print(NewObjectValue aValue) {
        print(JSWriterUtils.toClassName(aValue.getType()));
        print(".emptyInstance()");
    }

    public void print(InvokeStaticMethodValue aValue) {
        String theMethodName = aValue.getMethod().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        BytecodeMethodSignature theSignature = aValue.getMethod().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        List<Variable> theVariables = aValue.getArguments();

        print(JSWriterUtils.toClassName(aValue.getMethod().getClassIndex().getClassConstant()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
        print("(");

        for (int i = 0; i < theVariables.size(); i++) {
            if (i> 0) {
                print(",");
            }
            printVariableName(theVariables.get(i));
        }
        print(")");
    }

    public void print(DirectInvokeMethodValue aValue) {
        String theMethodName = aValue.getMethod().getNameIndex().getName().stringValue();
        BytecodeMethodSignature theSignature = aValue.getMethod().getDescriptorIndex().methodSignature();
        Variable theTarget = aValue.getTarget();
        List<Variable> theVariables = aValue.getArguments();

        print(JSWriterUtils.toClassName(aValue.getClassInfo()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
        print("(");

        printVariableName(theTarget);
        for (int i = 0; i < theVariables.size(); i++) {
            print(",");
            printVariableName(theVariables.get(i));
        }
        print(")");
    }

    public void print(InvokeVirtualMethodValue aValue) {
        String theMethodName = aValue.getMethod().getNameIndex().getName().stringValue();
        BytecodeMethodSignature theSignature = aValue.getMethod().getDescriptorIndex().methodSignature();
        Variable theTarget = aValue.getTarget();
        List<Variable> theVariables = aValue.getArguments();

        BytecodeVirtualMethodIdentifier theMethodIdentifier = linkerContext.getMethodCollection().identifierFor(theMethodName, theSignature);

        printVariableName(theTarget);
        print(".clazz.resolveVirtualMethod(");
        print(theMethodIdentifier.getIdentifier());
        print(")(");

        printVariableName(theTarget);
        for (int i = 0; i < theVariables.size(); i++) {
            print(",");
            printVariableName(theVariables.get(i));
        }
        print(")");
    }

    public void print(NullValue aValue) {
        print("null");
    }

    public void print(GetStaticValue aValue) {
        printStaticFieldReference(aValue.getField());
    }

    public void print(ExternalReferenceValue aValue) {
        print("frame.local");
        print(aValue.getVariableIndex() + 1);
    }

    public void printVariableName(Variable aVariable) {
        print("blockLocal");
        print(aVariable.getIndex() + 1);
    }

    public void printStaticFieldReference(BytecodeFieldRefConstant aField) {
        print(JSWriterUtils.toClassName(aField.getClassIndex().getClassConstant()));
        print(".staticFields.");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    public void printInstanceFieldReference(BytecodeFieldRefConstant aField) {
        print(".data.");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }
}
