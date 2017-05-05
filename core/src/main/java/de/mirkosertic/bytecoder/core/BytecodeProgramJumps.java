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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BytecodeProgramJumps {

    public static class Range {
        private final BytecodeOpcodeAddress start;
        private final BytecodeOpcodeAddress end;

        public Range(BytecodeOpcodeAddress start, BytecodeOpcodeAddress end) {
            this.start = start;
            this.end = end;
        }

        public BytecodeOpcodeAddress getStart() {
            return start;
        }

        public BytecodeOpcodeAddress getEnd() {
            return end;
        }

        public String rangeName() {
            return "range_" + start.getAddress()+ "_" + end.getAddress();
        }
    }

    private List<Range> ranges;

    public BytecodeProgramJumps() {
        ranges = new ArrayList<>();
    }

    public void registerRange(BytecodeOpcodeAddress a, BytecodeOpcodeAddress b) {
        Range theRange;
        if (a.getAddress()>b.getAddress()) {
            theRange = new Range(b, a);
        } else {
            theRange = new Range(a, b);
        }
        ranges.add(theRange);
    }

    public List<Range> startRangesAt(BytecodeOpcodeAddress aAddress) {
        List<Range> theResult = new ArrayList<>();
        for (Range theRange : ranges) {
            if (theRange.start.equals(aAddress)) {
                theResult.add(theRange);
            }
        }
        Collections.sort(theResult, Comparator.comparingInt(o -> o.getEnd().getAddress()));
        return theResult;
    }

    public List<Range> getEndRangesAt(BytecodeOpcodeAddress aAddress) {
        List<Range> theResult = new ArrayList<>();
        for (Range theRange : ranges) {
            if (theRange.end.equals(aAddress)) {
                theResult.add(theRange);
            }
        }
        Collections.sort(theResult, (o1, o2) -> Integer.compare(o2.start.getAddress(), o1.start.getAddress()));
        return theResult;
    }
}
