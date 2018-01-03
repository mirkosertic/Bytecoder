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

public class BytecodeOpcodeAddress {

    public static final BytecodeOpcodeAddress START_AT_ZERO = new BytecodeOpcodeAddress(0);

    private final int address;

    public BytecodeOpcodeAddress(int aAddress) {
        address = aAddress;
    }

    public BytecodeOpcodeAddress add(int aOffset) {
        return new BytecodeOpcodeAddress(address + aOffset);
    }

    public boolean isAfter(BytecodeOpcodeAddress aOtherAddress) {
        return address > aOtherAddress.address;
    }

    public boolean isBefore(BytecodeOpcodeAddress aOtherAddress) {
        return address < aOtherAddress.getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BytecodeOpcodeAddress that = (BytecodeOpcodeAddress) o;

        if (address != that.address)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return address;
    }

    public int getAddress() {
        return address;
    }

}
