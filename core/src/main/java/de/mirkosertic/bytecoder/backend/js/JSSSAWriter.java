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
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
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
import de.mirkosertic.bytecoder.ssa.DebugPosition;
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

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JSSSAWriter {

    protected final Program program;
    protected final BytecodeLinkerContext linkerContext;
    protected final JSPrintWriter writer;
    protected final CompileOptions options;
    private final ConstantPool constantPool;
    private boolean labelRequired;
    private final JSMinifier minifier;
    private final int indent;

    public JSSSAWriter(final CompileOptions aOptions, final Program aProgram, final int aIndent, final JSPrintWriter aWriter, final BytecodeLinkerContext aLinkerContext,
            final ConstantPool aConstantPool, final boolean aLabelRequired, final JSMinifier aMinifier) {
        program = aProgram;
        linkerContext = aLinkerContext;
        writer = aWriter;
        options = aOptions;
        constantPool = aConstantPool;
        labelRequired = aLabelRequired;
        minifier = aMinifier;
        indent = aIndent;
    }

    private JSSSAWriter withDeeperIndent() {
        return new JSSSAWriter(options, program, indent + 1, writer, linkerContext, constantPool, labelRequired, minifier);
    }

    public JSPrintWriter startLine() {
        return writer.tab(indent);
    }

    private void print(final Value aValue) {

        if (aValue instanceof Expression) {
            writeExpressionSourcemapInfo((Expression) aValue);
        }

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
        writer.text("Math.max(");
        print(aValue.incomingDataFlows().get(0));
        writer.text(",");
        print(aValue.incomingDataFlows().get(1));
        writer.text(")");
    }

    private void print(final MinExpression aValue) {
        writer.text("Math.min(");
        print(aValue.incomingDataFlows().get(0));
        writer.text(",");
        print(aValue.incomingDataFlows().get(1));
        writer.text(")");
    }

    private void print(final SqrtExpression aValue) {
        writer.text("Math.sqrt(");
        print(aValue.incomingDataFlows().get(0));
        writer.text(")");
    }

    private void print(final TypeOfExpression aValue) {
        print(aValue.incomingDataFlows().get(0));
        writer.text(".").text(minifier.toMethodName("getClass", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Class.class), new BytecodeTypeRef[0]))).text("()");
    }

    private void print(final StackTopExpression aValue) {
        writer.text("0");
    }

    private void print(final MemorySizeExpression aValue) {
        writer.text("0");
    }

    private void print(final ResolveCallsiteObjectExpression aValue) {
        writer.text("bytecoder.resolveStaticCallSiteObject(")
                .text(minifier.toClassName(aValue.getOwningClass().getThisInfo()))
                .text(",'")
                .text(aValue.getCallsiteId())
                .text("', function() {").newLine();

        final Program theProgram = aValue.getProgram();
        final RegionNode theBootstrapCode = aValue.getBootstrapMethod();

        final JSSSAWriter theNested = withDeeperIndent();

        for (final Variable theVariable : theProgram.globalVariables()) {
            theNested.startLine().text("var ").text(theVariable.getName()).assign().text("null;").newLine();
        }

        theNested.writeExpressions(theBootstrapCode.getExpressions());

        writer.text("})");
    }

    private void print(final RuntimeGeneratedTypeExpression aValue) {
        writer.text("bytecoder.dynamicType(");
        print(aValue.getMethodRef());
        writer.text(",");
        print(aValue.getStaticArguments());
        writer.text(")");
    }

    private void print(final MethodTypeExpression aValue) {
        writer.text("'").text(aValue.getSignature().toString()).text("'");
    }

    private void print(final MethodHandlesGeneratedLookupExpression aValue) {
        writer.text("null");
    }

    private void print(final ComputedMemoryLocationWriteExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        print(theIncomingData.get(0));
        writer.space().text("+").space();
        print(theIncomingData.get(1));
    }

    private void print(final ComputedMemoryLocationReadExpression aValue) {

        final List<Value> theIncomingData = aValue.incomingDataFlows();

        writer.text("bytecoderGlobalMemory[");
        print(theIncomingData.get(0));
        writer.space().text("+").space();
        print(theIncomingData.get(1));
        writer.text("]");
    }

    private void print(final MethodRefExpression aValue) {
        final String theMethodName = aValue.getMethodName();
        final BytecodeMethodSignature theSignature = aValue.getSignature();
        writer.text(minifier.toClassName(aValue.getClassName())).text(".").text(minifier.toMethodName(theMethodName, theSignature));
    }

    private void print(final FloorExpression aValue) {
        writer.text("Math.floor(");
        print(aValue.incomingDataFlows().get(0));
        writer.text(")");
    }

    private void print(final FloatingPointFloorExpression aValue) {
        writer.text("Math.floor(");
        print(aValue.incomingDataFlows().get(0));
        writer.text(")");
    }

    private void print(final FloatingPointCeilExpression aValue) {
        writer.text("Math.ceil(");
        print(aValue.incomingDataFlows().get(0));
        writer.text(")");
    }

    private void print(final UnknownExpression aValue) {
        writer.text("undefined");
    }

    private void print(final CurrentExceptionExpression aValue) {
        writer.text("CURRENTEXCEPTION.exception");
    }

    private void print(final MethodParameterValue aValue) {
        writer.text("p" + (aValue.getParameterIndex() + 1));
    }

    private void print(final SelfReferenceParameterValue aValue) {
        writer.text(Variable.THISREF_NAME);
    }

    private void print(final NewMultiArrayExpression aValue) {
        final BytecodeTypeRef theType = aValue.getType();
        final Object theDefaultValue = theType.defaultValue();
        final String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        writer.text("bytecoder.newMultiArray([");
        final List<Value> theDimensions = aValue.incomingDataFlows();
        for (int i=0;i<theDimensions.size();i++) {
            if (i>0) {
                writer.text(",");
            }
            print(theDimensions.get(i));
        }
        writer.text("],").text(theStrDefault).text(")");
    }

    private void print(final ClassReferenceValue aValue) {
        writer.text(minifier.toClassName(aValue.getType())).text(".i()");
    }

    private void print(final InstanceOfExpression aValue) {
        final Value theValue = aValue.incomingDataFlows().get(0);
        writer.text("(");
        print(theValue);
        writer.space().text("==").space().text("null").space().text("?").space().text("false").space().text(":");
        print(theValue);
        writer.text(".iof(");

        final BytecodeUtf8Constant theConstant = aValue.getType().getConstant();
        if (!theConstant.stringValue().startsWith("[")) {
            final BytecodeLinkedClass theLinkedClass = linkerContext.isLinkedOrNull(aValue.getType().getConstant());
            writer.text(minifier.toClassName(theLinkedClass.getClassName()));
        } else {
            final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
            writer.text(minifier.toClassName(theLinkedClass.getClassName()));
        }

        writer.text("))");
    }

    private void print(final LongValue aValue) {
        writer.text("" + aValue.getLongValue());
    }

    private void print(final ShortValue aValue) {
        writer.text("" + aValue.getShortValue());
    }

    private void print(final NegatedExpression aValue) {
        final Value theValue = aValue.incomingDataFlows().get(0);
        writer.text("(-");
        print(theValue);
        writer.text(")");
    }

    private void print(final CompareExpression aValue) {
        final List<Value> theIncomingData = aValue.incomingDataFlows();

        final Value theVariable1 = theIncomingData.get(0);
        final Value theVariable2 = theIncomingData.get(1);
        writer.text("(");
        print(theVariable1);
        writer.space().text(">").space();
        print(theVariable2);
        writer.space().text("?").space().text("1").space();
        writer.text(":").space().text("(");
        print(theVariable1);
        writer.space().text("<").space();
        print(theVariable2);
        writer.space().text("?").space().text("-1").space().text(":").space().text("0))");
    }

    private void print(final NewArrayExpression aValue) {
        final BytecodeTypeRef theType = aValue.getType();
        final Value theLength =aValue.incomingDataFlows().get(0);
        final Object theDefaultValue = theType.defaultValue();
        final String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        writer.text("bytecoder.newArray(");
        print(theLength);
        writer.text(",").text(theStrDefault).text(")");
    }

    private void print(final IntegerValue aValue) {
        writer.text("" + aValue.getIntValue());
    }

    private void print(final FloatValue aValue) {
        writer.text("" + aValue.getFloatValue());
    }

    private void print(final DoubleValue aValue) {
        writer.text("" + aValue.getDoubleValue());
    }

    private void print(final StringValue aValue) {
        final int theIndex = constantPool.register(aValue);
        writer.text("bytecoder.stringpool[").text("" + theIndex).text("]");
    }

    private void print(final ArrayLengthExpression aValue) {
        print(aValue.incomingDataFlows().get(0));
        writer.text(".data.length");
    }

    private void printArrayIndexReference(final Value aValue) {
        writer.text(".data[");
        print(aValue);
        writer.text("]");
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
                writer.text("Math.floor(");
                print(theValue);
                writer.text(")");
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

        writer.text("(");
        print(theIncomingData.get(0));
        switch (aValue.getOperator()) {
            case ADD:
                writer.space().text("+").space();
                break;
            case DIV:
                writer.space().text("/").space();
                break;
            case MUL:
                writer.space().text("*").space();
                break;
            case SUB:
                writer.space().text("-").space();
                break;
            case EQUALS:
                writer.space().text("==").space();
                break;
            case BINARYOR:
                writer.space().text("|").space();
                break;
            case LESSTHAN:
                writer.space().text("<").space();
                break;
            case BINARYAND:
                writer.space().text("&").space();
                break;
            case BINARYXOR:
                writer.space().text("^").space();
                break;
            case NOTEQUALS:
                writer.space().text("!=").space();
                break;
            case REMAINDER:
                writer.space().text("%").space();
                break;
            case GREATERTHAN:
                writer.space().text(">").space();
                break;
            case BINARYSHIFTLEFT:
                writer.space().text("<<").space();
                break;
            case GREATEROREQUALS:
                writer.space().text(">=").space();
                break;
            case BINARYSHIFTRIGHT:
                writer.space().text(">>").space();
                break;
            case LESSTHANOREQUALS:
                writer.space().text("<=").space();
                break;
            case BINARYUNSIGNEDSHIFTRIGHT:
                writer.space().text(">>>").space();
                break;
            default:
                throw new IllegalStateException("Unsupported operator : " + aValue.getOperator());
        }
        print(theIncomingData.get(1));
        writer.text(")");
    }

    private void print(final FixedBinaryExpression aValue) {
        final Value theValue1 = aValue.incomingDataFlows().get(0);
        print(theValue1);
        switch (aValue.getOperator()) {
            case ISNONNULL:
                writer.space().text("!=").space().text("null").space();
                break;
            case ISZERO:
                writer.space().text("==").space().text("0").space();
                break;
            case ISNULL:
                writer.space().text("==").space().text("null").space();
                break;
            default:
                throw new IllegalStateException("Unsupported operator : " + aValue.getOperator());
        }
    }

    private void print(final ByteValue aValue) {
        writer.text("" + aValue.getByteValue());
    }

    private void print(final NewObjectExpression aValue) {
        writer.text("new ").text(minifier.toClassName(aValue.getType())).text(".C()");
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
            writer.text("bytecoder.toJSString(");
            print(aValue);
            writer.text(")");
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
                final String theMethodName = minifier.toMethodName(theCallbackMethod.getName().stringValue(), theCallbackMethod.getSignature());

                writer.text("function() {").text("var v = ");
                print(aValue);
                writer.text(";var args = Array.prototype.slice.call(arguments);v").text(".").text(theMethodName).text("(v");
                final BytecodeTypeRef[] theArguments = theCallbackMethod.getSignature().getArguments();
                for (int i=0;i<theArguments.length;i++) {
                    writer.text(",");
                    final String theConversionFunction = conversionFunctionToBytecoderForOpaqueType(theArguments[i]);
                    if (theConversionFunction != null) {
                        writer.text(theConversionFunction).text("(").text("args[").text("" + i).text("])");
                    } else {
                        writer.text("args[").text("" + i).text("]");
                    }
                }
                writer.text(");");
                writer.text("}");
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
                    writer.text(theReturnConvertFunction).text("(");
                }
                writer.text("bytecoder.imports.").text(theModuleName).text(".").text(theObjectName).text("(");

                final List<Value> theVariables = aValue.incomingDataFlows();

                for (int i = 0; i < theVariables.size(); i++) {
                    if (i> 0) {
                        writer.text(",");
                    }
                    printToJSConvertedValue(theSignature.getArguments()[i], theVariables.get(i));
                }
                writer.text(")");

                if (theReturnConvertFunction != null) {
                    writer.text(")");
                }
            } else {
                // We continue the normal flow, as method implementation is provided by the bytecode
                writer.text(minifier.toClassName(aValue.getClassName())).text(".").text(minifier.toMethodName(theMethodName, theSignature)).text("(");

                final List<Value> theVariables = aValue.incomingDataFlows();

                for (int i = 0; i < theVariables.size(); i++) {
                    if (i> 0) {
                        writer.text(",");
                    }
                    print(theVariables.get(i));
                }
                writer.text(")");
            }
        } else {
            writer.text(minifier.toClassName(aValue.getClassName())).text(".i().").text(minifier.toMethodName(theMethodName, theSignature)).text("(");

            final List<Value> theVariables = aValue.incomingDataFlows();

            for (int i = 0; i < theVariables.size(); i++) {
                if (i> 0) {
                    writer.text(",");
                }
                print(theVariables.get(i));
            }
            writer.text(")");
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
            if ("<init>".equals(theMethodName)) {
                writer.text(minifier.toClassName(aValue.getClazz()));
            } else {
                final BytecodeResolvedMethods theResolvedMethods = theTargetClass.resolvedMethods();
                final BytecodeResolvedMethods.MethodEntry theEntry = theResolvedMethods.implementingClassOf(theMethodName, theSignature);
                writer.text(minifier.toClassName(theEntry.getProvidingClass().getClassName()));
            }
            writer.text(".").text(minifier.toMethodName(theMethodName, theSignature)).text("(");

            print(theTarget);

            for (final Value theArgument : theArguments) {
                writer.text(",");
                print(theArgument);
            }
            writer.text(")");
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
                writer.text("[");
                printToJSConvertedValue(aMethodSignature.getArguments()[0], aMethodArguments.get(0));
                writer.text("]=");

                printToJSConvertedValue(aMethodSignature.getArguments()[1], aMethodArguments.get(1));
            } else {
                final String theReturnConvertFunction = conversionFunctionToBytecoderForOpaqueType(aMethodSignature.getReturnType());
                if (theReturnConvertFunction != null) {
                    writer.text(theReturnConvertFunction).text("(");
                }

                // Get property
                print(aInvocationTarget);
                writer.text("[");
                printToJSConvertedValue(aMethodSignature.getArguments()[0], aMethodArguments.get(0));
                writer.text("]");

                if (theReturnConvertFunction != null) {
                    writer.text(")");
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
                writer.text(".").text(theOpaquePropertyName).text("=");

                printToJSConvertedValue(aMethodSignature.getArguments()[0], aMethodArguments.get(0));
            } else {
                final String theReturnConvertFunction = conversionFunctionToBytecoderForOpaqueType(aMethodSignature.getReturnType());
                if (theReturnConvertFunction != null) {
                    writer.text(theReturnConvertFunction).text("(");
                }

                // Get property
                print(aInvocationTarget);
                writer.text(".").text(theOpaquePropertyName);

                if (theReturnConvertFunction != null) {
                    writer.text(")");
                }
            }
        } else {

            final String theReturnConvertFunction = conversionFunctionToBytecoderForOpaqueType(aMethodSignature.getReturnType());
            if (theReturnConvertFunction != null) {
                writer.text(theReturnConvertFunction).text("(");
            }

            String theMethodName = aMethodImplementation.getName().stringValue();
            final BytecodeAnnotation theMethod = aMethodImplementation.getAttributes().getAnnotationByType(OpaqueMethod.class.getName());
            if (theMethod != null) {
                theMethodName = theMethod.getElementValueByName("value").stringValue();
            }

            // Simple method invocation
            print(aInvocationTarget);
            writer.text(".").text(theMethodName).text("(");

            for (int i = 0; i < aMethodArguments.size(); i++) {
                if (i > 0) {
                    writer.text(",");
                }
                printToJSConvertedValue(aMethodSignature.getArguments()[i], aMethodArguments.get(i));
            }

            writer.text(")");

            if (theReturnConvertFunction != null) {
                writer.text(")");
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

        if (!theClasses.stream().filter(t -> t.isOpaqueType()).collect(Collectors.toList()).isEmpty()) {
            throw new IllegalStateException("There seems to be some confusion here, either multiple OpaqueTypes with method named \"" + theMethodName + "\" or mix of Opaque and Non-Opaque virtual invocations in class list " + theClasses);
        }

        if (Objects.equals(aValue.getMethodName(), "invokeWithMagicBehindTheScenes")) {
            writer.text("(");
        } else {
            print(theTarget);
            writer.text(".").text(minifier.toMethodName(theMethodName, theSignature)).text("(");
        }

        print(theTarget);
        for (final Value theArgument : theArguments) {
            writer.text(",");
            print(theArgument);
        }
        writer.text(")");
    }

    private void print(final NullValue aValue) {
        writer.text("null");
    }

    private void print(final GetStaticExpression aValue) {
        printStaticFieldReference(aValue.getField(), program.getDebugInformation().debugPositionFor(aValue.getAddress()));
    }

    private void printVariableName(final Variable aVariable) {
        writer.text(minifier.toVariableName(aVariable.getName()));
    }

    private void printStaticFieldReference(final BytecodeFieldRefConstant aField, final DebugPosition aPosition) {
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(aField.getClassIndex().getClassConstant().getConstant()));
        final BytecodeResolvedFields theFields = theLinkedClass.resolvedFields();
        final BytecodeResolvedFields.FieldEntry theField = theFields.fieldByName(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
        writer.text(minifier.toClassName(theField.getProvidingClass().getClassName())).text(".i().").symbol(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(), aPosition);
    }

    private void printInstanceFieldReference(final BytecodeFieldRefConstant aField) {
        writer.text(".").text(minifier.toSymbol(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue()));
    }

    private String generateJumpCodeFor(final BytecodeOpcodeAddress aTarget) {
        return "currentLabel = " + aTarget.getAddress()+";continue controlflowloop;";
    }

    private void writeExpressionSourcemapInfo(final Expression aExpression) {
        final BytecodeOpcodeAddress theExpressionAddress = aExpression.getAddress();
        if (theExpressionAddress != null) {
            final DebugPosition thePosition = program.getDebugInformation().debugPositionFor(theExpressionAddress);
            if (thePosition != null) {
                writer.assignPositionToSourceFile(thePosition);
            }
        }
    }

    public void writeExpressions(final ExpressionList aExpressions) {
        for (final Expression theExpression : aExpressions.toList()) {
            if (options.isDebugOutput()) {
                final String theComment = theExpression.getComment();
                if (theComment != null && !theComment.isEmpty()) {
                    startLine().text("//").text(theComment).newLine();
                }
            }

            writeExpressionSourcemapInfo(theExpression);

            if (theExpression instanceof ReturnExpression) {
                final ReturnExpression theE = (ReturnExpression) theExpression;
                startLine().text("return;").newLine();
            } else if (theExpression instanceof VariableAssignmentExpression) {
                final VariableAssignmentExpression theE = (VariableAssignmentExpression) theExpression;

                final Variable theVariable = theE.getVariable();
                final Value theValue = theE.getValue();

                if (theValue instanceof ComputedMemoryLocationWriteExpression) {
                    continue;
                }

                final JSPrintWriter theWriter = startLine();

                if (!program.isGlobalVariable(theVariable)) {
                    theWriter.text("var ");
                }

                if (theVariable.resolveType().resolve() == TypeRef.Native.INT) {
                    if (!(theValue instanceof IntegerValue)) {
                        theWriter.text(minifier.toVariableName(theVariable.getName())).space().text("=").space().text("(");
                        print(theValue);
                        theWriter.text(") | 0");
                    } else {
                        theWriter.text(minifier.toVariableName(theVariable.getName())).space().text("=").space();
                        print(theValue);
                    }
                } else {
                    theWriter.text(minifier.toVariableName(theVariable.getName())).space().text("=").space();
                    print(theValue);
                }
                if (options.isDebugOutput()) {
                    theWriter.text("; // type is ").text(theVariable.resolveType().resolve().name() + " value type is " + theValue.resolveType()).newLine();
                } else {
                    theWriter.text(";").newLine();
                }
            } else if (theExpression instanceof PutStaticExpression) {
                final PutStaticExpression theE = (PutStaticExpression) theExpression;
                final BytecodeFieldRefConstant theField = theE.getField();
                final Value theValue = theE.incomingDataFlows().get(0);

                startLine();
                printStaticFieldReference(theField, program.getDebugInformation().debugPositionFor(theExpression.getAddress()));
                writer.assign();
                print(theValue);
                writer.text(";").newLine();
            } else if (theExpression instanceof ReturnValueExpression) {
                final ReturnValueExpression theE = (ReturnValueExpression) theExpression;
                final Value theValue = theE.incomingDataFlows().get(0);
                startLine().text("return ");
                print(theValue);
                writer.text(";").newLine();
            } else if (theExpression instanceof ThrowExpression) {
                final ThrowExpression theE = (ThrowExpression) theExpression;
                final Value theValue = theE.incomingDataFlows().get(0);
                startLine().text("throw {exception :");
                print(theValue);
                writer.text(", stack : new Error().stack};").newLine();
            } else if (theExpression instanceof InvokeVirtualMethodExpression) {
                final InvokeVirtualMethodExpression theE = (InvokeVirtualMethodExpression) theExpression;
                startLine();
                print(theE);
                writer.text(";").newLine();
            } else if (theExpression instanceof DirectInvokeMethodExpression) {
                final DirectInvokeMethodExpression theE = (DirectInvokeMethodExpression) theExpression;
                startLine();
                print(theE);
                writer.text(";").newLine();
            } else if (theExpression instanceof InvokeStaticMethodExpression) {
                final InvokeStaticMethodExpression theE = (InvokeStaticMethodExpression) theExpression;
                startLine();
                print(theE);
                writer.text(";").newLine();
            } else if (theExpression instanceof PutFieldExpression) {
                final PutFieldExpression theE = (PutFieldExpression) theExpression;

                final List<Value> theIncomingData = theE.incomingDataFlows();

                final Value theTarget = theIncomingData.get(0);
                final BytecodeFieldRefConstant theField = theE.getField();

                final Value thevalue = theIncomingData.get(1);

                startLine();

                print(theTarget);
                printInstanceFieldReference(theField);
                writer.assign();
                print(thevalue);
                writer.text(";").newLine();
            } else if (theExpression instanceof IFExpression) {
                final IFExpression theE = (IFExpression) theExpression;

                startLine().text("if (");
                print(theE.incomingDataFlows().get(0));
                writer.text(") {").newLine();

                withDeeperIndent().writeExpressions(theE.getExpressions());

                startLine().text("}").newLine();

            } else if (theExpression instanceof GotoExpression) {
                final GotoExpression theE = (GotoExpression) theExpression;
                startLine().text(generateJumpCodeFor(theE.getJumpTarget())).newLine();
            } else if (theExpression instanceof ArrayStoreExpression) {
                final ArrayStoreExpression theE = (ArrayStoreExpression) theExpression;

                final List<Value> theIncomingData = theE.incomingDataFlows();

                final Value theArray = theIncomingData.get(0);
                final Value theIndex = theIncomingData.get(1);
                final Value theValue = theIncomingData.get(2);

                startLine();

                print(theArray);
                printArrayIndexReference(theIndex);

                writer.assign();

                print(theValue);

                writer.text(";").newLine();
            } else if (theExpression instanceof CheckCastExpression) {
                final CheckCastExpression theE = (CheckCastExpression) theExpression;
                // Completely ignored
            } else if (theExpression instanceof TableSwitchExpression) {
                final TableSwitchExpression theE = (TableSwitchExpression) theExpression;
                final Value theValue = theE.incomingDataFlows().get(0);

                startLine();

                writer.newLine().text("if (");
                print(theValue);
                writer.space().text("<").space().text("" + theE.getLowValue());
                writer.space().text("||").space();
                print(theValue);
                writer.space().text(">").space().text("" + theE.getHighValue());
                writer.text(") {").newLine();

                writeExpressions(theE.getDefaultExpressions());

                writer.text("}").newLine();

                startLine().text("switch(");
                print(theValue);
                writer.space().text("-").space().text("" + theE.getLowValue()).text(") {").newLine();

                for (final Map.Entry<Long, ExpressionList> theEntry : theE.getOffsets().entrySet()) {

                    startLine().text(" case ").text("" + theEntry.getKey()).text(":").newLine();

                    withDeeperIndent().writeExpressions(theEntry.getValue());
                }

                writer.text("}").newLine();

                startLine().text("throw 'Illegal jump target!';").newLine();
            } else if (theExpression instanceof LookupSwitchExpression) {
                final LookupSwitchExpression theE = (LookupSwitchExpression) theExpression;

                startLine().text("switch(");
                print(theE.incomingDataFlows().get(0));
                writer.text(") {").newLine();

                for (final Map.Entry<Long, ExpressionList> theEntry : theE.getPairs().entrySet()) {

                    startLine().text(" case ").text("" + theEntry.getKey()).text(":").newLine();

                    withDeeperIndent().writeExpressions(theEntry.getValue());
                }

                writer.text("}").newLine();

                writeExpressions(theE.getDefaultExpressions());
            } else if (theExpression instanceof SetMemoryLocationExpression) {
                final SetMemoryLocationExpression theE = (SetMemoryLocationExpression) theExpression;

                final List<Value> theIncomingData = theE.incomingDataFlows();

                startLine().text("bytecoderGlobalMemory[");

                final ComputedMemoryLocationWriteExpression theValue = (ComputedMemoryLocationWriteExpression) theIncomingData.get(0);

                print(theValue);

                writer.text("]=");

                print(theIncomingData.get(1));

                writer.text(";").newLine();
            } else if (theExpression instanceof UnreachableExpression) {
                startLine().text("throw 'Unreachable';").newLine();
            } else if (theExpression instanceof BreakExpression) {
                final BreakExpression theBreak = (BreakExpression) theExpression;
                if (theBreak.isSetLabelRequired() && labelRequired) {
                    startLine().text("__l").assign().text("" + theBreak.jumpTarget().getAddress()).text(";").newLine();
                }
                if (!theBreak.isSilent()) {
                    startLine().text("break $").text(theBreak.blockToBreak().name()).text(";").newLine();
                }
            } else if (theExpression instanceof ContinueExpression) {
                final ContinueExpression theContinue = (ContinueExpression) theExpression;
                if (labelRequired) {
                    startLine().text("__l").assign().text("" + theContinue.jumpTarget().getAddress()).text(";").newLine();
                }
                startLine().text("continue $").text(theContinue.labelToReturnTo().name()).text(";").newLine();
            } else {
                throw new IllegalStateException("Not implemented : " + theExpression);
            }
        }
    }

    public void printRelooped(final Relooper.Block aBlock) {
        labelRequired = aBlock.containsMultipleBlock();
        if (labelRequired) {
            startLine().text("var __l").assign().text("null;").newLine();
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
        if (aBlock instanceof Relooper.IFThenElseBlock) {
            print((Relooper.IFThenElseBlock) aBlock);
            return;
        }
        throw new IllegalStateException("Not implemented : " + aBlock);
    }

    private void print(final Relooper.IFThenElseBlock aIfThenElseBlock) {
        JSSSAWriter theWriter = this;

        theWriter.writeExpressions(aIfThenElseBlock.getPrelude());

        if (aIfThenElseBlock.isLabelRequired()) {
            theWriter.startLine().text("$").text(aIfThenElseBlock.label().name()).colon().text("{").newLine();
            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.startLine().text("if").space().text("(");
        theWriter.print(aIfThenElseBlock.getCondition());
        theWriter.writer.text(")").space().text("{").newLine();

        theWriter.withDeeperIndent().print(aIfThenElseBlock.getTrueBlock());

        theWriter.startLine().text("}").space().text("else").space().text("{").newLine();

        theWriter.withDeeperIndent().print(aIfThenElseBlock.getFalseBlock());

        theWriter.startLine().text("}").newLine();

        if (aIfThenElseBlock.isLabelRequired()) {
            startLine().text("}").newLine();
        }

        print(aIfThenElseBlock.next());
    }

    private void print(final Relooper.SimpleBlock aSimpleBlock) {
        JSSSAWriter theWriter = this;

        if (aSimpleBlock.isLabelRequired()) {
            startLine().text("$").text(aSimpleBlock.label().name()).colon().text("{").newLine();
            if (options.isDebugOutput()) {
                startLine().text("// ").text(aSimpleBlock.internalLabel().getType().toString()).newLine();
            }

            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.writeExpressions(aSimpleBlock.expressions());

        if (aSimpleBlock.isLabelRequired()) {
            startLine().text("}").newLine();
        }

        print(aSimpleBlock.next());
    }

    private void print(final Relooper.LoopBlock aLoopBlock) {
        if (aLoopBlock.isLabelRequired()) {
            startLine().text("$").text(aLoopBlock.label().name()).colon().text("for").space().text("(;;)").space().text("{").newLine();
        } else {
            startLine().text("for").space().text("(;;)").space().text("{").newLine();
        }

        withDeeperIndent().print(aLoopBlock.inner());

        startLine().text("}").newLine();

        print(aLoopBlock.next());
    }

    private void print(final Relooper.MultipleBlock aMultiple) {

        if (aMultiple.isLabelRequired()) {
            startLine().text("$").text(aMultiple.label().name()).colon().text("for(;;)").space().text("switch").space().text("(__l) {").newLine();
        } else {
            startLine().text("for(;;)").space().text("switch").space().text("(__l) {").newLine();
        }

        for (final Relooper.Block theHandler : aMultiple.handlers()) {
            for (final RegionNode theEntry : theHandler.entries()) {
                startLine().space().text("case ").text("" + theEntry.getStartAddress().getAddress()).colon().newLine();
                if (options.isDebugOutput()) {
                    startLine().text(" // ").text(theEntry.getType().toString()).newLine();
                }
                withDeeperIndent().print(theHandler);
            }
        }

        startLine().text("}").newLine();

        print(aMultiple.next());
    }

    private void print(final Relooper.TryBlock aTryBlock) {

        if (aTryBlock.isLabelRequired()) {
            startLine().text("$").text(aTryBlock.label().name()).colon().text("try").space().text("{").newLine();
        } else {
            startLine().text("try").space().text("{").newLine();
        }

        withDeeperIndent().print(aTryBlock.inner());

        startLine().text("} catch (CURRENTEXCEPTION) {").newLine();

        final JSSSAWriter theHandler = withDeeperIndent();

        final Relooper.Block theFinally = aTryBlock.getFinallyBlock();
        JSSSAWriter theGuard = theHandler;
        if (theFinally != null) {
            theGuard.startLine().text("try {").newLine();
            theGuard = theHandler.withDeeperIndent();
        }

        for (final Relooper.TryBlock.CatchBlock theCatch : aTryBlock.getCatchBlocks()) {

            theGuard.startLine().text("if (");
            boolean first = true;
            for (final BytecodeUtf8Constant theInstanceCheck : theCatch.getCaughtExceptions()) {
                if (!first) {
                    theGuard.writer.space().text("||").space();
                }
                final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theInstanceCheck));
                theGuard.writer.text("CURRENTEXCEPTION.exception.iof(").text(minifier.toClassName(theLinkedClass.getClassName())).text(")");
                first = false;
            }

            theGuard.writer.text(") {").newLine();

            theGuard.withDeeperIndent().print(theCatch.getHandler());

            theGuard.startLine().text("}").newLine();
        }

        theGuard.startLine().text("throw CURRENTEXCEPTION;").newLine();

        if (theFinally != null) {
            theHandler.startLine().text("} catch (CURRENTEXCEPTION) {").newLine();

            final JSSSAWriter theFinallyDeeper = theHandler.withDeeperIndent();
            theFinallyDeeper.print(theFinally);
            theFinallyDeeper.startLine().text("throw CURRENTEXCEPTION;").newLine();

            theHandler.startLine().text("}").newLine();
        }

        startLine().text("}").newLine();

        print(aTryBlock.next());
    }
}