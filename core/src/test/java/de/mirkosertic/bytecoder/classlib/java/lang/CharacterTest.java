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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(BytecoderUnitTestRunner.class)
public class CharacterTest {

    @Test
    public void testDigit() {
        assertEquals(0, Character.digit('0', 10));
        assertEquals(10, Character.digit('a', 16));
        assertEquals(10, Character.digit('A', 16));
    }

    @Test
    public void testIsDigit() {
        assertTrue(Character.isDigit('0'));
        assertTrue(Character.isDigit('1'));
        assertTrue(Character.isDigit('2'));
        assertTrue(Character.isDigit('3'));
        assertTrue(Character.isDigit('4'));
        assertTrue(Character.isDigit('5'));
        assertTrue(Character.isDigit('6'));
        assertTrue(Character.isDigit('7'));
        assertTrue(Character.isDigit('8'));
        assertTrue(Character.isDigit('9'));
        assertFalse(Character.isDigit('A'));
    }

    @Test
    public void testIsLowerCase() {
        assertTrue(Character.isLowerCase('a'));
        assertFalse(Character.isLowerCase('A'));
    }

    @Test
    public void testIsUpperCase() {
        assertFalse(Character.isUpperCase('a'));
        assertTrue(Character.isUpperCase('A'));
    }

    @Test
    public void testToLowerCase() {
        assertTrue(Character.toLowerCase('A') == 'a');
        assertTrue(Character.toLowerCase('a') == 'a');
    }

}