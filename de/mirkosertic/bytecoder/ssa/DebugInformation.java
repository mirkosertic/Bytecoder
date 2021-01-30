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
            public DebugPosition debugPositionFor(final BytecodeOpcodeAddress aAddress) {
                return null;
            }

            @Override
            public String originalFileName() {
                return null;
            }
        };
    }

    public static DebugInformation jvm(final String aOriginalFileName, final BytecodeLineNumberTableAttributeInfo aLineNumberInfo) {
        return new DebugInformation() {
            @Override
            public DebugPosition debugPositionFor(final BytecodeOpcodeAddress aAddress) {
                final BytecodeLineNumberTableAttributeInfo.Entry[] theEntries = aLineNumberInfo.getEntries();
                for (int i=theEntries.length-1; i>=0; i--) {
                    final BytecodeLineNumberTableAttributeInfo.Entry theEntry = theEntries[i];
                    if (theEntry.getStartPc() <= aAddress.getAddress()) {
                        // DebugPosition Line-Number indices are zero-based
                        return new DebugPosition(aOriginalFileName, theEntry.getLineNumber() - 1);
                    }
                }
                return null;
            }

            @Override
            public String originalFileName() {
                return aOriginalFileName;
            }
        };
    }

    public abstract DebugPosition debugPositionFor(final BytecodeOpcodeAddress aAddress);

    public abstract String originalFileName();
}