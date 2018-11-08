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
        System.out.println("A");
        for (int j = 0; j < 2;j++) {
            System.out.println("B");
            try {
                System.out.println("C");
                final int k = 12;
                System.out.println("D");
                throwException(j);
                System.out.println("E");
                for (int z=0; z<100; z++) {
                    System.out.println("F");
                }
                System.out.println("G");
                throwException(j);
                System.out.println("H");
                throwException(j);
                System.out.println("I");
            } catch (final Exception e) {
                System.out.println("J");
                for (int k = 0; k<3;k++) {
                    System.out.println("K");
                    i++;
                }
                System.out.println("L");
                System.out.println(i);
            } finally {
                System.out.println("M");
                System.out.println(i);
            }
            System.out.println(i);
            System.out.println("N");
        }
        System.out.println(i);
        Assert.assertEquals(16, i, 0);
    }
}