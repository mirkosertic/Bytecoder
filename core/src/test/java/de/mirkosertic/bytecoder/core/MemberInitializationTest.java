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
public class MemberInitializationTest {

    public static int staticMember = 18;

    public static class StaticClassWithStuffInside {

        public int member = 12;
    }

    @Test
    public void testInstanceGetAndSet() {
        StaticClassWithStuffInside theInstance = new StaticClassWithStuffInside();
        int theResult = theInstance.member;
        Assert.assertEquals(12, theResult, 0);
        Assert.assertEquals(18, MemberInitializationTest.staticMember, 0);
    }
}
