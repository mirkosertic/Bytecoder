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

import de.mirkosertic.bytecoder.classlib.java.lang.TRuntimeException;
import de.mirkosertic.bytecoder.classlib.org.junit.TAssert;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class PrimitivesTest {

    public static class Container {

        public final Integer intValue = new Integer(10);
        public final Byte byteValue = new Byte((byte) 11);
        public final Short shortValue = new Short((short) 12);
        public final Boolean booleanTrueValue = new Boolean(true);
        public final Boolean booleanFalseValue = new Boolean(false);
        public final Float floatValue = new Float(13f);
    }

    @Test
    public void testPrimivites() throws TRuntimeException {
        Container theContainer = new Container();
        TAssert.assertEquals(10, theContainer.intValue.intValue(), 0);
        TAssert.assertEquals(10, theContainer.intValue.byteValue(), 0);
        TAssert.assertEquals(10, theContainer.intValue.floatValue(), 0);
        TAssert.assertEquals(10, theContainer.intValue.shortValue(), 0);

        TAssert.assertEquals(11, theContainer.byteValue.byteValue(), 0);
        TAssert.assertEquals(11, theContainer.byteValue.intValue(), 0);
        TAssert.assertEquals(11, theContainer.byteValue.shortValue(), 0);
        TAssert.assertEquals(11, theContainer.byteValue.floatValue(), 0);

        TAssert.assertEquals(12, theContainer.shortValue.shortValue(), 0);
        TAssert.assertEquals(12, theContainer.shortValue.byteValue(), 0);
        TAssert.assertEquals(12, theContainer.shortValue.intValue(), 0);
        TAssert.assertEquals(12, theContainer.shortValue.floatValue(), 0);

        TAssert.assertEquals(13, theContainer.floatValue.shortValue(), 0);
        TAssert.assertEquals(13, theContainer.floatValue.byteValue(), 0);
        TAssert.assertEquals(13, theContainer.floatValue.intValue(), 0);
        TAssert.assertEquals(13, theContainer.floatValue.floatValue(), 0);

        TAssert.assertTrue(theContainer.booleanTrueValue.booleanValue());
        TAssert.assertFalse(theContainer.booleanFalseValue.booleanValue());
    }
}