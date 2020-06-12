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

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.ProtectionDomain;

import de.mirkosertic.bytecoder.api.AnyTypeMatches;
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

    public String getPackageName() {
        return getName();
    }

    public String getCanonicalName() {
        return getName();
    }

    public boolean isEnum() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isAnonymousClass() {
        return false;
    }

    public boolean isLocalClass() {
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

    public Constructor[] getConstructors() {
        final Constructor[] theResult = new Constructor[1];
        theResult[0] = getConstructor(new Class[0]);
        return theResult;
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

    @EmulatedByRuntime
    public ClassLoader getClassLoader() {
        return null;
    }

    ClassLoader getClassLoader0() {
        return null;
    }

    public static Class forName(final AnyTypeMatches module, final String name) throws ClassNotFoundException {
        return forName(name, true, Class.class.getClassLoader());
    }

    public static Class forName(final String name) throws ClassNotFoundException {
        return forName(name, true, Class.class.getClassLoader());
    }

    @EmulatedByRuntime
    public static Class forName(final String name, final boolean initialize, final ClassLoader classLoader) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    public Type[] getGenericInterfaces() {
        return new Type[0];
    }

    @EmulatedByRuntime
    public Class[] getInterfaces() {
        return null;
    }

    @EmulatedByRuntime
    public Class getSuperclass() {
        return null;
    }

    public Constructor getDeclaredConstructor(final Class[] args) {
        return getConstructor(args);
    }

    @EmulatedByRuntime
    public Method getDeclaredMethod(final String name, final Class[] arguments) {
        return null;
    }

    @EmulatedByRuntime
    public boolean isInstance(final Object a) {
        return false;
    }

    @EmulatedByRuntime
    public Field getField(final String name) {
        return null;
    }

    @EmulatedByRuntime
    public Field[] getDeclaredFields() {
        return new Field[0];
    }

    public InputStream getResourceAsStream(final String aName) {
        return null;
    }

    @EmulatedByRuntime
    public boolean isAssignableFrom(final Class aOtherClass) {
        return false;
    }

    @EmulatedByRuntime
    public Method[] getMethods() {
        return new Method[0];
    }

    @EmulatedByRuntime
    public int getModifiers() {
        return 0;
    }

    public AnyTypeMatches getModule() {
        return null;
    }

    @EmulatedByRuntime
    public Method getMethod(final String aName, final Class[] argumentTypes) {
        return null;
    }

    @EmulatedByRuntime
    public Type getGenericSuperclass() {
        return null;
    }

    @EmulatedByRuntime
    public boolean isInterface() {
        return false;
    }

    @EmulatedByRuntime
    public Class getEnclosingClass() {
        return null;
    }

    public URL getResource(final String name) {
        return null;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public <T> T cast(final Object obj) {
        return (T) obj;
    }
}