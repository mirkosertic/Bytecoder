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

import org.junit.Test;

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.get_global_id;

public class ContextTest {

    @Test
    public void testSimpleAdd() throws Exception {
        PlatformFactory theFactory = new PlatformFactory();
        Platform thePlatform = theFactory.createPlatform();

        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        try (Context theContext = thePlatform.createContext()) {
            theContext.compute(4, new Kernel() {
                public void processWorkItem() {
                    int id = get_global_id(0);
                    float a = theA[id];
                    float b = theB[id];
                    theResult[id] = a + b;
                }
            });
        }

        for (float aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testComplexAdd() throws Exception {
        PlatformFactory theFactory = new PlatformFactory();
        Platform thePlatform = theFactory.createPlatform();

        final Vec2f[] theA = {new Vec2f(10f, 20f)};
        final Vec2f[] theB = {new Vec2f(10f, 20f)};
        final Vec2f[] theResult = new Vec2f[] {new Vec2f(-1f, -1f)};

        try (Context theContext = thePlatform.createContext()) {
            theContext.compute(1, new Kernel() {
                public void processWorkItem() {
                    int id = get_global_id(0);
                    float ax = theA[id].x;
                    float ay = theB[id].x;
                    theResult[id].x = ax + 100;
                    theResult[id].y = ay + 200;
                }
            });
        }

        for (Vec2f aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }

    @Test
    public void testVectorNormalize() throws Exception {
        PlatformFactory theFactory = new PlatformFactory();
        Platform thePlatform = theFactory.createPlatform();

        final Vec2f[] theA = {new Vec2f(10f, 20f)};
        final Vec2f[] theResult = new Vec2f[] {new Vec2f(-1f, -1f)};

        try (Context theContext = thePlatform.createContext()) {
            theContext.compute(1, new Kernel() {
                public void processWorkItem() {
                    int id = get_global_id(0);
                    Vec2f theVec = VectorFunctions.normalize(theA[id]);
                    theResult[id].x = theVec.x;
                }
            });
        }

        for (Vec2f aTheResult : theResult) {
            System.out.println(aTheResult);
        }
    }
}