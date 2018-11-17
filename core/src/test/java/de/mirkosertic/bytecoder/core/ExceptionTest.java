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
        try {
            i++;
            throwException(i);
            i++;
        } catch (Exception e) {
            i++;
        } finally {
            i+=3;
        }
        Assert.assertEquals(15, i, 0);
    }

    @Test
    public void testForLoop() {
        int i = 10;
        for (int j = 0; j<20;j++) {
            i = i + 3;
        }
        Assert.assertEquals(70, i, 0);
    }
}

/*

  private static void throwException(int);
    Code:
       0: new           #2                  // class java/lang/RuntimeException
       3: dup
       4: invokespecial #3                  // Method java/lang/RuntimeException."<init>":()V
       7: athrow

  public void testControlFlow();
    Code:
       0: bipush        10
       2: istore_1
       3: iinc          1, 1
       6: iload_1
       7: invokestatic  #4                  // Method throwException:(I)V
      10: iinc          1, 1
      13: iinc          1, 3
      16: goto          35
      19: astore_2
      20: iinc          1, 1
      23: iinc          1, 3
      26: goto          35
      29: astore_3
      30: iinc          1, 3
      33: aload_3
      34: athrow
      35: ldc           #6                  // float 15.0f
      37: iload_1
      38: i2f
      39: fconst_0
      40: invokestatic  #7                  // Method org/junit/Assert.assertEquals:(FFF)V
      43: return
    Exception table:
       from    to  target type
           3    13    19   Class java/lang/Exception
           3    13    29   any
          19    23    29   any


  public void testForLoop();
    Code:
       0: bipush        10
       2: istore_1
       3: iconst_0
       4: istore_2
       5: iload_2
       6: bipush        20
       8: if_icmpge     21
      11: iload_1
      12: iconst_3
      13: iadd
      14: istore_1
      15: iinc          2, 1
      18: goto          5
      21: ldc           #8                  // float 70.0f
      23: iload_1
      24: i2f
      25: fconst_0
      26: invokestatic  #7                  // Method org/junit/Assert.assertEquals:(FFF)V
      29: return

 */