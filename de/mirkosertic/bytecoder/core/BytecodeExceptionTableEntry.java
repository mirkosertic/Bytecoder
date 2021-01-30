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

public class BytecodeExceptionTableEntry {

    private final BytecodeOpcodeAddress startPC;
    private final BytecodeOpcodeAddress endPc;
    private final BytecodeOpcodeAddress handlerPc;
    private final int catchType;
    private final BytecodeConstantPool constantPool;

    public BytecodeExceptionTableEntry(BytecodeOpcodeAddress aStartPC, BytecodeOpcodeAddress aEndPc, BytecodeOpcodeAddress aHandlerPc, int aCatchType, BytecodeConstantPool aConstantPool) {
        startPC = aStartPC;
        endPc = aEndPc;
        handlerPc = aHandlerPc;
        catchType = aCatchType;
        constantPool = aConstantPool;
    }

    public BytecodeOpcodeAddress getStartPC() {
        return startPC;
    }

    public BytecodeOpcodeAddress getEndPc() {
        return endPc;
    }

    public BytecodeOpcodeAddress getHandlerPc() {
        return handlerPc;
    }

    public boolean isFinally() {
        return catchType == 0;
    }

    public int getCatchTypeAsInt() {
        return catchType;
    }

    public BytecodeClassinfoConstant getCatchType() {
        return (BytecodeClassinfoConstant) constantPool.constantByIndex(catchType - 1);
    }

    public boolean coveres(BytecodeOpcodeAddress aAddress) {
        return startPC.getAddress() <= aAddress.getAddress() && endPc.getAddress() > aAddress.getAddress();
    }
}
