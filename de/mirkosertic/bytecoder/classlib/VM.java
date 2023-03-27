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
package de.mirkosertic.bytecoder.classlib;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Export;

import java.lang.reflect.Field;
import java.util.Locale;

public class VM {

    public static final ClassLoader SYSTEM_LOADER = new ClassLoader() {
    };

    public static Object newInstanceFromDefaultConstructor(final Class clz) {
        return null;
    }

    public static Locale defaultLocale() {
        return new Locale("en", "US");
    }

    @EmulatedByRuntime
    public static native Object getObjectFromStaticField(final Class declaredClass, final Field field);

    @EmulatedByRuntime
    public static native Object getObjectFromInstanceField(final Object o, final Field field);

    @EmulatedByRuntime
    public static native void putObjectToStaticField(final Class declaredClass, final Field field, final Object value);

    @EmulatedByRuntime
    public static native void putObjectToInstanceField(final Object o, final Field field, final Object value);

    @EmulatedByRuntime
    public static native Class<?> bytePrimitiveClass();

    @EmulatedByRuntime
    public static native Class<?> charPrimitiveClass();

    @EmulatedByRuntime
    public static native Class<?> shortPrimitiveClass();

    @EmulatedByRuntime
    public static native Class<?> intPrimitiveClass();

    @EmulatedByRuntime
    public static native Class<?> floatPrimitiveClass();

    @EmulatedByRuntime
    public static native Class<?> doublePrimitiveClass();

    @EmulatedByRuntime
    public static native Class<?> longPrimitiveClass();

    @EmulatedByRuntime
    public static native Class<?> booleanPrimitiveClass();

    @Export("exceptionMessage")
    public static String exceptionMessage(final Exception e) {
        return e.getMessage();
    }

    @Export("objectToString")
    public static String objectToString(final Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString();
    }
}
