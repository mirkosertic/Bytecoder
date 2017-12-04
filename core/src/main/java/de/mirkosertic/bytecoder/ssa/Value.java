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

public class Value {

    private final Set<Value> providesValueFor;
    private final List<Value> consumesValueFrom;

    public Value() {
        providesValueFor = new HashSet<>();
        consumesValueFrom = new ArrayList<>();
    }

    public void consume(Value aValue) {
        aValue.providesValueFor.add(this);
        consumesValueFrom.add(aValue);
    }

    public Type resolveType() {
        return Type.UNKNOWN;
    }
}
