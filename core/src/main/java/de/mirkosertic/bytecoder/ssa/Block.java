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

import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Block {

    private final Map<Integer, Variable> variables;
    private final List<Expression> expressions;
    private final List<Variable> importedStack;

    public Block() {
        variables = new HashMap<>();
        expressions = new ArrayList<>();
        importedStack = new ArrayList<>();
    }

    public Variable newImportedStackVariable() {
        int theIndex = variables.size();
        Variable theNewVariable = new Variable(theIndex, new NullValue());
        variables.put(theIndex, theNewVariable);
        importedStack.add(theNewVariable);
        expressions.add(new InitVariableExpression(theNewVariable));
        return theNewVariable;
    }

    public Variable newImportedLocalVariable(int aIndex) {
        int theIndex = variables.size();
        Variable theNewVariable = new Variable(theIndex, new ExternalReferenceValue(aIndex));
        variables.put(theIndex, theNewVariable);
        expressions.add(new InitVariableExpression(theNewVariable));
        return theNewVariable;
    }

    public Variable newVariable(Value aValue)  {
        int theIndex = variables.size();
        Variable theNewVariable = new Variable(theIndex, aValue);
        variables.put(theIndex, theNewVariable);
        expressions.add(new InitVariableExpression(theNewVariable));
        return theNewVariable;
    }

    public void addExpression(Expression aExpression) {
        expressions.add(aExpression);
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public Set<BytecodeObjectTypeRef> getStaticReferences() {
        Set<BytecodeObjectTypeRef> theResult = new HashSet<>();
        for (Variable theVariable : variables.values()) {
            Value theValue = theVariable.getValue();
            if (theValue instanceof GetStaticValue) {
                GetStaticValue theStaticValue = (GetStaticValue) theValue;
                theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theStaticValue.getField().getClassIndex().getClassConstant().getConstant()));
            }
            if (theValue instanceof NewObjectValue) {
                NewObjectValue theNewObjectValue = (NewObjectValue) theValue;
                theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theNewObjectValue.getType().getConstant()));
            }
        }
        for (Expression theExpression : expressions) {
            if (theExpression instanceof PutStaticExpression) {
                PutStaticExpression theE = (PutStaticExpression) theExpression;
                theResult.add(BytecodeObjectTypeRef.fromUtf8Constant(theE.getField().getClassIndex().getClassConstant().getConstant()));
            }
        }
        return theResult;
    }
}
