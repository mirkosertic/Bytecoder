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
import java.util.List;

public class Variable {

    public static class Usage {

    }

    public static class ExpressionUsage extends Usage {

        private final Expression expression;

        public ExpressionUsage(Expression aExpression) {
            expression = aExpression;
        }
    }

    public static class ValueUsage extends Usage {

        private final Value value;

        public ValueUsage(Value aValue) {
            value = aValue;
        }
    }

    private Type type;
    private final String name;
    private Value value;
    private final List<Usage> usages;

    public Variable(Type aType, String aName, Value aValue) {
        type = aType;
        name = aName;
        value = aValue;
        usages = new ArrayList<>();
    }

    public Variable usedBy(Expression aExpression) {
        usages.add(new ExpressionUsage(aExpression));
        return this;
    }

    public Variable usedBy(Value aValue) {
        usages.add(new ValueUsage(aValue));
        return this;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Value getValue() {
        return value;
    }

    public Variable withNewValue(Value aNewValue) {
        return new Variable(type, name, aNewValue);
    }

    public void setValue(Value aNewValue) {
        value = aNewValue;
    }

    public void setType(Type aType) {
        type = aType;
    }

    public int getUsageCount() {
        return usages.size();
    }
}