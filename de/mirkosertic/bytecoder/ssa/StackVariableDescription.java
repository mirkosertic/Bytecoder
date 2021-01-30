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

public class StackVariableDescription implements VariableDescription {

    private final int pos;

    public StackVariableDescription(int aPos) {
        pos = aPos;
    }

    public int getPos() {
        return pos;
    }

    @Override
    public String toString() {
        return "Stack position " + pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        StackVariableDescription that = (StackVariableDescription) o;

        if (pos != that.pos)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pos;
    }
}
