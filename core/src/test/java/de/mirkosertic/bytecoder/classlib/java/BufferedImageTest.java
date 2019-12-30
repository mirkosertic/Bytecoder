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
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(additionalClassesToLink = {
        "de.mirkosertic.bytecoder.classlib.BytecoderGraphicsEnvironment",
        "sun.java2d.marlin.DMarlinRenderingEngine",
        "sun.java2d.loops.OpaqueCopyAnyToArgb",
        "sun.java2d.loops.OpaqueCopyArgbToAny",
        "sun.java2d.loops.XorCopyArgbToAny",
        "sun.java2d.loops.SetFillRectANY",
        "sun.java2d.loops.SetFillPathANY",
        "sun.java2d.loops.SetFillSpansANY",
        "sun.java2d.loops.SetDrawLineANY",
        "sun.java2d.loops.SetDrawPolygonsANY",
        "sun.java2d.loops.SetDrawPathANY",
        "sun.java2d.loops.SetDrawRectANY",
        "sun.java2d.loops.XorFillRectANY",
        "sun.java2d.loops.XorFillPathANY",
        "sun.java2d.loops.XorFillSpansANY",
        "sun.java2d.loops.XorDrawLineANY",
        "sun.java2d.loops.XorDrawPolygonsANY",
        "sun.java2d.loops.XorDrawPathANY",
        "sun.java2d.loops.XorDrawRectANY",
        "sun.java2d.loops.XorDrawGlyphListANY",
        "sun.java2d.loops.XorDrawGlyphListAAANY"})
public class BufferedImageTest {

    @Test
    @Ignore
    public void testCreateByReflection() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final Class clz = Class.forName("de.mirkosertic.bytecoder.classlib.BytecoderGraphicsEnvironment");
        Assert.assertNotNull(clz);
        final Object o = clz.getConstructor(new Class[0]).newInstance(new Object[0]);
        Assert.assertNotNull(o);
    }

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
