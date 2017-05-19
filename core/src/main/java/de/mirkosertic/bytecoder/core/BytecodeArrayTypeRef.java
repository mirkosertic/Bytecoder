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

public class BytecodeArrayTypeRef implements BytecodeTypeRef {

    private final BytecodeTypeRef type;
    private final int depth;

    public BytecodeArrayTypeRef(BytecodeTypeRef aType, int aDepth) {
        type = aType;
        depth = aDepth;
    }

    @Override
    public String name() {
        return "L" + depth + type.name();
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    public BytecodeTypeRef getType() {
        return type;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean matchesExactlyTo(BytecodeTypeRef aOtherType) {
        if (!(aOtherType instanceof BytecodeArrayTypeRef)) {
            return false;
        }
        BytecodeArrayTypeRef theOther = (BytecodeArrayTypeRef) aOtherType;
        if (!type.matchesExactlyTo(((BytecodeArrayTypeRef) aOtherType).type)) {
            return false;
        }
        if (depth != theOther.getDepth()) {
            return false;
        }
        return true;
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