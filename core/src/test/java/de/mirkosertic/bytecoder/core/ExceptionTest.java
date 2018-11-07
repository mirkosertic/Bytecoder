/*
 * Copyright 2018 Mirko Sertic
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class ExceptionTest {

    private static void throwException(final int aValue) {
        throw new RuntimeException();
    }

    @Test
    public void testControlFlow() {
        int i = 10;
        for (int j = 0; j < 10;j++) {
            try {
                final int k = 12;
                throwException(j);
                for (int z=0; z<100; z++) {
                }
                throwException(j);
                throwException(j);
            } catch (final Exception e) {
                for (int k = 0; k<10;k++) {
                    i++;
                }
                break;
            } finally {
                i = i + 2;
            }
        }
        Assert.assertEquals(22, i, 0);
    }
}