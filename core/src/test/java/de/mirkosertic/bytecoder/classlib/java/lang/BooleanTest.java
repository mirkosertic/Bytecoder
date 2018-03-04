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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(BytecoderUnitTestRunner.class)
public class BooleanTest {

    @Test
    public void testBooleanValue() {
        assertTrue(new Boolean(true).booleanValue());
        assertFalse(new Boolean(false).booleanValue());
    }

    @Test
    public void testValueOf() {
        assertSame(Boolean.TRUE, Boolean.valueOf(true));
        assertSame(Boolean.FALSE, Boolean.valueOf(false));

        assertSame(Boolean.FALSE, Boolean.valueOf(null));
        assertSame(Boolean.FALSE, Boolean.valueOf("lala"));
        assertSame(Boolean.TRUE, Boolean.valueOf("TRUE"));
        assertSame(Boolean.TRUE, Boolean.valueOf("true"));
    }

    @Test
    public void testParseBoolean() {
        assertSame(Boolean.TRUE, Boolean.parseBoolean("true"));
        assertSame(Boolean.TRUE, Boolean.parseBoolean("TRUE"));
        assertSame(Boolean.TRUE, Boolean.parseBoolean("TrUE"));
        assertSame(Boolean.FALSE, Boolean.parseBoolean(null));
        assertSame(Boolean.FALSE, Boolean.parseBoolean("false"));
    }
}