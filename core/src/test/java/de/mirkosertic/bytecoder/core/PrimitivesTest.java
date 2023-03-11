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

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class PrimitivesTest {

    public static class Container {

        public final Integer intValue = new Integer(10);
        public final Byte byteValue = new Byte((byte) 11);
        public final Short shortValue = new Short((short) 12);
        public final Boolean booleanTrueValue = new Boolean(true);
        public final Boolean booleanFalseValue = new Boolean(false);
        public final Float floatValue = new Float(13f);
        public final Double doubleValue = new Double(13d);
        private final Long longValue = new Long(57236352L);
    }

    @Test
    public void testPrimivites() {
        Container theContainer = new Container();
        Assert.assertEquals(10, theContainer.intValue.intValue(), 0);
        Assert.assertEquals(10, theContainer.intValue.byteValue(), 0);
        Assert.assertEquals(10, theContainer.intValue.floatValue(), 0);
        Assert.assertEquals(10, theContainer.intValue.shortValue(), 0);

        Assert.assertEquals(11, theContainer.byteValue.byteValue(), 0);
        Assert.assertEquals(11, theContainer.byteValue.intValue(), 0);
        Assert.assertEquals(11, theContainer.byteValue.shortValue(), 0);
        Assert.assertEquals(11, theContainer.byteValue.floatValue(), 0);

        Assert.assertEquals(12, theContainer.shortValue.shortValue(), 0);
        Assert.assertEquals(12, theContainer.shortValue.byteValue(), 0);
        Assert.assertEquals(12, theContainer.shortValue.intValue(), 0);
        Assert.assertEquals(12, theContainer.shortValue.floatValue(), 0);

        Assert.assertEquals(13, theContainer.floatValue.shortValue(), 0);
        Assert.assertEquals(13, theContainer.floatValue.byteValue(), 0);
        Assert.assertEquals(13, theContainer.floatValue.intValue(), 0);
        Assert.assertEquals(13, theContainer.floatValue.floatValue(), 0);

        Assert.assertTrue(theContainer.booleanTrueValue.booleanValue());
        Assert.assertFalse(theContainer.booleanFalseValue.booleanValue());

    }

    @Test
    public void testLongValue() {
        Long theLong = new Long(13);
        Container theContainer = new Container();
        Assert.assertEquals(57236352L, theContainer.longValue.longValue(), 0);
    }

    @Test
    public void testDoubleValue() {
        Double theDouble = new Double(13);
        Container theContainer = new Container();
        Assert.assertEquals(13, (float) theContainer.doubleValue.doubleValue(), 0);
    }
}
