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

import java.util.*;
import java.util.function.Function;

import de.mirkosertic.bytecoder.core.*;

public class GraphProgramGenerator implements ProgramGenerator {

    public static class ProxyValue extends Value {

        private final Value delegate;

        public ProxyValue(Value aDelegate) {
            this.delegate = aDelegate;
        }

        @Override
        public TypeRef resolveType() {
            return delegate.resolveType();
        }
    }

    public static class State {

        private final Map<Integer, ProxyValue> variableSlots;
        private final Map<Integer, ProxyValue> importedVariableSlots;
        private final Stack<ProxyValue> valueStack;
        private final List<ProxyValue> importedStack;

        protected State() {
            variableSlots = new HashMap<>();
            valueStack = new Stack<>();
            importedVariableSlots = new HashMap<>();
            importedStack = new ArrayList<>();
        }

        public ProxyValue variableInSlot(int aIndex) {
            ProxyValue theValue = variableSlots.get(aIndex);
            if (theValue == null) {
                theValue = new ProxyValue(new PHIFunction());
                importedVariableSlots.put(aIndex, theValue);
                variableSlots.put(aIndex, theValue);
            }
            return theValue;
        }

        public void setVariableInSlot(int aIndex, ProxyValue aValue) {
            variableSlots.put(aIndex, aValue);
        }

        public void push(ProxyValue aValue) {
            valueStack.push(aValue);
        }

        public ProxyValue pop() {
            if (valueStack.isEmpty()) {
                ProxyValue theImported = new ProxyValue(new PHIFunction());
                importedStack.add(theImported);
                return theImported;
            }
            return valueStack.pop();
        }

        public State clone() {
            State theNewState = new State();
            theNewState.variableSlots.putAll(variableSlots);
            theNewState.valueStack.addAll(valueStack);
            return this;
        }
    }

    public static class ParsingContext {
        private final Map<BytecodeBasicBlock, GraphNode> nodes;
        private final Set<GraphNode> visited;
        private final Map<GraphNode, State> finalStates;
        private final State initislState;

        public ParsingContext(Map<BytecodeBasicBlock, GraphNode> aNodes, State aInitialState) {
            nodes = aNodes;
            visited = new HashSet<>();
            finalStates = new HashMap<>();
            initislState = aInitialState;
        }

        public State getInitislState() {
            return initislState;
        }

        public boolean shouldVisit(GraphNode aNode) {
            return visited.add(aNode);
        }

        public void saveFinalState(GraphNode aNode, State aState) {
            finalStates.put(aNode, aState);
        }

        public State finalStateFor(GraphNode aNode) {
            return finalStates.get(aNode);
        }

        public BytecodeBasicBlock bytecodeBlockFor(GraphNode aNode) {
            for (Map.Entry<BytecodeBasicBlock, GraphNode> theEntry : nodes.entrySet()) {
                if (theEntry.getValue() == aNode) {
                    return theEntry.getKey();
                }
            }
            throw new IllegalStateException();
        }

        public GraphNode nodeByAddress(BytecodeOpcodeAddress aAddress) {
            for (GraphNode theNode : nodes.values()) {
                if (theNode.getStartAddress().equals(aAddress)) {
                    return theNode;
                }
            }
            throw new IllegalStateException();
        }
    }

    public static final ProgramGeneratorFactory FACTORY = GraphProgramGenerator::new;

    private final BytecodeLinkerContext linkerContext;

    public GraphProgramGenerator(BytecodeLinkerContext aLinkerContext) {
        linkerContext = aLinkerContext;
    }

