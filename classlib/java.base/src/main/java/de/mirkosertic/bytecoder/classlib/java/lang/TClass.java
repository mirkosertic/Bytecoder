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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TClass {

    @EmulatedByRuntime
    public boolean desiredAssertionStatus() {
        return false;
    }

    public String getTypeName() {
        return "";
    }

    @EmulatedByRuntime
    public String getName() {
        return "";
    }

    public Class getComponentType() {
        return null;
    }

    public Class<?> getDeclaringClass() throws SecurityException {
        return null;
    }

    public String getSimpleName() {
        return "";
    }

    public boolean isEnum() {
        return false;
    }

    @EmulatedByRuntime
    public Object[] getEnumConstants() {
        return null;
    }

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