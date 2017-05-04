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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class SimpleMathTest {

    public static int sum(int a, int b) {
        return a + b;
    }

    @Test
    public void testAdd() throws Exception {
        int c = sum(10, 20);
        if (c == 30) {
            throw new Exception();
        }
    }
}

/*
public class de.mirkosertic.bytecoder.core.SimpleMathTest {
  public de.mirkosertic.bytecoder.core.SimpleMathTest();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static int sum(int, int);
    Code:
       0: iload_0
       1: iload_1
       2: iadd
       3: ireturn

  public void testAdd() throws java.lang.Exception;
    Code:
       0: bipush        10
       2: bipush        20
       4: invokestatic  #2                  // Method sum:(II)I
       7: istore_1
       8: iload_1
       9: bipush        30
      11: if_icmpne     22
      14: new           #3                  // class java/lang/Exception
      17: dup
      18: invokespecial #4                  // Method java/lang/Exception."<init>":()V
      21: athrow
      22: return
}
 */