    private void resolve(GraphNode aNode, ParsingContext aParsingContext) {
        if (!aParsingContext.shouldVisit(aNode)) {
            return;
        }
        for (GraphNode thePrecesessor : aNode.getPredecessors()) {
            resolve(thePrecesessor, aParsingContext);
        }

        State theImportedState;
        // At this stage if there is more than one predecessor, we need to insert
        // functions, welse we can import the existing state
        if (aNode.getStartAddress().getAddress() == 0) {
            // this is the start node, so we need to take the imported state as a consideration
            theImportedState = aParsingContext.getInitislState();
        } if (aNode.getPredecessors().size() == 1) {
            theImportedState = aParsingContext.finalStateFor(aNode.getPredecessors().iterator().next());
        } if (aNode.getPredecessors().size() > 1) {
            // This would be a state full of phi functions
            theImportedState = new State();
        } else {
            // Not the first nide, bzt also no predecessors
            // Maybe an exception handler?
            theImportedState = new State();
        }
        BytecodeBasicBlock theBasicBlock = aParsingContext.bytecodeBlockFor(aNode);
        State theFinalState = processBlockForNode(theImportedState, theBasicBlock, aNode);
        aParsingContext.saveFinalState(aNode, theFinalState);

        for (GraphNode theSuccessor : aNode.getSuccessors()) {
            resolve(theSuccessor, aParsingContext);
        }
    }

    @Override
    public Program generateFrom(BytecodeClass aBytecodeClass, BytecodeMethod aMethod) {

        System.out.println("Compiling " + aBytecodeClass.getThisInfo().getConstant().stringValue() + "." + aMethod.getName().stringValue());

        BytecodeCodeAttributeInfo theCode = aMethod.getCode(aBytecodeClass);
        BytecodeProgram theBytecode = theCode.getProgramm();

        // This is the start block, naturally it does not have any predeseccots
        State theInitialState = new State();
        for (int i=0;i<theCode.getMaxLocals();i++) {
            theInitialState.setVariableInSlot(i, new ProxyValue(new NullValue()));
        }

        int theIndex = 0;
        if (!aMethod.getAccessFlags().isStatic()) {
            theInitialState.setVariableInSlot(theIndex++, new ProxyValue(new SelfReferenceParameterValue()));
        }
        for (int i=0;i<aMethod.getSignature().getArguments().length;i++) {
            theInitialState.setVariableInSlot(theIndex++, new ProxyValue(new MethodParameterValue(i, TypeRef.toType(aMethod.getSignature().getArguments()[i]))));
        }

        Program theProgram = new Program();
        ParsingContext theContext = new ParsingContext(toControlFlowGraph(theProgram, theBytecode), theInitialState);

        // Depth-First traversal of CFG
        GraphNode theStart = theContext.nodeByAddress(new BytecodeOpcodeAddress(0));
        resolve(theStart, theContext);

        return theProgram;
    }

