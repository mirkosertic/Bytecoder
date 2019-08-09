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

import java.nio.FloatBuffer;

@OpenCLType(name = "float16", elementCount = 16)
public class Float16 implements FloatSerializable {

    public float s0;
    public float s1;
    public float s2;
    public float s3;
    public float s4;
    public float s5;
    public float s6;
    public float s7;
    public float s8;
    public float s9;
    public float sa;
    public float sb;
    public float sc;
    public float sd;
    public float se;
    public float sf;

    @OpenCLFunction(value = "float16", literal = true)
    public static Float16 float16(final float aS0, final float aS1, final float aS2, final float aS3, final float aS4, final float aS5, final float aS6, final float aS7, final float aS8, final float aS9, final float aSa,
                                  final float aSb, final float aSc, final float aSd, final float aSe, final float aSf) {
        return new Float16(aS0, aS1, aS2, aS3, aS4, aS5, aS6, aS7, aS8, aS9, aSa, aSb, aSc, aSd, aSe, aSf);
    }

    private Float16(final float aS0, final float aS1, final float aS2, final float aS3, final float aS4, final float aS5, final float aS6, final float aS7, final float aS8, final float aS9, final float aSa,
                    final float aSb, final float aSc, final float aSd, final float aSe, final float aSf) {
        s0 = aS0;
        s1 = aS1;
        s2 = aS2;
        s3 = aS3;
        s4 = aS4;
        s5 = aS5;
        s6 = aS6;
        s7 = aS7;
        s8 = aS8;
        s9 = aS9;
        sa = aSa;
        sb = aSb;
        sc = aSc;
        sd = aSd;
        se = aSe;
        sf = aSf;
    }

    @Override
    public void writeTo(final FloatBuffer aBuffer) {
        aBuffer.put(s0).put(s1).put(s2).put(s3).put(s4).put(s5).put(s6).put(s7)
        .put(s8).put(s9).put(sa).put(sb).put(sc).put(sd).put(se).put(sf);
    }

    @Override
    public void readFrom(final FloatBuffer aBuffer) {
        s0 = aBuffer.get();
        s1 = aBuffer.get();
        s2 = aBuffer.get();
        s3 = aBuffer.get();
        s4 = aBuffer.get();
        s5 = aBuffer.get();
        s6 = aBuffer.get();
        s7 = aBuffer.get();
        s8 = aBuffer.get();
        s9 = aBuffer.get();
        sa = aBuffer.get();
        sb = aBuffer.get();
        sc = aBuffer.get();
        sd = aBuffer.get();
        se = aBuffer.get();
        sf = aBuffer.get();
    }

    @Override
    public String toString() {
        return "float16{" +
                "s0=" + s0 +
                ", s1=" + s1 +
                ", s2=" + s2 +
                ", s3=" + s3 +
                ", s4=" + s4 +
                ", s5=" + s5 +
                ", s6=" + s6 +
                ", s7=" + s7 +
                ", s8=" + s8 +
                ", s9=" + s9 +
                ", sa=" + sa +
                ", sb=" + sb +
                ", sc=" + sc +
                ", sd=" + sd +
                ", se=" + se +
                ", sf=" + sf +
                '}';
    }

    static Float16 normalize(final Float16 aVector) {
        throw new IllegalArgumentException("Not implemented for CPU emulation");
    }

    float length() {
        float theSquareSum = 0.0f;
        theSquareSum += s0 * s0;
        theSquareSum += s1 * s1;
        theSquareSum += s2 * s2;
        theSquareSum += s3 * s3;
        theSquareSum += s4 * s4;
        theSquareSum += s5 * s5;
        theSquareSum += s6 * s6;
        theSquareSum += s7 * s7;
        theSquareSum += s8 * s8;
        theSquareSum += s9 * s9;
        theSquareSum += sa * sa;
        theSquareSum += sb * sb;
        theSquareSum += sc * sc;
        theSquareSum += sd * sd;
        theSquareSum += se * se;
        theSquareSum += sf * sf;
        return (float) Math.sqrt(theSquareSum);

    }

    Float16 cross(final Float16 aOtherVector) {
        return new Float16(
            s0 * aOtherVector.s0,
            s1 * aOtherVector.s1,
            s2 * aOtherVector.s2,
            s3 * aOtherVector.s3,
            s4 * aOtherVector.s4,
            s5 * aOtherVector.s5,
            s6 * aOtherVector.s6,
            s7 * aOtherVector.s7,
            s8 * aOtherVector.s8,
            s9 * aOtherVector.s9,
            sa * aOtherVector.sa,
            sb * aOtherVector.sb,
            sc * aOtherVector.sc,
            sd * aOtherVector.sd,
            se * aOtherVector.se,
            sf * aOtherVector.sf
        );
    }

    float dot(final Float16 aOtherVector) {
        float theDotProduct = 0.0f;
        theDotProduct += s0 * aOtherVector.s0;
        theDotProduct += s1 * aOtherVector.s1;
        theDotProduct += s2 * aOtherVector.s2;
        theDotProduct += s3 * aOtherVector.s3;
        theDotProduct += s4 * aOtherVector.s4;
        theDotProduct += s5 * aOtherVector.s5;
        theDotProduct += s6 * aOtherVector.s6;
        theDotProduct += s7 * aOtherVector.s7;
        theDotProduct += s8 * aOtherVector.s8;
        theDotProduct += s9 * aOtherVector.s9;
        theDotProduct += sa * aOtherVector.sa;
        theDotProduct += sb * aOtherVector.sb;
        theDotProduct += sc * aOtherVector.sc;
        theDotProduct += sd * aOtherVector.sd;
        theDotProduct += se * aOtherVector.se;
        theDotProduct += sf * aOtherVector.sf;
        return theDotProduct;
    }
}