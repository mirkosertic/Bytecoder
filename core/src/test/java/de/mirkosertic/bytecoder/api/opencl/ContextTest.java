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

public class ContextTest {

    @Test
    public void testSimpleAdd() {
        PlatformFactory theFactory = new PlatformFactory();
        Platform thePlatform = theFactory.createPlatform();

        final float[] theA = {10f, 20f, 30f, 40f};
        final float[] theB = {100f, 200f, 300f, 400f};
        final float[] theResult = new float[4];

        try (Context theContext = thePlatform.createContext()) {
            theContext.compute(4, new Kernel() {
                public void add() {
                    int id = get_global_id(0);
                    float a = theA[id];
                    float b = theB[id];
                    theResult[id] = a + b;
                }
            });
        }

        for (int i=0; i<theResult.length;i++) {
            System.out.println(theResult[i]);
        }
    }
}