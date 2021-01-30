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
package de.mirkosertic.bytecoder.core;

import java.util.Arrays;
import java.util.Comparator;

public class BytecodeLineNumberTableAttributeInfo implements BytecodeAttributeInfo {

    public static class Entry {
        private final int startPc;
        private final int lineNumber;

        public Entry(final int startPc, final int lineNumber) {
            this.startPc = startPc;
            this.lineNumber = lineNumber;
        }

        public int getStartPc() {
            return startPc;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }

    private final Entry[] entries;

    public BytecodeLineNumberTableAttributeInfo(final Entry[] entries) {
        this.entries = entries;
        Arrays.sort(entries, Comparator.comparingInt(o -> o.lineNumber));
    }

    public Entry[] getEntries() {
        return entries;
    }
}
