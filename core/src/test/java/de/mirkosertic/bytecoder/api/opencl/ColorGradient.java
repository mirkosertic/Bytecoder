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

import java.awt.Color;

public class ColorGradient {

    public static class ControlPoint {

        private final float p;
        private final Color color;

        public ControlPoint(final float p, final Color color) {
            this.p = p;
            this.color = color;
        }
    }

    private final Color[] colors;

    public ColorGradient(final ControlPoint[] controlPoints, final int size) {
        colors = new Color[size];
        int currentPos = 0;
        Color lastColor = Color.BLACK;
        for (final ControlPoint p : controlPoints) {
            if (p.p == 0) {
                lastColor = p.color;
            } else {
                final Color targetColor = p.color;
                final int targetPos = (int) (size * p.p);
                final int width = targetPos - currentPos;

                final int deltaR = targetColor.getRed() - lastColor.getRed();
                final int deltaG = targetColor.getGreen() - lastColor.getGreen();
                final int deltaB = targetColor.getBlue() - lastColor.getBlue();

                for (int i=0;i<width;i++) {
                    final float percentNew = i / (float) width;
                    final int newR = (int) (lastColor.getRed() + deltaR * percentNew);
                    final int newG = (int) (lastColor.getGreen() + deltaG * percentNew);
                    final int newB = (int) (lastColor.getBlue() + deltaB * percentNew);
                    colors[currentPos++] = new Color(newR, newG, newB);
                }
                lastColor = targetColor;
            }
        }
    }

    public Color colorAt(final int aPosition) {
        return colors[aPosition];
    }
}
