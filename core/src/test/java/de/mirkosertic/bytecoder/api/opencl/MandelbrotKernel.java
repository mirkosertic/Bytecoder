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

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.get_global_id;

public class MandelbrotKernel extends Kernel {

    private final int maxIterations;
    private final int width;
    private final int height;
    private float x_min;
    private float y_min;
    private float x_max;
    private float y_max;
    private final int[] imageData;
    private float cellSize_width;
    private float cellSize_height;

    public MandelbrotKernel(final int aWidth, final int aHeight, final int aMaxIterations) {
        width = aWidth;
        height = aHeight;
        imageData = new int[width * height];
        maxIterations = aMaxIterations;
        x_min = -2f;
        x_max = 2f;
        y_min = -1.5f;
        y_max = 1.5f;
        fitCellSize();
    }

    private void fitCellSize() {
        cellSize_width = (x_max - x_min) / width;
        cellSize_height = (y_max - y_min) / height;
    }

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
        final int x = pixelIndex % width;
        final int y = pixelIndex / width;

        final float reC = x_min + (x * cellSize_width);
        final float imC = y_min + (y * cellSize_height);

        imageData[pixelIndex] = checkC(reC, imC);
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getImageData() {
        return imageData;
    }

    public void zoomInOut(final int amount) {
        final float width = x_max - x_min;
        final float height = y_max - y_min;

        final float centerX = x_min + width / 2;
        final float centerY = y_min + height / 2;

        final float newHalfWidth = width * (1 + 0.05f * amount) / 2;
        final float newHalfHeight = height * (1 + 0.05f * amount) / 2;

        x_min = centerX - newHalfWidth;
        x_max = centerX + newHalfWidth;
        y_min = centerY - newHalfHeight;
        y_max = centerY + newHalfHeight;

        fitCellSize();
    }

    public void focusOn(final int x, final int y) {

        final float halfWidth = (x_max - x_min) / 2;
        final float halfHeight = (y_max - y_min) / 2;

        final float newCenterX = x_min + x * cellSize_width;
        final float newCenterY = y_min + y * cellSize_height;

        x_min = newCenterX - halfWidth;
        x_max = newCenterX + halfWidth;
        y_min = newCenterY - halfHeight;
        y_max = newCenterY + halfHeight;
    }
}
