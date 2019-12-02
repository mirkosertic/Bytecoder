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
package de.mirkosertic.bytecoder.classlib.java.lang.invoke;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class StringConcatFactoryTest {

    private String append(final int aValue) {
        return "Hello" + aValue;
    }

    private String append(final String prefix, final int aValue) {
        return prefix + aValue;
    }

    @Test
    public void testWithConstant() {
        Assert.assertEquals("Hello42", append(42));
    }

    @Test
    public void testAppend() {
        Assert.assertEquals("Hello42", append("Hello", 42));
    }
}
