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

@OpenCLType(name = "float8", elementCount = 8)
public class Float8 implements FloatSerializable {

    public float s0;
    public float s1;
    public float s2;
    public float s3;
    public float s4;
    public float s5;
    public float s6;
    public float s7;

    @OpenCLFunction(value = "float8", literal = true)
    public static Float8 float8(final float aS0, final float aS1, final float aS2, final float aS3, final float aS4, final float aS5, final float aS6, final float aS7) {
        return new Float8(aS0, aS1,  aS2, aS3, aS4, aS5, aS6, aS7);
    }

    private Float8(final float aS0, final float aS1, final float aS2, final float aS3, final float aS4, final float aS5, final float aS6, final float aS7) {
        s0 = aS0;
        s1 = aS1;
        s2 = aS2;
        s3 = aS3;
        s4 = aS4;
        s5 = aS5;
        s6 = aS6;
        s7 = aS7;
    }

    @Override
    public void writeTo(final FloatBuffer aBuffer) {
        aBuffer.put(s0).put(s1).put(s2).put(s3).put(s4).put(s5).put(s6).put(s7);
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
    }

    @Override
    public String toString() {
        return "float8{" +
                "s0=" + s0 +
                ", s1=" + s1 +
                ", s2=" + s2 +
                ", s3=" + s3 +
                ", s4=" + s4 +
                ", s5=" + s5 +
                ", s6=" + s6 +
                ", s7=" + s7 +
                '}';
    }

    static Float8 normalize(final Float8 aVector) {
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
        return (float) Math.sqrt(theSquareSum);

    }

    Float8 cross(final Float8 aOtherVector) {
        return new Float8(
            s0 * aOtherVector.s0,
            s1 * aOtherVector.s1,
            s2 * aOtherVector.s2,
            s3 * aOtherVector.s3,
            s4 * aOtherVector.s4,
            s5 * aOtherVector.s5,
            s6 * aOtherVector.s6,
            s7 * aOtherVector.s7
        );
    }

    float dot(final Float8 aOtherVector) {
        float theDotProduct = 0.0f;
        theDotProduct += s0 * aOtherVector.s0;
        theDotProduct += s1 * aOtherVector.s1;
        theDotProduct += s2 * aOtherVector.s2;
        theDotProduct += s3 * aOtherVector.s3;
        theDotProduct += s4 * aOtherVector.s4;
        theDotProduct += s5 * aOtherVector.s5;
        theDotProduct += s6 * aOtherVector.s6;
        theDotProduct += s7 * aOtherVector.s7;
        return theDotProduct;
    }
}