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
package de.mirkosertic.bytecoder.core;

public class BytecodeObjectTypeRef implements BytecodeTypeRef {

    private final String className;

    public static BytecodeObjectTypeRef fromRuntimeClass(Class aClass) {
        return new BytecodeObjectTypeRef(aClass.getName());
    }

    public static BytecodeObjectTypeRef fromUtf8Constant(BytecodeUtf8Constant aConstant) {
        return new BytecodeObjectTypeRef(aConstant.stringValue().replace("/","."));
    }

    public BytecodeObjectTypeRef(String aClassName) {
        className = aClassName;
    }

    @Override
    public String name() {
        return className;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BytecodeObjectTypeRef that = (BytecodeObjectTypeRef) o;

        if (!className.equals(that.className))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean matchesExactlyTo(BytecodeTypeRef aOtherType) {
        if (!(aOtherType instanceof BytecodeObjectTypeRef)) {
            return false;
        }
        return className.equals(((BytecodeObjectTypeRef) aOtherType).className);
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public Object defaultValue() {
        return null;
    }
}