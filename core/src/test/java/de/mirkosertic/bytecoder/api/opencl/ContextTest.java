/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.api.opencl;

import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.get_global_id;
import static de.mirkosertic.bytecoder.api.opencl.VectorFunctions.normalize;

public class ContextTest {

    @Test
    public void testSimpleAddRelooper() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger(), new OpenCLOptions(false));

        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(4, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final float a = theA[id];
                    final float b = theB[id];
                    theResult[id] = a + b;
                }
            });
        }

        for (final float aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testSimpleAddStackifier() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger(), new OpenCLOptions(true));

        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(4, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final float a = theA[id];
                    final float b = theB[id];
                    theResult[id] = a + b;
                }
            });
        }

        for (final float aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testSimpleAddWithInlineMethod() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger(), new OpenCLOptions(true));

        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(4, new Kernel() {

                private float add(final float a, final float b) {
                    return a + b;
                }

                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final float a = theA[id];
                    final float b = theB[id];
                    theResult[id] = add(a, b);
                }
            });
        }

        for (final float aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testVectorNormalize() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger(), new OpenCLOptions(true));

        final Float2[] theA = {new Float2(10f, 20f)};
        final Float2[] theResult = new Float2[] {new Float2(-1f, -1f)};

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(1, new Kernel() {
                @Override
                public void processWorkItem() {
                    final int id = get_global_id(0);
                    final Float2 theVec = normalize(theA[id]);
                    theResult[id].s1 = theVec.s1;
                }
            });
        }

        for (final Float2 aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }
}