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

import java.lang.reflect.Field;
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

        private final Field field;
        private final Type type;

        KernelArgument(Field aField, Type aType) {
            this.field = aField;
            this.type = aType;
        }

        public Field getField() {
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

    public void registerReadFrom(Field aClassField) {
        KernelArgument theArgument = values.get(aClassField.getName());
        if (theArgument == null) {
            theArgument = new KernelArgument(aClassField, KernelArgument.Type.INPUT);
            values.put(aClassField.getName(), theArgument);
        }
    }

    public void registerWriteTo(Field aClassField) {
        KernelArgument theArgument = values.get(aClassField.getName());
        if (theArgument == null) {
            theArgument = new KernelArgument(aClassField, KernelArgument.Type.OUTPUT);
            values.put(aClassField.getName(), theArgument);
        } else {
            if (theArgument.type == KernelArgument.Type.INPUT) {
                theArgument = new KernelArgument(aClassField, KernelArgument.Type.INPUTOUTPUT);
                values.put(aClassField.getName(), theArgument);
            }
        }
    }

    public List<KernelArgument> arguments() {
        List<KernelArgument> theResult = new ArrayList<>(values.values());
        theResult.sort(Comparator.comparing(o -> o.field.getName()));
        return theResult;
    }
}
