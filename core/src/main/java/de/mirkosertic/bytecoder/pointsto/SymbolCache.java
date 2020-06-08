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
package de.mirkosertic.bytecoder.pointsto;

import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.PHIValue;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.HashMap;
import java.util.Map;

public class SymbolCache {

    private final Map<Object, Symbol> mappings;
    private final Map<PHIValue, String> phiVariables;

    public SymbolCache() {
        mappings = new HashMap<>();
        phiVariables = new HashMap<>();
    }

    public VariableSymbol variableSymbolForVariable(final Variable aVariable) {
        return (VariableSymbol) mappings.computeIfAbsent(aVariable, t -> new VariableSymbol(aVariable.getName()));
    }

    public VariableSymbol variableSymbolForPHI(final PHIValue aValue) {
        final String name = phiVariables.computeIfAbsent(aValue, t -> "PHI" + phiVariables.size());
        return (VariableSymbol) mappings.computeIfAbsent(aValue, t -> new VariableSymbol(name));
    }

    public ParamRef symbolForMethodParameter(final MethodParameterValue aValue) {
        return (ParamRef) mappings.computeIfAbsent(aValue, t -> new ParamRef(aValue.getParameterIndex()));
    }
}