    private State processBlockForNode(State axImportedState, BytecodeBasicBlock aBlock, GraphNode aNode) {
        State theState = axImportedState.clone();
        for (BytecodeInstruction theInstruction : aBlock.getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                theState.push(new ProxyValue(new NullValue()));
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                ProxyValue theValue = theState.pop();
                theState.push(theValue);
                theState.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH theConst = (BytecodeInstructionBIPUSH) theInstruction;
                theState.push(new ProxyValue(new IntegerValue(theConst.getByteValue())));
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theConst = (BytecodeInstructionICONST) theInstruction;
                theState.push(new ProxyValue(new IntegerValue(theConst.getIntConst())));
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theLoad = (BytecodeInstructionALOAD) theInstruction;
                theState.push(theState.variableInSlot(theLoad.getVariableIndex() - 1));
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                theState.push(theState.variableInSlot(theLoad.getVariableIndex() - 1));
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theStore = (BytecodeInstructionASTORE) theInstruction;
                ProxyValue theValue = theState.pop();
                theState.setVariableInSlot(theStore.getVariableIndex() - 1, theValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theStore = (BytecodeInstructionGenericSTORE) theInstruction;
                ProxyValue theValue = theState.pop();
                theState.setVariableInSlot(theStore.getVariableIndex() - 1, theValue);
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theGetStatic = (BytecodeInstructionGETSTATIC) theInstruction;
                theState.push(new ProxyValue(new GetStaticValue(theGetStatic.getConstant())));
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC thePutStatic = (BytecodeInstructionPUTSTATIC) theInstruction;
                ProxyValue theValue = theState.pop();
                aNode.getExpressions().add(new CommentExpression("Put static value of field " + thePutStatic.getConstant().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue()));
            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theNew = (BytecodeInstructionNEW) theInstruction;
                ProxyValue theValue = new ProxyValue(new NewObjectValue(theNew.getClassInfoForObjectToCreate()));
                theState.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                aNode.getExpressions().add(new ReturnExpression());
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                ProxyValue theException = theState.pop();
                aNode.getExpressions().add(new CommentExpression("Throw exception : " + theException));
                //aNode.getExpressions().add(new ThrowExpression());
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                ProxyValue theValueToReturn = theState.pop();
                aNode.getExpressions().add(new CommentExpression("Return with value: " + theValueToReturn));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                ProxyValue theValueToReturn = theState.pop();
                aNode.getExpressions().add(new CommentExpression("Return with value: " + theValueToReturn));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                ProxyValue theValueToReturn = theState.pop();
                aNode.getExpressions().add(new CommentExpression("Return with value: " + theValueToReturn));
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                ProxyValue theValue1 = theState.pop();
                ProxyValue theValue2 = theState.pop();
                Value theComputation = new Value() {
                    @Override
                    public TypeRef resolveType() {
                        return null;
                    }
                };
                theComputation.consume(Value.ConsumptionType.ARGUMENT, theValue1);
                theComputation.consume(Value.ConsumptionType.ARGUMENT, theValue2);
                theState.push(new ProxyValue(theComputation));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theINS = (BytecodeInstructionIFICMP) theInstruction;
                ProxyValue theValue1 = theState.pop();
                ProxyValue theValue2 = theState.pop();
                aNode.getExpressions().add(new CommentExpression("IF jump to " + theINS.getJumpTarget().getAddress()));
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                BytecodeInstructionINVOKESPECIAL theINS = (BytecodeInstructionINVOKESPECIAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<ProxyValue> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(theState.pop());
                }
                Collections.reverse(theArguments);
                ProxyValue theTarget = theState.pop();

                BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();

                if (theSignature.getReturnType().isVoid()) {
                    // Values were consumed
                } else {
                    Value theValue = new Value() {
                        @Override
                        public TypeRef resolveType() {
                            return TypeRef.toType(theSignature.getReturnType());
                        }
                    };
                    theValue.consume(Value.ConsumptionType.ARGUMENT, theTarget);
                    theValue.consume(Value.ConsumptionType.ARGUMENT, theArguments);
                    theState.push(new ProxyValue(theValue));
                }

                aNode.getExpressions().add(new CommentExpression("Invoke cons method " + theMethodName));

            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {
                BytecodeInstructionINVOKEINTERFACE theINS = (BytecodeInstructionINVOKEINTERFACE) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<ProxyValue> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(theState.pop());
                }
                Collections.reverse(theArguments);
                ProxyValue theTarget = theState.pop();

                BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodDescriptor().getClassIndex().getClassConstant().getConstant());
                String theMethodName = theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();

                if (theSignature.getReturnType().isVoid()) {
                    // Values were consumed
                } else {
                    Value theValue = new Value() {
                        @Override
                        public TypeRef resolveType() {
                            return TypeRef.toType(theSignature.getReturnType());
                        }
                    };
                    theValue.consume(Value.ConsumptionType.ARGUMENT, theTarget);
                    theValue.consume(Value.ConsumptionType.ARGUMENT, theArguments);
                    theState.push(new ProxyValue(theValue));
                }

                aNode.getExpressions().add(new CommentExpression("Invoke interface method " + theMethodName));

            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                BytecodeInstructionINVOKESTATIC theINS = (BytecodeInstructionINVOKESTATIC) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<ProxyValue> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(theState.pop());
                }
                Collections.reverse(theArguments);

                BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();

                if (theSignature.getReturnType().isVoid()) {
                    // Values were consumed
                } else {
                    Value theValue = new Value() {
                        @Override
                        public TypeRef resolveType() {
                            return TypeRef.toType(theSignature.getReturnType());
                        }
                    };
                    theValue.consume(Value.ConsumptionType.ARGUMENT, theArguments);
                    theState.push(new ProxyValue(theValue));
                }

                aNode.getExpressions().add(new CommentExpression("Invoke static method " + theMethodName));


            } else {
                throw new IllegalStateException("Unsupported instruction : " + theInstruction);
            }
        }
        return theState;
    }
}