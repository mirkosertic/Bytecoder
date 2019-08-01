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
public class IfThenElseTest {

    @Test
    public void testIfThenElse() {
        int x = 1;
        int y = 2;
        if (y == 2) {
            for (int k = 0; k< 10; k++) {
                x = x + 1;
            }
        } else {
            x = x + 3;
        }
        y = 6;
    }

    private static boolean floatIsDifferent(final float f1, final float f2, final float delta) {
        if (Float.compare(f1, f2) == 0) {
            return false;
        } else {
            return Math.abs(f1 - f2) > delta;
        }
    }

    private void failNotEquals(final String message, final float expected, final float actual) {
        throw new AssertionError();
    }

    @Test
    public void testIf() {
        final String message = "Message";
        final float expected = 10f;
        final float actual = 10f;
        final float delta = 0;
        if (floatIsDifferent(expected, actual, delta)) {
            failNotEquals(message, expected, actual);
        }
    }
}
