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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class ParameterOrderTest {

    public static class Computer {

        public float compute(final float a, final float b, final float c) {
            return a / b + c;
        }
    }

    @Test
    public void testCompute() {
        Assert.assertEquals(9, new Computer().compute(30, 15, 7), 0);
    }

    @Test
    public void testUnusedReturn() {
        final Computer c = new Computer();
        c.compute(1f, 2f, 3f);
    }
}
