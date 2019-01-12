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

public class LocalVariableDescription implements VariableDescription {

    private final int index;
    private final TypeRef typeRef;

    public LocalVariableDescription(final int index, final TypeRef typeRef) {
        this.index = index;
        this.typeRef = typeRef;
    }

    public int getIndex() {
        return index;
    }

    public TypeRef getTypeRef() {
        return typeRef;
    }

    @Override
    public String toString() {
        return "virtual variable #" + index + " of type " + typeRef;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final LocalVariableDescription that = (LocalVariableDescription) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return index;
    }
}
