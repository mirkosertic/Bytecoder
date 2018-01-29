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

public class VectorFunctions {

    @OpenCLFunction("normalize")
    public static Float2 normalize(Float2 aVector) {
        return Float2.normalize(aVector);
    }

    @OpenCLFunction("normalize")
    public static Float4 normalize(Float4 aVector) {
        return Float4.normalize(aVector);
    }

    @OpenCLFunction("length")
    public static float length(Float2 aVector) {
        return aVector.length();
    }

    @OpenCLFunction("length")
    public static float length(Float4 aVector) {
        return aVector.length();
    }

    @OpenCLFunction("cross")
    public static Float2 cross(Float2 aVector, Float2 aOtherVector) {
        return aVector.cross(aOtherVector);
    }

    @OpenCLFunction("cross")
    public static Float4 cross(Float4 aVector, Float4 aOtherVector) {
        return aVector.cross(aOtherVector);
    }

    @OpenCLFunction("dot")
    public static float dot(Float2 aVector, Float2 aOtherVector) {
        return aVector.dot(aOtherVector);
    }

    @OpenCLFunction("dot")
    public static float dot(Float4 aVector, Float4 aOtherVector) {
        return aVector.dot(aOtherVector);
    }
}
