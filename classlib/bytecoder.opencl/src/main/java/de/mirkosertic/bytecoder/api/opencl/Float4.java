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

@OpenCLType(name = "float4", elementCount = 4)
public class Float4 implements FloatSerializable {

    public float s0;
    public float s1;
    public float s2;
    public float s3;

    @OpenCLFunction(value = "float2", literal = true)
    public static Float4 float4(final float aS0, final float aS1, final float aS2, final float aS3) {
        return new Float4(aS0, aS1, aS2, aS3);
    }

    private Float4(final float aS0, final float aS1, final float aS2, final float aS3) {
        s0 = aS0;
        s1 = aS1;
        s2 = aS2;
        s3 = aS3;
    }

    @Override
    public void writeTo(final FloatBuffer aBuffer) {
        aBuffer.put(s0).put(s1).put(s2).put(s3);
    }

    @Override
    public void readFrom(final FloatBuffer aBuffer) {
        s0 = aBuffer.get();
        s1 = aBuffer.get();
        s2 = aBuffer.get();
        s3 = aBuffer.get();
    }

    @Override
    public String toString() {
        return "float4{" +
                "s0=" + s0 +
                ", s1=" + s1 +
                ", s2=" + s2 +
                ", s3=" + s3 +
                '}';
    }

    static Float4 normalize(final Float4 aVector) {
        throw new IllegalArgumentException("Not implemented for CPU emulation");
    }

    float length() {
        float theSquareSum = 0.0f;
        theSquareSum += s0 * s0;
        theSquareSum += s1 * s1;
        theSquareSum += s2 * s2;
        theSquareSum += s3 * s3;
        return (float) Math.sqrt(theSquareSum);

    }

    Float4 cross(final Float4 aOtherVector) {
        return new Float4(
            s0 * aOtherVector.s0,
            s1 * aOtherVector.s1,
            s2 * aOtherVector.s2,
            s3 * aOtherVector.s3
        );
    }

    float dot(final Float4 aOtherVector) {
        float theDotProduct = 0.0f;
        theDotProduct += s0 * aOtherVector.s0;
        theDotProduct += s1 * aOtherVector.s1;
        theDotProduct += s2 * aOtherVector.s2;
        theDotProduct += s3 * aOtherVector.s3;
        return theDotProduct;
    }
}