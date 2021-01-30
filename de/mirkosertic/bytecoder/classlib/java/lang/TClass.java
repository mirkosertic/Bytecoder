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

import de.mirkosertic.bytecoder.api.AnyTypeMatches;
import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.classlib.java.lang.reflect.TConstructor;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.ProtectionDomain;

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
        final String fqName = getName();
        final int p = fqName.lastIndexOf('.');
        if (p>=0) {
            return fqName.substring(p + 1);
        }
        return fqName;
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

    @EmulatedByRuntime
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
            return VM.bytePrimitiveClass();
        }
        if ("char".equals(aName)) {
            return VM.charPrimitiveClass();
        }
        if ("short".equals(aName)) {
            return VM.shortPrimitiveClass();
        }
        if ("int".equals(aName)) {
            return VM.intPrimitiveClass();
        }
        if ("float".equals(aName)) {
            return VM.floatPrimitiveClass();
        }
        if ("double".equals(aName)) {
            return VM.doublePrimitiveClass();
        }
        if ("long".equals(aName)) {
            return VM.longPrimitiveClass();
        }
        if ("boolean".equals(aName)) {
            return VM.booleanPrimitiveClass();
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

    public Field getField(final String name) throws NoSuchFieldException {
        for (final Field f : getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers()) && name.equals(f.getName())) {
                return f;
            }
        }
        final Class superClass = getSuperclass();
        if (superClass != null) {
            return superClass.getField(name);
        }
        throw new NoSuchFieldException(name);
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
        //noinspection unchecked
        return (T) obj;
    }
}