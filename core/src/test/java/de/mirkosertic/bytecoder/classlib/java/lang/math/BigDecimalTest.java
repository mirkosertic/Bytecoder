/*
 * Copyright 2021 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang.math;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;

@RunWith(UnitTestRunner.class)
public class BigDecimalTest {

    @Test
    public void testCurrentValue() {
        final BigDecimal value = BigDecimal.ONE;
        Assert.assertEquals(1, value.intValue());
    }

    @Test
    public void testAdd2() {
        final BigDecimal value = BigDecimal.ONE;
        final BigDecimal added = value.add(BigDecimal.valueOf(2));
        Assert.assertEquals(3, added.intValue());
    }
}
