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
package de.mirkosertic.bytecoder.classlib.shadow.java.lang;

import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(Math.class)
public class TMath {

    @Substitutes("<clinit>")
    public static void classinit() {
    }

    @Substitutes("abs")
    public static float abs(float a) {
        if (a<0) {
            return -a;
        }
        return a;
    }

    @Substitutes("abs")
    public static double abs(double a) {
        if (a<0) {
            return -a;
        }
        return a;
    }

    @Substitutes("sqrt")
    public static native double sqrt(double aValue);

    @Substitutes("ceil")
    public static native double ceil(double aValue);

    @Substitutes("getNaN")
    public static native float getNaN();

    @Substitutes("floor")
    public static native double floor(double aValue);

    @Substitutes("sin")
    public static native double sin(double aValue);

    @Substitutes("cos")
    public static native double cos(double aValue);

    @Substitutes("random")
    public static native double random();

    @Substitutes("toRadians")
    public static native double toRadians(double aValue);

    @Substitutes("toDegrees")
    public static native double toDegrees(double aValue);

    @Substitutes("tan")
    public static native double tan(double aValue);

    @Substitutes("max")
    public static native long max(long aValue1, long aValue2);

    @Substitutes("max")
    public static native int max(int aValue1, int aValue2);

    @Substitutes("min")
    public static native int min(int aValue1, int aValue2);
}