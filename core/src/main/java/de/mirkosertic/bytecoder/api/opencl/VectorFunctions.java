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
    public static Vec2f normalize(Vec2f aVector) {
        return aVector.normalize();
    }

    @OpenCLFunction("length")
    public static float length(Vec2f aVector) {
        return aVector.length();
    }

    @OpenCLFunction("length")
    public static float length(Vec4f aVector) {
        return aVector.length();
    }

    @OpenCLFunction("cross")
    public static Vec2f cross(Vec2f aVector, Vec2f aOtherVector) {
        return aVector.cross(aOtherVector);
    }

    @OpenCLFunction("cross")
    public static Vec4f cross(Vec4f aVector, Vec4f aOtherVector) {
        return aVector.cross(aOtherVector);
    }

    @OpenCLFunction("dot")
    public static float dot(Vec2f aVector, Vec2f aOtherVector) {
        return aVector.dot(aOtherVector);
    }

    @OpenCLFunction("dot")
    public static float dot(Vec4f aVector, Vec4f aOtherVector) {
        return aVector.dot(aOtherVector);
    }
}
