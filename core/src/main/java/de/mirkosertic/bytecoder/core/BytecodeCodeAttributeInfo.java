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

public class BytecodeCodeAttributeInfo implements BytecodeAttributeInfo {

    private int maxStack;
    private int maxLocals;
    private BytecodeProgramm programm;
    private BytecodeExceptionTableEntry[] exceptionTableEntries;
    private BytecodeAttributeInfo[] attributes;

    public BytecodeCodeAttributeInfo(int aMaxStack, int aMaxLocals, BytecodeProgramm aProgramm, BytecodeExceptionTableEntry[] aExceptionTableEntries, BytecodeAttributeInfo[] aAttributes) {
        maxStack = aMaxStack;
        maxLocals = aMaxLocals;
        programm = aProgramm;
        exceptionTableEntries = aExceptionTableEntries;
        attributes = aAttributes;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public BytecodeProgramm getProgramm() {
        return programm;
    }

    public BytecodeExceptionTableEntry[] getExceptionTableEntries() {
        return exceptionTableEntries;
    }

    public BytecodeAttributeInfo[] getAttributes() {
        return attributes;
    }
}
