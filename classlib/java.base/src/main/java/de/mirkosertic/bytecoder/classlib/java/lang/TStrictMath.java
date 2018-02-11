/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.Import;

public class TStrictMath {

    @Import(module = "math", name = "sqrt")
    public static native double sqrt(double aValue);

    @Import(module = "math", name = "sin")
    public static native double sin(double aValue);

    @Import(module = "math", name = "cos")
    public static native double cos(double aValue);

    @Import(module = "math", name = "round")
    public static native int round(float aFloat);

    @Import(module = "math", name = "atan2")
    public static native double atan2(double aValue1, double aValue2);
}