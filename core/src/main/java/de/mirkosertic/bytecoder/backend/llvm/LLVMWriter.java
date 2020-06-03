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
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.classlib.java.util.Quicksort;
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
import de.mirkosertic.bytecoder.core.BytecodeVTable;
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
import de.mirkosertic.bytecoder.ssa.DebugInformation;
import de.mirkosertic.bytecoder.ssa.DebugPosition;
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
import de.mirkosertic.bytecoder.ssa.InvokeDirectMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.IsNaNExpression;
import de.mirkosertic.bytecoder.ssa.LambdaConstructorReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaInterfaceReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaSpecialReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaVirtualReferenceExpression;
import de.mirkosertic.bytecoder.ssa.LambdaWithStaticImplExpression;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.LookupSwitchExpression;
import de.mirkosertic.bytecoder.ssa.MaxExpression;
import de.mirkosertic.bytecoder.ssa.MemorySizeExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandleExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeArgumentCheckExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.MinExpression;
import de.mirkosertic.bytecoder.ssa.NegatedExpression;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PtrOfExpression;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteInstanceExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.SqrtExpression;
import de.mirkosertic.bytecoder.ssa.StackTopExpression;
import de.mirkosertic.bytecoder.ssa.StaticDependencies;
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

    public static final String INSTANCEOFSUFFIX = "__instanceof";
    public static final String RUNTIMECLASSSUFFIX = "__runtimeclass";
    public static final String RUNTIMECLASSINITSTATUSSUFFIX = "__runtimeclassinitstatus";
    public static final String NEWINSTANCE_METHOD_NAME = "$newInstance";
    public static final String CLASSINITSUFFIX = "__init";
    public static final String VTABLESUFFIX = "__vtable";
    public static final String VTABLETYPESUFFIX = "__vtable__type";
    public static final String NEWINSTANCEHELPER = "bytecoder.newinstancehelper";
    public static final String INTERFACEDISPATCHSUFFIX = "__interfacedispatch";

    interface SymbolResolver {

        String globalFromStringPool(final String aValue);

        String resolveCallsiteBootstrapFor(BytecodeLinkedClass owningClass, String callsiteId, Program program, RegionNode bootstrapMethod);

        String methodTypeFactoryNameFor(final BytecodeMethodSignature aSignature);

        BytecodeVTable vtableFor(final BytecodeLinkedClass aClass);

        String methodHandleDelegateFor(MethodHandleExpression aValue);
    }

    private final PrintWriter target;
    private RegionNode currentNode;
    private DebugInformation currentDebugInformation;
    private LLVMDebugInformation.SubProgram currentSubProgram;
    private final NativeMemoryLayouter memoryLayouter;
    private final BytecodeLinkerContext linkerContext;
    private final SymbolResolver symbolResolver;
    private final boolean stackAllocationEnabled;

    public LLVMWriter(final PrintWriter output, final NativeMemoryLayouter memoryLayouter, final BytecodeLinkerContext linkerContext, final SymbolResolver symbolResolver, final boolean aEscapeAnalysisEnabled) {
        this.target = output;
        this.memoryLayouter = memoryLayouter;
        this.linkerContext = linkerContext;
        this.symbolResolver = symbolResolver;
        this.stackAllocationEnabled = aEscapeAnalysisEnabled;
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

    public void write(final BytecodeLinkedClass aOwningClass, final Program aProgram, final LLVMDebugInformation.SubProgram aSubProgram) {
        final ControlFlowGraph theGraph = aProgram.getControlFlowGraph();
        final RegionNode theStart = theGraph.startNode();
        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(theStart,
                RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        final List<RegionNode> theRegularFlow = order.getNodesInOrder();
        final Set<String> theAlreadySeenPHIs = new HashSet<>();
        currentDebugInformation = aProgram.getDebugInformation();
        currentSubProgram = aSubProgram;
        target.println("entry:");

        // We calculate the static dependencies that should only be initialized once for this method
        final StaticDependencies staticDependencies = new StaticDependencies(aProgram);
        // And we call the class initializers
        for (final BytecodeLinkedClass theClass : staticDependencies.list()) {
            if (!theClass.getClassName().name().equals(MemoryManager.class.getName())) {
                target.print("    %");
                target.print(LLVMWriterUtils.runtimeClassVariableName(theClass.getClassName()));
                // We know the following JVM classes were initialized by the bootstrap,
                // so we can safely access them without init invocation
                if (theClass.getClassName().name().equals(String.class.getName()) ||
                    theClass.getClassName().name().equals(Array.class.getName()) ||
                    theClass.getClassName().name().equals(VM.class.getName()) ||
                    theClass.getClassName().name().equals(Quicksort.class.getName())) {

                    aProgram.getLinkerContext().getStatistics().context("ClassInitialization")
                            .counter("Avoided initializations").increment();

                    target.print(" = load i32, i32* @");
                    target.print(LLVMWriterUtils.toClassName(theClass.getClassName()));
                    target.println(LLVMWriter.RUNTIMECLASSSUFFIX);
                } else {
                    target.print(" = call i32 @");
                    target.print(LLVMWriterUtils.toClassName(theClass.getClassName()));
                    target.print(LLVMWriter.CLASSINITSUFFIX);
                    target.println("()");
                }
            }
        }

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

        final BytecodeVTable table;
        if (e.getInvokedClass().isArray()) {
            final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
            target.println("    ;; vtable of " + theLinkedClass.getClassName().name());
            table = symbolResolver.vtableFor(theLinkedClass);
        } else {
            final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass((BytecodeObjectTypeRef) e.getInvokedClass());
            target.println("    ;; vtable of " + theLinkedClass.getClassName().name());
            table = symbolResolver.vtableFor(theLinkedClass);
        }
        BytecodeVTable.Slot slot;
        boolean fallbackToInterfaceInvocation;
        try {
            slot = table.slotOf(e.getMethodName(), e.getSignature());
            fallbackToInterfaceInvocation = false;
        } catch (final IllegalArgumentException ex) {
            // Fallback to interface invocation
            fallbackToInterfaceInvocation = true;
            slot = null;
        }

        if (e.isInterfaceInvocation() || fallbackToInterfaceInvocation) {
            // Interface invocation uses the dispatcher method instead of the virtual table lookuo

            // Compute offset to dispatcher function
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
            target.print(toTempSymbol(e, "vtableref_offset"));
            target.print(" = add i32 %");
            target.print(toTempSymbol(e, "vtableref"));
            target.println(", 4");

            target.print("    %");
            target.print(toTempSymbol(e, "vtableref_offset_ptr"));
            target.print(" = inttoptr i32 %");
            target.print(toTempSymbol(e, "vtableref_offset"));
            target.println(" to i32*");

            target.print("    %");
            target.print(toTempSymbol(e, "dispatcher"));
            target.print(" = load i32, i32* %");
            target.println(toTempSymbol(e, "vtableref_offset_ptr"));

            target.print("    %");
            target.print(toTempSymbol(e, "vtable"));
            target.print(" = inttoptr i32 %");
            target.print(toTempSymbol(e, "dispatcher"));
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

            return;
        }

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

        // Now we need to compute the offset in the table
        target.println("    ;; slot " + slot.getPos() + "," + e.getMethodName() + ", " + e.getSignature());
        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = add i32 %");
        target.print(toTempSymbol(e, "vtableref"));
        target.print(", ");
        target.println(8 + 4 * (slot.getPos()));

        target.print("    %");
        target.print(toTempSymbol(e, "vtablefunptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "vtable"));
        target.println(" to i32 *");

        target.print("    %");
        target.print(toTempSymbol(e, "resolved"));
        target.print("= load i32, i32* %");
        target.println(toTempSymbol(e, "vtablefunptr"));

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

    private void tempify(final NewMultiArrayExpression e) {
        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.println(" = ptrtoint %dmbcArray__vtable__type* @dmbcArray__vtable to i32");
    }

    private void tempify(final ArrayEntryExpression e) {
        target.print("    %");
        target.print(toTempSymbol(e, "index"));
        target.print(" = mul i32 8,");
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

    private void tempify(final NewInstanceExpression e) {
        final String theClassName = LLVMWriterUtils.toClassName(BytecodeObjectTypeRef.fromUtf8Constant(e.getType().getConstant()));
        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = ptrtoint %");
        target.print(theClassName);
        target.print(VTABLETYPESUFFIX);
        target.print("* @");
        target.print(theClassName);
        target.print(VTABLESUFFIX);
        target.println(" to i32");
    }

    private void tempify(final NewInstanceAndConstructExpression e) {
        if (!e.isEscaping() && stackAllocationEnabled) {
            // Perform stack allocation
            final String theClassName = LLVMWriterUtils.toClassName(e.getClazz());
            target.print("    %");
            target.print(toTempSymbol(e, "vtable"));
            target.print(" = ptrtoint %");
            target.print(theClassName);
            target.print(LLVMWriter.VTABLETYPESUFFIX);
            target.print("* @");
            target.print(theClassName);
            target.print(LLVMWriter.VTABLESUFFIX);
            target.println(" to i32");

            final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(e.getClazz());
            final int theInstanceSize = theLayout.instanceSize();
            target.print("    %");
            target.print(toTempSymbol(e, "alloc"));
            target.print(" = alloca i32, i32 ");
            target.println(theInstanceSize / 4);

            target.print("    %");
            target.print(toTempSymbol(e, "alloc_int"));
            target.print(" = ptrtoint i32* %");
            target.print(toTempSymbol(e, "alloc"));
            target.println(" to i32");

            final BytecodeObjectTypeRef theMemoryManagerClass = BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class);

            target.print("    call void(i32,i32,i32,i32,i32) @");
            target.print(LLVMWriterUtils.toMethodName(theMemoryManagerClass, "initStackObject",
                    new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT})));
            target.print("(");
            target.print("i32 0, i32 %");
            target.print(toTempSymbol(e, "alloc_int"));
            target.print(",i32 ");
            target.print(theLayout.instanceSize());
            target.print(", i32 %");
            target.print(LLVMWriterUtils.runtimeClassVariableName(e.getClazz()));
            target.print(",i32 %");
            target.print(toTempSymbol(e, "vtable"));
            target.print(")");

            currentSubProgram.writeDebugSuffixFor(e, target);
            target.println();

            target.print("    call void (i32");
            for (int i = 0; i < e.getSignature().getArguments().length; i++) {
                target.print(",");
                target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
            }
            target.print(") @");
            target.print(LLVMWriterUtils.toMethodName(e.getClazz(), "<init>", e.getSignature()));
            target.print("(i32 %");
            target.print(toTempSymbol(e, "alloc_int"));
            for (int i = 0; i < e.incomingDataFlows().size(); i++) {
                target.print(",");
                target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
                target.print(" ");
                writeResolved(e.incomingDataFlows().get(i));
            }

            target.print(")");

            currentSubProgram.writeDebugSuffixFor(e, target);
            target.println();
        }
    }

    private void tempify(final NewArrayExpression e) {

        final BytecodeObjectTypeRef theArrayClass = BytecodeObjectTypeRef.fromRuntimeClass(Array.class);
        final String theClassName = LLVMWriterUtils.toClassName(theArrayClass);
        target.print("    %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = ptrtoint %");
        target.print(theClassName);
        target.print(LLVMWriter.VTABLETYPESUFFIX);
        target.print("* @");
        target.print(theClassName);
        target.print(LLVMWriter.VTABLESUFFIX);
        target.println(" to i32");

        if (!e.isEscaping() && stackAllocationEnabled) {

            // Perform stack allocation
            target.print("    %");
            target.print(toTempSymbol(e, "size"));
            target.print(" = ");
            writeSameAssignmentHack(TypeRef.Native.INT, e.incomingDataFlows().get(0));
            target.println();

            target.print("    %");
            target.print(toTempSymbol(e, "size2"));
            target.print(" = mul i32 8, %");
            target.print(toTempSymbol(e, "size"));
            target.println();

            target.print("    %");
            target.print(toTempSymbol(e, "size3"));
            target.print(" = add i32 20, %");
            target.print(toTempSymbol(e, "size2"));
            target.println();

            target.print("    %");
            target.print(toTempSymbol(e, "alloc"));
            target.print(" = alloca i32, i32 %");
            target.print(toTempSymbol(e, "size3"));
            target.println();

            target.print("    %");
            target.print(toTempSymbol(e, "alloc_int"));
            target.print(" = ptrtoint i32* %");
            target.print(toTempSymbol(e, "alloc"));
            target.println(" to i32");

            final BytecodeObjectTypeRef theMemoryManagerClass = BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class);

            target.print("    call void(i32,i32,i32,i32,i32) @");
            target.print(LLVMWriterUtils.toMethodName(theMemoryManagerClass, "initStackArray",
                    new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT})));
            target.print("(");
            target.print("i32 0, i32 %");
            target.print(toTempSymbol(e, "alloc_int"));
            target.print(",i32 %");
            target.print(toTempSymbol(e, "size"));
            target.print(", i32 %");
            target.print(LLVMWriterUtils.runtimeClassVariableName(theArrayClass));
            target.print(",i32 %");
            target.print(toTempSymbol(e, "vtable"));
            target.print(")");

            currentSubProgram.writeDebugSuffixFor(e, target);
            target.println();
        }
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
        target.print(toTempSymbol(e, "offset"));
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
        target.print(toTempSymbol(e, "offset"));
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

        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = add i32 %");
        target.print(LLVMWriterUtils.runtimeClassVariableName(theClass));
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
        target.println(";; flooring to " + e.resolveType().resolve() + " from type " + value.resolveType().resolve());

        if (value instanceof BinaryExpression)  {
            tempify((BinaryExpression) value);
        }
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
        if (value instanceof FixedBinaryExpression) {
            final Value v = value.incomingDataFlows().get(0);
            if (v instanceof Expression) {
                tempifyValue(v);
                target.print("    ");
                target.print("%");
                target.print(toTempSymbol(v, "exp"));
                target.print(" = ");
                write(v, true);
                target.println();
            }
        }
        if (value instanceof BinaryExpression) {
            final Value v1 = value.incomingDataFlows().get(0);
            if (v1 instanceof Expression) {
                tempifyValue(v1);
                target.print("    ");
                target.print("%");
                target.print(toTempSymbol(v1, "exp"));
                target.print(" = ");
                write(v1, true);
                target.println();
            }
            final Value v2 = value.incomingDataFlows().get(1);
            if (v2 instanceof Expression) {
                tempifyValue(v2);
                target.print("    ");
                target.print("%");
                target.print(toTempSymbol(v2, "exp"));
                target.print(" = ");
                write(v2, true);
                target.println();
            }
        }

        target.print("    %");
        target.print(toTempSymbol(value, "exp"));
        target.print(" = ");
        write(value, true);
        target.println();
    }

    private void tempify(final BinaryExpression e) {
        final Value v1 = e.incomingDataFlows().get(0);
        final Value v2 = e.incomingDataFlows().get(1);

        if (e.getOperator() == BinaryExpression.Operator.BINARYSHIFTLEFT ||
            e.getOperator() == BinaryExpression.Operator.BINARYSHIFTRIGHT ||
            e.getOperator() == BinaryExpression.Operator.BINARYUNSIGNEDSHIFTRIGHT) {
            if (v1.resolveType().resolve() == TypeRef.Native.LONG) {
                // We need to convert the second operand from int to long
                target.print("    %");
                target.print(toTempSymbol(e, "v2_ext"));
                target.print(" = sext i32 ");
                writeResolved(v2);
                target.println(" to i64");
                return;
            }
        }
        if (e.getOperator() == BinaryExpression.Operator.DIV) {
            target.println(";; division with target type " + e.resolveType().resolve());
            switch (v1.resolveType().resolve()) {
                case FLOAT:
                case DOUBLE:
                    return;
                case LONG:
                    // We need a conversion
                    target.print("    %");
                    target.print(toTempSymbol(e, "v1"));
                    target.print(" = sitofp i64 ");
                    write(v1, true);
                    target.println(" to double");

                    target.print("    %");
                    target.print(toTempSymbol(e, "v2"));
                    target.print(" = sitofp i64 ");
                    write(v2, true);
                    target.println(" to double");
                    return;
                default:
                    // We need a conversion
                    target.print("    %");
                    target.print(toTempSymbol(e, "v1"));
                    target.print(" = sitofp i32 ");
                    write(v1, true);
                    target.println(" to float");

                    target.print("    %");
                    target.print(toTempSymbol(e, "v2"));
                    target.print(" = sitofp i32 ");
                    write(v2, true);
                    target.println(" to float");
            }
        }
    }

    private void tempifyValue(final Value v) {
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

        } else if (v instanceof BinaryExpression) {

            tempify((BinaryExpression) v);

        } else if (v instanceof TypeOfExpression) {

            tempify((TypeOfExpression) v);

        } else if (v instanceof NewInstanceExpression) {

            tempify((NewInstanceExpression) v);

        } else if (v instanceof NewInstanceAndConstructExpression) {

            tempify((NewInstanceAndConstructExpression) v);

        } else if (v instanceof NewArrayExpression) {

            tempify((NewArrayExpression) v);

        } else if (v instanceof NewMultiArrayExpression) {

            tempify((NewMultiArrayExpression) v);

        } else if (v instanceof ArrayEntryExpression) {

            tempify((ArrayEntryExpression) v);

        } else if (v instanceof InvokeVirtualMethodExpression) {

            tempify((InvokeVirtualMethodExpression) v);

        } else if (v instanceof MemorySizeExpression) {

            tempify((MemorySizeExpression) v);

        } else if (v instanceof GetStaticExpression) {

            tempify((GetStaticExpression) v);

        } else if (v instanceof FloorExpression) {

            tempify((FloorExpression) v);

        } else if (v instanceof InvokeDirectMethodExpression) {

            // Nothing to be done here

        } else if (v instanceof CompareExpression) {

            // Nothing to be done here

        } else if (v instanceof NewInstanceFromDefaultConstructorExpression) {

            // Nothing to be done here

        } else if (v instanceof NewInstanceAndConstructExpression) {

            // Nothing to be done here

        }
    }

    private void tempify(final Expression e) {
        if (e instanceof InvokeVirtualMethodExpression) {
            tempify((InvokeVirtualMethodExpression) e);
        }
        if (e instanceof IFExpression) {
            tempify((IFExpression) e);
        }
        for (final Value v : e.incomingDataFlows()) {
            tempifyValue(v);
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
            } else if (e instanceof InvokeDirectMethodExpression) {
                target.print("    ");
                write((InvokeDirectMethodExpression) e);
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
        target.print(" = call i1 @bytecoder.exceedsrange(i32 %");
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
        target.print(" = mul i32 8,");
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
        if (!e.isEscaping() && stackAllocationEnabled) {

            linkerContext.getStatistics().context("Codegenerator")
                    .counter("ArrayOnStackAllocations").increment();

            target.print("add i32 0, %");
            target.print(toTempSymbol(e, "alloc_int"));

            final DebugPosition debugPosition = currentDebugInformation.debugPositionFor(e.getAddress());
            if (debugPosition != null) {
                target.print(";; does not escape, please verify in ");
                target.print(debugPosition.getFileName());
                target.print(" @ ");
                target.println(debugPosition.getLineNumber() + 1);
            } else {
                target.println(";; does not escape, please verify");
            }

        } else {
            linkerContext.getStatistics().context("Codegenerator")
                    .counter("ArrayOnHeapAllocations").increment();

            final String theMethodName = LLVMWriterUtils.toMethodName(
                    BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                    "newArray",
                    new BytecodeMethodSignature(BytecodePrimitiveTypeRef.INT, new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

            target.print("call i32 @");
            target.print(theMethodName);
            target.print("(i32 0,i32 ");
            writeResolved(e.incomingDataFlows().get(0));
            target.print(",i32 %");
            target.print(LLVMWriterUtils.runtimeClassVariableName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
            target.print(",i32 %");
            target.print(toTempSymbol(e, "vtable"));
            target.print(")");
            currentSubProgram.writeDebugSuffixFor(e, target);
        }
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

        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(theClass);
        final BytecodeResolvedFields theStaticFields = theLinkedClass.resolvedFields();
        final BytecodeResolvedFields.FieldEntry theField = theStaticFields.fieldByName(e.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        target.print("    %");
        target.print(toTempSymbol(e, "offset"));
        target.print(" = add i32 %");
        target.print(LLVMWriterUtils.runtimeClassVariableName(theLinkedClass.getClassName()));
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

    private void write(final InvokeDirectMethodExpression e) {

        final BytecodeLinkedClass theTargetClass = linkerContext.resolveClass(e.getInvokedClass());
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
            target.print(LLVMWriterUtils.toMethodName(e.getInvokedClass(), theMethodName, theSignature));
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

        final BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(expression.getField().getClassIndex().getClassConstant().getConstant()));
        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theLinkedClass.getClassName());
        final BytecodeResolvedFields theInstanceFields = theLinkedClass.resolvedFields();
        final BytecodeResolvedFields.FieldEntry theField = theInstanceFields.fieldByName(expression.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

        target.print("    %");
        target.print(toTempSymbol(expression, "offset"));
        target.print(" = add i32 ");
        writeResolved(object);
        target.print(",");
        target.print(theLayout.offsetForInstanceMember(theField.getValue().getName().stringValue()));
        target.println();

        target.print("    %");
        target.print(toTempSymbol(expression, "ptr"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(expression, "offset"));
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
        if (value instanceof TypeConversionExpression) {
            target.println("    ; converting from " + value.incomingDataFlows().get(0).resolveType().resolve() + " to " + expression.getVariable().resolveType().resolve());
        }
        target.print("    ");
        target.print("%");
        target.print(expression.getVariable().getName());
        target.print("_ = ");
        if (value instanceof Variable) {
            switch (value.resolveType().resolve()) {
                case DOUBLE:
                    target.print("fadd double %");
                    target.print(((Variable) value).getName());
                    target.print("_, 0.0");
                    break;
                case FLOAT:
                    target.print("fadd float %");
                    target.print(((Variable) value).getName());
                    target.print("_, 0.0");
                    break;
                case LONG:
                    target.print("add i64 %");
                    target.print(((Variable) value).getName());
                    target.print("_, 0");
                    break;
                default:
                    target.print("add i32 %");
                    target.print(((Variable) value).getName());
                    target.print("_, 0");
                    break;
            }
        } else {
            if (value instanceof DoubleValue) {
                target.print("fadd double 0.0,");
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
                target.print("add i64 0,");
            }
            if (value instanceof PHIValue) {
                switch (value.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fadd double 0.0,");
                        break;
                    case FLOAT:
                        target.print("fadd float 0.0,");
                        break;
                    case LONG:
                        target.print("add i64 0,");
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
        } else if (aValue instanceof NewInstanceAndConstructExpression) {
            write((NewInstanceAndConstructExpression) aValue);
        } else if (aValue instanceof GetFieldExpression) {
            write((GetFieldExpression) aValue);
        } else if (aValue instanceof InvokeDirectMethodExpression) {
            write((InvokeDirectMethodExpression) aValue);
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
        } else if (aValue instanceof NewInstanceExpression) {
            write((NewInstanceExpression) aValue);
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
        } else if (aValue instanceof ResolveCallsiteInstanceExpression) {
            write((ResolveCallsiteInstanceExpression) aValue);
        } else if (aValue instanceof DoubleValue) {
            write((DoubleValue) aValue);
        } else if (aValue instanceof FloatingPointCeilExpression) {
            write((FloatingPointCeilExpression) aValue);
        } else if (aValue instanceof FloatingPointFloorExpression) {
            write((FloatingPointFloorExpression) aValue);
        } else if (aValue instanceof MaxExpression) {
            write((MaxExpression) aValue);
        } else if (aValue instanceof LambdaWithStaticImplExpression) {
            write((LambdaWithStaticImplExpression) aValue);
        } else if (aValue instanceof LambdaConstructorReferenceExpression) {
            write((LambdaConstructorReferenceExpression) aValue);
        } else if (aValue instanceof LambdaVirtualReferenceExpression) {
            write((LambdaVirtualReferenceExpression) aValue);
        } else if (aValue instanceof LambdaInterfaceReferenceExpression) {
            write((LambdaInterfaceReferenceExpression) aValue);
        } else if (aValue instanceof LambdaSpecialReferenceExpression) {
            write((LambdaSpecialReferenceExpression) aValue);
        } else if (aValue instanceof EnumConstantsExpression) {
            write((EnumConstantsExpression) aValue);
        } else if (aValue instanceof MethodTypeArgumentCheckExpression) {
            write((MethodTypeArgumentCheckExpression) aValue);
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
        } else if (aValue instanceof MethodHandleExpression) {
            write((MethodHandleExpression) aValue);
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

    private void write(final MethodHandleExpression aValue) {

        if (aValue.getAdapterAnnotation() == null) {
            // An easy one, we can directly refer to the implementation method here
            target.print("ptrtoint ");
            target.print(LLVMWriterUtils.toSignature(aValue.getImplementationSignature()));

            final String theMethodName = LLVMWriterUtils.toMethodName(
                    aValue.getClassName(),
                    aValue.getMethodName(),
                    aValue.getImplementationSignature());

            target.print("* @");
            target.print(theMethodName);
            target.print(" to i32");

            return;
        }

        // We compile a delegate function as this is needed to
        // combine static and dynamic arguments for method invocation
        target.print("ptrtoint ");
        target.print(LLVMWriterUtils.toSignature(aValue.getAdapterAnnotation().getCaptureSignature()));

        final String theMethodName = symbolResolver.methodHandleDelegateFor(aValue);

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
        target.print(LLVMWriterUtils.runtimeClassVariableName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
        target.write(",i32 %");
        target.write(toTempSymbol(e, "vtable"));
        target.write(")");
        currentSubProgram.writeDebugSuffixFor(e, target);
    }

    private void write(final SqrtExpression e) {
        if (e.resolveType().resolve() == TypeRef.Native.DOUBLE) {
            target.print("call double @llvm.sqrt.f64(double ");
            write(e.incomingDataFlows().get(0), true);
            target.print(")");
        } else {
            target.print("call float @llvm.sqrt.f32(float ");
            write(e.incomingDataFlows().get(0), true);
            target.print(")");
        }
    }

    private void write(final MethodTypeArgumentCheckExpression e) {
        final TypeRef.Native theExpectedType = e.getExpectedType();
        final Value theMethodType = e.incomingDataFlows().get(0);
        final Value theIndex = e.incomingDataFlows().get(1);

        target.print("call i32 @bytecoder.checkmethodtype(i32 ");
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

    private void write(final LambdaWithStaticImplExpression e) {
        target.print("call i32 @bytecoder.newLambda(i32 ");
        write(e.getType(), true);
        target.print(",i32 ");
        write(e.getStaticRef(), true);
        target.print(",i32 ");
        write(e.getStaticArguments(), true);
        target.print(")");
    }

    private void write(final LambdaConstructorReferenceExpression e) {
        target.print("call i32 @bytecoder.newLambda(i32 ");
        write(e.getType(), true);
        target.print(",i32 ");
        write(e.getConstructorRef(), true);
        target.print(",i32 ");
        write(e.getStaticArguments(), true);
        target.print(")");
    }

    private void write(final LambdaInterfaceReferenceExpression e) {
        target.print("call i32 @bytecoder.newLambda(i32 ");
        write(e.getType(), true);
        target.print(",i32 ");
        write(e.getInterfaceRef(), true);
        target.print(",i32 ");
        write(e.getStaticArguments(), true);
        target.print(")");
    }

    private void write(final LambdaVirtualReferenceExpression e) {
        target.print("call i32 @bytecoder.newLambda(i32 ");
        write(e.getType(), true);
        target.print(",i32 ");
        write(e.getVirtualRef(), true);
        target.print(",i32 ");
        write(e.getStaticArguments(), true);
        target.print(")");
    }

    private void write(final LambdaSpecialReferenceExpression e) {
        target.print("call i32 @bytecoder.newLambda(i32 ");
        write(e.getType(), true);
        target.print(",i32 ");
        write(e.getSpecialRef(), true);
        target.print(",i32 ");
        write(e.getStaticArguments(), true);
        target.print(")");
    }

    private void write(final MaxExpression e) {
        target.print("call ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));

        switch (e.resolveType().resolve()) {
            case DOUBLE:
                target.print(" @llvm.maximum.f64");
                break;
            case FLOAT:
                target.print(" @llvm.maximum.f32");
                break;
            case LONG:
                target.print(" @bytecoder.maximum.i64");
                break;
            default:
                target.print(" @bytecoder.maximum.i32");
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
        if (e.resolveType().resolve() == TypeRef.Native.DOUBLE) {
            target.print("call double @llvm.floor.f64(double ");
            writeResolved(e.incomingDataFlows().get(0));
            target.print(")");
        } else {
            target.print("call float @llvm.floor.f32(float ");
            writeResolved(e.incomingDataFlows().get(0));
            target.print(")");
        }
    }

    private void write(final FloatingPointCeilExpression e) {
        if (e.resolveType().resolve() == TypeRef.Native.DOUBLE) {
            target.print("call double @llvm.ceil.f64(double ");
            writeResolved(e.incomingDataFlows().get(0));
            target.print(")");
        } else {
            target.print("call float @llvm.ceil.f32(float ");
            writeResolved(e.incomingDataFlows().get(0));
            target.print(")");
        }
    }

    private void write(final ResolveCallsiteInstanceExpression e) {
        final BytecodeLinkedClass theOwningClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(e.getOwningClass().getThisInfo().getConstant()));
        target.print("call i32 @");
        target.print(symbolResolver.resolveCallsiteBootstrapFor(theOwningClass,
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
        if (theValue.resolveType().resolve() == TypeRef.Native.DOUBLE) {
            target.print("call i32 @bytecoder.isnan.double(");
            target.print(LLVMWriterUtils.toType(theValue.resolveType()));
            target.print(" ");
            writeResolved(theValue);
            target.print(")");
        } else {
            target.print("call i32 @bytecoder.isnan.float(");
            target.print(LLVMWriterUtils.toType(theValue.resolveType()));
            target.print(" ");
            writeResolved(theValue);
            target.print(")");
        }
    }

    private void write(final TypeOfExpression e) {
        target.print("load i32, i32* %");
        target.print(toTempSymbol(e, "ptr"));
    }

    private void write(final MinExpression e) {
        target.print("call ");
        target.print(LLVMWriterUtils.toType(e.resolveType()));
        switch (e.resolveType().resolve()) {
            case DOUBLE:
                target.print(" @llvm.minimum.f64");
                break;
            case FLOAT:
                target.print(" @llvm.minimum.f32");
                break;
            case LONG:
                target.print(" @bytecoder.minimum.i64");
                break;
            default:
                target.print(" @bytecoder.minimum.i32");
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
        switch (e.incomingDataFlows().get(0).resolveType().resolve()) {
            case DOUBLE:
                target.print("fptosi double ");
                writeResolved(e.incomingDataFlows().get(0));
                switch (e.resolveType().resolve()) {
                    case LONG:
                        target.print(" to i64");
                        break;
                    default:
                        target.print(" to i32");
                        break;
                }
                break;
            case LONG:
                target.print("fptosi double ");
                writeResolved(e.incomingDataFlows().get(0));
                switch (e.resolveType().resolve()) {
                    case LONG:
                        target.print(" to i64");
                        break;
                    default:
                        target.print(" to i32");
                        break;
                }
                break;
            default:
                target.print("fptosi float ");
                writeResolved(e.incomingDataFlows().get(0));
                switch (e.resolveType().resolve()) {
                    case LONG:
                        target.print(" to i64");
                        break;
                    default:
                        target.print(" to i32");
                        break;
                }
                break;
        }
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
                target.print("call i32 @bytecoder.compare.float(");
                break;
            default:
                target.print("call i32 @bytecoder.compare.i32(");
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

    private void write(final NewInstanceExpression e) {
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
        target.print(LLVMWriterUtils.runtimeClassVariableName(BytecodeObjectTypeRef.fromUtf8Constant(e.getType().getConstant())));
        target.print(",i32 %");
        target.print(toTempSymbol(e, "vtable"));
        target.print(")");
        currentSubProgram.writeDebugSuffixFor(e, target);
    }

    private void write(final InstanceOfExpression e) {
        target.print("call i32 @bytecoder.instanceof(i32 ");
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
        target.print("add i32 %");
        target.print(LLVMWriterUtils.runtimeClassVariableName(e.getType()));
        target.print(", 0");
    }

    private void write(final StringValue e) {
        target.print("load i32, i32* @");
        target.print(symbolResolver.globalFromStringPool(e.getStringValue()));
    }

    private void write(final SystemHasStackExpression e) {
        target.print("add i32 0, 0");
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

    private void write(final NewInstanceAndConstructExpression e) {

        if (!e.isEscaping() && stackAllocationEnabled) {

            linkerContext.getStatistics().context("Codegenerator")
                    .counter("ObjectOnStackAllocations").increment();

            target.print("add i32 0, %");
            target.print(toTempSymbol(e, "alloc_int"));

            final DebugPosition debugPosition = currentDebugInformation.debugPositionFor(e.getAddress());
            if (debugPosition != null) {
                target.print(";; does not escape, please verify in ");
                target.print(debugPosition.getFileName());
                target.print(" @ ");
                target.println(debugPosition.getLineNumber() + 1);
            } else {
                target.println(";; does not escape, please verify");
            }

        } else {

            linkerContext.getStatistics().context("Codegenerator")
                    .counter("ObjectOnHeapAllocations").increment();

            target.print("call i32 (i32");
            for (int i = 0; i < e.getSignature().getArguments().length; i++) {
                target.print(",");
                target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
            }
            target.print(") @");
            target.print(LLVMWriterUtils.toMethodName(e.getClazz(), LLVMWriter.NEWINSTANCE_METHOD_NAME, e.getSignature()));
            target.print("(i32 %");
            target.print(LLVMWriterUtils.runtimeClassVariableName(e.getClazz()));
            for (int i = 0; i < e.incomingDataFlows().size(); i++) {
                target.print(",");
                target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
                target.print(" ");
                writeResolved(e.incomingDataFlows().get(i));
            }
            target.println(")");
        }
    }

    private void write(final StackTopExpression e) {
        target.print("load i32, i32* @stacktop");
    }

    private void writeSameAssignmentHack(final TypeRef theType, final Value aValue) {
        switch (theType.resolve()) {
            case FLOAT:
                target.print("fadd float 0.0,");
                break;
            case DOUBLE:
                target.print("fadd double 0.0,");
                break;
            case LONG:
                target.print("add i64 0,");
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
            case DOUBLE: {
                // Convert floating point to something else
                switch (e.resolveType().resolve()) {
                    case DOUBLE: {
                        // No conversion needed
                        writeSameAssignmentHack(theTargetType, theSource);
                        return;
                    }
                    case FLOAT: {
                        // Reduce double to float
                        target.print("fptrunc double ");
                        writeResolved(theSource);
                        target.print(" to float");
                        return;
                    }
                    case LONG: {
                        // Convert double to i64
                        // NaN == 0
                        target.print("call i64 @bytecoder.double.to.i64(double ");
                        writeResolved(theSource);
                        target.print(")");
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
                    case CHAR: {
                        // Convert doule to i32
                        // NaN == 0
                        target.print("call i32 @bytecoder.double.to.i32(double ");
                        writeResolved(theSource);
                        target.print(")");
                        return;
                    }
                    default:
                        throw new IllegalStateException("Coversion to " + e.resolveType() + " not supported!");
                }
            }
            case FLOAT: {
                // Convert floating point to something else
                switch (e.resolveType().resolve()) {
                    case DOUBLE: {
                        // Widening to double
                        target.print("fpext float ");
                        writeResolved(theSource);
                        target.print(" to double");
                        return;
                    }
                    case FLOAT: {
                        // No conversion needed
                        writeSameAssignmentHack(theTargetType, theSource);
                        return;
                    }
                    case LONG: {
                        // Convert float to i64
                        // NaN == 0
                        target.print("call i64 @bytecoder.float.to.i64(float ");
                        writeResolved(theSource);
                        target.print(")");
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
                    case CHAR: {
                        // Convert float to i32
                        // NaN == 0
                        target.print("call i32 @bytecoder.float.to.i32(float ");
                        writeResolved(theSource);
                        target.print(")");
                        return;
                    }
                    default:
                        throw new IllegalStateException("Coversion to " + e.resolveType() + " not supported!");
                }
            }
            case LONG: {
                // Convert long to something else
                switch (e.resolveType().resolve()) {
                    case DOUBLE: {
                        // Widening to double
                        target.print("sitofp i64 ");
                        writeResolved(theSource);
                        target.print(" to double");
                        return;
                    }
                    case FLOAT: {
                        target.print("sitofp i64 ");
                        writeResolved(theSource);
                        target.print(" to float");
                        return;
                    }
                    case LONG: {
                        // No conversion needed
                        writeSameAssignmentHack(theTargetType, theSource);
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
                    case CHAR: {
                        target.print("trunc i64 ");
                        writeResolved(theSource);
                        target.print(" to i32");
                        return;
                    }
                    default:
                        throw new IllegalStateException("Coversion to " + e.resolveType() + " not supported!");
                }
            }
            case INT:
            case BYTE:
            case SHORT:
            case CHAR: {
                // Convert integer type to something else
                // Convert floating point to something else
                switch (e.resolveType().resolve()) {
                    case DOUBLE: {
                        // Convert i32 to double
                        target.write("sitofp i32 ");
                        writeResolved(theSource);
                        target.write(" to double");
                        return;
                    }
                    case FLOAT: {
                        // Convert i32 to float
                        target.write("sitofp i32 ");
                        writeResolved(theSource);
                        target.write(" to float");
                        return;
                    }
                    case LONG: {
                        target.write("sext i32 ");
                        writeResolved(theSource);
                        target.write(" to i64");
                        return;
                    }
                    case INT:
                    case SHORT:
                    case BYTE:
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
                    case DOUBLE:
                        target.print("fadd double");
                        break;
                    case FLOAT:
                        target.print("fadd float");
                        break;
                    case LONG:
                        target.print("add i64");
                        break;
                    default:
                        target.print("add i32");
                        break;
                }
                break;
            case SUB:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fsub double");
                        break;
                    case FLOAT:
                        target.print("fsub float");
                        break;
                    case LONG:
                        target.print("sub i64");
                        break;
                    default:
                        target.print("sub i32");
                        break;
                }
                break;
            case MUL:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fmul double");
                        break;
                    case FLOAT:
                        target.print("fmul float");
                        break;
                    case LONG:
                        target.print("mul i64");
                        break;
                    default:
                        target.print("mul i32");
                        break;
                }
                break;
            case REMAINDER:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("frem double");
                        break;
                    case FLOAT:
                        target.print("frem float");
                        break;
                    case LONG:
                        target.print("srem i64");
                        break;
                    default:
                        target.print("srem i32");
                        break;
                }
                break;
            case GREATERTHAN:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fcmp ogt double");
                        break;
                    case FLOAT:
                        target.print("fcmp ogt float");
                        break;
                    case LONG:
                        target.print("icmp sgt i64");
                        break;
                    default:
                        target.print("icmp sgt i32");
                        break;
                }
                break;
            case GREATEROREQUALS:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fcmp oge double");
                        break;
                    case FLOAT:
                        target.print("fcmp oge float");
                        break;
                    case LONG:
                        target.print("icmp sge i64");
                        break;
                    default:
                        target.print("icmp sge i32");
                        break;
                }
                break;
            case LESSTHAN:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fcmp olt double");
                        break;
                    case FLOAT:
                        target.print("fcmp olt float");
                        break;
                    case LONG:
                        target.print("icmp slt i64");
                        break;
                    default:
                        target.print("icmp slt i32");
                        break;
                }
                break;
            case LESSTHANOREQUALS:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fcmp ole double");
                        break;
                    case FLOAT:
                        target.print("fcmp ole float");
                        break;
                    case LONG:
                        target.print("icmp sle i64");
                        break;
                    default:
                        target.print("icmp sle i32");
                        break;
                }
                break;
            case EQUALS:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fcmp oeq double");
                        break;
                    case FLOAT:
                        target.print("fcmp oeq float");
                        break;
                    case LONG:
                        target.print("icmp eq i64");
                        break;
                    default:
                        target.print("icmp eq i32");
                        break;
                }
                break;
            case NOTEQUALS:
                switch (theValue1.resolveType().resolve()) {
                    case DOUBLE:
                        target.print("fcmp one double");
                        break;
                    case FLOAT:
                        target.print("fcmp one float");
                        break;
                    case LONG:
                        target.print("icmp ne i64");
                        break;
                    default:
                        target.print("icmp ne i32");
                        break;
                }
                break;
            case BINARYSHIFTLEFT:
                switch (theValue1.resolveType().resolve()) {
                    case LONG:
                        target.print("shl i64");
                        break;
                    default:
                        target.print("shl i32");
                        break;
                }
                break;
            case BINARYSHIFTRIGHT:
                switch (theValue1.resolveType().resolve()) {
                    case LONG:
                        target.print("ashr i64");
                        break;
                    default:
                        target.print("ashr i32");
                        break;
                }
                break;
            case BINARYUNSIGNEDSHIFTRIGHT:
                switch (theValue1.resolveType().resolve()) {
                    case LONG:
                        target.print("lshr i64");
                        break;
                    default:
                        target.print("lshr i32");
                        break;
                }
                break;
            case BINARYOR:
                switch (theValue1.resolveType().resolve()) {
                    case LONG:
                        target.print("or i64");
                        break;
                    default:
                        target.print("or i32");
                        break;
                }
                break;
            case BINARYXOR:
                switch (theValue1.resolveType().resolve()) {
                    case LONG:
                        target.print("xor i64");
                        break;
                    default:
                        target.print("xor i32");
                        break;
                }
                break;
            case BINARYAND:
                switch (theValue1.resolveType().resolve()) {
                    case LONG:
                        target.print("and i64");
                        break;
                    default:
                        target.print("and i32");
                        break;
                }
                break;
            case DIV:
                writeDivExpression(aValue);
                return;
            default:
                throw new IllegalStateException("Not implemented : " + aValue.getOperator());
        }
        target.print(" ");
        final List<Value> v = aValue.incomingDataFlows();
        for (int i=0;i<v.size();i++) {
            final Value value = v.get(i);
            if (i>0) {
                target.print(",");
                switch (aValue.getOperator()) {
                    case BINARYSHIFTLEFT:
                    case BINARYSHIFTRIGHT:
                    case BINARYUNSIGNEDSHIFTRIGHT:
                        if (v.get(0).resolveType().resolve() == TypeRef.Native.LONG) {
                            target.print("%");
                            target.print(toTempSymbol(aValue, "v2_ext"));
                        } else {
                            writeResolved(value);
                        }
                        break;
                    default:
                        writeResolved(value);
                        break;
                }
            } else {
                writeResolved(value);
            }
        }
    }

    private void writeDivExpression(final BinaryExpression e) {
        final Value left = e.incomingDataFlows().get(0);
        final Value right = e.incomingDataFlows().get(1);
        switch (left.resolveType().resolve()) {
            case DOUBLE:
                target.print("fdiv double ");
                writeResolved(left);
                target.print(", ");
                writeResolved(right);
                return;
            case FLOAT:
                target.print("fdiv float ");
                writeResolved(left);
                target.print(", ");
                writeResolved(right);
                return;
            case LONG:
                target.print("fdiv double %");
                target.print(toTempSymbol(e, "v1"));
                target.print(", %" );
                target.print(toTempSymbol(e, "v2"));
                break;
            default:
                target.print("fdiv float %");
                target.print(toTempSymbol(e, "v1"));
                target.print(", %" );
                target.print(toTempSymbol(e, "v2"));
                break;
        }
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
        target.print(LLVMWriterUtils.toMethodName(aValue.getInvokedClass(), aValue.getMethodName(), aValue.getSignature()));

        if (aValue.getInvokedClass().name().equals(MemoryManager.class.getName())) {
            target.print("(i32 0");
        } else {
            target.print("(i32 %");
            target.print(LLVMWriterUtils.runtimeClassVariableName(aValue.getInvokedClass()));
        }
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