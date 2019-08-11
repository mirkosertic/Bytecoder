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
package de.mirkosertic.bytecoder.api.opencl;

import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.get_global_id;

public class MandelbrotOpenCL {

    final int m_width = 1024;
    final int m_height = 768;
    final int[] m_imageData;
    final int m_itercount;

    public MandelbrotOpenCL() throws Exception {
        final Platform thePlatform = PlatformFactory.resolve().createPlatform(new Slf4JLogger(), new OpenCLOptions(true));
        //final Platform thePlatform = new CPUPlatform(new Slf4JLogger());

        final long startTime = System.currentTimeMillis();

        final float x_min = -2f;
        final float x_max = 2f;
        final float y_min = -1.5f;
        final float y_max = 1.5f;

        final int maxIterations = 1000;
        final float cellSize_width = (x_max - x_min) / m_width;
        final float cellSize_height = (y_max - y_min) / m_height;

        final int[] imageData = new int[m_width * m_height];

        try (final Context theContext = thePlatform.createContext()) {
            theContext.compute(imageData.length, new Kernel() {

                private int checkC(final float reC, final float imC) {
                    float reZ=0,imZ=0,reZ_minus1=0,imZ_minus1=0;
                    int i;
                    for (i=0;i<maxIterations;i++) {
                        imZ=2*reZ_minus1*imZ_minus1+imC;
                        reZ=reZ_minus1*reZ_minus1-imZ_minus1*imZ_minus1+reC;
                        if (reZ*reZ+imZ*imZ>4) return i;
                        reZ_minus1=reZ;
                        imZ_minus1=imZ;
                    }
                    return i;
                }

                @Override
                public void processWorkItem() {
                    final int pixelIndex = get_global_id(0);
                    final int x = pixelIndex % m_width;
                    final int y = pixelIndex / m_width;

                    final float reC = x_min + (x * cellSize_width);
                    final float imC = y_min + (y * cellSize_height);

                    imageData[pixelIndex] = checkC(reC, imC);
                }
            });
        }

        final long duration = System.currentTimeMillis() - startTime;
        System.out.println("Duration = " + duration);
        m_imageData = imageData;
        m_itercount = maxIterations;
    }
}
