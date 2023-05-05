/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.optim;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class OptimizerTest {

    public static class X {
        int y;
    }

    @Test
    public void testHelloWorld() {
        OptimizerTest.main(new String[0]);
    }

    public static void main(final String[] args) {
        final X x = new X();
        x.y = args.length;
        //System.out.println("Hello world!");
    }
}
