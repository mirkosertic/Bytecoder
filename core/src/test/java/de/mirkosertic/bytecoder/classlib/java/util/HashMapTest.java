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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(BytecoderUnitTestRunner.class)
public class HashMapTest {

    @Test
    public void containsGetPutUpdate() throws Exception {
        final HashMap<Integer, Integer> theMap = new HashMap<>();
        assertFalse(theMap.containsKey(new Integer(10)));
        assertNull(theMap.get(new Integer(1024)));
        final Integer theOldValue = theMap.put(new Integer(255), new Integer(3000));
        assertNull(theOldValue);
        assertTrue(theMap.containsKey(new Integer(255)));
        assertEquals(new Integer(3000), theMap.get(new Integer(255)));

        final Integer theOldValue2 = theMap.put(new Integer(255), new Integer(4000));
        assertEquals(new Integer(3000), theOldValue2);
        assertEquals(new Integer(4000), theMap.get(new Integer(255)));
    }

    @Test
    public void testKeySetIterator() {
        final Map<Integer, String> map = new HashMap<>();
        map.put(1, "1");
        map.put(2, "2");
        map.put(3, "3");
        for (final Map.Entry<Integer, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}