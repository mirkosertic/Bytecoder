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

import java.util.*;

public class BytecodeProgramJumps {

    public static class Range {

        public enum OverlapCheckResult {
            NO_OVERLAP,
            OTHER_CONTAINS_START,
            OTHER_CONTAINS_END;
        }

        private final boolean startFix;
        private BytecodeOpcodeAddress start;
        private final boolean endFix;
        private BytecodeOpcodeAddress end;

        public Range(boolean aStartFix, BytecodeOpcodeAddress aStart, boolean aEndFix, BytecodeOpcodeAddress aEnd) {
            startFix = aStartFix;
            start = aStart;
            endFix = aEndFix;
            end = aEnd;
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

        private boolean contains(BytecodeOpcodeAddress aAddress) {
            return aAddress.getAddress() >= start.getAddress() && aAddress.getAddress() <= end.getAddress();
        }

        public OverlapCheckResult checkOverlapWith(Range aOtherRange) {
            if (aOtherRange.start.getAddress() < start.getAddress() && aOtherRange.end.getAddress() > end.getAddress()) {
                return OverlapCheckResult.NO_OVERLAP;
            }
            if (start.getAddress() < aOtherRange.getStart().getAddress() && end.getAddress() > aOtherRange.getEnd().getAddress()) {
                return OverlapCheckResult.NO_OVERLAP;
            }
            if (!contains(aOtherRange.start) && !contains(aOtherRange.end)) {
                return OverlapCheckResult.NO_OVERLAP;
            }
            if (aOtherRange.contains(start)) {
                return OverlapCheckResult.OTHER_CONTAINS_START;
            }
            if (aOtherRange.contains(end)) {
                return OverlapCheckResult.OTHER_CONTAINS_END;
            }
            throw new IllegalStateException("Don't know what to do! " + start.getAddress() + " " + end.getAddress() + "-> " + aOtherRange.start.getAddress() +  " " + aOtherRange.end.getAddress());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Range range = (Range) o;

            if (!start.equals(range.start)) return false;
            return end.equals(range.end);
        }

        @Override
        public int hashCode() {
            int result = start.hashCode();
            result = 31 * result + end.hashCode();
            return result;
        }
    }

    private List<Range> ranges;

    public BytecodeProgramJumps() {
        ranges = new ArrayList<>();
    }

    public void registerJumpFromAToB(BytecodeOpcodeAddress a, BytecodeOpcodeAddress b) {
        if (a.getAddress() < b.getAddress()) {
            ranges.add(new Range(false, a, true, b));
        } else {
            ranges.add(new Range(true, b, false, a));
        }
    }

    public List<Range> startRangesAt(BytecodeOpcodeAddress aAddress) {
        List<Range> theResult = new ArrayList<>();
        for (Range theRange : ranges) {
            if (theRange.start.equals(aAddress)) {
                theResult.add(theRange);
            }
        }
        Collections.sort(theResult, (o1, o2) -> Integer.compare(o2.getEnd().getAddress(), o1.getEnd().getAddress()));
        return theResult;
    }

    public List<Range> endRangesAt(BytecodeOpcodeAddress aAddress) {
        List<Range> theResult = new ArrayList<>();
        for (Range theRange : ranges) {
            if (theRange.end.equals(aAddress)) {
                theResult.add(theRange);
            }
        }
        Collections.sort(theResult, (o1, o2) -> Integer.compare(o2.start.getAddress(), o1.start.getAddress()));
        return theResult;
    }

    public Range findClosestRangeToJumpFrom(BytecodeOpcodeAddress aSource, BytecodeOpcodeAddress aTarget) {
        if (aSource.getAddress() < aTarget.getAddress()) {
            // Jump Forward
            List<Range> theRanges = endRangesAt(aTarget);
            if (theRanges.size() == 1) {
                return theRanges.get(0);
            }
            for (int i=theRanges.size()-1;i>=0;i--) {
                Range theRange = theRanges.get(i);
                if (theRange.start.getAddress() >=aSource.getAddress()) {
                    return theRange;
                }
            }
            throw new IllegalStateException();
        } else {
            // Jump Backward
            List<Range> theRanges = startRangesAt(aTarget);
            if (theRanges.size() > 0) {
                return theRanges.get(0);
            }
            throw new IllegalStateException();
        }
    }

    public void tryToOptimize() {
        // Try every endFix
        for (Range theRange : ranges) {
            if (theRange.endFix) {
                for (Range theOtherRange : ranges) {
                    if (theRange != theOtherRange) {
                        switch (theRange.checkOverlapWith(theOtherRange)) {
                            case NO_OVERLAP:
                                break;
                            case OTHER_CONTAINS_START:
                                theRange.start = theOtherRange.start;
                                break;
                            case OTHER_CONTAINS_END:
                                theOtherRange.start = theRange.start;
                                break;
                        }
                    }
                }
            }
        }
        Set<Range> theUniqueRanges = new HashSet<>();
        theUniqueRanges.addAll(ranges);
        ranges.clear();
        ranges.addAll(theUniqueRanges);
    }
}
