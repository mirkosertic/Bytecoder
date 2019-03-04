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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethod;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethodsAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeDoubleConstant;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionANEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARRAYLENGTH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionBIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionCHECKCAST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionD2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP2X1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUPX1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUPX2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionF2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericAND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLDC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericNEG;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericREM;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericUSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericXOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionI2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFACMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFCOND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFICMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNONNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIINC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINSTANCEOF;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEDYNAMIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionL2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLOOKUPSWITCH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITORENTER;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITOREXIT;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWMULTIARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionSIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionTABLESWITCH;
import de.mirkosertic.bytecoder.core.BytecodeIntegerConstant;
import de.mirkosertic.bytecoder.core.BytecodeInvokeDynamicConstant;
import de.mirkosertic.bytecoder.core.BytecodeLineNumberTableAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLocalVariableTableAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeLocalVariableTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeLongConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodHandleConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeMethodTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeReferenceIndex;
import de.mirkosertic.bytecoder.core.BytecodeSourceFileAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class NaiveProgramGenerator implements ProgramGenerator {

    public final static ProgramGeneratorFactory FACTORY = NaiveProgramGenerator::new;

    private final BytecodeLinkerContext linkerContext;

    private NaiveProgramGenerator(final BytecodeLinkerContext aLinkerContext) {
        linkerContext = aLinkerContext;
    }

    private DebugInformation debugInformationFor(final BytecodeClass aOwningClass, final BytecodeCodeAttributeInfo aCode) {
        final BytecodeSourceFileAttributeInfo theSourceFileAttribute = aOwningClass.getAttributes().getByType(BytecodeSourceFileAttributeInfo.class);
        final BytecodeLineNumberTableAttributeInfo theLineInfoAttribute = aCode.attributeByType(BytecodeLineNumberTableAttributeInfo.class);
        if (theSourceFileAttribute != null && theLineInfoAttribute != null) {
            final String theSourceFileName = theSourceFileAttribute.getFileName();
            final String theClassName = aOwningClass.getThisInfo().getConstant().stringValue();
            final int p = theClassName.lastIndexOf("/");
            final String thePackageName = theClassName.substring(0, p);
            final String theOriginaFileName = thePackageName.replace(".","/") + "/" + theSourceFileName;
            return DebugInformation.jvm(theOriginaFileName, theLineInfoAttribute);
        }
        return DebugInformation.empty();
    }

    @Override
    public Program generateFrom(final BytecodeClass aOwningClass, final BytecodeMethod aMethod) {

        final BytecodeCodeAttributeInfo theCode = aMethod.getCode(aOwningClass);
        final Program theProgram;

        // Initialize programm arguments
        BytecodeLocalVariableTableAttributeInfo theDebugInfos = null;
        if (theCode != null) {
            theDebugInfos = theCode.attributeByType(BytecodeLocalVariableTableAttributeInfo.class);
            theProgram = new Program(debugInformationFor(aOwningClass, theCode));
        } else {
            theProgram = new Program(DebugInformation.empty());
        }

        int theCurrentIndex = 0;
        if (!aMethod.getAccessFlags().isStatic()) {
            theProgram.addArgument(new LocalVariableDescription(theCurrentIndex, TypeRef.Native.REFERENCE), Variable.createThisRef());
            theCurrentIndex++;
        }
        final BytecodeTypeRef[] theTypes = aMethod.getSignature().getArguments();
        for (int i=0;i<theTypes.length;i++) {
            final BytecodeTypeRef theRef = theTypes[i];
            final TypeRef theType = TypeRef.toType(theRef);
            if (theDebugInfos != null) {
                final BytecodeLocalVariableTableEntry theEntry = theDebugInfos.matchingEntryFor(BytecodeOpcodeAddress.START_AT_ZERO, theCurrentIndex);
                if (theEntry != null) {
                    final String theVariableName = "_" + theDebugInfos.resolveVariableName(theEntry);
                    theProgram.addArgument(new LocalVariableDescription(theCurrentIndex, theType), Variable.createMethodParameter(i + 1, theVariableName, theType));
                } else {
                    theProgram.addArgument(new LocalVariableDescription(theCurrentIndex, theType), Variable.createMethodParameter(i + 1, theType));
                }
            } else {
                theProgram.addArgument(new LocalVariableDescription(theCurrentIndex, theType), Variable.createMethodParameter(i + 1, theType));
            }

            theCurrentIndex++;
            if (theRef == BytecodePrimitiveTypeRef.LONG || theRef == BytecodePrimitiveTypeRef.DOUBLE) {
                theCurrentIndex++;
            }
        }

        if (aMethod.getAccessFlags().isAbstract() || aMethod.getAccessFlags().isNative()) {
            return theProgram;
        }

        final BytecodeProgram.FlowInformation theFlowInformation = theCode.getProgram().toFlow();
        theProgram.setFlowInformation(theFlowInformation);

        // Ok, now we transform it to GraphNodes with yet empty content
        final Map<BytecodeBasicBlock, RegionNode> theCreatedBlocks = new HashMap<>();

        final ControlFlowGraph theGraph = theProgram.getControlFlowGraph();

        // Create CFG edges from flowinfo
        for (final BytecodeBasicBlock theBlock : theFlowInformation.knownBlocks()) {
            final RegionNode theSingleAssignmentBlock;
            switch (theBlock.getType()) {
            case NORMAL:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), RegionNode.BlockType.NORMAL);
                break;
            case EXCEPTION_HANDLER:
                for (final BytecodeUtf8Constant theClassInfo : theBlock.getCatchType()) {
                    linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo));
                }
                theSingleAssignmentBlock = theGraph
                        .createAt(theBlock.getStartAddress(), RegionNode.BlockType.EXCEPTION_HANDLER);
                break;
            case FINALLY:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), RegionNode.BlockType.FINALLY);
                break;
            default:
                throw new IllegalStateException("Unsupported block type : " + theBlock.getType());
            }
            theCreatedBlocks.put(theBlock, theSingleAssignmentBlock);
        }

        // Initialize Block dependency graph
        for (final Map.Entry<BytecodeBasicBlock, RegionNode> theEntry : theCreatedBlocks.entrySet()) {
            // Normal program flow
            for (final BytecodeBasicBlock theSuccessor : theEntry.getKey().getSuccessors()) {
                final RegionNode theSuccessorBlock = theCreatedBlocks.get(theSuccessor);
                if (theSuccessorBlock == null) {
                    throw new IllegalStateException("Cannot find successor block " + theSuccessor.getStartAddress().getAddress() + " from " + theEntry.getKey().getStartAddress().getAddress());
                }
                theEntry.getValue().addSuccessor(theSuccessorBlock);
            }
        }

        // Now we can add the SSA instructions to the graph nodes
        final RegionNode theStart = theProgram.getControlFlowGraph().startNode();

        // First of all, we need to mark the back-edges of the graph
        theProgram.getControlFlowGraph().calculateReachabilityAndMarkBackEdges();

        try {
            final Set<RegionNode> theVisited = new HashSet<>();

            // Now we can continue to create the program flow
            final ParsingHelperCache theParsingHelperCache = new ParsingHelperCache(theProgram, aMethod, theStart, theDebugInfos);

            // This will traverse the CFG from bottom to top
            final Set<RegionNode> theFinalNodes = theProgram.getControlFlowGraph().finalNodes();
            for (final RegionNode theNode : theFinalNodes) {
                initializeBlock(aOwningClass, aMethod, theNode, theVisited, theParsingHelperCache,
                        theFlowInformation, theProgram);
            }

            // Check if there are infinite looping blocks
            // Additionally, we have to add gotos
            for (final RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                final ExpressionList theCurrentList = theNode.getExpressions();
                final Expression theLast = theCurrentList.lastExpression();
                if (theLast instanceof GotoExpression) {
                    final GotoExpression theGoto = (GotoExpression) theLast;
                    if (Objects.equals(theGoto.getJumpTarget(), theNode.getStartAddress())) {
                        theCurrentList.remove(theGoto);
                    }
                }
                if (!theNode.getExpressions().endWithNeverReturningExpression()) {
                    Map<RegionNode.Edge, RegionNode> theSuccessors = theNode.getSuccessors();

                    for (final Expression theExpression : theCurrentList.toList()) {
                        if (theExpression instanceof IFExpression) {
                            final IFExpression theIF = (IFExpression) theExpression;
                            final BytecodeOpcodeAddress theGoto = theIF.getGotoAddress();
                            theSuccessors = theSuccessors.entrySet().stream().filter(t -> !Objects
                                    .equals(t.getValue().getStartAddress(), theGoto)).collect(
                                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        }
                    }

                    List<RegionNode> theSuccessorRegions = theSuccessors.values().stream().filter(t -> t.getType() == RegionNode.BlockType.NORMAL).collect(
                            Collectors.toList());

                    if (theSuccessorRegions.size() == 1) {
                        theNode.getExpressions().add(new GotoExpression(theProgram, null, theSuccessorRegions.get(0).getStartAddress()).withComment("Resolving pass thru direct"));
                    } else {
                        // Special case, the node includes gotos and a fall thru to the same node
                        theSuccessors = theNode.getSuccessors();
                        theSuccessorRegions = theSuccessors.values().stream().filter(t -> t.getType() == RegionNode.BlockType.NORMAL).collect(
                                Collectors.toList());

                        if (theSuccessorRegions.size() == 1) {
                            theNode.getExpressions().add(new GotoExpression(theProgram, null, theSuccessorRegions.get(0).getStartAddress())
                                    .withComment("Resolving pass thru direct"));
                        } else {
                            throw new IllegalStateException(
                                    "Invalid number of successors : " + theSuccessors.size() + " for " + theNode.getStartAddress()
                                            .getAddress());
                        }
                    }
                }
            }

            // Check that all PHI-propagations for back-edges are set
            for (final RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                final ParsingHelper theHelper = theParsingHelperCache.resolveFinalStateForNode(theNode);
                for (final Map.Entry<RegionNode.Edge, RegionNode> theEdge : theNode.getSuccessors().entrySet()) {
                    if (theEdge.getKey().getType() == RegionNode.EdgeType.BACK) {
                        final RegionNode theReceiving = theEdge.getValue();
                        final BlockState theReceivingState = theReceiving.toStartState();
                        for (final Map.Entry<VariableDescription, Value> theEntry : theReceivingState.getPorts().entrySet()) {
                            final Variable theReceivingTarget = (Variable) theEntry.getValue();
                            final Value theExportingValue = theHelper.requestValue(theEntry.getKey());
                            if (theExportingValue == null) {
                                throw new IllegalStateException("No value for " + theEntry.getKey() + " to jump from " + theNode.getStartAddress().getAddress() + " to " + theReceiving.getStartAddress().getAddress());
                            }
                            theReceivingTarget.initializeWith(theExportingValue);
                        }
                    }
                }
            }

            // Make sure that all jump conditions are met
            for (final RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                forEachExpressionOf(theNode, aPoint -> {
                    if (aPoint.expression instanceof GotoExpression) {
                        final GotoExpression theGoto = (GotoExpression) aPoint.expression;
                        final RegionNode theGotoNode = theProgram.getControlFlowGraph().nodeStartingAt(theGoto.getJumpTarget());
                        final BlockState theImportingState = theGotoNode.toStartState();
                        for (final Map.Entry<VariableDescription, Value> theImporting : theImportingState.getPorts().entrySet()) {
                            final ParsingHelper theHelper = theParsingHelperCache.resolveFinalStateForNode(theNode);
                            final Value theExportingValue = theHelper.requestValue(theImporting.getKey());
                            if (theExportingValue == null) {
                                throw new IllegalStateException("No value for " + theImporting.getKey() + " to jump from " + theNode.getStartAddress().getAddress() + " to " + theGotoNode.getStartAddress().getAddress());
                            }
                        }
                    }
                });
            }

            // Insert PHI value resolving at required places
            for (final RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                forEachExpressionOf(theNode, aPoint -> {
                    if (aPoint.expression instanceof GotoExpression) {
                        final GotoExpression theGoto = (GotoExpression) aPoint.expression;
                        final RegionNode theGotoNode = theProgram.getControlFlowGraph().nodeStartingAt(theGoto.getJumpTarget());
                        final BlockState theImportingState = theGotoNode.toStartState();
                        final StringBuilder theComments = new StringBuilder();
                        for (final Map.Entry<VariableDescription, Value> theImporting : theImportingState.getPorts().entrySet()) {
                            theComments.append(theImporting.getKey()).append(" is of type ")
                                    .append(theImporting.getValue().resolveType().resolve()).append(" with values ")
                                    .append(theImporting.getValue().incomingDataFlows());
                            final Value theReceivingValue = theImporting.getValue();
                            final ParsingHelper theHelper = theParsingHelperCache.resolveFinalStateForNode(theNode);
                            final Value theExportingValue = theHelper.requestValue(theImporting.getKey());
                            if (theExportingValue == null) {
                                throw new IllegalStateException("No value for " + theImporting.getKey() + " to jump from " + theNode.getStartAddress().getAddress() + " to " + theGotoNode.getStartAddress().getAddress());
                            }
                            if (theReceivingValue != theExportingValue) {
                                final VariableAssignmentExpression theInit = new VariableAssignmentExpression(theProgram, null, (Variable) theReceivingValue, theExportingValue);
                                aPoint.expressionList.addBefore(theInit, theGoto);
                            }
                        }
                        theGoto.withComment(theComments.toString());
                    }
                });
            }

        } catch (final Exception e) {
            throw new ControlFlowProcessingException("Error processing CFG for " + aOwningClass.getThisInfo().getConstant().stringValue() + "." + aMethod.getName().stringValue(), e, theProgram.getControlFlowGraph());
        }

        return theProgram;
    }

    static class TraversalPoint {
        public final ExpressionList expressionList;
        public final Expression expression;

        public TraversalPoint(final ExpressionList aExpressionList, final Expression aExpression) {
            expressionList = aExpressionList;
            expression = aExpression;
        }
    }

    public void forEachExpressionOf(final RegionNode aNode, final Consumer<TraversalPoint> aConsumer)  {
        forEachExpressionOf(aNode.getExpressions(), aConsumer);
    }

    public void forEachExpressionOf(final ExpressionList aList, final Consumer<TraversalPoint> aConsumer) {
        for (final Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (final ExpressionList theList : theContainer.getExpressionLists()) {
                    forEachExpressionOf(theList, aConsumer);
                }
            }

            aConsumer.accept(new TraversalPoint(aList, theExpression));
        }
    }

    private void initializeBlock(
            final BytecodeClass aOwningClass,
            final BytecodeMethod aMethod,
            final RegionNode aCurrentBlock,
            final Set<RegionNode> aAlreadyVisited,
            final ParsingHelperCache aCache,
            final BytecodeProgram.FlowInformation aFlowInformation,
            final Program aProgram) {

        // Resolve predecessor nodes. without them we would not have an initial state for the current node
        // We have to ignore back edges!!
        final Set<RegionNode> thePredecessors = aCurrentBlock.getPredecessorsIgnoringBackEdges();

        // First the normal flow
        for (final RegionNode thePredecessor : thePredecessors) {
            initializeBlock(aOwningClass, aMethod, thePredecessor, aAlreadyVisited, aCache, aFlowInformation, aProgram);
        }

        if (aAlreadyVisited.add(aCurrentBlock)) {

            final ParsingHelper theParsingState;
            if (aCurrentBlock.getType() != RegionNode.BlockType.NORMAL) {
                theParsingState = aCache.resolveInitialPHIStateForNode(aCurrentBlock);
                if (!aMethod.getAccessFlags().isStatic()) {
                    theParsingState.setLocalVariable(aCurrentBlock.getStartAddress(), theParsingState.numberOfLocalVariables(),
                            TypeRef.Native.REFERENCE, Variable.createThisRef());
                }
                theParsingState.push(new CurrentExceptionExpression(aProgram, null));
            } else if (aCurrentBlock.getStartAddress().getAddress() == 0) {
                // Programm is at start address, so we need the initial state
                theParsingState = aCache.resolveFinalStateForNode(null);
            } else if (thePredecessors.size() == 1) {
                // Only one predecessor
                final RegionNode thePredecessor = thePredecessors.iterator().next();
                final ParsingHelper theResolved = aCache.resolveFinalStateForNode(thePredecessor);
                if (theResolved == null) {
                    throw new IllegalStateException("No fully resolved predecessor found!");
                }
                theParsingState = aCache.resolveInitialStateFromPredecessorFor(aCurrentBlock, theResolved);
            } else {
                // we have more than one predecessor
                // we need to create PHI functions for all the disjunct states in local variables and the stack
                theParsingState = aCache.resolveInitialPHIStateForNode(aCurrentBlock);
            }

            initializeBlockWith(aOwningClass, aMethod, aCurrentBlock, aFlowInformation, theParsingState, aProgram);

            // register the final state after program flow
            aCache.registerFinalStateForNode(aCurrentBlock, theParsingState);
        }
    }

    private void initializeBlockWith(final BytecodeClass aOwningClass,
                                     final BytecodeMethod aMethod,
                                     final RegionNode aTargetBlock,
                                     final BytecodeProgram.FlowInformation aFlowInformation,
                                     final ParsingHelper aHelper,
                                     final Program aProgram) {

        // Finally we can start to parse the program
        final BytecodeBasicBlock theBytecodeBlock = aFlowInformation.blockAt(aTargetBlock.getStartAddress());

        for (final BytecodeInstruction theInstruction : theBytecodeBlock.getInstructions()) {

            if (theInstruction instanceof BytecodeInstructionNOP) {
                final BytecodeInstructionNOP theINS = (BytecodeInstructionNOP) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITORENTER) {
                final BytecodeInstructionMONITORENTER theINS = (BytecodeInstructionMONITORENTER) theInstruction;
                // Pop the reference for the lock from the stack
                aHelper.pop();
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITOREXIT) {
                final BytecodeInstructionMONITOREXIT theINS = (BytecodeInstructionMONITOREXIT) theInstruction;
                // Pop the reference for the lock from the stack
                aHelper.pop();
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                final BytecodeInstructionCHECKCAST theINS = (BytecodeInstructionCHECKCAST) theInstruction;
                final Value theValue = aHelper.peek();

                // Completely ignored
                //aTargetBlock.getExpressions().add(new CheckCastExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, theINS.getTypeCheck()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                final BytecodeInstructionPOP theINS = (BytecodeInstructionPOP) theInstruction;
                aHelper.pop();
            } else if (theInstruction instanceof BytecodeInstructionPOP2) {
                final BytecodeInstructionPOP2 theINS = (BytecodeInstructionPOP2) theInstruction;
                final Value theValue = aHelper.pop();
                switch (theValue.resolveType().resolve()) {
                case LONG:
                    break;
                case DOUBLE:
                    break;
                default:
                    aHelper.pop();
                }
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                final BytecodeInstructionDUP theINS = (BytecodeInstructionDUP) theInstruction;
                final Value theValue = aHelper.peek();
                aHelper.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionDUP2) {
                final BytecodeInstructionDUP2 theINS = (BytecodeInstructionDUP2) theInstruction;
                final Value theValue1 = aHelper.pop();
                if (theValue1.resolveType().resolve() == TypeRef.Native.LONG || theValue1.resolveType().resolve() == TypeRef.Native.DOUBLE) {
                    // Category 2
                    aHelper.push(theValue1);
                    aHelper.push(theValue1);
                } else {
                    // Category 1
                    final Value theValue2 = aHelper.pop();
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                }
            } else if (theInstruction instanceof BytecodeInstructionDUP2X1) {
                final BytecodeInstructionDUP2X1 theINS = (BytecodeInstructionDUP2X1) theInstruction;
                final Value theValue1 = aHelper.pop();
                if (theValue1.resolveType().resolve() == TypeRef.Native.LONG || theValue1.resolveType().resolve() == TypeRef.Native.DOUBLE) {
                    final Value theValue2 = aHelper.pop();

                    aHelper.push(theValue1);
                    aHelper.push(theValue2);
                    aHelper.push(theValue2);
                } else {
                    final Value theValue2 = aHelper.pop();
                    final Value theValue3 = aHelper.pop();

                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                    aHelper.push(theValue3);
                    aHelper.push(theValue2);
                    aHelper.push(theValue2);
                }
            } else if (theInstruction instanceof BytecodeInstructionDUPX1) {
                final BytecodeInstructionDUPX1 theINS = (BytecodeInstructionDUPX1) theInstruction;
                final Value theValue1 = aHelper.pop();
                final Value theValue2 = aHelper.pop();

                aHelper.push(theValue1);
                aHelper.push(theValue2);
                aHelper.push(theValue1);

            } else if (theInstruction instanceof BytecodeInstructionDUPX2) {
                final BytecodeInstructionDUPX2 theINS = (BytecodeInstructionDUPX2) theInstruction;
                final Value theValue1 = aHelper.pop();
                final Value theValue2 = aHelper.pop();

                if (theValue2.resolveType().resolve() == TypeRef.Native.LONG || theValue2.resolveType().resolve() == TypeRef.Native.DOUBLE) {
                    // Form 2
                    aHelper.push(theValue1);
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                } else {
                    // Form 1
                    final Value theValue3 = aHelper.pop();

                    aHelper.push(theValue1);
                    aHelper.push(theValue3);
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                }
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                final BytecodeInstructionGETSTATIC theINS = (BytecodeInstructionGETSTATIC) theInstruction;
                final GetStaticExpression theValue = new GetStaticExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getConstant());
                final Variable theVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), theValue);
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                final BytecodeInstructionASTORE theINS = (BytecodeInstructionASTORE) theInstruction;
                final Value theValue = aHelper.pop();
                aHelper.setLocalVariable(theInstruction.getOpcodeAddress(), theINS.getVariableIndex(), theValue.resolveType(), theValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                final BytecodeInstructionGenericSTORE theINS = (BytecodeInstructionGenericSTORE) theInstruction;
                final Value theValue = aHelper.pop();
                final Variable theOtherVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), theValue.resolveType().resolve(), theValue);
                aHelper.setLocalVariable(theInstruction.getOpcodeAddress(), theINS.getVariableIndex(), TypeRef.toType(theINS.getType()), theOtherVariable);
            } else if (theInstruction instanceof BytecodeInstructionObjectArrayLOAD) {
                final BytecodeInstructionObjectArrayLOAD theINS = (BytecodeInstructionObjectArrayLOAD) theInstruction;
                final Value theIndex = aHelper.pop();
                final Value theTarget = aHelper.pop();
                final TypeRef theType = theTarget.resolveType();
                if (theType instanceof TypeRef.ArrayTypeRef) {
                    final TypeRef.ArrayTypeRef theArrayTypeRef = (TypeRef.ArrayTypeRef) theTarget.resolveType();
                    final Variable theVariable = aTargetBlock.newVariable(
                            theInstruction.getOpcodeAddress(), TypeRef.toType(theArrayTypeRef.arrayType()), new ArrayEntryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theTarget, theIndex));
                    aHelper.push(theVariable);
                } else {
                    final Variable theVariable = aTargetBlock.newVariable(
                            theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new ArrayEntryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theTarget, theIndex));
                    aHelper.push(theVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionGenericArrayLOAD) {
                final BytecodeInstructionGenericArrayLOAD theINS = (BytecodeInstructionGenericArrayLOAD) theInstruction;
                final Value theIndex = aHelper.pop();
                final Value theTarget = aHelper.pop();

                final Variable theVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new ArrayEntryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theTarget, theIndex));
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArraySTORE) {
                final BytecodeInstructionGenericArraySTORE theINS = (BytecodeInstructionGenericArraySTORE) theInstruction;
                final Value theValue = aHelper.pop();
                final Value theIndex = aHelper.pop();
                final Value theTarget = aHelper.pop();
                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionObjectArraySTORE) {
                final BytecodeInstructionObjectArraySTORE theINS = (BytecodeInstructionObjectArraySTORE) theInstruction;
                final Value theValue = aHelper.pop();
                final Value theIndex = aHelper.pop();
                final Value theTarget = aHelper.pop();
                aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                final BytecodeInstructionACONSTNULL theINS = (BytecodeInstructionACONSTNULL) theInstruction;
                aHelper.push(new NullValue());
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                final BytecodeInstructionPUTFIELD theINS = (BytecodeInstructionPUTFIELD) theInstruction;
                final Value theValue = aHelper.pop();
                final Value theTarget = aHelper.pop();
                aTargetBlock.getExpressions().add(new PutFieldExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getFieldRefConstant(), theTarget, theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                final BytecodeInstructionGETFIELD theINS = (BytecodeInstructionGETFIELD) theInstruction;
                final Value theTarget = aHelper.pop();
                final Variable theVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), new GetFieldExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getFieldRefConstant(), theTarget));
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                final BytecodeInstructionPUTSTATIC theINS = (BytecodeInstructionPUTSTATIC) theInstruction;
                final Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new PutStaticExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getConstant(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                final BytecodeInstructionGenericLDC theINS = (BytecodeInstructionGenericLDC) theInstruction;
                final BytecodeConstant theConstant = theINS.constant();
                if (theConstant instanceof BytecodeDoubleConstant) {
                    final BytecodeDoubleConstant theC = (BytecodeDoubleConstant) theConstant;
                    aHelper.push(new DoubleValue(theC.getDoubleValue()));
                } else if (theConstant instanceof BytecodeLongConstant) {
                    final BytecodeLongConstant theC = (BytecodeLongConstant) theConstant;
                    aHelper.push(new LongValue(theC.getLongValue()));
                } else if (theConstant instanceof BytecodeFloatConstant) {
                    final BytecodeFloatConstant theC = (BytecodeFloatConstant) theConstant;
                    aHelper.push(new FloatValue(theC.getFloatValue()));
                } else if (theConstant instanceof BytecodeIntegerConstant) {
                    final BytecodeIntegerConstant theC = (BytecodeIntegerConstant) theConstant;
                    aHelper.push(new IntegerValue(theC.getIntegerValue()));
                } else if (theConstant instanceof BytecodeStringConstant) {
                    final BytecodeStringConstant theC = (BytecodeStringConstant) theConstant;
                    aHelper.push(new StringValue(theC.getValue().stringValue()));
                } else if (theConstant instanceof BytecodeClassinfoConstant) {
                    final BytecodeClassinfoConstant theC = (BytecodeClassinfoConstant) theConstant;

                    final BytecodeUtf8Constant theUTF8 = theC.getConstant();
                    if (theUTF8.stringValue().startsWith("[")) {
                        aHelper.push(new ClassReferenceValue(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
                    } else {
                        aHelper.push(new ClassReferenceValue(BytecodeObjectTypeRef.fromUtf8Constant(theC.getConstant())));
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported constant type : " + theConstant);
                }
            } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                final BytecodeInstructionBIPUSH theINS = (BytecodeInstructionBIPUSH) theInstruction;
                aHelper.push(new IntegerValue(theINS.getByteValue()));
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                final BytecodeInstructionSIPUSH theINS = (BytecodeInstructionSIPUSH) theInstruction;
                aHelper.push(new IntegerValue(theINS.getShortValue()));
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                final BytecodeInstructionICONST theINS = (BytecodeInstructionICONST) theInstruction;
                aHelper.push(new IntegerValue(theINS.getIntConst()));
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                final BytecodeInstructionFCONST theINS = (BytecodeInstructionFCONST) theInstruction;
                aHelper.push(new FloatValue(theINS.getFloatValue()));
            } else if (theInstruction instanceof BytecodeInstructionDCONST) {
                final BytecodeInstructionDCONST theINS = (BytecodeInstructionDCONST) theInstruction;
                aHelper.push(new DoubleValue(theINS.getDoubleConst()));
            } else if (theInstruction instanceof BytecodeInstructionLCONST) {
                final BytecodeInstructionLCONST theINS = (BytecodeInstructionLCONST) theInstruction;
                aHelper.push(new LongValue(theINS.getLongConst()));
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                final BytecodeInstructionGenericNEG theINS = (BytecodeInstructionGenericNEG) theInstruction;
                final Value theValue = aHelper.pop();
                final Variable theNegatedValue = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), theValue.resolveType(), new NegatedExpression(aProgram, theInstruction.getOpcodeAddress(), theValue));
                aHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                final BytecodeInstructionARRAYLENGTH theINS = (BytecodeInstructionARRAYLENGTH) theInstruction;
                final Value theValue = aHelper.pop();
                final Variable theNegatedValue = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, new ArrayLengthExpression(aProgram, theInstruction.getOpcodeAddress(), theValue));
                aHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                final BytecodeInstructionGenericLOAD theINS = (BytecodeInstructionGenericLOAD) theInstruction;
                final Value theValue = aHelper.getLocalVariable(theINS.getVariableIndex(), TypeRef.toType(theINS.getType()));
                final Variable theSnapshot = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), theValue.resolveType(), theValue);
                aHelper.push(theSnapshot);
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                final BytecodeInstructionALOAD theINS = (BytecodeInstructionALOAD) theInstruction;
                final Value theValue = aHelper.getLocalVariable(theINS.getVariableIndex(), TypeRef.Native.REFERENCE);
                final Variable theSnapshot = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), theValue.resolveType(), theValue);
                aHelper.push(theSnapshot);
            } else if (theInstruction instanceof BytecodeInstructionGenericCMP) {
                final BytecodeInstructionGenericCMP theINS = (BytecodeInstructionGenericCMP) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, new CompareExpression(aProgram, theInstruction.getOpcodeAddress(), theValue1, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionLCMP) {
                final BytecodeInstructionLCMP theINS = (BytecodeInstructionLCMP) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, new CompareExpression(aProgram, theInstruction.getOpcodeAddress(), theValue1, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                final BytecodeInstructionIINC theINS = (BytecodeInstructionIINC) theInstruction;
                final Value theValueToIncrement = aHelper.getLocalVariable(theINS.getIndex(), TypeRef.Native.INT);
                aHelper.setLocalVariable(theInstruction.getOpcodeAddress(), theINS.getIndex(), TypeRef.Native.INT, new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.INT, theValueToIncrement, BinaryExpression.Operator.ADD, new IntegerValue(theINS.getConstant())));
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                final BytecodeInstructionGenericREM theINS = (BytecodeInstructionGenericREM) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.REMAINDER, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                final BytecodeInstructionGenericADD theINS = (BytecodeInstructionGenericADD) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.ADD, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                final BytecodeInstructionGenericDIV theINS = (BytecodeInstructionGenericDIV) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();

                final Variable theNewVariable;
                final Value theDivValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.DIV, theValue2);
                switch (theINS.getType()) {
                case FLOAT:
                    theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theDivValue);
                    break;
                case DOUBLE:
                    theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theDivValue);
                    break;
                default:
                    theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new FloorExpression(aProgram, theInstruction.getOpcodeAddress(), theDivValue, TypeRef.toType(theINS.getType())));
                    break;
                }

                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                final BytecodeInstructionGenericMUL theINS = (BytecodeInstructionGenericMUL) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.MUL, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                final BytecodeInstructionGenericSUB theINS = (BytecodeInstructionGenericSUB) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.SUB, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                final BytecodeInstructionGenericXOR theINS = (BytecodeInstructionGenericXOR) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYXOR, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                final BytecodeInstructionGenericOR theINS = (BytecodeInstructionGenericOR) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYOR, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                final BytecodeInstructionGenericAND theINS = (BytecodeInstructionGenericAND) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYAND, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                final BytecodeInstructionGenericSHL theINS = (BytecodeInstructionGenericSHL) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYSHIFTLEFT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                final BytecodeInstructionGenericSHR theINS = (BytecodeInstructionGenericSHR) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYSHIFTRIGHT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericUSHR) {
                final BytecodeInstructionGenericUSHR theINS = (BytecodeInstructionGenericUSHR) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYUNSIGNEDSHIFTRIGHT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                final BytecodeInstructionIFNULL theINS = (BytecodeInstructionIFNULL) theInstruction;
                final Value theValue = aHelper.pop();
                final FixedBinaryExpression theBinaryValue = new FixedBinaryExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, FixedBinaryExpression.Operator.ISNULL);

                final ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(aProgram, theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                final BytecodeInstructionIFNONNULL theINS = (BytecodeInstructionIFNONNULL) theInstruction;
                final Value theValue = aHelper.pop();
                final FixedBinaryExpression theBinaryValue = new FixedBinaryExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, FixedBinaryExpression.Operator.ISNONNULL);

                final ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(aProgram, theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                final BytecodeInstructionIFICMP theINS = (BytecodeInstructionIFICMP) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final BinaryExpression theBinaryValue;
                switch (theINS.getType()) {
                case lt:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.LESSTHAN, theValue2);
                    break;
                case eq:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.EQUALS, theValue2);
                    break;
                case gt:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.GREATERTHAN, theValue2);
                    break;
                case ge:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.GREATEROREQUALS, theValue2);
                    break;
                case le:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.LESSTHANOREQUALS, theValue2);
                    break;
                case ne:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.NOTEQUALS, theValue2);
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }

                final ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(aProgram, theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFACMP) {
                final BytecodeInstructionIFACMP theINS = (BytecodeInstructionIFACMP) theInstruction;
                final Value theValue2 = aHelper.pop();
                final Value theValue1 = aHelper.pop();
                final BinaryExpression theBinaryValue;
                switch (theINS.getType()) {
                case eq:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.EQUALS, theValue2);
                    break;
                case ne:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.NOTEQUALS, theValue2);
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }

                final ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(aProgram, theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                final BytecodeInstructionIFCOND theINS = (BytecodeInstructionIFCOND) theInstruction;
                final Value theValue = aHelper.pop();
                final BinaryExpression theBinaryValue;
                switch (theINS.getType()) {
                case lt:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.LESSTHAN, new IntegerValue(0));
                    break;
                case eq:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.EQUALS, new IntegerValue(0));
                    break;
                case gt:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.GREATERTHAN, new IntegerValue(0));
                    break;
                case ge:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.GREATEROREQUALS, new IntegerValue(0));
                    break;
                case le:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.LESSTHANOREQUALS, new IntegerValue(0));
                    break;
                case ne:
                    theBinaryValue = new BinaryExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.NOTEQUALS, new IntegerValue(0));
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }

                final ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(aProgram, theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                final BytecodeInstructionObjectRETURN theINS = (BytecodeInstructionObjectRETURN) theInstruction;
                final Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new ReturnValueExpression(aProgram, theInstruction.getOpcodeAddress(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                final BytecodeInstructionGenericRETURN theINS = (BytecodeInstructionGenericRETURN) theInstruction;
                final Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new ReturnValueExpression(aProgram, theInstruction.getOpcodeAddress(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                final BytecodeInstructionATHROW theINS = (BytecodeInstructionATHROW) theInstruction;
                final Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new ThrowExpression(aProgram, theInstruction.getOpcodeAddress(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                final BytecodeInstructionRETURN theINS = (BytecodeInstructionRETURN) theInstruction;
                aTargetBlock.getExpressions().add(new ReturnExpression(aProgram, theInstruction.getOpcodeAddress()));
            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                final BytecodeInstructionNEW theINS = (BytecodeInstructionNEW) theInstruction;

                final BytecodeClassinfoConstant theClassInfo = theINS.getClassInfoForObjectToCreate();
                final BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo.getConstant());
                if (Objects.equals(theObjectType.name(), Address.class.getName())) {
                    // At this time the exact location is unknown, the value
                    // will be set at constructor invocation time
                    final Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT);
                    aHelper.push(theNewVariable);
                } else {
                    final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theObjectType), new NewObjectExpression(aProgram, theInstruction.getOpcodeAddress(), theClassInfo));
                    aHelper.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                final BytecodeInstructionNEWARRAY theINS = (BytecodeInstructionNEWARRAY) theInstruction;
                final Value theLength = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(
                        theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new NewArrayExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getPrimitiveType(), theLength));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWMULTIARRAY) {
                final BytecodeInstructionNEWMULTIARRAY theINS = (BytecodeInstructionNEWMULTIARRAY) theInstruction;
                final List<Value> theDimensions = new ArrayList<>();
                for (int i=0;i<theINS.getDimensions();i++) {
                    theDimensions.add(aHelper.pop());
                }
                Collections.reverse(theDimensions);
                final BytecodeTypeRef theTypeRef = linkerContext.getSignatureParser().toFieldType(new BytecodeUtf8Constant(theINS.getTypeConstant().getConstant().stringValue()));
                final Variable theNewVariable = aTargetBlock.newVariable(
                        theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new NewMultiArrayExpression(aProgram, theInstruction.getOpcodeAddress(), theTypeRef, theDimensions));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                final BytecodeInstructionANEWARRAY theINS = (BytecodeInstructionANEWARRAY) theInstruction;
                final Value theLength = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(
                        theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new NewArrayExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getObjectType(), theLength));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                final BytecodeInstructionGOTO theINS = (BytecodeInstructionGOTO) theInstruction;
                aTargetBlock.getExpressions().add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getJumpAddress()));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                final BytecodeInstructionL2Generic theINS = (BytecodeInstructionL2Generic) theInstruction;
                final Value theValue = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                final BytecodeInstructionI2Generic theINS = (BytecodeInstructionI2Generic) theInstruction;
                final Value theValue = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                final BytecodeInstructionF2Generic theINS = (BytecodeInstructionF2Generic) theInstruction;
                final Value theValue = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                final BytecodeInstructionD2Generic theINS = (BytecodeInstructionD2Generic) theInstruction;
                final Value theValue = aHelper.pop();
                final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                final BytecodeInstructionINVOKESPECIAL theINS = (BytecodeInstructionINVOKESPECIAL) theInstruction;
                final BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                final List<Value> theArguments = new ArrayList<>();
                final BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (final BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                final Variable theTarget = (Variable) aHelper.pop();
                final BytecodeObjectTypeRef theType = BytecodeObjectTypeRef
                        .fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                if (Objects.equals(theType, BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                    theTarget.initializeWith(theArguments.get(0));
                    aTargetBlock.getExpressions().add(new VariableAssignmentExpression(aProgram, theInstruction.getOpcodeAddress(), theTarget, theArguments.get(0)));
                } else {
                    final String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType()
                            .getNameIndex().getName().stringValue();
                    if ("getClass".equals(theMethodName) && BytecodeLinkedClass.GET_CLASS_SIGNATURE
                            .matchesExactlyTo(theSignature)) {
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), new TypeOfExpression(aProgram, theInstruction.getOpcodeAddress(), theTarget));
                        aHelper.push(theNewVariable);
                    } else {
                        final DirectInvokeMethodExpression theExpression = new DirectInvokeMethodExpression(aProgram, theInstruction.getOpcodeAddress(), theType,
                                theMethodName, theSignature, theTarget, theArguments);
                        if (theSignature.getReturnType().isVoid()) {
                            aTargetBlock.getExpressions().add(theExpression);
                        } else {
                            final Variable theNewVariable = aTargetBlock
                                    .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theExpression);
                            aHelper.push(theNewVariable);
                        }
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {
                final BytecodeInstructionINVOKEVIRTUAL theINS = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;
                final BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                if (theSignature.matchesExactlyTo(BytecodeLinkedClass.GET_CLASS_SIGNATURE) && "getClass".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    final Value theValue = new TypeOfExpression(aProgram, theInstruction.getOpcodeAddress(), aHelper.pop());
                    final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                    aHelper.push(theNewVariable);
                    continue;
                }

                final List<Value> theArguments = new ArrayList<>();
                final BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (final BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                final Value theTarget = aHelper.pop();
                final InvokeVirtualMethodExpression theExpression = new InvokeVirtualMethodExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getMethodReference().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments, false);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.getExpressions().add(theExpression);
                } else {
                    final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theExpression);
                    aHelper.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {
                final BytecodeInstructionINVOKEINTERFACE theINS = (BytecodeInstructionINVOKEINTERFACE) theInstruction;
                final BytecodeMethodSignature theSignature = theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                final List<Value> theArguments = new ArrayList<>();
                final BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (final BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                final Value theTarget = aHelper.pop();
                final InvokeVirtualMethodExpression theExpression = new InvokeVirtualMethodExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments, true);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.getExpressions().add(theExpression);
                } else {
                    final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theExpression);
                    aHelper.push(theNewVariable);
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                final BytecodeInstructionINVOKESTATIC theINS = (BytecodeInstructionINVOKESTATIC) theInstruction;
                final BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                final List<Value> theArguments = new ArrayList<>();
                final BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (final BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                final BytecodeClassinfoConstant theTargetClass = theINS.getMethodReference().getClassIndex().getClassConstant();
                final BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theTargetClass.getConstant());
                if (Objects.equals(theObjectType.name(), VM.class.getName()) && "newRuntimeGeneratedType".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    final RuntimeGeneratedTypeExpression theValue = new RuntimeGeneratedTypeExpression(aProgram, theInstruction.getOpcodeAddress(), theArguments.get(0), theArguments.get(1), theArguments.get(2));
                    final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);
                    aHelper.push(theNewVariable);
                } else if (Objects.equals(theObjectType.name(), MemoryManager.class.getName()) && "initTestMemory".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    // This invocation can be skipped!!!
                } else if (Objects.equals(theObjectType.name(), Address.class.getName())) {
                    final String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex()
                            .getName().stringValue();
                    switch (theMethodName) {
                    case "setIntValue": {

                        final Value theTarget = theArguments.get(0);
                        final Value theOffset = theArguments.get(1);
                        final Value theNewValue = theArguments.get(2);

                        final ComputedMemoryLocationWriteExpression theLocation = new ComputedMemoryLocationWriteExpression(aProgram, theInstruction.getOpcodeAddress(), theTarget, theOffset);
                        final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, theLocation);
                        aTargetBlock.getExpressions().add(new SetMemoryLocationExpression(aProgram, theInstruction.getOpcodeAddress(), theNewVariable, theNewValue));
                        break;
                    }
                    case "getStart": {

                        final Value theTarget = theArguments.get(0);
                        final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, theTarget);

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getStackTop": {

                        final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, new StackTopExpression(aProgram, theInstruction.getOpcodeAddress()));

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getMemorySize": {

                        final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, new MemorySizeExpression(aProgram, theInstruction.getOpcodeAddress()));

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getIntValue": {

                        final Value theTarget = theArguments.get(0);
                        final Value theOffset = theArguments.get(1);

                        final ComputedMemoryLocationReadExpression theLocation = new ComputedMemoryLocationReadExpression(aProgram, theInstruction.getOpcodeAddress(), theTarget, theOffset);
                        final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, theLocation);
                        aHelper.push(theNewVariable);

                        break;
                    }
                    case "unreachable": {
                        aTargetBlock.getExpressions().add(new UnreachableExpression(aProgram, theInstruction.getOpcodeAddress()));
                        break;
                    }
                    default:
                        throw new IllegalStateException("Not implemented : " + theMethodName);
                    }
                } else {
                    final BytecodeObjectTypeRef theClassToInvoke = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                    linkerContext.resolveClass(theClassToInvoke)
                            .resolveStaticMethod(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());

                    final BytecodeMethodSignature theCalledSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                    if ("sqrt".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.StrictMath".equals(theClassToInvoke.name())) {
                        final Value theValue = new SqrtExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theCalledSignature.getReturnType()),
                                theArguments.get(0));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("sqrt".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                                && "java.lang.Math".equals(theClassToInvoke.name())) {
                        final Value theValue = new SqrtExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theCalledSignature.getReturnType()),
                                theArguments.get(0));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("floor".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.Math".equals(theClassToInvoke.name())) {
                        final Value theValue = new FloatingPointFloorExpression(aProgram, theInstruction.getOpcodeAddress(), theArguments.get(0), TypeRef.toType(theCalledSignature.getReturnType()));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("floor".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.StrictMath".equals(theClassToInvoke.name())) {
                        final Value theValue = new FloatingPointFloorExpression(aProgram, theInstruction.getOpcodeAddress(), theArguments.get(0), TypeRef.toType(theCalledSignature.getReturnType()));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("ceil".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.Math".equals(theClassToInvoke.name())) {
                        final Value theValue = new FloatingPointCeilExpression(aProgram, theInstruction.getOpcodeAddress(), theArguments.get(0), TypeRef.toType(theCalledSignature.getReturnType()));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("ceil".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.StrictMath".equals(theClassToInvoke.name())) {
                        final Value theValue = new FloatingPointCeilExpression(aProgram, theInstruction.getOpcodeAddress(), theArguments.get(0), TypeRef.toType(theCalledSignature.getReturnType()));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("min".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.Math".equals(theClassToInvoke.name())) {
                        final Value theValue = new MinExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theCalledSignature.getReturnType()),
                                theArguments.get(0), theArguments.get(1));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("min".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.StrictMath".equals(theClassToInvoke.name())) {
                        final Value theValue = new MinExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theCalledSignature.getReturnType()),
                                theArguments.get(0), theArguments.get(1));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("max".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.Math".equals(theClassToInvoke.name())) {
                        final Value theValue = new MaxExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theCalledSignature.getReturnType()),
                                theArguments.get(0), theArguments.get(1));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("max".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "java.lang.StrictMath".equals(theClassToInvoke.name())) {
                        final Value theValue = new MaxExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.toType(theCalledSignature.getReturnType()),
                                theArguments.get(0), theArguments.get(1));
                        final Variable theNewVariable = aTargetBlock
                                .newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else if ("newInstance".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                                && "java.lang.reflect.Array".equals(theClassToInvoke.name())) {

                        final Value theArrayType = theArguments.get(0);
                        final Value theArraySize = theArguments.get(1);

                        final Value theValue = new NewArrayExpression(aProgram, theInstruction.getOpcodeAddress(), BytecodeObjectTypeRef.fromRuntimeClass(Object.class), theArraySize);
                        final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else {
                        final InvokeStaticMethodExpression theExpression = new InvokeStaticMethodExpression(aProgram, theInstruction.getOpcodeAddress(),
                                theClassToInvoke,
                                theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                theCalledSignature,
                                theArguments);
                        if (theSignature.getReturnType().isVoid()) {
                            aTargetBlock.getExpressions().add(theExpression);
                        } else {
                            final Variable theNewVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.toType(theSignature.getReturnType()), theExpression);
                            aHelper.push(theNewVariable);
                        }
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                final BytecodeInstructionINSTANCEOF theINS = (BytecodeInstructionINSTANCEOF) theInstruction;

                final Value theValueToCheck = aHelper.pop();
                final InstanceOfExpression theValue = new InstanceOfExpression(aProgram, theInstruction.getOpcodeAddress(), theValueToCheck, theINS.getTypeRef());

                final Variable theCheckResult = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.BOOLEAN, theValue);
                aHelper.push(theCheckResult);
            } else if (theInstruction instanceof BytecodeInstructionTABLESWITCH) {
                final BytecodeInstructionTABLESWITCH theINS = (BytecodeInstructionTABLESWITCH) theInstruction;
                final Value theValue = aHelper.pop();

                final ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getDefaultJumpTarget()));

                final Map<Long, ExpressionList> theOffsets = new HashMap<>();
                final long[] theJumpTargets = theINS.getOffsets();
                for (int i=0;i<theJumpTargets.length;i++) {
                    final ExpressionList theJump = new ExpressionList();
                    theJump.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getOpcodeAddress().add((int) theJumpTargets[i])));
                    theOffsets.put((long) i, theJump);
                }

                aTargetBlock.getExpressions().add(new TableSwitchExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, theINS.getLowValue(), theINS.getHighValue(),
                        theDefault, theOffsets));
            } else if (theInstruction instanceof BytecodeInstructionLOOKUPSWITCH) {
                final BytecodeInstructionLOOKUPSWITCH theINS = (BytecodeInstructionLOOKUPSWITCH) theInstruction;
                final Value theValue = aHelper.pop();

                final ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getDefaultJumpTarget()));

                final Map<Long, ExpressionList> thePairs = new HashMap<>();
                for (final BytecodeInstructionLOOKUPSWITCH.Pair thePair : theINS.getPairs()) {
                    final ExpressionList thePairExpressions = new ExpressionList();
                    thePairExpressions.add(new GotoExpression(aProgram, theInstruction.getOpcodeAddress(), theINS.getOpcodeAddress().add((int) thePair.getOffset())));
                    thePairs.put(thePair.getMatch(), thePairExpressions);
                }

                aTargetBlock.getExpressions().add(new LookupSwitchExpression(aProgram, theInstruction.getOpcodeAddress(), theValue, theDefault, thePairs));
            } else if (theInstruction instanceof BytecodeInstructionINVOKEDYNAMIC) {
                final BytecodeInstructionINVOKEDYNAMIC theINS = (BytecodeInstructionINVOKEDYNAMIC) theInstruction;

                final BytecodeInvokeDynamicConstant theConstant = theINS.getCallSite();
                final BytecodeMethodSignature theInitSignature = theConstant.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();


                final BytecodeBootstrapMethodsAttributeInfo theBootStrapMethods = aOwningClass.getAttributes().getByType(BytecodeBootstrapMethodsAttributeInfo.class);
                final BytecodeBootstrapMethod theBootstrapMethod = theBootStrapMethods.methodByIndex(theConstant.getBootstrapMethodAttributeIndex().getIndex());

                final BytecodeMethodHandleConstant theMethodRef = theBootstrapMethod.getMethodRef();
                final BytecodeMethodRefConstant theBootstrapMethodToInvoke = (BytecodeMethodRefConstant) theMethodRef.getReferenceIndex().getConstant();

                final Program theProgram = new Program(DebugInformation.empty());
                final RegionNode theInitNode = theProgram.getControlFlowGraph().createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);

                switch (theMethodRef.getReferenceKind()) {
                case REF_invokeStatic: {

                    final BytecodeObjectTypeRef theClassWithBootstrapMethod = BytecodeObjectTypeRef
                            .fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant());

                    final BytecodeMethodSignature theSignature = theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType()
                            .getDescriptorIndex().methodSignature();

                    final List<Value> theArguments = new ArrayList<>();
                    // Add the three default constants
                    // TMethodHandles.Lookup aCaller,
                    theArguments.add(theInitNode
                            .newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new MethodHandlesGeneratedLookupExpression(aProgram, theInstruction.getOpcodeAddress(), theClassWithBootstrapMethod)));
                    theArguments.add(theInitNode.newVariable(theInstruction.getOpcodeAddress(),
                            TypeRef.Native.REFERENCE, new StringValue(theConstant.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())));
                    // TMethodType aInvokedType,
                    theArguments.add(theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new MethodTypeExpression(aProgram, theInstruction.getOpcodeAddress(),
                            theInitSignature)));

                    // Revolve the static arguments
                    for (final BytecodeConstant theArgumentConstant : theBootstrapMethod.getArguments()) {

                        if (theArgumentConstant instanceof BytecodeMethodTypeConstant) {
                            final BytecodeMethodTypeConstant theMethodType = (BytecodeMethodTypeConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE,
                                    new MethodTypeExpression(aProgram, theInstruction.getOpcodeAddress(), theMethodType.getDescriptorIndex().methodSignature())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeStringConstant) {
                            final BytecodeStringConstant thePrimitive = (BytecodeStringConstant) theArgumentConstant;
                            theArguments.add(theInitNode
                                    .newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new StringValue(thePrimitive.getValue().stringValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeLongConstant) {
                            final BytecodeLongConstant thePrimitive = (BytecodeLongConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.LONG, new LongValue(thePrimitive.getLongValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeIntegerConstant) {
                            final BytecodeIntegerConstant thePrimitive = (BytecodeIntegerConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.INT, new LongValue(thePrimitive.getIntegerValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeFloatConstant) {
                            final BytecodeFloatConstant thePrimitive = (BytecodeFloatConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.FLOAT, new FloatValue(thePrimitive.getFloatValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeDoubleConstant) {
                            final BytecodeDoubleConstant thePrimitive = (BytecodeDoubleConstant) theArgumentConstant;
                            theArguments
                            .add(theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.DOUBLE, new DoubleValue(thePrimitive.getDoubleValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeMethodHandleConstant) {
                            final BytecodeMethodHandleConstant theMethodHandle = (BytecodeMethodHandleConstant) theArgumentConstant;
                            final BytecodeReferenceIndex theReference = theMethodHandle.getReferenceIndex();
                            final BytecodeMethodRefConstant theReferenceConstant = (BytecodeMethodRefConstant) theReference
                                    .getConstant();
                            theArguments.add(theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new MethodRefExpression(aProgram, theInstruction.getOpcodeAddress(),
                                    BytecodeObjectTypeRef.fromUtf8Constant(theReferenceConstant.getClassIndex().getClassConstant().getConstant()),
                                    theReferenceConstant.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theReferenceConstant.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature())));
                            continue;
                        }
                        throw new IllegalStateException("Unsupported argument type : " + theArgumentConstant);
                    }

                    // Ok, is the last argument of the bootstrap method a vararg argument
                    final BytecodeTypeRef theLastArgument = theSignature.getArguments()[theSignature.getArguments().length - 1];
                    if (theLastArgument.isArray()) {
                        // Yes, so we have to wrap everything from this position on in an array
                        final int theSignatureLength = theSignature.getArguments().length;
                        final int theArgumentsLength = theArguments.size();

                        final int theVarArgsLength = theArgumentsLength - theSignatureLength + 1;
                        final Variable theNewVarargsArray = theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, new NewArrayExpression(aProgram, theInstruction.getOpcodeAddress(),
                                BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new IntegerValue(theVarArgsLength)));
                        for (int i = theSignatureLength - 1; i < theArgumentsLength; i++) {
                            final Value theVariable = theArguments.get(i);
                            theArguments.remove(theVariable);
                            theInitNode.getExpressions().add(new ArrayStoreExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theNewVarargsArray, new IntegerValue(i - theSignatureLength + 1), theVariable));
                        }
                        theArguments.add(theNewVarargsArray);
                    }

                    final InvokeStaticMethodExpression theInvokeStaticValue = new InvokeStaticMethodExpression(aProgram, theInstruction.getOpcodeAddress(),
                            BytecodeObjectTypeRef.fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant()),
                            theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                            theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(),
                            theArguments);
                    final Variable theNewVariable = theInitNode.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theInvokeStaticValue);
                    theInitNode.getExpressions().add(new ReturnValueExpression(aProgram, theInstruction.getOpcodeAddress(), theNewVariable));

                    // First step, we construct a callsite
                    final ResolveCallsiteObjectExpression theValue = new ResolveCallsiteObjectExpression(theInstruction.getOpcodeAddress(), aOwningClass.getThisInfo().getConstant().stringValue() + "_" + aMethod.getName().stringValue() + "_" + theINS.getOpcodeAddress().getAddress(), aOwningClass, theProgram, theInitNode);
                    final Variable theCallsiteVariable = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theValue);

                    final List<Value> theInvokeArguments = new ArrayList<>();

                    final Variable theArray = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(),
                            TypeRef.Native.REFERENCE, new NewArrayExpression(aProgram, theInstruction.getOpcodeAddress(), BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new IntegerValue(theInitSignature.getArguments().length)));

                    for (int i=theInitSignature.getArguments().length-1;i>=0;i--) {
                        final Value theIndex = new IntegerValue(i);
                        Value theStoredValue = aHelper.pop();

                        if (theStoredValue.resolveType() == TypeRef.Native.INT) {
                            // Create Integer object to contain int
                            final BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromRuntimeClass(Integer.class);
                            final BytecodeTypeRef[] args_def = new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT};
                            final BytecodeMethodSignature sig = new BytecodeMethodSignature(theType, args_def);
                            final List<Value> args = new ArrayList<>();
                            args.add(theStoredValue);

                            theStoredValue = new InvokeStaticMethodExpression(aProgram, theInstruction.getOpcodeAddress(), theType, "valueOf", sig, args);
                            theStoredValue = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theStoredValue);
                        }

                        aTargetBlock.getExpressions().add(new ArrayStoreExpression(aProgram, theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theArray, theIndex, theStoredValue));
                    }

                    theInvokeArguments.add(theArray);

                    final InvokeVirtualMethodExpression theInvokeValue = new InvokeVirtualMethodExpression(aProgram, theInstruction.getOpcodeAddress(), "invokeExact",
                            new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                                    new BytecodeTypeRef[] {
                                            new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), 1) }),
                            theCallsiteVariable, theInvokeArguments, false);

                    final Variable theInvokeExactResult = aTargetBlock.newVariable(theInstruction.getOpcodeAddress(), TypeRef.Native.REFERENCE, theInvokeValue);
                    aHelper.push(theInvokeExactResult);

                    break;
                }
                default:
                    throw new IllegalStateException(
                            "Nut supported reference kind for invoke dynamic : " + theMethodRef.getReferenceKind());
                }
            } else {
                throw new IllegalArgumentException("Not implemented : " + theInstruction);
            }
        }

        aHelper.finalizeExportState();
    }
}