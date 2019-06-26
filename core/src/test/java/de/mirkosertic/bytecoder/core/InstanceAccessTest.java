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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class InstanceAccessTest {

    public static class StaticClassWithStuffInside {

        public int member;
    }

    @Test
    public void testInstanceGetAndSet() {
        final StaticClassWithStuffInside theInstance = new StaticClassWithStuffInside();
        theInstance.member = 12;
        final StaticClassWithStuffInside theInstance2 = new StaticClassWithStuffInside();
        theInstance2.member = 14;

        final int theResult = theInstance.member;
        Assert.assertEquals(12, theResult, 0);

        final int theResult2 = theInstance2.member;
        Assert.assertEquals(14, theResult2, 0);
    }
}
