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
package de.mirkosertic.bytecoder.core.backend.opencl;

import de.mirkosertic.bytecoder.core.ir.ResolvedField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenCLInputOutputs {

    public static class KernelArgument {

        public enum Type {
            INPUT, OUTPUT, INPUTOUTPUT
        }

        private final ResolvedField field;
        private final Type type;

        KernelArgument(final ResolvedField field, final Type type) {
            this.field = field;
            this.type = type;
        }

        public ResolvedField getField() {
            return field;
        }

        public Type getType() {
            return type;
        }
    }

    private final Map<String, KernelArgument> values;

    public OpenCLInputOutputs() {
        values = new HashMap<>();
    }

    public void registerReadFrom(final ResolvedField resolvedField) {
        values.computeIfAbsent(resolvedField.name,
                k -> new KernelArgument(resolvedField, KernelArgument.Type.INPUT));
    }

    public void registerWriteTo(final ResolvedField resolvedField) {
        KernelArgument theArgument = values.get(resolvedField.name);
        if (theArgument == null) {
            theArgument = new KernelArgument(resolvedField, KernelArgument.Type.OUTPUT);
            values.put(resolvedField.name, theArgument);
        } else {
            if (theArgument.type == KernelArgument.Type.INPUT) {
                theArgument = new KernelArgument(resolvedField, KernelArgument.Type.INPUTOUTPUT);
                values.put(resolvedField.name, theArgument);
            }
        }
    }

    public List<KernelArgument> arguments() {
        final List<KernelArgument> result = new ArrayList<>(values.values());
        result.sort(Comparator.comparing(o -> o.field.name));
        return result;
    }
}
