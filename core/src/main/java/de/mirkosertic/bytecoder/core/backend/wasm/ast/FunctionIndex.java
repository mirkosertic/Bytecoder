/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.backend.wasm.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionIndex {

    private final List<Function> functions;
    private final Map<String, Function> labelToFunction;

    FunctionIndex() {
        functions = new ArrayList<>();
        labelToFunction = new HashMap<>();
    }

    public int size() {
        return functions.size();
    }

    public Function get(final int aIndex) {
        return functions.get(aIndex);
    }

    public void add(final Function function) {
        functions.add(function);
        labelToFunction.put(function.getLabel().toLowerCase(), function);
    }

    public int indexOf(final Function value) {
        return functions.indexOf(value);
    }

    public List<ExportableFunction> exportableFunctions() {
        return functions.stream().filter(t -> t instanceof ExportableFunction).map(t -> (ExportableFunction) t).collect(Collectors.toList());
    }

    public <T extends Function> T findByLabel(final String label) {
        final String key = label.toLowerCase();
        final T result = (T) labelToFunction.get(key);
        if (result == null) {
            throw new IllegalArgumentException("No such method : " + label);
        }
        return result;
    }
}
