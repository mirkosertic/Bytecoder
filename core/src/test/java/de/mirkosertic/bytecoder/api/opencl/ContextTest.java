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

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.get_global_id;

import org.junit.Test;

import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

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

    /*
    @Test
    public void testComplexAdd() throws Exception {
        Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger());

        final Float2[] theA = {new Float2(10f, 20f)};
        final Float2[] theB = {new Float2(10f, 20f)};
        final Float2[] theResult = new Float2[] {new Float2(-1f, -1f)};

        try (Context theContext = thePlatform.createContext()) {
            theContext.compute(1, new Kernel() {
                public void processWorkItem() {
                    int id = get_global_id(0);
                    float aS0 = theA[id].s0;
                    float aS1 = theB[id].s1;
                    theResult[id].s0 = aS0 + 100;
                    theResult[id].s1 = aS1 + 200;
                }
            });
        }

        for (Float2 aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testVectorNormalize() throws Exception {
        Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger());

        final Float2[] theA = {new Float2(10f, 20f)};
        final Float2[] theResult = new Float2[] {new Float2(-1f, -1f)};

        try (Context theContext = thePlatform.createContext()) {
            theContext.compute(1, new Kernel() {
                public void processWorkItem() {
                    int id = get_global_id(0);
                    Float2 theVec = VectorFunctions.normalize(theA[id]);
                    theResult[id].s1 = theVec.s1;
                }
            });
        }

        for (Float2 aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }*/
}