/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.complex;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
public class PromiseTest {

    @Test
    public void testResolve() {
        final Object[] theResult = new Object[1];
        final Promise<String, String> theOriginal = new Promise<>(new Promise.Executor() {
            @Override
            public void process(final PromiseResolver aResolver, final PromiseRejector aRejector) {
                aResolver.resolve("Hello");
            }
        });
        theOriginal.thenContinue(new Promise.NoReturnHandler<String>() {
            @Override
            public void process(final String aResult) {
                theResult[0] = aResult;
            }
        });
        Assert.assertEquals("Hello", theResult[0]);
    }
}
