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

import javax.swing.*;
import java.awt.*;

public class MandelbrotExample extends JPanel {

    private final MandelbrotOpenCL am;
    private final MandelbrotKernel kernel;

    public MandelbrotExample() throws Exception {
        am = new MandelbrotOpenCL();
        kernel = am.compute();
        setPreferredSize(new Dimension(kernel.getWidth(), kernel.getHeight()));
    }

    @Override
    public void paint (final Graphics g) {
        final int[] imageData = kernel.getImageData();
        final int width = kernel.getWidth();
        final int maxIterations = kernel.getMaxIterations();
        for (int pixelIndex=0;pixelIndex<imageData.length;pixelIndex++) {
            final int x = pixelIndex % width;
            final int y = pixelIndex / width;
            final int iterCount = imageData[pixelIndex];
            if (iterCount == 0 || iterCount == maxIterations) {
                g.setColor(Color.black);
            } else {
                switch (iterCount % 16) {
                    case 0:
                        g.setColor(new Color(66, 30, 15));
                        break;
                    case 1:
                        g.setColor(new Color(25, 7, 26));
                        break;
                    case 2:
                        g.setColor(new Color(9, 1, 47));
                        break;
                    case 3:
                        g.setColor(new Color(4, 4, 73));
                        break;
                    case 4:
                        g.setColor(new Color(0, 7, 100));
                        break;
                    case 5:
                        g.setColor(new Color(12, 44, 138));
                        break;
                    case 6:
                        g.setColor(new Color(24, 82, 177));
                        break;
                    case 7:
                        g.setColor(new Color(57, 125, 209));
                        break;
                    case 8:
                        g.setColor(new Color(134, 181, 229));
                        break;
                    case 9:
                        g.setColor(new Color(211, 236, 248));
                        break;
                    case 10:
                        g.setColor(new Color(241, 233, 191));
                        break;
                    case 11:
                        g.setColor(new Color(248, 201, 95));
                        break;
                    case 12:
                        g.setColor(new Color(255, 170, 0));
                        break;
                    case 13:
                        g.setColor(new Color(204, 128, 0));
                        break;
                    case 14:
                        g.setColor(new Color(153, 87, 0));
                        break;
                    case 15:
                        g.setColor(new Color(106, 52, 3));
                        break;
                }
            }
            g.drawLine(x,y,x,y);
        }
    }

    public static void main(final String[] args) throws Exception {
        final JFrame test = new JFrame("Mandelbrot");
        test.setResizable(false);
        test.setContentPane(new MandelbrotExample());
        test.pack();
        test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        test.setVisible(true);
    }
}