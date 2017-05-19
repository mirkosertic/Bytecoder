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

public enum BytecodePrimitiveTypeRef implements BytecodeTypeRef {

    BOOLEAN {
        @Override
        public Object defaultValue() {
            return false;
        }
    },
    BYTE {
        @Override
        public Object defaultValue() {
            return (byte) 0;
        }
    },
    CHAR {
        @Override
        public Object defaultValue() {
            return (byte) 0;
        }
    },
    DOUBLE {
        @Override
        public Object defaultValue() {
            return 0.0d;
        }
    },
    FLOAT {
        @Override
        public Object defaultValue() {
            return 0.0f;
        }
    },
    LONG {
        @Override
        public Object defaultValue() {
            return 0L;
        }
    },
    SHORT {
        @Override
        public Object defaultValue() {
            return (short) 0;
        }
    },
    INT {
        @Override
        public Object defaultValue() {
            return 0;
        }
    },
    VOID {
        @Override
        public Object defaultValue() {
            throw new IllegalStateException("Should never be invoked!");
        }
    };

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean matchesExactlyTo(BytecodeTypeRef aOtherType) {
        return equals(aOtherType);
    }

    @Override
    public boolean isVoid() {
        return this == VOID;
    }
}
