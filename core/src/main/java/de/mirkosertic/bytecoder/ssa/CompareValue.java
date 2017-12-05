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

public class CompareValue extends Value {

    public CompareValue(Variable aValue1, Variable aValue2) {
        consume(ConsumptionType.ARGUMENT, aValue1);
        consume(ConsumptionType.ARGUMENT, aValue2);
    }

    @Override
    public TypeRef resolveType() {
        Value theValue1 = resolveFirstArgument();
        Value theValue2 = resolveSecondArgument();

        if (theValue1.resolveType().resolve() == TypeRef.Native.DOUBLE && theValue2.resolveType().resolve() == TypeRef.Native.DOUBLE) {
            return TypeRef.Native.DOUBLE;
        }
        if (theValue1.resolveType().resolve() == TypeRef.Native.FLOAT && theValue2.resolveType().resolve() == TypeRef.Native.FLOAT) {
            return TypeRef.Native.FLOAT;
        }
        return TypeRef.Native.INT;
    }
}
