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
package de.mirkosertic.bytecoder.classlib.sun.java2d.pipe;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import sun.awt.geom.PathConsumer2D;
import sun.java2d.pipe.AATileGenerator;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.RenderingEngine;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

@SubstitutesInClass(completeReplace = true)
public abstract class TRenderingEngine {

    private static RenderingEngine impl;

    public static RenderingEngine getInstance() {
        return impl;
    }

    public abstract Shape createStrokedShape(Shape var1, float var2, int var3, int var4, float var5, float[] var6, float var7);

    public abstract void strokeTo(Shape var1, AffineTransform var2, BasicStroke var3, boolean var4, boolean var5, boolean var6, PathConsumer2D var7);

    public abstract AATileGenerator getAATileGenerator(Shape var1, AffineTransform var2, Region var3, BasicStroke var4, boolean var5, boolean var6, int[] var7);

    public abstract AATileGenerator getAATileGenerator(double var1, double var3, double var5, double var7, double var9, double var11, double var13, double var15, Region var17, int[] var18);

    public abstract float getMinimumAAPenSize();

    public static void feedConsumer(PathIterator var0, PathConsumer2D var1) {
        for(float[] var2 = new float[6]; !var0.isDone(); var0.next()) {
            switch(var0.currentSegment(var2)) {
                case 0:
                    var1.moveTo(var2[0], var2[1]);
                    break;
                case 1:
                    var1.lineTo(var2[0], var2[1]);
                    break;
                case 2:
                    var1.quadTo(var2[0], var2[1], var2[2], var2[3]);
                    break;
                case 3:
                    var1.curveTo(var2[0], var2[1], var2[2], var2[3], var2[4], var2[5]);
                    break;
                case 4:
                    var1.closePath();
            }
        }
    }
}
