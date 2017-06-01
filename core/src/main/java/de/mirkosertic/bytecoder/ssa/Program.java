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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;

public class Program {

    private final List<Block> blocks;
    private final Map<Integer, Variable> variables;

    public Program() {
        blocks = new ArrayList<>();
        variables = new HashMap<>();
    }

    public void add(Block aBlock) {
        blocks.add(aBlock);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public List<Variable> getVariables() {
        return new ArrayList<>(variables.values());
    }

    public Variable createVariable(Value aValue) {
        int theIndex = variables.size();
        Variable theNewVariable = new Variable(theIndex, aValue);
        variables.put(theIndex, theNewVariable);
        return theNewVariable;
    }

    public Set<BytecodeObjectTypeRef> getStaticReferences() {
        Set<BytecodeObjectTypeRef> theResult = new HashSet<>();
        for (Variable theVariable : variables.values()) {
            Value theValue = theVariable.getValue();
            if (theValue instanceof GetStaticValue) {
                GetStaticValue theStaticValue = (GetStaticValue) theValue;
                theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theStaticValue.getField().getClassIndex().getClassConstant().getConstant()));
            }
            if (theValue instanceof ClassReferenceValue) {
                ClassReferenceValue theClassRef = (ClassReferenceValue) theValue;
                theResult.add(theClassRef.getType());
            }
            if (theValue instanceof NewArrayValue) {
                NewArrayValue theNewArray = (NewArrayValue) theValue;
                if (theNewArray.getType() instanceof BytecodeObjectTypeRef) {
                    theResult.add((BytecodeObjectTypeRef) theNewArray.getType());
                }
            }
            if (theValue instanceof NewMultiArrayValue) {
                NewMultiArrayValue theNewArray = (NewMultiArrayValue) theValue;
                if (theNewArray.getType() instanceof BytecodeObjectTypeRef) {
                    theResult.add((BytecodeObjectTypeRef) theNewArray.getType());
                }
            }
            if (theValue instanceof NewObjectValue) {
                NewObjectValue theNewObjectValue = (NewObjectValue) theValue;
                theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theNewObjectValue.getType().getConstant()));
            }
        }
        for (Block theBlock : blocks) {
            for (Expression theExpression : theBlock.getExpressions()) {
                if (theExpression instanceof PutStaticExpression) {
                    PutStaticExpression theE = (PutStaticExpression) theExpression;
                    theResult.add(BytecodeObjectTypeRef
                            .fromUtf8Constant(theE.getField().getClassIndex().getClassConstant().getConstant()));
                }
            }
        }
        return theResult;
    }
}
