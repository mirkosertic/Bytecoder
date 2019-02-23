/*
 * Copyright 2019 Mirko Sertic
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

import de.mirkosertic.bytecoder.core.BytecodeLineNumberTableAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public abstract class DebugInformation {

    public static DebugInformation empty() {
        return new DebugInformation() {
            @Override
            public DebugPosition debugPositionFor(BytecodeOpcodeAddress aAddress) {
                return null;
            }
        };
    }

    public static DebugInformation jvm(String aOriginalFileName, BytecodeLineNumberTableAttributeInfo aLineNumberInfo) {
        return new DebugInformation() {
            @Override
            public DebugPosition debugPositionFor(BytecodeOpcodeAddress aAddress) {
                BytecodeLineNumberTableAttributeInfo.Entry[] theEntries = aLineNumberInfo.getEntries();
                for (int i=0;i<theEntries.length;i++) {
                    BytecodeLineNumberTableAttributeInfo.Entry theEntry = theEntries[i];
                    if (theEntry.getStartPc() == aAddress.getAddress()) {
                        return new DebugPosition(aOriginalFileName, theEntry.getLineNumber());
                    }
                }
                return null;
            }
        };
    }

    public abstract DebugPosition debugPositionFor(final BytecodeOpcodeAddress aAddress);
}