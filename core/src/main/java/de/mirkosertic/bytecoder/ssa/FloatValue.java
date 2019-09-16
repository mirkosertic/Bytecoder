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

public class FloatValue extends PrimitiveValue implements Constant {

    private final float floatValue;

    public FloatValue(final float aFloatValue) {
        floatValue = aFloatValue;
    }

    public float getFloatValue() {
        return floatValue;
    }

    @Override
    public TypeRef resolveType() {
        return TypeRef.Native.FLOAT;
    }
}
