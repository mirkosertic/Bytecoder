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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MandelbrotExample extends JPanel {

    private final MandelbrotOpenCL am;
    private final MandelbrotKernel kernel;
    private final ColorGradient colorGradient;

    public MandelbrotExample() throws Exception {
        am = new MandelbrotOpenCL();
        kernel = am.compute();

        colorGradient = new ColorGradient(new ColorGradient.ControlPoint[] {
            new ColorGradient.ControlPoint(0f, new Color(80, 80, 80)),
            new ColorGradient.ControlPoint(0.1f, new Color(180, 180, 80)),
            new ColorGradient.ControlPoint(0.16f, new Color(32, 107, 203)),
            new ColorGradient.ControlPoint(0.42f, new Color(237, 255, 255)),
            new ColorGradient.ControlPoint(0.6425f, new Color(255, 170, 0)),
            new ColorGradient.ControlPoint(0.9075f, new Color(200, 2, 0)),
            new ColorGradient.ControlPoint(1f, new Color(10, 180, 80)),
        }, kernel.getMaxIterations());

        setPreferredSize(new Dimension(kernel.getWidth(), kernel.getHeight()));
        addMouseWheelListener(e -> {
            kernel.zoomInOut(e.getScrollAmount() * e.getWheelRotation());
            refreshDisplay();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                kernel.focusOn(e.getX(), e.getY());
                refreshDisplay();
            }
        });
    }

    private void refreshDisplay() {
        try {
            am.compute();
            MandelbrotExample.this.invalidate();
            MandelbrotExample.this.repaint();
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
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
            if (iterCount == maxIterations) {
                g.setColor(Color.black);
            } else {
                g.setColor(colorGradient.colorAt(iterCount));
            }
            g.drawLine(x,y,x,y);
        }
        g.setColor(Color.white);
        g.drawString(String.format("Bytecoder OpenCL Frame Time : %dms", am.getComputingTime()), 10, 20);
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