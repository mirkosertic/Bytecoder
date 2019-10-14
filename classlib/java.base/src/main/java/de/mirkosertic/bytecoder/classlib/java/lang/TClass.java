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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.ProtectionDomain;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.java.lang.reflect.TConstructor;

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
        return getName();
    }

    public boolean isEnum() {
        return false;
    }

    @EmulatedByRuntime
    public Object[] getEnumConstants() {
        return null;
    }

    public Constructor getConstructor(final Class[] constructorArgs) {
        if (constructorArgs.length != 0) {
            throw new IllegalStateException("Only zero-args constructors are supported yet!");
        }
        return (Constructor) (Object) new TConstructor((Class) (Object) this, new Class[0]);
    }

    public Object newInstance() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return getConstructor(new Class[0]).newInstance(new Object[0]);
    }

    public static Class<?> getPrimitiveClass(final String aName) {
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

    public ProtectionDomain getProtectionDomain() {
        return null;
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public static Class forName(final String name) throws ClassNotFoundException {
        return forName(name, true, Class.class.getClassLoader());
    }

    @EmulatedByRuntime
    public static Class forName(final String name, final boolean initialize, final ClassLoader classLoader) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }
}