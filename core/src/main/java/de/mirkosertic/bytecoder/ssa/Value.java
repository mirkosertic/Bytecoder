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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Value {

    public static enum ConsumptionType {
        ARGUMENT, INVOCATIONTARGET, INITIALIZATION, PHIPROPAGATE
    }

    public static class Consumption {
        private final ConsumptionType type;
        private final Value value;

        public Consumption(ConsumptionType aType, Value aValue) {
            type = aType;
            value = aValue;
        }
    }

    private final Set<Value> providesValueFor;
    private final List<Consumption> consumesValueFrom;

    public Value() {
        providesValueFor = new HashSet<>();
        consumesValueFrom = new ArrayList<>();
    }

    public <T extends Value> List<T> consumedValues(ConsumptionType aType) {
        List<T> theResult = new ArrayList<>();
        for (Consumption theConsumption : consumesValueFrom) {
            if (theConsumption.type == aType) {
                theResult.add((T) theConsumption.value);
            }
        }
        return theResult;
    }

    public void consume(ConsumptionType aType, List<? extends Value> aValues) {
        for (Value theValue : aValues) {
            consume(aType, theValue);
        }
    }

    public <T extends Value> T resolveFirstArgument() {
        return (T) consumedValues(ConsumptionType.ARGUMENT).get(0);
    }

    public <T extends Value> T resolveSecondArgument() {
        return (T) consumedValues(ConsumptionType.ARGUMENT).get(1);
    }

    public <T extends Value> T resolveThirdArgument() {
        return (T) consumedValues(ConsumptionType.ARGUMENT).get(2);
    }

    public void consume(ConsumptionType aType, Value aValue) {
        aValue.providesValueFor.add(this);
        consumesValueFrom.add(new Consumption(aType, aValue));
    }

    public abstract TypeRef resolveType();

    public void unbind() {
        for (Value theValue : providesValueFor) {
            theValue.removeConsumptionFor(theValue);
        }
    }

    private void removeConsumptionFor(Value aValue) {
        List<Consumption> theValuesToRemove = new ArrayList<>();
        for (Consumption theValue : consumesValueFrom) {
            if (theValue.value == aValue) {
                theValuesToRemove.add(theValue);
            }
        }
        consumesValueFrom.removeAll(theValuesToRemove);
    }
}
