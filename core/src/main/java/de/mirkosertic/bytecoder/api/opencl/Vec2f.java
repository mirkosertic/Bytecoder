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
public class Vec2f implements FloatSerializable {

    public float x;
    public float y;

    public Vec2f(float aX, float aY) {
        this.x = aX;
        this.y = aY;
    }

    @Override
    public void writeTo(FloatBuffer aBuffer) {
        aBuffer.put(x).put(y);
    }

    @Override
    public void readFrom(FloatBuffer aBuffer) {
        x = aBuffer.get();
        y = aBuffer.get();
    }

    @Override
    public String toString() {
        return "Vec2f{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}