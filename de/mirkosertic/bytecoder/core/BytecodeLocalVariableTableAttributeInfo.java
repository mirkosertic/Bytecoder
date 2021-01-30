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

public class BytecodeLocalVariableTableAttributeInfo implements BytecodeAttributeInfo {

    private final BytecodeConstantPool constantPool;
    private final BytecodeLocalVariableTableEntry[] entries;

    public BytecodeLocalVariableTableAttributeInfo(BytecodeConstantPool aConstantPool, BytecodeLocalVariableTableEntry[] aEntry) {
        entries = aEntry;
        constantPool = aConstantPool;
    }

    public BytecodeLocalVariableTableEntry matchingEntryFor(BytecodeOpcodeAddress aAddress, int aIndex) {
        for (BytecodeLocalVariableTableEntry theEntry : entries) {
            if (theEntry.getIndex() == aIndex) {
                if (aAddress.getAddress() >= theEntry.getStartPC()  && aAddress.getAddress() < theEntry.getStartPC() + theEntry.getLength()) {
                    return theEntry;
                }
            }
        }
        return null;
    }

    public String resolveVariableName(BytecodeLocalVariableTableEntry aEntry) {
        if (aEntry.getStartPC() == 0) {
            return ((BytecodeUtf8Constant) constantPool.constantByIndex(aEntry.getNameIndex() - 1)).stringValue();
        }
        return ((BytecodeUtf8Constant) constantPool.constantByIndex(aEntry.getNameIndex() - 1)).stringValue() + "_" + aEntry.getStartPC();
    }
}
