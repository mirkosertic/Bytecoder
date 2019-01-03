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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class TrackTest {

    private class TrackElement {

        public final int curveFactor;
        public final String[] segments;
        public final double height;

        public TrackElement(int aCurveFactor, String[] aSegments, double aHeight) {
            curveFactor = aCurveFactor;
            segments = aSegments;
            height = aHeight;
        }
    }

    private static String[] ODD = new String[10];
    private static String[] EVEN = new String[10];

    private TrackElement compute(int aPosition) {
        int theCurveFactor = 2;
        double theHeight = (Math.cos(Math.toRadians(aPosition * 10)) * 1);
        return new TrackElement(theCurveFactor, aPosition % 2 == 0 ? ODD : EVEN, theHeight);
    }

    @Test
    public void testCalculation() {
        TrackElement result = compute(0);
    }
}
