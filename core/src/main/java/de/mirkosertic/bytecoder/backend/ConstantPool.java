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
package de.mirkosertic.bytecoder.backend;

import java.util.ArrayList;
import java.util.List;

import de.mirkosertic.bytecoder.ssa.StringValue;

public class ConstantPool {

    private List<StringValue> knownValues;

    public ConstantPool() {
        knownValues = new ArrayList<>();
    }

    public int register(StringValue aValue) {
        int p = knownValues.indexOf(aValue);
        if (p>=0) {
            return p;
        }
        knownValues.add(aValue);
        return knownValues.size() - 1;
    }

    public List<StringValue> stringValues() {
        return knownValues;
    }

    public static String simpleClassName(String aName) {
        final int p = aName.lastIndexOf(".");
        if (p>0) {
            return aName.substring(p + 1);
        }
        return aName;

    }
}