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

import static de.mirkosertic.bytecoder.api.opencl.VectorFunctions.dot;
import static de.mirkosertic.bytecoder.api.opencl.VectorFunctions.length;

import org.junit.Test;

import de.mirkosertic.bytecoder.backend.opencl.CPUPlatform;

public class AliceCarolBobDaveTest {

    @Test
    public void testSimilarity() throws Exception {
        Vec4f theAlice = new Vec4f(5f, 1f, 0f, 6f);
        Vec4f theCarol = new Vec4f(0f, 10f, 3f, 0f);
        Vec4f theBob = new Vec4f(7f, 2f, 1f, 8f);
        Vec4f theDave = new Vec4f(2f, 6f, 3f, 2f);

        Vec4f[] theInputs = new Vec4f[] {theAlice, theCarol, theBob, theDave};

        int[] theMostSimilar = new int[theInputs.length];
        float[] theMostSimilarity = new float[theInputs.length];

        Platform thePlatform = new CPUPlatform();
        try (Context theContext = thePlatform.createContext()) {
            theContext.compute(theInputs.length, new Kernel() {
                @Override
                public void processWorkItem() {
                    int theCurrentWorkItemId = get_global_id(0);
                    Vec4f theCurrent = theInputs[theCurrentWorkItemId];
                    float theCurrentLength = length(theCurrent);
                    int theMax = 4;

                    float theMaxSimilarity = -1;
                    int theMaxIndex = -1;

                    for (int i = 0;i<theMax;i++) {
                        if (i != theCurrentWorkItemId) {
                            Vec4f theOther = theInputs[i];
                            float theOtherLength = length(theOther);

                            if (theCurrentLength * theOtherLength != 0) {
                                float theSimilarity = dot(theCurrent, theOther) / (theCurrentLength * theOtherLength);

                                if (theSimilarity > theMaxSimilarity) {
                                    theMaxSimilarity = theSimilarity;
                                    theMaxIndex = i;
                                }
                            }
                        }
                    }

                    theMostSimilar[theCurrentWorkItemId] = theMaxIndex;
                    theMostSimilarity[theCurrentWorkItemId] = theMaxSimilarity;
                }
            });
        }

        for (int i=0;i<theInputs.length;i++) {
            System.out.println("Most similar match for input " + i + " is " + theMostSimilar[i] + " with a similarity of " + theMostSimilarity[i]);
        }
    }
}