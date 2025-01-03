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
public class InstanceOfTest {

    public static class Type1 {
    }

    public interface Interf {
    }

    public static class Type2 extends Type1 implements Interf {
    }


    @Test
    public void testInstanceOf() {
        Assert.assertFalse(null instanceof Object);
        Assert.assertTrue(new Type1() instanceof Object);
        Assert.assertTrue(new Type1() instanceof Type1);
        Assert.assertFalse(new Type1() instanceof Type2);
        Assert.assertFalse(new Type1() instanceof Interf);

        Assert.assertTrue(new Type2() instanceof Object);
        Assert.assertTrue(new Type2() instanceof Type1);
        Assert.assertTrue(new Type2() instanceof Type2);
        Assert.assertTrue(new Type2() instanceof Interf);
    }
}
