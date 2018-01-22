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
public class Vec4f implements FloatSerializable {

    public float s1;
    public float s2;
    public float s3;
    public float s4;

    public Vec4f(float aS1, float aS2, float aS3, float aS4) {
        s1 = aS1;
        s2 = aS2;
        s3 = aS3;
        s4 = aS4;
    }

    @Override
    public void writeTo(FloatBuffer aBuffer) {
        aBuffer.put(s1).put(s2).put(s3).put(s4);
    }

    @Override
    public void readFrom(FloatBuffer aBuffer) {
        s1 = aBuffer.get();
        s2 = aBuffer.get();
        s3 = aBuffer.get();
        s4 = aBuffer.get();
    }

    @Override
    public String toString() {
        return "Vec4f{" +
                "s1=" + s1 +
                ", s2=" + s2 +
                ", s3=" + s3 +
                ", s4=" + s4 +
                '}';
    }

    Vec4f normalize() {
        return this;
    }

    float length() {
        float theSquareSum = 0.0f;
        theSquareSum += s1 * s1;
        theSquareSum += s2 * s2;
        theSquareSum += s3 * s3;
        theSquareSum += s4 * s4;
        return (float) Math.sqrt(theSquareSum);

    }

    Vec4f cross(Vec4f aOtherVector) {
        return new Vec4f(
            s1 * aOtherVector.s1,
            s2 * aOtherVector.s2,
            s3 * aOtherVector.s3,
            s4 * aOtherVector.s4
        );
    }

    float dot(Vec4f aOtherVector) {
        float theDotProduct = 0.0f;
        theDotProduct += s1 * aOtherVector.s1;
        theDotProduct += s2 * aOtherVector.s2;
        theDotProduct += s3 * aOtherVector.s3;
        theDotProduct += s4 * aOtherVector.s4;
        return theDotProduct;
    }
}