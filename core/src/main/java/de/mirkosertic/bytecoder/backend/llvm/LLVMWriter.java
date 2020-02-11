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
import de.mirkosertic.bytecoder.ssa.MinExpression;
import de.mirkosertic.bytecoder.ssa.NegatedExpression;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewInstanceFromDefaultConstructorExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectAndConstructExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeExpression;
import de.mirkosertic.bytecoder.ssa.SetEnumConstantsExpression;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
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
import de.mirkosertic.bytecoder.ssa.VariableDescription;

import java.io.PrintWriter;
import java.util.HashMap;
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
    }

    private final PrintWriter target;
    private RegionNode currentNode;
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

    public void write(final Program aProgram) {
        final ControlFlowGraph theGraph = aProgram.getControlFlowGraph();
        final RegionNode theStart = theGraph.startNode();
        final GraphDFSOrder<RegionNode> order = new GraphDFSOrder(theStart,
                RegionNode.NODE_COMPARATOR,
                RegionNode.FORWARD_EDGE_FILTER_REGULAR_FLOW_ONLY);
        final List<RegionNode> regularFlow = order.getNodesInOrder();
        final Set<String> alreadySeenPHIs = new HashSet<>();
        for (final RegionNode theBlock : regularFlow) {
            currentNode = theBlock;
            final BytecodeOpcodeAddress theBlockStart = theBlock.getStartAddress();
            if (theBlockStart.getAddress() == 0) {
                target.println("entry:");
            } else {
                target.print("block");
                target.print(theBlockStart.getAddress());
                target.println(":");
            }
            final BlockState theLiveIn = theBlock.liveIn();
            for (final Map.Entry<VariableDescription, Value> theEntry : theLiveIn.getPorts().entrySet()) {
                if (theEntry.getValue() instanceof PHIValue) {
                    final PHIValue phi = (PHIValue) theEntry.getValue();

                    final String tempName = toTempSymbol(phi, "phi");
                    final Set<RegionNode> thePreds = currentNode.getPredecessors();

                    final Map<RegionNode, Value> theIncoming = new HashMap<>();
                    for (final RegionNode pred : thePreds) {
                        if (regularFlow.contains(pred)) {
                            final Value theOut = pred.liveOut().getPorts().get(phi.getDescription());
                            theIncoming.put(pred, theOut);
                        }
                    }

                    final Set<Value> theIncomingValues = theIncoming.values().stream().collect(Collectors.toSet());
                    if (thePreds.size() > 1 && (theIncomingValues.size() > 1) || (theIncomingValues.size() == 1 && !theIncomingValues.contains(phi))) {

                        if (alreadySeenPHIs.add(tempName)) {
                            target.print("    %");
                            target.print(tempName);
                            target.print(" = phi ");
                            target.print(LLVMWriterUtils.toType(phi.resolveType()));
                            target.print(" ");
                            boolean first = true;
                            for (final Map.Entry<RegionNode, Value> theIncomingEntry : theIncoming.entrySet()) {
                                final RegionNode pred = theIncomingEntry.getKey();

                                // Only consider regular flow here
                                final Value theOut = theIncomingEntry.getValue();

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

                                    target.print(",");
                                    if (pred.getStartAddress().getAddress() == 0) {
                                        target.print("%entry");
                                    } else {
                                        target.print("%block");
                                        target.print(pred.getStartAddress().getAddress());
                                    }

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

                                    target.print(",");
                                    if (pred.getStartAddress().getAddress() == 0) {
                                        target.print("%entry");
                                    } else {
                                        target.print("%block");
                                        target.print(pred.getStartAddress().getAddress());
                                    }

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
        target.println("()");
    }

    private void tempify(final InvokeVirtualMethodExpression e) {
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
        target.print(toTempSymbol(e, "vtable"));
        target.print(" = inttoptr i32 %");
        target.print(toTempSymbol(e, "ptr"));
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
        target.println("()");
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
        target.println(" to i32*");
    }

    private void tempify(final NewObjectExpression e) {
        final String theClassName = LLVMWriterUtils.toClassName(BytecodeObjectTypeRef.fromUtf8Constant(e.getType().getConstant()));
        target.print("    %");
        target.print(toTempSymbol(e, "classinit"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.println("()");
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
        final String theClassName = LLVMWriterUtils.toClassName(theClass);

        final Value object = e.incomingDataFlows().get(0);
        target.print("    call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.println("()");

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
        target.println(" to ");
        target.print(LLVMWriterUtils.toType(TypeRef.toType(theField.getValue().getTypeRef())));
        target.println("*");
    }

    private void tempify(final GetStaticExpression e) {
        final BytecodeObjectTypeRef theClass = BytecodeObjectTypeRef.fromUtf8Constant(e.getField().getClassIndex().getClassConstant().getConstant());
        final String theClassName = LLVMWriterUtils.toClassName(theClass);

        target.print("    %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.println("()");

        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theClass);
        final int theStaticOffset = theLayout.offsetForClassMember(e.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

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
        target.println(" to i32*");
    }

    private void tempify(final FloorExpression e) {
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
        for (final Value v : e.incomingDataFlows()) {
            if (v instanceof ComputedMemoryLocationReadExpression) {

                tempify((ComputedMemoryLocationReadExpression) v);

            } else if (v instanceof ComputedMemoryLocationWriteExpression) {

                tempify((ComputedMemoryLocationWriteExpression) v);

            } else if (v instanceof GetFieldExpression) {

                tempify((GetFieldExpression) v);

            } else if (v instanceof ArrayLengthExpression) {

                tempify((ArrayLengthExpression) v);

            } else if (v instanceof TypeOfExpression) {

                tempify((TypeOfExpression) v);

            } else if (v instanceof NewObjectExpression) {

                tempify((NewObjectExpression) v);

            } else if (v instanceof NewArrayExpression) {

                tempify((NewArrayExpression) v);

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

            } else if (v instanceof Expression) {

                target.print("    %");
                target.print(toTempSymbol(v, "exp"));
                target.print(" = ");
                write(v, false);

                target.println();
            }
        }
    }

    private void write(final ExpressionList list) {
        for (final Expression e : list.toList()) {
            target.println(";; exp = " + e.getClass());
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
        target.println("    ]");
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

        target.print("    %");
        target.print(toTempSymbol(e, "sub"));
        target.print(" = sub i32 %");
        target.print(toTempSymbol(e, "value"));
        target.print(", ");
        target.println(e.getLowValue());

        target.print("    br label %");
        target.println(toTempSymbol(e, "else"));

        target.print(toTempSymbol(e, "else"));
        target.println(":");

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
        target.println("    ]");
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
        target.println(" to i32*");

        target.print("    store ");
        target.print(LLVMWriterUtils.toType(e.getArrayType()));
        target.print(" ");
        writeResolved(e.incomingDataFlows().get(2));
        target.print(", i32* %");
        target.println(toTempSymbol(e, "ptrptr"));
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
    }

    private void write(final ThrowExpression e) {
        target.println("    call void @llvm.trap()");
        target.println("    unreachable");
    }

    private void write(final PutStaticExpression e) {
        final BytecodeObjectTypeRef theClass = BytecodeObjectTypeRef.fromUtf8Constant(e.getField().getClassIndex().getClassConstant().getConstant());
        final String theClassName = LLVMWriterUtils.toClassName(theClass);

        target.print("    %");
        target.print(toTempSymbol(e, "runtimeclass"));
        target.print(" = call i32 @");
        target.print(theClassName);
        target.print(CLASSINITSUFFIX);
        target.println("()");

        final NativeMemoryLayouter.MemoryLayout theLayout = memoryLayouter.layoutFor(theClass);
        final int theStaticOffset = theLayout.offsetForClassMember(e.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

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
        target.println(" to i32*");

        target.print("    store i32 ");
        writeResolved(e.incomingDataFlows().get(0));
        target.print(",i32* %");
        target.println(toTempSymbol(e, "ptr"));
    }


    private void write(final InvokeVirtualMethodExpression e) {

        final Value value = e.incomingDataFlows().get(0);

        // Invoke function
        target.print("call ");
        target.print(LLVMWriterUtils.toSignature(e.getSignature()));
        target.print(" %");
        target.print(toTempSymbol(e, "resolved_ptr"));
        target.print(" (i32 ");
        writeResolved(value);
        for (int i=0;i<e.getSignature().getArguments().length;i++) {
            target.print(",");
            target.print(LLVMWriterUtils.toType(TypeRef.toType(e.getSignature().getArguments()[i])));
            target.print(" ");
            writeResolved(e.incomingDataFlows().get(i + 1));
        }
        target.print(")");
    }

    private void write(final DirectInvokeMethodExpression e) {

        target.print("call ");
        target.print(LLVMWriterUtils.toSignature(e.getSignature()));
        target.print(" @");
        if (!e.getMethodName().equals("<init>")) {
            final BytecodeLinkedClass theTargetClass = linkerContext.resolveClass(e.getClazz());
            final BytecodeResolvedMethods theResolvedMethods = theTargetClass.resolvedMethods();
            final BytecodeResolvedMethods.MethodEntry theEntry = theResolvedMethods.implementingClassOf(e.getMethodName(), e.getSignature());

            target.print(LLVMWriterUtils.toMethodName(theEntry.getProvidingClass().getClassName(), e.getMethodName(), e.getSignature()));
        } else {
            target.print(LLVMWriterUtils.toMethodName(e.getClazz(), e.getMethodName(), e.getSignature()));
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
        target.println(")");
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
        target.println(toTempSymbol(expression, "ptr"));
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
        target.print("    br i1 ");
        writeResolved(expression.incomingDataFlows().get(0));
        if (expression.getAddress().getAddress() == 0) {
            target.print(", label %entry");
        } else {
            target.print(", label %block");
            target.print(expression.getGotoAddress().getAddress());
        }

        final Set<BytecodeOpcodeAddress> forwardNodes = currentNode.outgoingEdges()
                .filter((Predicate<Edge<? extends EdgeType, ? extends Node>>) edge -> RegionNode.ALL_SUCCCESSORS_REGULAR_FLOW_ONLY
                        .test((Edge<EdgeType, RegionNode>) edge))
                .map(t -> t.targetNode().getStartAddress()).collect(Collectors.toSet());
        forwardNodes.remove(expression.getGotoAddress());

        if (forwardNodes.size() == 1) {
            final BytecodeOpcodeAddress theElse = forwardNodes.iterator().next();

            if (theElse.getAddress() == 0) {
                target.print(", label %entry");
            } else {
                target.print(", label %block");
                target.print(theElse.getAddress());
            }
            target.println();
        } else {
            throw new IllegalArgumentException("Expected one node for else branch of if statement, got " + forwardNodes);
        }
    }

    private void write(final GotoExpression expression) {
        target.print("    br label %");
        final BytecodeOpcodeAddress jumpTo = expression.jumpTarget();
        if (jumpTo.getAddress() == 0) {
            target.println("entry");
        } else {
            target.print("block");
            target.print(jumpTo.getAddress());
            target.println();
        }
    }

    private void write(final ReturnExpression expression) {
        target.print("    ");
        target.println("ret void");
    }

    private void write(final ReturnValueExpression expression) {
        final Value result = expression.incomingDataFlows().get(0);
        target.print("    ");
        target.print("ret ");
        target.print(LLVMWriterUtils.toType(result.resolveType()));
        target.print(" ");
        writeResolved(result);
        target.println();
    }

    private void write(final VariableAssignmentExpression expression) {
        final Value value = expression.incomingDataFlows().get(0);
        target.println(";; value = " + value.getClass());
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
        } else {
            throw new IllegalStateException("Not implemented : " + aValue.getClass());
        }
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
        target.print(" @llvm.maximum.");
        switch (e.resolveType().resolve()) {
            case FLOAT:
            case DOUBLE:
                target.print("f32");
                break;
            default:
                target.print("i32");
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
        final long doubleBits = Double.doubleToLongBits(e.getDoubleValue());
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
        target.print(" @llvm.minimum.");
        switch (e.resolveType().resolve()) {
            case FLOAT:
            case DOUBLE:
                target.print("f32");
                break;
            default:
                target.print("i32");
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
        target.print(", i32* %");
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

    private void write(final HeapBaseExpression e) {
        target.print("load i32, i32* @__heap_base");
    }

    private void write(final GetStaticExpression e) {
        target.print("load i32 ");
        target.print(",i32* %");
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
        target.println(toTempSymbol(e, "ptr"));
    }

    private void write(final NewObjectAndConstructExpression e) {
        target.print("call i32(");
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
                target.print("add");
                break;
            case SUB:
                target.print("sub");
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
        final Value right = e.incomingDataFlows().get(0);
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
    }
}