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

import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(includeJVM = false)
public class RuntimeClassTest {

    @Test
    public void testRuntimeClass() {
        final String str1 = new String("1");
        final String str2 = new String("1");
        Assert.assertSame(str1.getClass(), str2.getClass());
        Assert.assertNotSame(str1.getClass(), RuntimeClassTest.class);
    }

    @Test
    public void testGetName() {
        System.out.println(RuntimeClassTest.class.getName());
        Assert.assertEquals("RuntimeClassTest", RuntimeClassTest.class.getName());
    }
}
