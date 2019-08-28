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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.EnumMap;

@RunWith(BytecoderUnitTestRunner.class)
public class EnumMapTest {

    enum TestEnum {
        key1, key2
    }

    @Test
    public void testGetValue() {
        final EnumMap<TestEnum, String> map = new EnumMap<TestEnum, String>(TestEnum.class);
        //map.put(TestEnum.key1, "Hello");
        //Assert.assertEquals(1, map.size());
        //Assert.assertNull(map.get(TestEnum.key2));
        //Assert.assertEquals("Hello", map.get(TestEnum.key1));
    }
}
