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
package de.mirkosertic.bytecoder.classlib.java;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(additionalClassesToLink = {"de.mirkosertic.bytecoder.classlib.BytecoderGraphicsEnvironment"})
public class BufferedImageTest {

    @Test
    @Ignore
    public void testCreate() {
        final BufferedImage bi = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        bi.setRGB(50, 50, 255);
        final Graphics g = bi.getGraphics();
        g.setColor(Color.red);
        g.drawLine(0,0, 200, 100);
        final int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
        System.out.println(pixels.length);
        Assert.assertEquals(20000, pixels.length);
    }
}
