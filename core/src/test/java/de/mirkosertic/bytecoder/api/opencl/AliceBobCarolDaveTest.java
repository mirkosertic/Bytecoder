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
import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.get_global_size;
import static de.mirkosertic.bytecoder.api.opencl.VectorFunctions.dot;
import static de.mirkosertic.bytecoder.api.opencl.VectorFunctions.length;

import org.junit.Test;

import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

public class AliceBobCarolDaveTest {

    @Test
    public void testSimilarity() throws Exception {
        // The data of our four friends
        Float4 theAlice = new Float4(5f, 1f, 0f, 6f);
        Float4 theBob = new Float4(0f, 10f, 3f, 0f);
        Float4 theCarol = new Float4(2f, 6f, 3f, 2f);
        Float4 theDave = new Float4(7f, 2f, 1f, 8f);

        // We need an input for our kernel, a list of vectors
        Float4[] theInputs = new Float4[] {theAlice, theCarol, theBob, theDave};

        // This is the computed output
        int[] theMostSimilar = new int[theInputs.length];
        float[] theMostSimilarity = new float[theInputs.length];

        // We obtain a platform
        Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger());
        // All computation is done within a context. A context is
        // used to cache memory buffers and compiled kernels
        try (Context theContext = thePlatform.createContext()) {

            // We fire up the computations
            theContext.compute(theInputs.length, new Kernel() {

                // This method is called for every workitem
                @Override
                public void processWorkItem() {
                    // This is the id of the current work item
                    int theCurrentWorkItemId = get_global_id(0);
                    // This is the total number of work items
                    int theMax = get_global_size(0);

                    // We obtain the current work item from the list
                    Float4 theCurrent = theInputs[theCurrentWorkItemId];
                    float theCurrentLength = length(theCurrent);

                    float theMaxSimilarity = -1;
                    int theMaxIndex = -1;

                    // And compute the similarities with all other work item
                    // except itself
                    for (int i = 0;i<theMax;i++) {
                        if (i != theCurrentWorkItemId) {
                            Float4 theOther = theInputs[i];
                            float theOtherLength = length(theOther);

                            float theLength = theCurrentLength * theOtherLength;

                            if (theLength != 0) {
                                float theSimilarity = dot(theCurrent, theOther) / (theLength);

                                if (theSimilarity > theMaxSimilarity) {
                                    theMaxSimilarity = theSimilarity;
                                    theMaxIndex = i;
                                }
                            }
                        }
                    }

                    // The highest similarity is written to the output
                    theMostSimilar[theCurrentWorkItemId] = theMaxIndex;
                    theMostSimilarity[theCurrentWorkItemId] = theMaxSimilarity;
                }
            });
        }

        // Output the results
        for (int i=0;i<theInputs.length;i++) {
            System.out.println("Most similar match for input " + i + " is " + theMostSimilar[i] + " with a similarity of " + theMostSimilarity[i]);
        }
    }

    @Test
    public void testSimilarityWithMethodInKernel() throws Exception {
        // The data of our four friends
        Float4 theAlice = new Float4(5f, 1f, 0f, 6f);
        Float4 theBob = new Float4(0f, 10f, 3f, 0f);
        Float4 theCarol = new Float4(2f, 6f, 3f, 2f);
        Float4 theDave = new Float4(7f, 2f, 1f, 8f);

        // We need an input for our kernel, a list of vectors
        Float4[] theInputs = new Float4[] {theAlice, theCarol, theBob, theDave};

        // This is the computed output
        int[] theMostSimilar = new int[theInputs.length];
        float[] theMostSimilarity = new float[theInputs.length];

        // We obtain a platform
        Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger());
        // All computation is done within a context. A context is
        // used to cache memory buffers and compiled kernels
        try (Context theContext = thePlatform.createContext()) {

            // We fire up the computations
            theContext.compute(theInputs.length, new Kernel() {

                private float similarityOf(Float4 a, Float4 b) {
                    return dot(a, b) / length(a) * length(b);
                }

                // This method is called for every workitem
                @Override
                public void processWorkItem() {
                    // This is the id of the current work item
                    int theCurrentWorkItemId = get_global_id(0);
                    // This is the total number of work items
                    int theMax = get_global_size(0);

                    // We obtain the current work item from the list
                    Float4 theCurrent = theInputs[theCurrentWorkItemId];

                    float theMaxSimilarity = -1;
                    int theMaxIndex = -1;

                    // And compute the similarities with all other work item
                    // except itself
                    for (int i = 0;i<theMax;i++) {
                        if (i != theCurrentWorkItemId) {
                            Float4 theOther = theInputs[i];

                            float theSimilarity = similarityOf(theCurrent, theOther);

                            if (theSimilarity > theMaxSimilarity) {
                                theMaxSimilarity = theSimilarity;
                                theMaxIndex = i;
                            }
                        }
                    }

                    // The highest similarity is written to the output
                    theMostSimilar[theCurrentWorkItemId] = theMaxIndex;
                    theMostSimilarity[theCurrentWorkItemId] = theMaxSimilarity;
                }
            });
        }

        // Output the results
        for (int i=0;i<theInputs.length;i++) {
            System.out.println("Most similar match for input " + i + " is " + theMostSimilar[i] + " with a similarity of " + theMostSimilarity[i]);
        }
    }


    @Test
    public void testPerformance() throws Exception {
        int theMaxSize = 100000;
        Float4[] theInputs = new Float4[theMaxSize];
        for (int i=0;i<theMaxSize;i++) {
            theInputs[i] = new Float4((float) Math.random() * 10, (float) Math.random() * 10, (float) Math.random() * 10, (float) Math.random() * 10);
        }

        int[] theMostSimilar = new int[theInputs.length];
        float[] theMostSimilarity = new float[theInputs.length];

        long theStart = System.currentTimeMillis();

        Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger());
        //Platform thePlatform = new CPUPlatform(new Slf4JLogger());

        PlatformProperties thePlatformProps = thePlatform.getPlatformProperties();
        System.out.println("Platform is   : " + thePlatformProps.getName());

        DeviceProperties theDevProps = thePlatform.getDeviceProperties();
        System.out.println("Device        : " + theDevProps.getName());
        System.out.println(" # CU         : " + theDevProps.getNumberOfComputeUnits());
        System.out.println(" Clock freq.  : " + theDevProps.getClockFrequency());
        System.out.println(" Max workgroup: " + theDevProps.getMaxWorkGroupSize());

        try (Context theContext = thePlatform.createContext()) {

            theContext.compute(theInputs.length, new Kernel() {
                @Override
                public void processWorkItem() {
                    int theCurrentWorkItemId = get_global_id(0);
                    int theMax = get_global_size(0);

                    Float4 theCurrent = theInputs[theCurrentWorkItemId];
                    float theCurrentLength = length(theCurrent);

                    float theMaxSimilarity = -1;
                    int theMaxIndex = -1;

                    for (int i = 0; i < theMax; i++) {
                        if (i != theCurrentWorkItemId) {
                            Float4 theOther = theInputs[i];
                            float theOtherLength = length(theOther);

                            float theLength = theCurrentLength * theOtherLength;

                            if (theLength != 0) {
                                float theSimilarity = dot(theCurrent, theOther) / (theLength);

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

        long theDuration = System.currentTimeMillis() - theStart;
        System.out.println("Took " + theDuration);
    }
}