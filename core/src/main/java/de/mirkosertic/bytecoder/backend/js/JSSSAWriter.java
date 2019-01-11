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

import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.api.OpaqueIndexed;
import de.mirkosertic.bytecoder.api.OpaqueMethod;
import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
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
import de.mirkosertic.bytecoder.ssa.FloatingPointCeilExpression;
import de.mirkosertic.bytecoder.ssa.FloatingPointFloorExpression;
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
import de.mirkosertic.bytecoder.ssa.MaxExpression;
import de.mirkosertic.bytecoder.ssa.MemorySizeExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.MethodRefExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.MinExpression;
import de.mirkosertic.bytecoder.ssa.NegatedExpression;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
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
import de.mirkosertic.bytecoder.ssa.UnknownExpression;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JSSSAWriter extends IndentSSAWriter {

    private final ConstantPool constantPool;
    private boolean labelRequired;

    public JSSSAWriter(final CompileOptions aOptions, final Program aProgram, final String aIndent, final PrintWriter aWriter, final BytecodeLinkerContext aLinkerContext,
            final ConstantPool aConstantPool, final boolean aLabelRequired) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        constantPool = aConstantPool;
        labelRequired = aLabelRequired;
    }

    private JSSSAWriter withDeeperIndent() {
        return new JSSSAWriter(options, program, indent + "    ", writer, linkerContext, constantPool, labelRequired);
    }

    private void print(final Value aValue) {
        if (aValue instanceof Variable) {
            printVariableName((Variable) aValue);
        } else if (aValue instanceof GetStaticExpression) {
            print((GetStaticExpression) aValue);
        } else if (aValue instanceof NullValue) {
            print((NullValue) aValue);
        } else if (aValue instanceof InvokeVirtualMethodExpression) {
            print((InvokeVirtualMethodExpression) aValue);
        } else if (aValue instanceof InvokeStaticMethodExpression) {
            print((InvokeStaticMethodExpression) aValue);
        } else if (aValue instanceof NewObjectExpression) {
            print((NewObjectExpression) aValue);
        } else if (aValue instanceof ByteValue) {
            print((ByteValue) aValue);
        } else if (aValue instanceof BinaryExpression) {
            print((BinaryExpression) aValue);
        } else if (aValue instanceof GetFieldExpression) {
            print((GetFieldExpression) aValue);
        } else if (aValue instanceof TypeConversionExpression) {
            print((TypeConversionExpression) aValue);
        } else if (aValue instanceof ArrayEntryExpression) {
            print((ArrayEntryExpression) aValue);
        } else if (aValue instanceof ArrayLengthExpression) {
            print((ArrayLengthExpression) aValue);
        } else if (aValue instanceof StringValue) {
            print((StringValue) aValue);
        } else if (aValue instanceof IntegerValue) {
            print((IntegerValue) aValue);
        } else if (aValue instanceof NewArrayExpression) {
            print((NewArrayExpression) aValue);
        } else if (aValue instanceof DirectInvokeMethodExpression) {
            print((DirectInvokeMethodExpression) aValue);
        } else if (aValue instanceof FloatValue) {
            print((FloatValue) aValue);
        } else if (aValue instanceof DoubleValue) {
            print((DoubleValue) aValue);
        } else if (aValue instanceof CompareExpression) {
            print((CompareExpression) aValue);
        } else if (aValue instanceof NegatedExpression) {
            print((NegatedExpression) aValue);
        } else if (aValue instanceof FixedBinaryExpression) {
            print((FixedBinaryExpression) aValue);
        } else if (aValue instanceof ShortValue) {
            print((ShortValue) aValue);
        } else if (aValue instanceof InstanceOfExpression) {
            print((InstanceOfExpression) aValue);
        } else if (aValue instanceof LongValue) {
            print((LongValue) aValue);
        } else if (aValue instanceof ClassReferenceValue) {
            print((ClassReferenceValue) aValue);
        } else if (aValue instanceof NewMultiArrayExpression) {
            print((NewMultiArrayExpression) aValue);
        } else if (aValue instanceof SelfReferenceParameterValue) {
            print((SelfReferenceParameterValue) aValue);
        } else if (aValue instanceof MethodParameterValue) {
            print((MethodParameterValue) aValue);
        } else if (aValue instanceof CurrentExceptionExpression) {
            print((CurrentExceptionExpression) aValue);
        } else if (aValue instanceof UnknownExpression) {
            print((UnknownExpression) aValue);
        } else if (aValue instanceof FloorExpression) {
            print((FloorExpression) aValue);
        } else if (aValue instanceof MethodRefExpression) {
            print((MethodRefExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationReadExpression) {
            print((ComputedMemoryLocationReadExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationWriteExpression) {
            print((ComputedMemoryLocationWriteExpression) aValue);
        } else if (aValue instanceof MethodHandlesGeneratedLookupExpression) {
            print((MethodHandlesGeneratedLookupExpression) aValue);
        } else if (aValue instanceof MethodTypeExpression) {
            print((MethodTypeExpression) aValue);
        } else if (aValue instanceof RuntimeGeneratedTypeExpression) {
            print((RuntimeGeneratedTypeExpression) aValue);
        } else if (aValue instanceof ResolveCallsiteObjectExpression) {
            print((ResolveCallsiteObjectExpression) aValue);
        } else if (aValue instanceof StackTopExpression) {
            print((StackTopExpression) aValue);
        } else if (aValue instanceof MemorySizeExpression) {
            print((MemorySizeExpression) aValue);
        } else if (aValue instanceof TypeOfExpression) {
            print((TypeOfExpression) aValue);
        } else if (aValue instanceof SqrtExpression) {
            print((SqrtExpression) aValue);
        } else if (aValue instanceof MaxExpression) {
            print((MaxExpression) aValue);
        } else if (aValue instanceof MinExpression) {
            print((MinExpression) aValue);
        } else if (aValue instanceof FloatingPointFloorExpression) {
            print((FloatingPointFloorExpression) aValue);
        } else if (aValue instanceof FloatingPointCeilExpression) {
            print((FloatingPointCeilExpression) aValue);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue);
        }
    }

    private void print(final MaxExpression aValue) {
        print("Math.max(");
        print(aValue.incomingDataFlows().get(0));
        print(",");
        print(aValue.incomingDataFlows().get(1));
        print(")");
    }

    private void print(final MinExpression aValue) {
        print("Math.min(");
        print(aValue.incomingDataFlows().get(0));
        print(",");
        print(aValue.incomingDataFlows().get(1));
        print(")");
    }

    private void print(final SqrtExpression aValue) {
        print("Math.sqrt(");
        print(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void print(final TypeOfExpression aValue) {
        print(aValue.incomingDataFlows().get(0));
        print(".ClassgetClass()");
    }

    private void print(final StackTopExpression aValue) {
        print("0");
    }

    private void print(final MemorySizeExpression aValue) {
        print("0");
    }

    private void print(final ResolveCallsiteObjectExpression aValue) {
        print("bytecoder.resolveStaticCallSiteObject(");
        print(JSWriterUtils.toClassName(aValue.getOwningClass().getThisInfo()));
        print(",'");
        print(aValue.getCallsiteId());
        println("', function() {");

        final Program theProgram = aValue.getProgram();
        final RegionNode theBootstrapCode = aValue.getBootstrapMethod();

        final JSSSAWriter theNested = withDeeperIndent();

        for (final Variable theVariable : theProgram.globalVariables()) {
            theNested.print("var ");
            theNested.print(theVariable.getName());
            theNested.println(" = null;");
        }

        theNested.writeExpressions(theBootstrapCode.getExpressions());

        print("})");
    }

    private void print(final RuntimeGeneratedTypeExpression aValue) {
        print("bytecoder.dynamicType(");
        print(aValue.getMethodRef());
        print(",");
        print(aValue.getStaticArguments());
        print(")");
    }

    private void print(final MethodTypeExpression aValue) {
        print("'");
        print(aValue.getSignature().toString());
        print("'");
    }

    private void print(final MethodHandlesGeneratedLookupExpression aValue) {
        print("null");
    }

    private void print(final ComputedMemoryLocationWriteExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        print(theIncomingData.get(0));
        print(" + ");
        print(theIncomingData.get(1));
    }

    private void print(final ComputedMemoryLocationReadExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        print("bytecoderGlobalMemory[");
        print(theIncomingData.get(0));
        print(" + ");
        print(theIncomingData.get(1));
        print("]");
    }

    private void print(final MethodRefExpression aValue) {
        final String theMethodName = aValue.getMethodName();
        final BytecodeMethodSignature theSignature = aValue.getSignature();
        print(JSWriterUtils.toClassName(aValue.getClassName()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
    }

    private void print(final FloorExpression aValue) {
        print("Math.floor(");
        print(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void print(final FloatingPointFloorExpression aValue) {
        print("Math.floor(");
        print(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void print(final FloatingPointCeilExpression aValue) {
        print("Math.ceil(");
        print(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void print(final UnknownExpression aValue) {
        print("undefined");
    }

    private void print(final CurrentExceptionExpression aValue) {
        print("CURRENTEXCEPTION");
    }

    private void print(final MethodParameterValue aValue) {
        print("p" + (aValue.getParameterIndex() + 1));
    }

    private void print(final SelfReferenceParameterValue aValue) {
        print("thisRef");
    }

    private void print(final NewMultiArrayExpression aValue) {
        final BytecodeTypeRef theType = aValue.getType();
        final Object theDefaultValue = theType.defaultValue();
        final String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newMultiArray(");
        print("[");
        final List<Value> theDimensions = aValue.incomingDataFlows();
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

    private void print(final ClassReferenceValue aValue) {
        print(JSWriterUtils.toClassName(aValue.getType()));
    }

    private void print(final InstanceOfExpression aValue) {
        final Value theValue = aValue.incomingDataFlows().get(0);
        print("(");
        print(theValue);
        print(" == null ? false : ");
        print(theValue);
        print(".instanceOf(");

        final BytecodeUtf8Constant theConstant = aValue.getType().getConstant();
        if (!theConstant.stringValue().startsWith("[")) {
            final BytecodeLinkedClass theLinkedClass = linkerContext.isLinkedOrNull(aValue.getType().getConstant());
            print(JSWriterUtils.toClassName(theLinkedClass.getClassName()));
        } else {
            final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
            print(JSWriterUtils.toClassName(theLinkedClass.getClassName()));
        }

        print(")");
        print(")");
    }

    private void print(final LongValue aValue) {
        print(aValue.getLongValue());
    }

    private void print(final ShortValue aValue) {
        print(aValue.getShortValue());
    }

    private void print(final NegatedExpression aValue) {
        final Value theValue = aValue.incomingDataFlows().get(0);
        print("(-");
        print(theValue);
        print(")");
    }

    private void print(final CompareExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        final Value theVariable1 = theIncomingData.get(0);
        final Value theVariable2 = theIncomingData.get(1);
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

    private void print(final NewArrayExpression aValue) {
        final BytecodeTypeRef theType = aValue.getType();
        final Value theLength =aValue.incomingDataFlows().get(0);
        final Object theDefaultValue = theType.defaultValue();
        final String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newArray(");
        print(theLength);
        print(",");
        print(theStrDefault);
        print(")");
    }

    private void print(final IntegerValue aValue) {
        print(aValue.getIntValue());
    }

    private void print(final FloatValue aValue) {
        print(aValue.getFloatValue());
    }

    private void print(final DoubleValue aValue) {
        print(aValue.getDoubleValue());
    }

    private void print(final StringValue aValue) {
        final int theIndex = constantPool.register(aValue);
        print("bytecoder.stringpool[");
        print(theIndex);
        print("]");
    }

    private void print(final ArrayLengthExpression aValue) {
        print(aValue.incomingDataFlows().get(0));
        print(".data.length");
    }

    private void printArrayIndexReference(final Value aValue) {
        print(".data[");
        print(aValue);
        print("]");
    }

    private void print(final ArrayEntryExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        final Value theArray = theIncomingData.get(0);
        final Value theIndex = theIncomingData.get(1);
        print(theArray);
        printArrayIndexReference(theIndex);
    }

    private void print(final TypeConversionExpression aValue) {
        final TypeRef theTargetType = aValue.resolveType();
        final Value theValue = aValue.incomingDataFlows().get(0);
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

    private void print(final GetFieldExpression aValue) {
        final Value theTarget = aValue.incomingDataFlows().get(0);
        final BytecodeFieldRefConstant theField = aValue.getField();
        print(theTarget);
        printInstanceFieldReference(theField);
    }

    private void print(final BinaryExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        print("(");
        print(theIncomingData.get(0));
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
        print(theIncomingData.get(1));
        print(")");
    }

    private void print(final FixedBinaryExpression aValue) {
        final Value theValue1 = aValue.incomingDataFlows().get(0);
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

    private void print(final ByteValue aValue) {
        print(aValue.getByteValue());
    }

    private void print(final NewObjectExpression aValue) {
        print("new ");
        print(JSWriterUtils.toClassName(aValue.getType()));
        print(".Create()");
    }

    private String conversionFunctionToBytecoderForOpaqueType(final BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            return null;
        } else if (aTypeRef.isArray()) {
            throw new IllegalStateException("Type conversion to " + aTypeRef.name() + " is not supported!");
        } else if (aTypeRef.matchesExactlyTo(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
            return("bytecoder.toBytecoderString");
        } else {
            final BytecodeObjectTypeRef theObjectType = (BytecodeObjectTypeRef) aTypeRef;
            final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(theObjectType);
            if (theLinkedClass.isOpaqueType()) {
                return null;
            } else {
                throw new IllegalStateException("Type conversion from " + aTypeRef.name() + " is not supported!");
            }
        }
    }

    private void printToJSConvertedValue(final BytecodeTypeRef aTypeRef, final Value aValue) {
        if (aTypeRef.isPrimitive()) {
            print(aValue);
        } else if (aTypeRef.isArray()) {
            throw new IllegalStateException("Type conversion to " + aTypeRef.name() + " is not supported!");
        } else if (aTypeRef.matchesExactlyTo(BytecodeObjectTypeRef.fromRuntimeClass(String.class))) {
            print("bytecoder.toJSString(");
            print(aValue);
            print(")");
        } else {
            final BytecodeObjectTypeRef theObjectType = (BytecodeObjectTypeRef) aTypeRef;
            final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(theObjectType);
            if (theLinkedClass.isOpaqueType()) {
                print(aValue);
            } else if (theLinkedClass.isCallback()) {

                final BytecodeResolvedMethods theMethods = theLinkedClass.resolvedMethods();
                final List<BytecodeMethod> availableCallbacks = theMethods.stream().filter(t -> !t.getValue().isConstructor() && !t.getValue().isClassInitializer()
                        && !t.getProvidingClass().getClassName().name().equals(Object.class.getName())).map(t -> t.getValue()).collect(Collectors.toList());
                if (availableCallbacks.size() != 1) {
                    throw new IllegalStateException("Invalid number of callback methods available for type " + theLinkedClass.getClassName().name() + ", expected 1, got " + availableCallbacks.size());
                }

                final BytecodeMethod theCallbackMethod = availableCallbacks.get(0);
                final String theMethodName = JSWriterUtils.toMethodName(theCallbackMethod.getName().stringValue(), theCallbackMethod.getSignature());

                print("function() {");
                print("var v = ");
                print(aValue);
                print(";var args = Array.prototype.slice.call(arguments);v");
                print(".");
                print(theMethodName);
                print("(v");
                final BytecodeTypeRef[] theArguments = theCallbackMethod.getSignature().getArguments();
                for (int i=0;i<theArguments.length;i++) {
                    print(",");
                    final String theConversionFunction = conversionFunctionToBytecoderForOpaqueType(theArguments[i]);
                    if (theConversionFunction != null) {
                        print(theConversionFunction);
                        print("(");
                        print("args[");
                        print(i);
                        print("])");
                    } else {
                        print("args[");
                        print(i);
                        print("]");
                    }
                }
                print(");");
                print("}");
            } else {
                throw new IllegalStateException("Type conversion to " + aTypeRef.name() + " is not supported!");
            }
        }
    }

    private void print(final InvokeStaticMethodExpression aValue) {

        final BytecodeLinkedClass theClass = linkerContext.resolveClass(aValue.getClassName());
        final String theMethodName = aValue.getMethodName();
        final BytecodeMethodSignature theSignature = aValue.getSignature();

        if (theClass.isOpaqueType()) {
            // It is an opaque type
            // In this case, the method can be annotated with an @Import
            // or just be native
            final BytecodeMethod theMethod = theClass.getBytecodeClass().methodByNameAndSignatureOrNull(theMethodName, theSignature);
            final BytecodeAnnotation theImport = theMethod.getAttributes().getAnnotationByType(Import.class.getName());
            if (theImport != null) {
                // Method implementation is provided the runtime,we need special handling
                final String theModuleName = theImport.getElementValueByName("module").stringValue();
                final String theObjectName = theImport.getElementValueByName("name").stringValue();

                if (theSignature.getReturnType().isVoid()) {
                    throw new IllegalStateException("Don't know how to handle imported method with return type void!");
                }

                final String theReturnConvertFunction = conversionFunctionToBytecoderForOpaqueType(theSignature.getReturnType());
                if (theReturnConvertFunction != null) {
                    print(theReturnConvertFunction);
                    print("(");
                }
                print("bytecoder.imports.");
                print(theModuleName);
                print(".");
                print(theObjectName);
                print("(");

                final List<Value> theVariables = aValue.incomingDataFlows();

                for (int i = 0; i < theVariables.size(); i++) {
                    if (i> 0) {
                        print(",");
                    }
                    printToJSConvertedValue(theSignature.getArguments()[i], theVariables.get(i));
                }
                print(")");

                if (theReturnConvertFunction != null) {
                    print(")");
                }
            } else {
                // We continue the normal flow, as method implementation is provided by the bytecode
                print(JSWriterUtils.toClassName(aValue.getClassName()));
                print(".");
                print(JSWriterUtils.toMethodName(theMethodName, theSignature));
                print("(");

                final List<Value> theVariables = aValue.incomingDataFlows();

                for (int i = 0; i < theVariables.size(); i++) {
                    if (i> 0) {
                        print(",");
                    }
                    print(theVariables.get(i));
                }
                print(")");
            }
        } else {

            print(JSWriterUtils.toClassName(aValue.getClassName()));
            print(".");
            print(JSWriterUtils.toMethodName(theMethodName, theSignature));
            print("(");

            final List<Value> theVariables = aValue.incomingDataFlows();

            for (int i = 0; i < theVariables.size(); i++) {
                if (i> 0) {
                    print(",");
                }
                print(theVariables.get(i));
            }
            print(")");

        }
    }

    private void print(final DirectInvokeMethodExpression aValue) {

        final BytecodeLinkedClass theTargetClass = linkerContext.resolveClass(aValue.getClazz());

        final String theMethodName = aValue.getMethodName();
        final BytecodeMethodSignature theSignature = aValue.getSignature();

        final List<Value> theIncomingData = aValue.incomingDataFlows();
        final Value theTarget = theIncomingData.get(0);
        final List<Value> theArguments = theIncomingData.subList(1, theIncomingData.size());

        final BytecodeMethod theMethod = theTargetClass.getBytecodeClass().methodByNameAndSignatureOrNull(theMethodName, theSignature);

        if (theTargetClass.isOpaqueType() && !theMethod.isConstructor()) {
            writeOpaqueMethodInvocation(theSignature, theTarget, theArguments, theMethod);
        } else {

            if (!"<init>".equals(theMethodName)) {
                print(theTarget);
                print(".");
                print(JSWriterUtils.toMethodName(theMethodName, theSignature));
            } else {
                print(JSWriterUtils.toClassName(aValue.getClazz()));
                print(".");
                print(JSWriterUtils.toMethodName(theMethodName, theSignature));
            }
            print("(");

            print(theTarget);

            for (final Value theArgument : theArguments) {
                print(",");
                print(theArgument);
            }
            print(")");
        }
    }

    private void writeOpaqueMethodInvocation(final BytecodeMethodSignature aMethodSignature, final Value aInvocationTarget, final List<Value> aMethodArguments, final BytecodeMethod aMethodImplementation) {
        final BytecodeAnnotation theSimpleProperty = aMethodImplementation.getAttributes().getAnnotationByType(OpaqueProperty.class.getName());
        final BytecodeAnnotation theIndexedProperty = aMethodImplementation.getAttributes().getAnnotationByType(
                OpaqueIndexed.class.getName());
        if (theIndexedProperty != null) {
            if (aMethodSignature.getReturnType().isVoid()) {
                // Set property
                print(aInvocationTarget);
                print("[");
                printToJSConvertedValue(aMethodSignature.getArguments()[0], aMethodArguments.get(0));
                print("]");
                print("=");

                printToJSConvertedValue(aMethodSignature.getArguments()[1], aMethodArguments.get(1));
            } else {
                final String theReturnConvertFunction = conversionFunctionToBytecoderForOpaqueType(aMethodSignature.getReturnType());
                if (theReturnConvertFunction != null) {
                    print(theReturnConvertFunction);
                    print("(");
                }

                // Get property
                print(aInvocationTarget);
                print("[");
                printToJSConvertedValue(aMethodSignature.getArguments()[0], aMethodArguments.get(0));
                print("]");

                if (theReturnConvertFunction != null) {
                    print(")");
                }
            }

        } else if (theSimpleProperty != null) {
            //
            final String theMethodName = aMethodImplementation.getName().stringValue();
            // This is a property access
            final BytecodeAnnotation.ElementValue theValue = theSimpleProperty.getElementValueByName("value");
            String theOpaquePropertyName;
            if (theValue == null) {
                if (theMethodName.startsWith("get")) {
                    theOpaquePropertyName = theMethodName.substring(3);
                    theOpaquePropertyName = Character.toLowerCase(theOpaquePropertyName.charAt(0)) + theOpaquePropertyName.substring(1);
                } else if (theMethodName.startsWith("is")) {
                    theOpaquePropertyName = theMethodName.substring(2);
                    theOpaquePropertyName = Character.toLowerCase(theOpaquePropertyName.charAt(0)) + theOpaquePropertyName.substring(1);
                } else if (theMethodName.startsWith("set")) {
                    theOpaquePropertyName = theMethodName.substring(3);
                    theOpaquePropertyName = Character.toLowerCase(theOpaquePropertyName.charAt(0)) + theOpaquePropertyName.substring(1);
                } else {
                    theOpaquePropertyName = theMethodName;
                }
            } else {
                theOpaquePropertyName = theValue.stringValue();
            }

            if (aMethodSignature.getReturnType().isVoid()) {
                // Set property
                print(aInvocationTarget);
                print(".");
                print(theOpaquePropertyName);
                print("=");

                printToJSConvertedValue(aMethodSignature.getArguments()[0], aMethodArguments.get(0));
            } else {
                final String theReturnConvertFunction = conversionFunctionToBytecoderForOpaqueType(aMethodSignature.getReturnType());
                if (theReturnConvertFunction != null) {
                    print(theReturnConvertFunction);
                    print("(");
                }

                // Get property
                print(aInvocationTarget);
                print(".");
                print(theOpaquePropertyName);

                if (theReturnConvertFunction != null) {
                    print(")");
                }
            }
        } else {

            final String theReturnConvertFunction = conversionFunctionToBytecoderForOpaqueType(aMethodSignature.getReturnType());
            if (theReturnConvertFunction != null) {
                print(theReturnConvertFunction);
                print("(");
            }

            String theMethodName = aMethodImplementation.getName().stringValue();
            final BytecodeAnnotation theMethod = aMethodImplementation.getAttributes().getAnnotationByType(OpaqueMethod.class.getName());
            if (theMethod != null) {
                theMethodName = theMethod.getElementValueByName("value").stringValue();
            }

            // Simple method invocation
            print(aInvocationTarget);
            print(".");
            print(theMethodName);
            print("(");

            for (int i = 0; i < aMethodArguments.size(); i++) {
                if (i > 0) {
                    print(",");
                }
                printToJSConvertedValue(aMethodSignature.getArguments()[i], aMethodArguments.get(i));
            }

            print(")");

            if (theReturnConvertFunction != null) {
                print(")");
            }
        }
    }

    private void print(final InvokeVirtualMethodExpression aValue) {
        final String theMethodName = aValue.getMethodName();
        final BytecodeMethodSignature theSignature = aValue.getSignature();

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        final Value theTarget = theIncomingData.get(0);
        final List<Value> theArguments = theIncomingData.subList(1, theIncomingData.size());

        // Check if we are invoking something on an opaque type
        final BytecodeVirtualMethodIdentifier theMethodIdentifier = linkerContext.getMethodCollection().identifierFor(theMethodName, theSignature);
        final List<BytecodeLinkedClass> theClasses = linkerContext.getAllClassesAndInterfacesWithMethod(theMethodIdentifier);
        if (theClasses.size() == 1) {
            final BytecodeLinkedClass theTargetClass = theClasses.get(0);
            final BytecodeMethod theMethod = theTargetClass.getBytecodeClass().methodByNameAndSignatureOrNull(theMethodName, theSignature);
            if (theTargetClass.isOpaqueType() && !theMethod.isConstructor()) {
                writeOpaqueMethodInvocation(theSignature, theTarget, theArguments, theMethod);
                return;
            }
        }
        if (Objects.equals(aValue.getMethodName(), "invokeWithMagicBehindTheScenes")) {
            print("(");
        } else {
            print(theTarget);
            print(".");
            print(JSWriterUtils.toMethodName(theMethodName, theSignature));
            print("(");
        }

        print(theTarget);
        for (final Value theArgument : theArguments) {
            print(",");
            print(theArgument);
        }
        print(")");
    }

    private void print(final NullValue aValue) {
        print("null");
    }

    private void print(final GetStaticExpression aValue) {
        printStaticFieldReference(aValue.getField());
    }

    private void printVariableName(final Variable aVariable) {
        print(aVariable.getName());
    }

    private void printStaticFieldReference(final BytecodeFieldRefConstant aField) {
        print(JSWriterUtils.toClassName(aField.getClassIndex().getClassConstant()));
        print(".");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private void printInstanceFieldReference(final BytecodeFieldRefConstant aField) {
        print(".");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private String generateJumpCodeFor(final BytecodeOpcodeAddress aTarget) {
        return "currentLabel = " + aTarget.getAddress()+";continue controlflowloop;";
    }

    public void writeExpressions(final ExpressionList aExpressions) {
        for (final Expression theExpression : aExpressions.toList()) {
            if (options.isDebugOutput()) {
                final String theComment = theExpression.getComment();
                if (theComment != null && !theComment.isEmpty()) {
                    print("// ");
                    println(theComment);
                }
            }
            if (theExpression instanceof ReturnExpression) {
                final ReturnExpression theE = (ReturnExpression) theExpression;
                print("return");
                println(";");
            } else if (theExpression instanceof VariableAssignmentExpression) {
                final VariableAssignmentExpression theE = (VariableAssignmentExpression) theExpression;

                final Variable theVariable = theE.getVariable();
                final Value theValue = theE.getValue();

                if (theValue instanceof ComputedMemoryLocationWriteExpression) {
                    continue;
                }
                if (!program.isGlobalVariable(theVariable)) {
                    print("var ");
                }

                print(theVariable.getName());
                print(" = ");
                print(theValue);
                if (options.isDebugOutput()) {
                    print("; // type is ");
                    println(theVariable.resolveType().resolve().name() + " value type is " + theValue.resolveType());
                } else {
                    println(";");
                }
            } else if (theExpression instanceof PutStaticExpression) {
                final PutStaticExpression theE = (PutStaticExpression) theExpression;
                final BytecodeFieldRefConstant theField = theE.getField();
                final Value theValue = theE.incomingDataFlows().get(0);
                printStaticFieldReference(theField);
                print(" = ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof ReturnValueExpression) {
                final ReturnValueExpression theE = (ReturnValueExpression) theExpression;
                final Value theValue = theE.incomingDataFlows().get(0);
                print("return ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof ThrowExpression) {
                final ThrowExpression theE = (ThrowExpression) theExpression;
                final Value theValue = theE.incomingDataFlows().get(0);
                print("throw ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof InvokeVirtualMethodExpression) {
                final InvokeVirtualMethodExpression theE = (InvokeVirtualMethodExpression) theExpression;
                print(theE);
                println(";");
            } else if (theExpression instanceof DirectInvokeMethodExpression) {
                final DirectInvokeMethodExpression theE = (DirectInvokeMethodExpression) theExpression;
                print(theE);
                println(";");
            } else if (theExpression instanceof InvokeStaticMethodExpression) {
                final InvokeStaticMethodExpression theE = (InvokeStaticMethodExpression) theExpression;
                print(theE);
                println(";");
            } else if (theExpression instanceof PutFieldExpression) {
                final PutFieldExpression theE = (PutFieldExpression) theExpression;

                final List<Value> theIncomingData = theE.incomingDataFlows();

                final Value theTarget = theIncomingData.get(0);
                final BytecodeFieldRefConstant theField = theE.getField();

                final Value thevalue = theIncomingData.get(1);
                print(theTarget);

                printInstanceFieldReference(theField);
                print(" = ");
                print(thevalue);
                println(";");
            } else if (theExpression instanceof IFExpression) {
                final IFExpression theE = (IFExpression) theExpression;
                print("if (");
                print(theE.incomingDataFlows().get(0));
                println(") {");

                withDeeperIndent().writeExpressions(theE.getExpressions());

                println("}");

            } else if (theExpression instanceof GotoExpression) {
                final GotoExpression theE = (GotoExpression) theExpression;
                println(generateJumpCodeFor(theE.getJumpTarget()));
            } else if (theExpression instanceof ArrayStoreExpression) {
                final ArrayStoreExpression theE = (ArrayStoreExpression) theExpression;

                final List<Value> theIncomingData = theE.incomingDataFlows();

                final Value theArray = theIncomingData.get(0);
                final Value theIndex = theIncomingData.get(1);
                final Value theValue = theIncomingData.get(2);
                print(theArray);
                printArrayIndexReference(theIndex);
                print(" = ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof CheckCastExpression) {
                final CheckCastExpression theE = (CheckCastExpression) theExpression;
                // Completely ignored
            } else if (theExpression instanceof TableSwitchExpression) {
                final TableSwitchExpression theE = (TableSwitchExpression) theExpression;
                final Value theValue = theE.incomingDataFlows().get(0);

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

                for (final Map.Entry<Long, ExpressionList> theEntry : theE.getOffsets().entrySet()) {
                    print(" case ");
                    print(theEntry.getKey());
                    println(":");
                    print("     ");
                    writeExpressions(theEntry.getValue());
                }

                println("}");
                println("throw 'Illegal jump target!';");
            } else if (theExpression instanceof LookupSwitchExpression) {
                final LookupSwitchExpression theE = (LookupSwitchExpression) theExpression;
                print("switch(");
                print(theE.incomingDataFlows().get(0));
                println(") {");

                for (final Map.Entry<Long, ExpressionList> theEntry : theE.getPairs().entrySet()) {
                    print(" case ");
                    print(theEntry.getKey());
                    println(":");

                    print("     ");
                    writeExpressions(theEntry.getValue());
                }

                println("}");

                writeExpressions(theE.getDefaultExpressions());
            } else if (theExpression instanceof SetMemoryLocationExpression) {
                final SetMemoryLocationExpression theE = (SetMemoryLocationExpression) theExpression;

                final List<Value> theIncomingData = theE.incomingDataFlows();

                print("bytecoderGlobalMemory[");

                final ComputedMemoryLocationWriteExpression theValue = (ComputedMemoryLocationWriteExpression) theIncomingData.get(0);

                print(theValue);

                print("] = ");

                print(theIncomingData.get(1));
                println(";");
            } else if (theExpression instanceof UnreachableExpression) {
                println("throw 'Unreachable';");
            } else if (theExpression instanceof BreakExpression) {
                final BreakExpression theBreak = (BreakExpression) theExpression;
                if (theBreak.isSetLabelRequired() && labelRequired) {
                    print("__label__ = ");
                    print(theBreak.jumpTarget().getAddress());
                    println(";");
                }
                if (!theBreak.isSilent()) {
                    print("break $");
                    print(theBreak.blockToBreak().name());
                    println(";");
                }
            } else if (theExpression instanceof ContinueExpression) {
                final ContinueExpression theContinue = (ContinueExpression) theExpression;
                if (labelRequired) {
                    print("__label__ = ");
                    print(theContinue.jumpTarget().getAddress());
                    println(";");
                }
                print("continue $");
                print(theContinue.labelToReturnTo().name());
                println(";");
            } else {
                throw new IllegalStateException("Not implemented : " + theExpression);
            }
        }
    }

    public void printRelooped(final Relooper.Block aBlock) {
        labelRequired = aBlock.containsMultipleBlock();
        if (labelRequired) {
            println("var __label__ = null;");
        }
        print(aBlock);
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
        if (aBlock instanceof Relooper.TryBlock) {
            print((Relooper.TryBlock) aBlock);
            return;
        }
        throw new IllegalStateException("Not implemented : " + aBlock);
    }

    private void print(final Relooper.SimpleBlock aSimpleBlock) {
        JSSSAWriter theWriter = this;
        if (aSimpleBlock.isLabelRequired()) {
            theWriter.print("$");
            theWriter.print(aSimpleBlock.label().name());
            theWriter.println(" : {");
            theWriter.printlnComment(aSimpleBlock.internalLabel().getType().toString());

            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.writeExpressions(aSimpleBlock.internalLabel().getExpressions());

        if (aSimpleBlock.isLabelRequired()) {
            theWriter.println("}");
        }

        print(aSimpleBlock.next());
    }

    private void print(final Relooper.LoopBlock aLoopBlock) {
        if (aLoopBlock.isLabelRequired()) {
            print("$");
            print(aLoopBlock.label().name());
            print(" : ");
        }
        println("for (;;) {");

        final JSSSAWriter theDeeper = withDeeperIndent();
        theDeeper.print(aLoopBlock.inner());

        println("}");

        print(aLoopBlock.next());
    }

    private void print(final Relooper.MultipleBlock aMultiple) {

        if (aMultiple.isLabelRequired()) {
            print("$");
            print(aMultiple.label().name());
            print(" : ");
        }
        println("for(;;) switch (__label__) {");

        final JSSSAWriter theDeeper = withDeeperIndent();
        for (final Relooper.Block theHandler : aMultiple.handlers()) {
            for (final RegionNode theEntry : theHandler.entries()) {
                theDeeper.print("case ");
                theDeeper.print(theEntry.getStartAddress().getAddress());
                theDeeper.println(" :");
                theDeeper.printlnComment(theEntry.getType().toString());


                final JSSSAWriter theHandlerWriter = theDeeper.withDeeperIndent();
                theHandlerWriter.print(theHandler);
            }
        }

        println("}");
        print(aMultiple.next());
    }

    private void print(final Relooper.TryBlock aTryBlock) {

        if (aTryBlock.isLabelRequired()) {
            print("$");
            print(aTryBlock.label().name());
            print(" : ");
        }
        println("try {");

        final JSSSAWriter theDeeper = withDeeperIndent();
        theDeeper.print(aTryBlock.inner());

        println("} catch (CURRENTEXCEPTION) {");

        final JSSSAWriter theHandler = withDeeperIndent();

        final Relooper.Block theFinally = aTryBlock.getFinallyBlock();
        JSSSAWriter theGuard = theHandler;
        if (theFinally != null) {
            theGuard.println("try {");
            theGuard = theHandler.withDeeperIndent();
        }

        for (final Relooper.TryBlock.CatchBlock theCatch : aTryBlock.getCatchBlocks()) {

            theGuard.print("if (");
            boolean first = true;
            for (final BytecodeUtf8Constant theInstanceCheck : theCatch.getCaughtExceptions()) {
                if (!first) {
                    theGuard.print(" || ");
                }
                final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theInstanceCheck));
                theGuard.print("e.instanceOf(");
                theGuard.print(JSWriterUtils.toClassName(theLinkedClass.getClassName()));
                theGuard.print(")");
                first = false;
            }

            theGuard.println(") {");

            theGuard.withDeeperIndent().print(theCatch.getHandler());

            theGuard.println("}");
        }

        theGuard.println("throw CURRENTEXCEPTION;");

        if (theFinally != null) {
            theGuard.println("} catch (CURRENTEXCEPTION) {");

            final JSSSAWriter theFinallyDeeper = theGuard.withDeeperIndent();
            theFinallyDeeper.print(theFinally);
            theFinallyDeeper.println("throw CURRENTEXCEPTION;");

            theGuard.println("}");
        }

        println("}");

        print(aTryBlock.next());
    }
}