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

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(BytecoderUnitTestRunner.class)
public class HashSetTest {

    @Test
    public void testAddContainsRemove() {
        HashSet<String> theValue = new HashSet<>();
        assertEquals(0, theValue.size(), 0);
        assertTrue(theValue.isEmpty());
        assertTrue(theValue.add("1"));
        assertTrue(theValue.contains("1"));
        assertFalse(theValue.add("1"));
        assertFalse(theValue.contains("2"));
        assertEquals(1, theValue.size(), 0);
        assertFalse(theValue.isEmpty());
        assertFalse(theValue.remove("2"));
        assertEquals(1, theValue.size(), 0);
        assertTrue(theValue.remove("1"));
        assertEquals(0, theValue.size(), 0);
        assertTrue(theValue.isEmpty());
        assertFalse(theValue.contains("1"));
    }
}