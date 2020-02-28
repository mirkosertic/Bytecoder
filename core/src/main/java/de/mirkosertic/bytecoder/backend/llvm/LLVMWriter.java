/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.llvm;

import de.mirkosertic.bytecoder.backend.NativeMemoryLayouter;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.graph.Edge;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.GraphDFSOrder;
import de.mirkosertic.bytecoder.graph.Node;
import de.mirkosertic.bytecoder.ssa.ArrayEntryExpression;
import de.mirkosertic.bytecoder.ssa.ArrayLengthExpression;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.BlockState;
import de.mirkosertic.bytecoder.ssa.ByteValue;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.CompareExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationReadExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationWriteExpression;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.DataEndExpression;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.EnumConstantsExpression;
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
import de.mirkosertic.bytecoder.ssa.HeapBaseExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InstanceOfExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.IsNaNExpression;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.LookupSwitchExpression;
import de.mirkosertic.bytecoder.ssa.MaxExpression;
import de.mirkosertic.bytecoder.ssa.MemorySizeExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupExpression;
import de.mirkosertic.bytecoder.ssa.MethodRefExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeArgumentCheckExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.MinExpression;
import de.mirkosertic.bytecoder.ssa.NegatedExpression;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PtrOfExpression;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ReinterpretAsNativeExpression;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeExpression;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.SqrtExpression;
import de.mirkosertic.bytecoder.ssa.StackTopExpression;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.SuperTypeOfExpression;
import de.mirkosertic.bytecoder.ssa.SystemHasStackExpression;
import de.mirkosertic.bytecoder.ssa.TableSwitchExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionExpression;
import de.mirkosertic.bytecoder.ssa.TypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;
import de.mirkosertic.bytecoder.ssa.VariableDescription;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LLVMWriter implements AutoCloseable {

    public static final String VTABLEFUNCTIONSUFFIX = "__resolvevtableindex";
    public static final String INSTANCEOFSUFFIX = "__instanceof";
    public static final String RUNTIMECLASSSUFFIX = "__runtimeclass";
    public static final int GENERATED_INSTANCEOF_METHOD_ID = -1;
    public static final String NEWINSTANCE_METHOD_NAME = "$newInstance";
    public static final String CLASSINITSUFFIX = "__init";
    public static final String NEWINSTANCEHELPER = "newinstancehelper";

    interface SymbolResolver {

        String globalFromStringPool(final String aValue);

        String resolveCallsiteBootstrapFor(BytecodeClass owningClass, String callsiteId, Program program, RegionNode bootstrapMethod);

        String methodTypeFactoryNameFor(final BytecodeMethodSignature aSignature);
    }

    private final PrintWriter target;
    private RegionNode currentNode;
    private LLVMDebugInformation.SubProgram currentSubProgram;
    private final NativeMemoryLayouter memoryLayouter;
    private final BytecodeLinkerContext linkerContext;
    private final SymbolResolver symbolResolver;

    public LLVMWriter(final PrintWriter output, final NativeMemoryLayouter memoryLayouter, final BytecodeLinkerContext linkerContext, final SymbolResolver symbolResolver) {
        this.target = output;
        this.memoryLayouter = memoryLayouter;
        this.linkerContext = linkerContext;
        this.symbolResolver = symbolResolver;
    }

    @Override
    public void close() {
    }

    private String toTempSymbol(final Value v, final String suffix) {
        return "t_" + System.identityHashCode(v) + "_" + suffix + "_";
    }

    private BytecodeResolvedFields.FieldEntry implementingClassForStaticField(final BytecodeObjectTypeRef aClass, final String aFieldName) {
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(aClass);
        final BytecodeResolvedFields theFields = theLinkedClass.resolvedFields();
        return theFields.fieldByName(aFieldName);
    }

    private static class PHIValuePair {

        private final String nodeLabel;
        private final Value phiValue;

        public PHIValuePair(final String nodeLabel, final Value phiValue) {
            this.nodeLabel = nodeLabel;
            this.phiValue = phiValue;
        }
    }

    private List<PHIValuePair> phiValuePairFor(final RegionNode aTarget, final PHIValue aPHI, final Set<RegionNode> aPreds, final List<RegionNode> aRegularFlow) {
        final List<PHIValuePair> theResult = new ArrayList<>();
        for (final RegionNode thePredecessor : aPreds) {
            if (aRegularFlow.contains(thePredecessor)) {
                final Value theOut = thePredecessor.liveOut().getPorts().get(aPHI.getDescription());
                theResult.add(new PHIValuePair("block" + thePredecessor.getStartAddress().getAddress(), theOut));

                // Special case for table switch expressions, as the introduce artificial blocks
                for (final Expression e : thePredecessor.getExpressions().toList()) {
                    if (e instanceof TableSwitchExpression) {
                        final TableSwitchExpression ts = (TableSwitchExpression) e;
                        for (final Map.Entry<Long, ExpressionList> theOffset : ts.getOffsets().entrySet()) {
                            for (final Expression e2 : theOffset.getValue().toList()) {
                                if (e2 instanceof GotoExpression) {
                                    final GotoExpression theGoto = (GotoExpression) e2;
                                    if (theGoto.jumpTarget().equals(aTarget.getStartAddress())) {
                                        // We have something!!
                                        theResult.add(new PHIValuePair(toTempSymbol(e, "else"), theOut));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return theResult;
    }

    public void write(final Program aProgram, final LLVMDebugInformation.SubProgram aSubProgram) {
        final ControlFlowGraph theGraph = aProgram.getControlFlowGraph();
        final RegionNode theStart = theGraph.startNode();
        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(theStart,
                RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        final List<RegionNode> theRegularFlow = order.getNodesInOrder();
        final Set<String> theAlreadySeenPHIs = new HashSet<>();
        currentSubProgram = aSubProgram;
        target.println("entry:");
        target.println("    br label %block0");
        for (final RegionNode theBlock : theRegularFlow) {
            currentNode = theBlock;
            final BytecodeOpcodeAddress theBlockStart = theBlock.getStartAddress();
            target.print("block");
            target.print(theBlockStart.getAddress());
            target.println(":");
            final BlockState theLiveIn = theBlock.liveIn();
            for (final Map.Entry<VariableDescription, Value> theEntry : theLiveIn.getPorts().entrySet()) {
                if (theEntry.getValue() instanceof PHIValue) {
                    final PHIValue phi = (PHIValue) theEntry.getValue();

                    final String thePHITempName = toTempSymbol(phi, "phi");

                    final Set<RegionNode> thePreds = currentNode.getPredecessors();
                    final List<PHIValuePair> theIncoming = phiValuePairFor(currentNode, phi, thePreds, theRegularFlow);
                    final Set<Value> theIncomingValues = theIncoming.stream().map(t -> t.phiValue).collect(Collectors.toSet());

                    if (thePreds.size() > 1 && (theIncomingValues.size() > 1) || (theIncomingValues.size() == 1 && !theIncomingValues.contains(phi))) {

                        if (theAlreadySeenPHIs.add(thePHITempName)) {
                            target.print("    %");
                            target.print(thePHITempName);
                            target.print(" = phi ");
                            target.print(LLVMWriterUtils.toType(phi.resolveType()));
                            target.print(" ");
                            boolean first = true;
                            for (final PHIValuePair theIncomingEntry : theIncoming) {

                                final Value theOut = theIncomingEntry.phiValue;

                                if (theOut instanceof Variable) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        target.print(",");
                                    }
                                    target.print("[");

                                    target.print("%");
                                    target.print(((Variable) theOut).getName());
                                    target.print("_");

                                    target.print(",%");
                                    target.print(theIncomingEntry.nodeLabel);

                                    target.print("]");
                                } else if (theOut instanceof PHIValue) {

                                    if (first) {
                                        first = false;
                                    } else {
                                        target.print(",");
                                    }
                                    target.print("[");

                                    target.print("%");
                                    target.print(toTempSymbol(theOut, "phi"));

                                    target.print(",%");
                                    target.print(theIncomingEntry.nodeLabel);

                                    target.print("]");

                                } else {
                                    throw new RuntimeException("Unhandled type for PHI input : " + theOut.getClass());
                                }
                            }
                        }
                        target.println();
                    }
                }
            }
            write(theBlock);
        }
    }

    private void write(final RegionNode block) {
        write(block.getExpressions());
    }

    private void tempify(final InvokeStaticMethodExpression e) {
        final BytecodeObjectTypeRef theClass = e.getClassName();
        final String theClassName = LLVMWriterUtils.toClassName(theClass);

        target.print("    %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);

        target.print("()");
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();
   }

    private void tempify(final InvokeVirtualMethodExpression e) {

        final BytecodeTypeRef theInvokedClassName = e.getInvokedClass();
        if (!theInvokedClassName.isPrimitive() && !theInvokedClassName.isArray()) {
            final BytecodeLinkedClass theInvokedClass = linkerContext.resolveClass((BytecodeObjectTypeRef) theInvokedClassName);
            if (theInvokedClass.isOpaqueType()) {
                final BytecodeResolvedMethods theMethods = theInvokedClass.resolvedMethods();
                final List<BytecodeResolvedMethods.MethodEntry> theImplMethods = theMethods.stream().filter(
                        t -> t.getValue().getName().stringValue().equals(e.getMethodName()) &&
                                t.getValue().getSignature().matchesExactlyTo(e.getSignature()))
                        .collect(Collectors.toList());
                if (theImplMethods.size() != 1) {
                    throw new IllegalStateException("Cannot find unique method " + e.getMethodName() + " with signature " + e.getSignature() + " in " + theInvokedClassName.name());
                }
                final BytecodeMethod theMethod = theImplMethods.get(0).getValue();
                if (!theMethod.isConstructor()) {
                    // At this point, we are creating a direct call invocation to the function
                    // Which is imported fom the WASM Host environment
                    return;
                }
            }
        }

        final Value value = e.incomingDataFlows().get(0);

        // Compute offset to vtable resolver function
        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = ");
        target.print("add i32 ");
        write(value, true);
        target.print(", 4");
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "vtableptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "ptr"));
        target.println(" to i32*");

        target.print("    %");
        target.print(toTempSymbol(e, "vtableref"));
        target.print(" = load i32, i32* %");
        target.println(toTempSymbol(e, "vtableptr"));

        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "vtableref"));
        target.println(" to i32(i32,i32)*");

        // Resolve the index of the virtual identifier
        target.print("    %");
        target.print(toTempSymbol(e, "resolved"));
        target.print(" = call i32(i32,i32) %");
        target.print(toTempSymbol(e, "vtable"));
        target.print("(i32 ");
        write(value, true);
        target.print(",");

        final BytecodeVirtualMethodIdentifier theMethodIdentifier = linkerContext.getMethodCollection().identifierFor(e.getMethodName(), e.getSignature());

        target.print("i32 ");
        target.print(theMethodIdentifier.getIdentifier());
        target.println(")");

        // Invoke function
        target.print("    %");
        target.print(toTempSymbol(e, "resolved_ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "resolved"));
        target.print(" to ");
        target.print(LLVMWriterUtils.toSignature(e.getSignature()));
        target.println("*");
    }

    private void tempify(final ArrayLengthExpression e) {
        final Value value = e.incomingDataFlows().get(0);

        // Compute offset to vtable resolver function
        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = ");
        target.print("add i32 ");
        write(value, true);
        target.print(", 16");
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "offset"));
        target.println(" to i32*");
    }

    private void tempify(final NewArrayExpression e) {
        final String theClassName = LLVMWriterUtils.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        target.print("    %");
        target.print(toTempSymbol(e, "classinit"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.print("()");
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();
        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = ptrtoint i32(i32,i32)* @");
        target.print(theClassName);
        target.print(VTABLEFUNCTIONSUFFIX);
        target.println(" to i32");
    }

    private void tempify(final NewMultiArrayExpression e) {
        final String theClassName = LLVMWriterUtils.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        target.print("    %");
        target.print(toTempSymbol(e, "classinit"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.print("()");
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = ptrtoint i32(i32,i32)* @");
        target.print(theClassName);
        target.print(VTABLEFUNCTIONSUFFIX);
        target.println(" to i32");
    }

    private void tempify(final ArrayEntryExpression e) {
        target.print("    %");
        target.print(toTempSymbol(e, "index"));
        target.print(" = mul i32 4,");
        write(e.incomingDataFlows().get(1), true);
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "index2"));
        target.print(" = add i32 ");
        write(e.incomingDataFlows().get(0), true);
        target.print(", %");
        target.print(toTempSymbol(e, "index"));
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = add i32 20, %");
        target.println(toTempSymbol(e, "index2"));

        target.print("    %");
        target.print(toTempSymbol(e, "ptrptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" to ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.println("*");
    }

    private void tempify(final NewObjectExpression e) {
        final String theClassName = LLVMWriterUtils.toClassName(BytecodeObjectTypeRef.fromUtf8Constant(e.getType().getConstant()));
        target.print("    %");
        target.print(toTempSymbol(e, "classinit"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.print("()");
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = ptrtoint i32(i32,i32)* @");
        target.print(theClassName);
        target.print(VTABLEFUNCTIONSUFFIX);
        target.println(" to i32");
    }

    private void tempify(final TypeOfExpression e) {
        final Value value = e.incomingDataFlows().get(0);

        // Compute offset to vtable resolver function
        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = ");
        target.print("inttoptr i32 ");
        write(value, true);
        target.println(" to i32*");
    }

    private void tempify(final ComputedMemoryLocationReadExpression e) {
        final Value origin = e.incomingDataFlows().get(0);
        final Value offset = e.incomingDataFlows().get(1);
        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = ");
        target.print("add i32 ");
        write(origin, true);
        target.print(", ");
        write(offset, true);
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "offset"));
        target.println(" to i32*");
    }

    private void tempify(final ComputedMemoryLocationWriteExpression e) {
        final Value origin = e.incomingDataFlows().get(0);
        final Value offset = e.incomingDataFlows().get(1);
        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = ");
        target.print("add i32 ");
        write(origin, true);
        target.print(", ");
        write(offset, true);
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "offset"));
        target.println(" to i32*");
    }

    private void tempify(final MemorySizeExpression e) {
        target.print("    %");
        target.print(toTempSymbol(e, "raw"));
        target.println(" = call i32 @llvm.wasm.memory.size.i32(i32 0)");
    }

    private void tempify(final GetFieldExpression e) {
        final BytecodeObjectTypeRef theClass = BytecodeObjectTypeRef.fromUtf8Constant(e.getField().getClassIndex().getClassConstant().getConstant());
        final Value object = e.incomingDataFlows().get(0);

        target.print("    %");
        target.print(toTempSymbol(e, "exp"));
        target.print(" = add i32 ");
        write(object, true);
        target.print(",");

        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(theClass);
        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theClass);
        final BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
        final BytecodeResolvedFields.FieldEntry theField = theInstanceFields.fieldByName(e.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
        target.print(theLayout.offsetForInstanceMember(theField.getValue().getName().stringValue()));
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "exp"));
        target.print(" to ");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.println("*");
    }

    private void tempify(final GetStaticExpression e) {

        final BytecodeResolvedFields.FieldEntry theEntry = implementingClassForStaticField(BytecodeObjectTypeRef.fromUtf8Constant(e.getField().getClassIndex().getClassConstant().getConstant()),
                e.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        final BytecodeObjectTypeRef theClass = theEntry.getProvidingClass().getClassName();
        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theClass);
        final int theStaticOffset = theLayout.offsetForClassMember(theEntry.getValue().getName().stringValue());
        final String theClassName = LLVMWriterUtils.toClassName(theClass);

        target.print("    %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.println("()");

        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = add i32 %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(",");
        target.println(theStaticOffset);

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" to ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.println("*");
    }

    private void tempify(final FloorExpression e) {
        final Value value = e.incomingDataFlows().get(0);
        target.print("    %");
        target.print(toTempSymbol(value, "exp"));
        target.print(" = ");
        write(value, true);
        target.println();
    }

    private void tempify(final EnumConstantsExpression e) {

        target.print("    %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(" = ");

        final Value theClassRef = e.incomingDataFlows().get(0);
        if (theClassRef instanceof Variable) {
            writeSameAssignmentHack(TypeRef.Native.REFERENCE, theClassRef);
        } else {
            write(theClassRef, true);
        }
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = add i32 12, %");
        target.println(toTempSymbol(e, "runtimeclass"));

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "offset"));
        target.println(" to i32*");
    }

    private void tempify(final IFExpression e) {
        final Value value = e.incomingDataFlows().get(0);
        target.print("    %");
        target.print(toTempSymbol(value, "exp"));
        target.print(" = ");
        write(value, true);
        target.println();
    }

    private void tempify(final Expression e) {
        if (e instanceof InvokeStaticMethodExpression) {
            tempify((InvokeStaticMethodExpression) e);
        }
        if (e instanceof InvokeVirtualMethodExpression) {
            tempify((InvokeVirtualMethodExpression) e);
        }
        if (e instanceof IFExpression) {
            tempify((IFExpression) e);
        }
        for (final Value v : e.incomingDataFlows()) {
            if (v instanceof ComputedMemoryLocationReadExpression) {

                tempify((ComputedMemoryLocationReadExpression) v);

            } else if (v instanceof ComputedMemoryLocationWriteExpression) {

                tempify((ComputedMemoryLocationWriteExpression) v);

            } else if (v instanceof GetFieldExpression) {

                tempify((GetFieldExpression) v);

            } else if (v instanceof EnumConstantsExpression) {

                tempify((EnumConstantsExpression) v);

            } else if (v instanceof ArrayLengthExpression) {

                tempify((ArrayLengthExpression) v);

            } else if (v instanceof TypeOfExpression) {

                tempify((TypeOfExpression) v);

            } else if (v instanceof NewObjectExpression) {

                tempify((NewObjectExpression) v);

            } else if (v instanceof NewArrayExpression) {

                tempify((NewArrayExpression) v);

            } else if (v instanceof NewMultiArrayExpression) {

                tempify((NewMultiArrayExpression) v);

            } else if (v instanceof ArrayEntryExpression) {

                tempify((ArrayEntryExpression) v);

            } else if (v instanceof InvokeVirtualMethodExpression) {

                tempify((InvokeVirtualMethodExpression) v);

            } else if (v instanceof InvokeStaticMethodExpression) {

                tempify((InvokeStaticMethodExpression) v);

            } else if (v instanceof MemorySizeExpression) {

                tempify((MemorySizeExpression) v);

            } else if (v instanceof GetStaticExpression) {

                tempify((GetStaticExpression) v);

            } else if (v instanceof FloorExpression) {

                tempify((FloorExpression) v);

            } else if (v instanceof DirectInvokeMethodExpression) {

                // Nothing to be done here

            } else if (v instanceof CompareExpression) {

                // Nothing to be done here

            } else if (v instanceof NewInstanceFromDefaultConstructorExpression) {

                // Nothing to be done here

            } else if (v instanceof NewObjectAndConstructExpression) {

                // Nothing to be done here

            }
        }
    }

    private void write(final ExpressionList list) {
        for (final Expression e : list.toList()) {
            tempify(e);
            if (e instanceof ReturnExpression) {
                write((ReturnExpression) e);
            } else if (e instanceof VariableAssignmentExpression) {
                write((VariableAssignmentExpression) e);
            } else if (e instanceof ReturnValueExpression) {
                write((ReturnValueExpression) e);
            } else if (e instanceof GotoExpression) {
                write((GotoExpression) e);
            } else if (e instanceof IFExpression) {
                write((IFExpression) e);
                // Terminal expression
                return;
            } else if (e instanceof InvokeStaticMethodExpression) {
                target.print("    ");
                write((InvokeStaticMethodExpression) e);
                target.println();
            } else if (e instanceof SetMemoryLocationExpression) {
                write((SetMemoryLocationExpression) e);
            } else if (e instanceof UnreachableExpression) {
                write((UnreachableExpression) e);
            } else if (e instanceof PutFieldExpression) {
                write((PutFieldExpression) e);
            } else if (e instanceof DirectInvokeMethodExpression) {
                target.print("    ");
                write((DirectInvokeMethodExpression) e);
            } else if (e instanceof InvokeVirtualMethodExpression) {
                target.print("    ");
                write((InvokeVirtualMethodExpression) e);
                target.println();
            } else if (e instanceof PutStaticExpression) {
                write((PutStaticExpression) e);
            } else if (e instanceof ThrowExpression) {
                write((ThrowExpression) e);
            } else if (e instanceof ArrayStoreExpression) {
                write((ArrayStoreExpression) e);
            } else if (e instanceof TableSwitchExpression) {
                write((TableSwitchExpression) e);
            } else if (e instanceof SetEnumConstantsExpression) {
                write((SetEnumConstantsExpression) e);
            } else if (e instanceof LookupSwitchExpression) {
                write((LookupSwitchExpression) e);
            } else {
                throw new IllegalStateException("Not implemented : " + e.getClass());
            }
        }
    }

    private void write(final SetEnumConstantsExpression e) {
        final Value classptr = e.incomingDataFlows().get(0);
        final Value value = e.incomingDataFlows().get(1);

        target.print("    %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(" = ");
        writeResolved(classptr);
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = ");
        target.print("add i32 %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(", 12");
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "offset"));
        target.println(" to i32*");

        target.print("    store i32 ");
        writeResolved(value);
        target.print(", i32* %");
        target.println(toTempSymbol(e, "ptr"));
    }

    private void write(final LookupSwitchExpression e) {
        target.print("    switch i32 ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(", label %block");
        target.print(e.getDefaultJumpTarget().getAddress());
        target.println(" [");
        for (final Map.Entry<Long, ExpressionList> theEntry : e.getPairs().entrySet()) {
            target.print("       i32 ");
            target.print(theEntry.getKey());
            target.print(",");
            for (final Expression ex : theEntry.getValue().toList()) {
                if (ex instanceof GotoExpression) {
                    final GotoExpression g = (GotoExpression) ex;
                    target.print(" label %block");
                    target.println(g.jumpTarget().getAddress());
                }
            }
        }
        target.print("    ]");
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();

    }

    private void write(final TableSwitchExpression e) {
        target.print("    %");
        target.print(toTempSymbol(e, "value"));
        target.print(" = add i32 0,");
        writeResolved(e.incomingDataFlows().get(0));
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "cond"));
        target.print(" = call i1 @exceedsrange(i32 %");
        target.print(toTempSymbol(e, "value"));
        target.print(", i32 ");
        target.print(e.getLowValue());
        target.print(", i32 ");
        target.print(e.getHighValue());
        target.println(")");

        target.print("    br i1 %");
        target.print(toTempSymbol(e, "cond"));
        target.print(", label %block");
        target.print(e.getDefaultJumpTarget().getAddress());
        target.print(", label %");
        target.println(toTempSymbol(e, "else"));

        target.print(toTempSymbol(e, "else"));
        target.println(":");

        target.print("    %");
        target.print(toTempSymbol(e, "sub"));
        target.print(" = sub i32 %");
        target.print(toTempSymbol(e, "value"));
        target.print(", ");
        target.println(e.getLowValue());

        target.print("    switch i32 %");
        target.print(toTempSymbol(e, "sub"));
        target.print(", label %");
        target.print(toTempSymbol(e, "trap"));
        target.println(" [");
        for (final Map.Entry<Long, ExpressionList> theEntry : e.getOffsets().entrySet()) {
            target.print("       i32 ");
            target.print(theEntry.getKey());
            target.print(",");
            for (final Expression ex : theEntry.getValue().toList()) {
                if (ex instanceof GotoExpression) {
                    final GotoExpression g = (GotoExpression) ex;
                    target.print(" label %block");
                    target.println(g.jumpTarget().getAddress());
                }
            }
        }
        target.print("    ]");
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();

        target.print(toTempSymbol(e, "trap"));
        target.println(":");
        target.println("    call void @llvm.trap()");
        target.println("    unreachable");
    }

    private void write(final ArrayStoreExpression e) {
        target.print("    %");
        target.print(toTempSymbol(e, "index"));
        target.print(" = mul i32 4,");
        writeResolved(e.incomingDataFlows().get(1));
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "index2"));
        target.print(" = add i32 ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(", %");
        target.print(toTempSymbol(e, "index"));
        target.println();

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = add i32 20, %");
        target.println(toTempSymbol(e, "index2"));

        target.print("    %");
        target.print(toTempSymbol(e, "ptrptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" to ");
        target.print(LLVMWriterUtils.toType(e.getArrayType()));
        target.println("*");

        target.print("    store ");
        target.print(LLVMWriterUtils.toType(e.getArrayType()));
        target.print(" ");
        writeResolved(e.incomingDataFlows().get(2));
        target.print(", ");
        target.print(LLVMWriterUtils.toType(e.getArrayType()));
        target.print("* %");
        target.print(toTempSymbol(e, "ptrptr"));
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();

    }

    private void write(final NewArrayExpression e) {
        final String theMethodName = LLVMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "newArray",
                new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        target.print("call i32 @");
        target.print(theMethodName);
        target.print("(i32 0,i32 ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(",i32 %");
        target.print(toTempSymbol(e, "classinit"));
        target.print(",i32 %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(")");
        currentSubProgram.writeDebugSuffixFor(e, target);
    }

    private void write(final ThrowExpression e) {
        target.println("    call void @llvm.trap()");
        target.println("    unreachable");
    }

    private void write(final PutStaticExpression e) {

        final BytecodeResolvedFields.FieldEntry theEntry = implementingClassForStaticField(BytecodeObjectTypeRef.fromUtf8Constant(e.getField().getClassIndex().getClassConstant().getConstant()),
                e.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        final BytecodeObjectTypeRef theClass = theEntry.getProvidingClass().getClassName();
        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theClass);
        final int theStaticOffset = theLayout.offsetForClassMember(theEntry.getValue().getName().stringValue());
        final String theClassName = LLVMWriterUtils.toClassName(theClass);

        target.print("    %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.println("()");

        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(theClass);
        final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
        final BytecodeResolvedFields.FieldEntry theField = theStaticFields.fieldByName(e.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = add i32 %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(",");
        target.println(theStaticOffset);

        target.print("    %");
        target.print(toTempSymbol(e, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" to ");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.println("*");

        target.print("    store ");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.print(" ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(",");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.print("* %");
        target.print(toTempSymbol(e, "ptr"));
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();

    }

    private void write(final InvokeVirtualMethodExpression e) {

        final Value invocationTarget = e.incomingDataFlows().get(0);

        final BytecodeTypeRef theInvokedClassName = e.getInvokedClass();
        if (!theInvokedClassName.isPrimitive() && !theInvokedClassName.isArray()) {
            final BytecodeLinkedClass theInvokedClass = linkerContext.resolveClass((BytecodeObjectTypeRef) theInvokedClassName);
            if (theInvokedClass.isOpaqueType()) {
                final BytecodeResolvedMethods theMethods = theInvokedClass.resolvedMethods();
                final List<BytecodeResolvedMethods.MethodEntry> theImplMethods = theMethods.stream().filter(
                        t -> t.getValue().getName().stringValue().equals(e.getMethodName()) &&
                                t.getValue().getSignature().matchesExactlyTo(e.getSignature()))
                        .collect(Collectors.toList());
                if (theImplMethods.size() != 1) {
                    throw new IllegalStateException("Cannot find unique method " + e.getMethodName() + " with signature " + e.getSignature() + " in " + theInvokedClassName.name());
                }
                final BytecodeLinkedClass theImplClass = theImplMethods.get(0).getProvidingClass();
                final BytecodeMethod theMethod = theImplMethods.get(0).getValue();
                if (!theMethod.isConstructor()) {
                    // At this point, we are creating a direct call invocation to the function
                    // Which is imported fom the WASM Host environment

                    target.print("call ");
                    target.print(LLVMWriterUtils.toSignature(e.getSignature()));
                    target.print(" @");
                    target.print(LLVMWriterUtils.toMethodName(theImplClass.getClassName(), e.getMethodName(), e.getSignature()));
                    target.print(" (i32 ");
                    writeResolved(invocationTarget);
                    for (int i=0;i<e.getSignature().getArguments().length;i++) {
                        target.print(",");
                        target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
                        target.print(" ");
                        writeResolved(e.incomingDataFlows().get(i + 1));
                    }
                    target.print(")");
                    currentSubProgram.writeDebugSuffixFor(e, target);
                    return;
                }
            }
        }

        // Invoke function
        target.print("call ");
        target.print(LLVMWriterUtils.toSignature(e.getSignature()));
        target.print(" %");
        target.print(toTempSymbol(e, "resolved_ptr"));
        target.print(" (i32 ");
        writeResolved(invocationTarget);
        for (int i=0;i<e.getSignature().getArguments().length;i++) {
            target.print(",");
            target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
            target.print(" ");
            writeResolved(e.incomingDataFlows().get(i + 1));
        }
        target.print(")");
        currentSubProgram.writeDebugSuffixFor(e, target);
    }

    private void write(final DirectInvokeMethodExpression e) {

        final BytecodeLinkedClass theTargetClass = linkerContext.resolveClass(e.getClazz());
        final String theMethodName = e.getMethodName();
        final BytecodeMethodSignature theSignature = e.getSignature();

        if (theTargetClass.isOpaqueType() && !theMethodName.equals("<init>")) {
            target.print("call ");
            target.print(LLVMWriterUtils.toSignature(theSignature));
            target.print(" @");
            target.print(LLVMWriterUtils.toMethodName(theTargetClass.getClassName(), theMethodName, theSignature));
            target.print("(");
            final List<Value> theValues = e.incomingDataFlows();
            for (int i=0;i<theValues.size();i++) {
                if (i>0) {
                    target.print(",");
                }
                if (i == 0) {
                    target.print("i32");
                } else {
                    target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i - 1])));
                }
                target.print(" ");
                writeResolved(theValues.get(i));
            }
            target.print(")");
            return;
        }

        target.print("call ");
        target.print(LLVMWriterUtils.toSignature(theSignature));
        target.print(" @");
        if (!e.getMethodName().equals("<init>")) {
            final BytecodeResolvedMethods theResolvedMethods = theTargetClass.resolvedMethods();
            final BytecodeResolvedMethods.MethodEntry theEntry = theResolvedMethods.implementingClassOf(theMethodName, theSignature);

            target.print(LLVMWriterUtils.toMethodName(theEntry.getProvidingClass().getClassName(), theMethodName, theSignature));
        } else {
            target.print(LLVMWriterUtils.toMethodName(e.getClazz(), theMethodName, theSignature));
        }
        target.print("(");
        final List<Value> theValues = e.incomingDataFlows();
        for (int i=0;i<theValues.size();i++) {
            if (i>0) {
                target.print(",");
            }
            if (i == 0) {
                target.print("i32");
            } else {
                target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i - 1])));
            }
            target.print(" ");
            writeResolved(theValues.get(i));
        }
        target.print(")");
        currentSubProgram.writeDebugSuffixFor(e, target);
        target.println();
    }

    private void write(final PutFieldExpression expression) {
        final Value object = expression.incomingDataFlows().get(0);
        final Value value = expression.incomingDataFlows().get(1);

        target.print("    %");
        target.print(toTempSymbol(expression, "exp"));
        target.print(" = add i32 ");
        writeResolved(object);
        target.print(",");

        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(expression.getField().getClassIndex().getClassConstant().getConstant()));
        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theLinkedClass.getClassName());
        final BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
        final BytecodeResolvedFields.FieldEntry theField = theInstanceFields.fieldByName(expression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        target.print(theLayout.offsetForInstanceMember(theField.getValue().getName().stringValue()));
        target.println();

        target.print("    %");
        target.print(toTempSymbol(expression, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(expression, "exp"));
        target.print(" to ");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.println("*");


        target.print("    store ");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.print(" ");
        writeResolved(value);
        target.print(",");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.print("* %");
        target.print(toTempSymbol(expression, "ptr"));
        currentSubProgram.writeDebugSuffixFor(expression, target);
        target.println();

    }

    private void write(final UnreachableExpression expression) {
        target.println("    call void @llvm.trap()");
        target.println("    unreachable");
    }

    private void write(final SetMemoryLocationExpression expression) {
        final Value location = expression.incomingDataFlows().get(0);
        final Value value = expression.incomingDataFlows().get(1);
        target.print("    store i32 ");
        writeResolved(value);
        target.print(", ");
        writeResolved(location);
        target.println();
    }

    private void write(final IFExpression expression) {
        final Set<BytecodeOpcodeAddress> forwardNodes = currentNode.outgoingEdges()
                .filter((Predicate<Edge<? extends EdgeType, ? extends Node>>) edge -> RegionNode.ALL_SUCCCESSORS_REGULAR_FLOW_ONLY
                        .test((Edge<EdgeType, RegionNode>) edge))
                .map(t -> t.targetNode().getStartAddress()).collect(Collectors.toSet());
        if (forwardNodes.size() == 2) {
            target.print("    br i1 ");
            writeResolved(expression.incomingDataFlows().get(0));
            target.print(", label %block");
            target.print(expression.getGotoAddress().getAddress());

            forwardNodes.remove(expression.getGotoAddress());

            if (forwardNodes.size() == 1) {
                final BytecodeOpcodeAddress theElse = forwardNodes.iterator().next();
                target.print(", label %block");
                target.print(theElse.getAddress());
                currentSubProgram.writeDebugSuffixFor(expression, target);
                target.println();
            } else {
                throw new IllegalArgumentException("Expected one node for else branch of if statement, got " + forwardNodes);
            }
        } else if (forwardNodes.size() == 1) {
            // True and false jump targets are identical, we still make a conditional jump
            // as the condition must still be evaluated. This is done to prevent the optimizer from removing it
            target.print("    br i1 ");
            writeResolved(expression.incomingDataFlows().get(0));
            target.print(", label %block");
            target.print(expression.getGotoAddress().getAddress());
            target.print(", label %block");
            target.print(expression.getGotoAddress().getAddress());
            currentSubProgram.writeDebugSuffixFor(expression, target);
            target.println();
        } else {
            // Don't know what to do here
            throw new IllegalArgumentException("Expected one node for else branch of if statement, got " + forwardNodes);
        }
    }

    private void write(final GotoExpression expression) {
        target.print("    br label %");
        final BytecodeOpcodeAddress jumpTo = expression.jumpTarget();
        target.print("block");
        target.println(jumpTo.getAddress());
    }

    private void write(final ReturnExpression expression) {
        target.print("    ");
        target.print("ret void");
        currentSubProgram.writeDebugSuffixFor(expression, target);
        target.println();
    }

    private void write(final ReturnValueExpression expression) {
        final Value result = expression.incomingDataFlows().get(0);
        target.print("    ");
        target.print("ret ");
        target.print(LLVMWriterUtils.toType(result.resolveType()));
        target.print(" ");
        writeResolved(result);
        currentSubProgram.writeDebugSuffixFor(expression, target);
        target.println();
    }

    private void write(final VariableAssignmentExpression expression) {
        final Value value = expression.incomingDataFlows().get(0);
        target.print("    ");
        target.print("%");
        target.print(expression.getVariable().getName());
        target.print("_ = ");
        if (value instanceof Variable) {
            switch (value.resolveType().resolve()) {
                case FLOAT:
                case DOUBLE:
                    target.print("fadd float %");
                    target.print(((Variable) value).getName());
                    target.print("_, 0.0");
                    break;
                default:
                    target.print("add i32 %");
                    target.print(((Variable) value).getName());
                    target.print("_, 0");
                    break;
            }
        } else {
            if (value instanceof DoubleValue) {
                target.print("fadd float 0.0,");
            }
            if (value instanceof FloatValue) {
                target.print("fadd float 0.0,");
            }
            if (value instanceof IntegerValue) {
                target.print("add i32 0,");
            }
            if (value instanceof ShortValue) {
                target.print("add i32 0,");
            }
            if (value instanceof ByteValue) {
                target.print("add i32 0,");
            }
            if (value instanceof LongValue) {
                target.print("add i32 0,");
            }
            if (value instanceof PHIValue) {
                switch (value.resolveType().resolve()) {
                    case FLOAT:
                    case DOUBLE:
                        target.print("fadd float 0.0,");
                        break;
                    default:
                        target.print("add i32 0,");
                        break;
                }
            }
            write(value, true);
        }
        currentSubProgram.writeDebugSuffixFor(expression, target);
        target.println();
    }

    private void writeResolved(final Value aValue) {
        if (aValue instanceof Expression && (!(aValue instanceof ComputedMemoryLocationWriteExpression))) {
            target.write("%");
            target.write(toTempSymbol(aValue, "exp"));
        } else {
            write(aValue, true);
        }
    }

    private void write(final Value aValue, final boolean useDirectVarRef) {
        if (aValue instanceof Variable) {
            final Variable v = (Variable) aValue;
            if (useDirectVarRef) {
                target.print("%");
            } else {
                switch (v.resolveType().resolve()) {
                    case FLOAT:
                    case DOUBLE:
                        target.print("fadd float 0,%");
                        break;
                    default:
                        target.print("add i32 0,%");
                        break;
                }
            }
            target.print(v.getName());
            target.print("_");
        } else if (aValue instanceof IntegerValue) {
            write((IntegerValue) aValue);
        } else if (aValue instanceof LongValue) {
            write((LongValue) aValue);
        } else if (aValue instanceof InvokeStaticMethodExpression) {
            write((InvokeStaticMethodExpression) aValue);
        } else if (aValue instanceof BinaryExpression) {
            write((BinaryExpression) aValue);
        } else if (aValue instanceof PHIValue) {
            write((PHIValue) aValue);
        } else if (aValue instanceof MemorySizeExpression) {
            write((MemorySizeExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationWriteExpression) {
            write((ComputedMemoryLocationWriteExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationReadExpression) {
            write((ComputedMemoryLocationReadExpression) aValue);
        } else if (aValue instanceof TypeConversionExpression) {
            write((TypeConversionExpression) aValue);
        } else if (aValue instanceof StackTopExpression) {
            write((StackTopExpression) aValue);
        } else if (aValue instanceof NewObjectAndConstructExpression) {
            write((NewObjectAndConstructExpression) aValue);
        } else if (aValue instanceof GetFieldExpression) {
            write((GetFieldExpression) aValue);
        } else if (aValue instanceof DirectInvokeMethodExpression) {
            write((DirectInvokeMethodExpression) aValue);
        } else if (aValue instanceof NullValue) {
            write((NullValue) aValue);
        } else if (aValue instanceof GetStaticExpression) {
            write((GetStaticExpression) aValue);
        } else if (aValue instanceof HeapBaseExpression) {
            write((HeapBaseExpression) aValue);
        } else if (aValue instanceof SystemHasStackExpression) {
            write((SystemHasStackExpression) aValue);
        } else if (aValue instanceof DataEndExpression) {
            write((DataEndExpression) aValue);
        } else if (aValue instanceof StringValue) {
            write((StringValue) aValue);
        } else if (aValue instanceof InvokeVirtualMethodExpression) {
            write((InvokeVirtualMethodExpression) aValue);
        } else if (aValue instanceof ClassReferenceValue) {
            write((ClassReferenceValue) aValue);
        } else if (aValue instanceof NewArrayExpression) {
            write((NewArrayExpression) aValue);
        } else if (aValue instanceof ArrayLengthExpression) {
            write((ArrayLengthExpression) aValue);
        } else if (aValue instanceof FixedBinaryExpression) {
            write((FixedBinaryExpression) aValue);
        } else if (aValue instanceof ArrayEntryExpression) {
            write((ArrayEntryExpression) aValue);
        } else if (aValue instanceof NewInstanceFromDefaultConstructorExpression) {
            write((NewInstanceFromDefaultConstructorExpression) aValue);
        } else if (aValue instanceof InstanceOfExpression) {
            write((InstanceOfExpression) aValue);
        } else if (aValue instanceof NewObjectExpression) {
            write((NewObjectExpression) aValue);
        } else if (aValue instanceof CompareExpression) {
            write((CompareExpression) aValue);
        } else if (aValue instanceof NegatedExpression) {
            write((NegatedExpression) aValue);
        } else if (aValue instanceof FloorExpression) {
            write((FloorExpression) aValue);
        } else if (aValue instanceof MinExpression) {
            write((MinExpression) aValue);
        } else if (aValue instanceof TypeOfExpression) {
            write((TypeOfExpression) aValue);
        } else if (aValue instanceof IsNaNExpression) {
            write((IsNaNExpression) aValue);
        } else if (aValue instanceof FloatValue) {
            write((FloatValue) aValue);
        } else if (aValue instanceof ByteValue) {
            write((ByteValue) aValue);
        } else if (aValue instanceof ResolveCallsiteObjectExpression) {
            write((ResolveCallsiteObjectExpression) aValue);
        } else if (aValue instanceof DoubleValue) {
            write((DoubleValue) aValue);
        } else if (aValue instanceof FloatingPointCeilExpression) {
            write((FloatingPointCeilExpression) aValue);
        } else if (aValue instanceof FloatingPointFloorExpression) {
            write((FloatingPointFloorExpression) aValue);
        } else if (aValue instanceof MaxExpression) {
            write((MaxExpression) aValue);
        } else if (aValue instanceof RuntimeGeneratedTypeExpression) {
            write((RuntimeGeneratedTypeExpression) aValue);
        } else if (aValue instanceof EnumConstantsExpression) {
            write((EnumConstantsExpression) aValue);
        } else if (aValue instanceof MethodTypeArgumentCheckExpression) {
            write((MethodTypeArgumentCheckExpression) aValue);
        } else if (aValue instanceof ReinterpretAsNativeExpression) {
            write((ReinterpretAsNativeExpression) aValue);
        } else if (aValue instanceof SqrtExpression) {
            write((SqrtExpression) aValue);
        } else if (aValue instanceof NewMultiArrayExpression) {
            write((NewMultiArrayExpression) aValue);
        } else if (aValue instanceof SuperTypeOfExpression) {
            write((SuperTypeOfExpression) aValue);
        } else if (aValue instanceof MethodHandlesGeneratedLookupExpression) {
            write((MethodHandlesGeneratedLookupExpression) aValue);
        } else if (aValue instanceof MethodTypeExpression) {
            write((MethodTypeExpression) aValue);
        } else if (aValue instanceof MethodRefExpression) {
            write((MethodRefExpression) aValue);
        } else if (aValue instanceof PtrOfExpression) {
            write((PtrOfExpression) aValue);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue.getClass());
        }
    }

    private void write(final PtrOfExpression aValue) {
        final Value v = aValue.incomingDataFlows().get(0);
        if (v instanceof Variable) {
            writeSameAssignmentHack(v.resolveType(), v);
        } else {
            write(v, true);
        }
    }

    private void write(final MethodRefExpression e) {
        target.print("ptrtoint ");

        target.print(LLVMWriterUtils.toSignature(e.getSignature()));

        final String theMethodName = LLVMWriterUtils.toMethodName(
                e.getClassName(),
                e.getMethodName(),
                e.getSignature());

        target.print("* @");
        target.print(theMethodName);

        target.print(" to i32");
    }

    private void write(final MethodTypeExpression e) {
        final BytecodeMethodSignature theSignature = e.getSignature();
        target.print("call i32 @");
        target.print(symbolResolver.methodTypeFactoryNameFor(theSignature));
        target.print("()");
    }

    private void write(final MethodHandlesGeneratedLookupExpression e) {
        target.print("add i32 0, 0");
    }

    private void write(final SuperTypeOfExpression e) {
        target.print("call i32 @jlClass_jlClassgetSuperclass(i32 ");
        write(e.incomingDataFlows().get(0), true);
        target.print(")");
    }

    private void write(final NewMultiArrayExpression e) {
        final List<Value> theDimensions = e.incomingDataFlows();

        final String theMethodName;
        switch (theDimensions.size()) {
            case 1:
                theMethodName = LLVMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "newArray",
                        new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
                break;
            case 2:
                theMethodName = LLVMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "newArray",
                        new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));
                break;
            default:
                throw new IllegalStateException("Unsupported number of dimensions : " + theDimensions.size());
        }

        target.write("call i32 @");
        target.write(theMethodName);
        target.write("(i32 0");
        for (final Value theDimension : theDimensions) {
            target.write(",i32 ");
            write(theDimension, true);
        }
        target.write(",i32 %");
        target.write(toTempSymbol(e, "classinit"));
        target.write(",i32 %");
        target.write(toTempSymbol(e, "vtable"));
        target.write(")");
        currentSubProgram.writeDebugSuffixFor(e, target);
    }

    private void write(final SqrtExpression e) {
        target.print("call float @llvm.sqrt.f32(float ");
        write(e.incomingDataFlows().get(0), true);
        target.print(")");
    }

    private void write(final ReinterpretAsNativeExpression e) {
        final Value theValue = e.incomingDataFlows().get(0);
        switch (e.getExpectedType()) {
            case FLOAT:
            case DOUBLE:
                target.print("sitofp i32 ");
                write(theValue, true);
                target.print(" to float");
                break;
            default:
                writeSameAssignmentHack(theValue.resolveType(), theValue);
                break;
        }
    }

    private void write(final MethodTypeArgumentCheckExpression e) {
        final TypeRef.Native theExpectedType = e.getExpectedType();
        final Value theMethodType = e.incomingDataFlows().get(0);
        final Value theIndex = e.incomingDataFlows().get(1);

        target.print("call i32 @checkmethodtype(i32 ");
        write(theMethodType, true);
        target.print(", i32 ");
        write(theIndex, true);
        target.print(", i32 ");
        target.print(- theExpectedType.ordinal());
        target.print(")");
    }

    private void write(final EnumConstantsExpression e) {
        target.print("load i32, i32* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final RuntimeGeneratedTypeExpression e) {
        target.print("call i32 @newlambda(i32 ");
        write(e.getType(), true);
        target.print(",i32 ");
        write(e.getMethodRef(), true);
        target.print(",i32 ");
        write(e.getStaticArguments(), true);
        target.print(")");
    }

    private void write(final MaxExpression e) {
        target.print("call ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));

        switch (e.resolveType().resolve()) {
            case FLOAT:
            case DOUBLE:
                target.print(" @llvm.maximum.f32");
                break;
            default:
                target.print(" @maximum");
                break;
        }
        target.print("(");
        target.print(LLVMWriterUtils.toType(e.incomingDataFlows().get(0).resolveType()));
        target.print(" ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(",");
        target.print(LLVMWriterUtils.toType(e.incomingDataFlows().get(1).resolveType()));
        target.print(" ");
        writeResolved(e.incomingDataFlows().get(1));
        target.print(")");
    }

    private void write(final FloatingPointFloorExpression e) {
        target.print("call float @llvm.floor.f32(float ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(")");
    }

    private void write(final FloatingPointCeilExpression e) {
        target.print("call float @llvm.ceil.f32(float ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(")");
    }

    private void write(final ResolveCallsiteObjectExpression e) {
        target.print("call i32 @");
        target.print(symbolResolver.resolveCallsiteBootstrapFor(e.getOwningClass(),
                e.getCallsiteId(),
                e.getProgram(),
                e.getBootstrapMethod()
        ));
        target.print("()");
    }

    private void write(final ByteValue e) {
        target.print(e.getByteValue());
    }

    private void write(final FloatValue e) {
        final long doubleBits = Double.doubleToRawLongBits(e.getFloatValue());
        target.print(String.format("0x%X", doubleBits));
    }

    private void write(final DoubleValue e) {
        final long doubleBits = Double.doubleToRawLongBits((float) e.getDoubleValue());
        target.print(String.format("0x%X", doubleBits));
    }

    private void write(final IsNaNExpression e) {
        final Value theValue = e.incomingDataFlows().get(0);
        target.print("call i32 @isnan(");
        target.print(LLVMWriterUtils.toType(theValue.resolveType()));
        target.print(" ");
        writeResolved(theValue);
        target.print(")");
    }

    private void write(final TypeOfExpression e) {
        target.print("load i32, i32* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final MinExpression e) {
        target.print("call ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        switch (e.resolveType().resolve()) {
            case FLOAT:
            case DOUBLE:
                target.print(" @llvm.minimum.f32");
                break;
            default:
                target.print(" @minimum");
                break;
        }
        target.print("(");
        target.print(LLVMWriterUtils.toType(e.incomingDataFlows().get(0).resolveType()));
        target.print(" ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(",");
        target.print(LLVMWriterUtils.toType(e.incomingDataFlows().get(1).resolveType()));
        target.print(" ");
        writeResolved(e.incomingDataFlows().get(1));
        target.print(")");
    }

    private void write(final FloorExpression e) {
        target.print("fptosi float ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(" to i32");
    }

    private void write(final NegatedExpression e) {
        switch (e.resolveType().resolve()) {
            case FLOAT:
            case DOUBLE:
                target.print("fneg ");
                target.print(LLVMWriterUtils.toType(e.resolveType()));
                target.print(" ");
                writeResolved(e.incomingDataFlows().get(0));
                break;
            default:
                target.print("mul ");
                target.print(LLVMWriterUtils.toType(e.resolveType()));
                target.print(" ");
                writeResolved(e.incomingDataFlows().get(0));
                target.print(",-1");
                break;
        }
    }

    private void write(final CompareExpression e) {
        final Value theValue1 = e.incomingDataFlows().get(0);
        final Value theValue2 = e.incomingDataFlows().get(1);

        final TypeRef.Native theValue1Type = theValue1.resolveType().resolve();
        final TypeRef.Native theValue2Type = theValue2.resolveType().resolve();
        if (theValue1Type != theValue2Type) {
            throw new IllegalStateException("Does not support mixed types : " + theValue1Type + " -> " + theValue2Type);
        }

        switch (theValue1Type) {
            case FLOAT:
            case DOUBLE:
                target.print("call i32 @compare_f32(");
                break;
            default:
                target.print("call i32 @compare_i32(");
                break;
        }
        target.print(LLVMWriterUtils.toType(theValue1.resolveType()));
        target.print(" ");
        writeResolved(theValue1);
        target.print(",");
        target.print(LLVMWriterUtils.toType(theValue2.resolveType()));
        target.print(" ");
        writeResolved(theValue2);
        target.print(")");
    }

    private void write(final NewObjectExpression e) {
        final String theMethodName = LLVMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "newObject",
                new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        target.print("call i32 @");
        target.print(theMethodName);
        target.print("(i32 0,i32 ");
        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(BytecodeObjectTypeRef.fromUtf8Constant(e.getType().getConstant()));
        target.print(theLayout.instanceSize());
        target.print(",i32 %");
        target.print(toTempSymbol(e, "classinit"));
        target.print(",i32 %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(")");
        currentSubProgram.writeDebugSuffixFor(e, target);
    }

    private void write(final InstanceOfExpression e) {
        target.print("call i32 @instanceof(i32 ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(",i32 ");
        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(e.getType().getConstant()));
        target.print(theLinkedClass.getUniqueId());
        target.print(")");
    }

    private void write(final NewInstanceFromDefaultConstructorExpression e) {
        target.write("call i32 @");
        target.write(NEWINSTANCEHELPER);
        target.write("(i32 ");
        writeResolved(e.incomingDataFlows().get(0));
        target.write(")");
    }

    private void write(final ArrayEntryExpression e) {
        target.print("load ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.print(", ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.print("* %");
        target.print(toTempSymbol(e, "ptrptr"));
        target.println();
    }

    private void write(final FixedBinaryExpression e) {
        switch (e.getOperator()) {
            case ISNULL:
                target.print("icmp eq i32 ");
                writeResolved(e.incomingDataFlows().get(0));
                target.print(",0");
                break;
            case ISZERO:
                target.print("icmp eq i32 ");
                writeResolved(e.incomingDataFlows().get(0));
                target.print(",0");
                break;
            case ISNONNULL:
                target.print("icmp ne i32 ");
                writeResolved(e.incomingDataFlows().get(0));
                target.print(",0");
                break;
            default:
                throw new IllegalStateException("Not implemented : " + e.getOperator());
        }
    }

    private void write(final ArrayLengthExpression e) {
        target.print("load i32 ");
        target.print(",i32* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final ClassReferenceValue e) {
        target.print("call i32 @");
        target.print(LLVMWriterUtils.toClassName(e.getType()));
        target.print(LLVMWriter.CLASSINITSUFFIX);
        target.print("()");
    }

    private void write(final StringValue e) {
        target.print("load i32, i32* @");
        target.print(symbolResolver.globalFromStringPool(e.getStringValue()));
    }

    private void write(final SystemHasStackExpression e) {
        target.print("add i32 0, 1");
    }

    private void write(final HeapBaseExpression e) {
        target.print("ptrtoint i32* @__heap_base to i32");
    }

    private void write(final DataEndExpression e) {
        target.print("ptrtoint i32* @__data_end to i32");
    }

    private void write(final GetStaticExpression e) {
        target.print("load ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.print(", ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.print("* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final NullValue e) {
        target.print("add i32 0, 0");
    }

    private void write(final GetFieldExpression e) {
        target.print("load ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.print(", ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        target.print("* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final NewObjectAndConstructExpression e) {
        target.print("call i32 (");
        for (int i=0;i<e.getSignature().getArguments().length;i++) {
            if (i>0) {
                target.print(",");
            }
            target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
        }
        target.print(") @");
        target.print(LLVMWriterUtils.toMethodName(e.getClazz(), LLVMWriter.NEWINSTANCE_METHOD_NAME, e.getSignature()));
        target.print("(");
        for (int i=0;i<e.incomingDataFlows().size();i++) {
            if (i>0) {
                target.print(",");
            }
            target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
            target.print(" ");
            writeResolved(e.incomingDataFlows().get(i));
        }
        target.println(")");
    }

    private void write(final StackTopExpression e) {
        target.print("load i32, i32* @stacktop");
    }

    private void writeSameAssignmentHack(final TypeRef theType, final Value aValue) {
        switch (theType.resolve()) {
            case FLOAT:
            case DOUBLE:
                target.print("fadd float 0.0,");
                break;
            default:
                target.print("add i32 0,");
                break;
        }
        writeResolved(aValue);
    }

    private void write(final TypeConversionExpression e) {
        final TypeRef theTargetType = e.resolveType();
        final Value theSource = e.incomingDataFlows().get(0);
        if (Objects.equals(theTargetType.resolve(), theSource.resolveType().resolve())) {
            // Same type, nothing to be done here
            writeSameAssignmentHack(theTargetType, theSource);
            return;
        }
        switch (theSource.resolveType().resolve()) {
            case DOUBLE:
            case FLOAT: {
                // Convert floating point to something else
                switch (e.resolveType().resolve()) {
                    case DOUBLE:
                    case FLOAT: {
                        // No conversion needed
                        writeSameAssignmentHack(theTargetType, theSource);
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
                    case LONG:
                    case CHAR: {
                        // Convert f32 to i32
                        // NaN == 0
                        target.print("call i32 @toi32(float ");
                        writeResolved(theSource);
                        target.print(")");
                        return;
                    }
                    default:
                        throw new IllegalStateException("Coversion to " + e.resolveType() + " not supported!");
                }
            }
            case INT:
            case LONG:
            case BYTE:
            case SHORT:
            case CHAR: {
                // Convert integer type to something else
                // Convert floating point to something else
                switch (e.resolveType().resolve()) {
                    case DOUBLE:
                    case FLOAT: {
                        // Convert i32 to f32
                        target.write("sitofp i32 ");
                        writeResolved(theSource);
                        target.write(" to float");
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
                    case LONG:
                    case CHAR: {
                        // No conversion needed
                        writeSameAssignmentHack(theTargetType, theSource);
                        return;
                    }
                    default:
                        throw new IllegalStateException("target type " + e.resolveType() + " not supported!");
                }
            }
            default:
                throw new IllegalStateException("Conversion to " + e.resolveType() + " not supported!");
        }
   }

    private void write(final ComputedMemoryLocationReadExpression e) {
        target.print("load i32, i32* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final ComputedMemoryLocationWriteExpression e) {
        target.print("i32* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final MemorySizeExpression aValue) {
        target.print("mul i32 %");
        target.print(toTempSymbol(aValue, "raw"));
        target.print(", 65536");
    }

    private void write(final PHIValue aValue) {
        target.print("%");
        target.print(toTempSymbol(aValue, "phi"));
    }

    private void write(final BinaryExpression aValue) {
        final Value theValue1 = aValue.incomingDataFlows().get(0);
        switch (aValue.getOperator()) {
            case ADD:
                switch (theValue1.resolveType().resolve()) {
                    case FLOAT:
                    case DOUBLE:
                        target.print("fadd");
                        break;
                    default:
                        target.print("add");
                        break;
                }
                break;
            case SUB:
                switch (theValue1.resolveType().resolve()) {
                    case FLOAT:
                    case DOUBLE:
                        target.print("fsub");
                        break;
                    default:
                        target.print("sub");
                        break;
                }
                break;
            case MUL:
                switch (theValue1.resolveType().resolve()) {
                    case FLOAT:
                    case DOUBLE:
                        target.print("fmul");
                        break;
                    default:
                        target.print("mul");
                        break;
                }
                break;
            case REMAINDER:
                switch (theValue1.resolveType().resolve()) {
                    case FLOAT:
                    case DOUBLE:
                        target.print("frem");
                        break;
                    default:
                        target.print("srem");
                        break;
                }
                break;
            case GREATERTHAN:
                target.print("icmp sgt");
                break;
            case GREATEROREQUALS:
                target.print("icmp sge");
                break;
            case LESSTHAN:
                target.print("icmp slt");
                break;
            case LESSTHANOREQUALS:
                target.print("icmp sle");
                break;
            case EQUALS:
                target.print("icmp eq");
                break;
            case NOTEQUALS:
                target.print("icmp ne");
                break;
            case BINARYSHIFTLEFT:
                target.print("shl");
                break;
            case BINARYSHIFTRIGHT:
                target.print("ashr");
                break;
            case BINARYUNSIGNEDSHIFTRIGHT:
                target.print("lshr");
                break;
            case BINARYOR:
                target.print("or");
                break;
            case BINARYXOR:
                target.print("xor");
                break;
            case BINARYAND:
                target.print("and");
                break;
            case DIV:
                writeDivExpression(aValue);
                return;
            default:
                throw new IllegalStateException("Not implemented : " + aValue.getOperator());
        }
        target.print(" ");
        target.print(LLVMWriterUtils.toType(aValue.resolveType()));
        target.print(" ");
        final List<Value> v = aValue.incomingDataFlows();
        for (int i=0;i<v.size();i++) {
            if (i>0) {
                target.print(",");
            }
            writeResolved(v.get(i));
        }
    }

    private void writeDivExpression(final BinaryExpression e) {
        final Value left = e.incomingDataFlows().get(0);
        final Value right = e.incomingDataFlows().get(1);
        target.print("call float @div_");
        target.print(LLVMWriterUtils.toType(left.resolveType()));
        target.print(LLVMWriterUtils.toType(right.resolveType()));
        target.print("(");
        target.print(LLVMWriterUtils.toType(left.resolveType()));
        target.print(" ");
        writeResolved(left);
        target.print(",");
        target.print(LLVMWriterUtils.toType(right.resolveType()));
        target.print(" ");
        writeResolved(right);
        target.print(")");
    }

    private void write(final IntegerValue aValue) {
        target.print(aValue.getIntValue());
    }

    private void write(final LongValue aValue) {
        target.print(aValue.getLongValue());
    }

    private void write(final InvokeStaticMethodExpression aValue) {
        target.print("call ");
        target.print(LLVMWriterUtils.toSignature(aValue.getSignature()));
        target.print(" @");
        target.print(LLVMWriterUtils.toMethodName(aValue.getClassName(), aValue.getMethodName(), aValue.getSignature()));
        target.print("(i32 %");
        target.print(toTempSymbol(aValue, "runtimeclass"));
        final List<Value> args = aValue.incomingDataFlows();
        for (int i=0;i<args.size();i++) {
            target.print(",");
            final Value theValue = args.get(i);

            target.print(LLVMWriterUtils.toType(TypeRef.toType(aValue.getSignature().getArguments()[i])));
            target.print(" ");

            writeResolved(theValue);
        }
        target.print(")");
        currentSubProgram.writeDebugSuffixFor(aValue, target);

    }
}