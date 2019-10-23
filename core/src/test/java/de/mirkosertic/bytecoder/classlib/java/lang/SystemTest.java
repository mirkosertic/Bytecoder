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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class SystemTest {

    @Test
    public void testGetEnv() {
        Assert.assertNull(System.getenv("UNKNOWN"));
    }

    @Test
    public void testPrintString() {
        System.out.println("Hello world!");
    }

    @Test
    public void testPrintSingleChars() {
        final String text = "Hello world!";
        for (int i=0;i<text.length();i++) {
            System.out.write((int) text.charAt(i));
        }
        System.out.println();
    }
}
