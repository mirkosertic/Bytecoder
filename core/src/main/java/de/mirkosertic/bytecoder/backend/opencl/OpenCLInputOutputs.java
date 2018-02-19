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
package de.mirkosertic.bytecoder.backend.opencl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;

public class OpenCLInputOutputs {

    public static class KernelArgument {

        public enum Type {
            INPUT, OUTPUT, INPUTOUTPUT
        }

        private final BytecodeResolvedFields.FieldEntry field;
        private final Type type;

        KernelArgument(BytecodeResolvedFields.FieldEntry aField, Type aType) {
            field = aField;
            type = aType;
        }

        public BytecodeResolvedFields.FieldEntry getField() {
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

    public void registerReadFrom(BytecodeResolvedFields.FieldEntry aLinkedField) {
        values.computeIfAbsent(aLinkedField.getValue().getName().stringValue(),
                k -> new KernelArgument(aLinkedField, KernelArgument.Type.INPUT));
    }

    public void registerWriteTo(BytecodeResolvedFields.FieldEntry aLinkedField) {
        KernelArgument theArgument = values.get(aLinkedField.getValue().getName().stringValue());
        if (theArgument == null) {
            theArgument = new KernelArgument(aLinkedField, KernelArgument.Type.OUTPUT);
            values.put(aLinkedField.getValue().getName().stringValue(), theArgument);
        } else {
            if (theArgument.type == KernelArgument.Type.INPUT) {
                theArgument = new KernelArgument(aLinkedField, KernelArgument.Type.INPUTOUTPUT);
                values.put(aLinkedField.getValue().getName().stringValue(), theArgument);
            }
        }
    }

    public List<KernelArgument> arguments() {
        List<KernelArgument> theResult = new ArrayList<>(values.values());
        theResult.sort(Comparator.comparing(o -> o.field.getValue().getName().stringValue()));
        return theResult;
    }
}
