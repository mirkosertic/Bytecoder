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

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class Expression extends Value {

    private String comment;
    private final BytecodeOpcodeAddress address;
    private final Program program;

    protected Expression(final Program program, final BytecodeOpcodeAddress address) {
        this.address = address;
        this.program = program;
    }

    public <T extends Expression> T withComment(final String aComment) {
        comment = aComment;
        return (T) this;
    }

    public Program getProgram() {
        return program;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public TypeRef resolveType() {
        return TypeRef.Native.VOID;
    }

    public BytecodeOpcodeAddress getAddress() {
        return address;
    }
}
