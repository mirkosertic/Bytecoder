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

@SubstitutesInClass(Class.class)
public class TClass {

    @Substitutes("<clinit>")
    private static void emptyClassInit() {
    }

    @Substitutes("desiredAssertionStatus")
    public boolean desiredAssertionStatus() {
        return false;
    }

    @Substitutes("getTypeName")
    public String getTypeName() {
        return "";
    }

    @Substitutes("getName")
    public String getName() {
        return "";
    }

    @Substitutes("getComponentType")
    public Class getComponentType() {
        return null;
    }

    @Substitutes("getDeclaringClass")
    public Class<?> getDeclaringClass() throws SecurityException {
        return null;
    }

    @Substitutes("getSimpleName")
    public String getSimpleName() {
        return "";
    }

    @Substitutes("isEnum")
    public boolean isEnum() {
        return false;
    }

    @Substitutes("getEnumConstants")
    public Object[] getEnumConstants() {
        return null;
    }

    @Substitutes("getPrimitiveClass")
    public static Class<?> getPrimitiveClass(String aName) {
        if ("byte".equals(aName)) {
            return Byte.class;
        }
        if ("char".equals(aName)) {
            return Character.class;
        }
        if ("short".equals(aName)) {
            return Short.class;
        }
        if ("int".equals(aName)) {
            return Integer.class;
        }
        if ("float".equals(aName)) {
            return Float.class;
        }
        if ("double".equals(aName)) {
            return Double.class;
        }
        if ("long".equals(aName)) {
            return Long.class;
        }
        if ("boolean".equals(aName)) {
            return Boolean.class;
        }
        throw new RuntimeException(aName);
    }
}