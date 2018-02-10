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

@OpenCLType(name = "float2", elementCount = 2)
public class Float2 implements FloatSerializable {

    public float s0;
    public float s1;

    public Float2(float aS0, float aS1) {
        s0 = aS0;
        s1 = aS1;
    }

    @Override
    public void writeTo(FloatBuffer aBuffer) {
        aBuffer.put(s0).put(s1);
    }

    @Override
    public void readFrom(FloatBuffer aBuffer) {
        s0 = aBuffer.get();
        s1 = aBuffer.get();
    }

    @Override
    public String toString() {
        return "Vec2f{" +
                "s0=" + s0 +
                ", s1=" + s1 +
                '}';
    }

    static Float2 normalize(Float2 aVector) {
        throw new IllegalArgumentException("Not implemented for CPU emulation");
    }

    float length() {
        float theSquareSum = 0.0f;
        theSquareSum += s0 * s0;
        theSquareSum += s1 * s1;
        return (float) Math.sqrt(theSquareSum);

    }

    Float2 cross(Float2 aOtherVector) {
        return new Float2(
            s0 * aOtherVector.s0,
            s1 * aOtherVector.s1
        );
    }

    float dot(Float2 aOtherVector) {
        float theDotProduct = 0.0f;
        theDotProduct += s0 * aOtherVector.s0;
        theDotProduct += s1 * aOtherVector.s1;
        return theDotProduct;
    }
}