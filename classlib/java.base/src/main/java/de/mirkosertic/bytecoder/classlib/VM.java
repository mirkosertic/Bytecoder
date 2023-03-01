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

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Locale;

public class VM {

    public static final ClassLoader SYSTEM_LOADER = new ClassLoader() {
    };

    public static abstract class ImplementingCallsite extends ConstantCallSite {

        public ImplementingCallsite(final MethodHandle target) {
            super(target);
        }

        public abstract Object invokeExact(Object... args);
    }

    public static class LambdaStaticImplCallsite extends ImplementingCallsite {

        private final String methodName;
        private final MethodType constructedType;
        private final MethodHandle implMethod;

        public LambdaStaticImplCallsite(final String aMethodName, final MethodType aConstructedType, final MethodHandle aImplMethod) {
            super(null);
            methodName = aMethodName;
            constructedType = aConstructedType;
            implMethod = aImplMethod;
        }

        @Override
        public Object invokeExact(final Object... args) {
            return newLambdaStaticInvocation(methodName, constructedType, implMethod, args);
        }
    }

    public static class InvokeInterfaceCallsite extends ImplementingCallsite {

        private final MethodType constructedType;
        private final MethodHandle delegateMethod;

        public InvokeInterfaceCallsite(final MethodType aConstructedType, final MethodHandle aDelegateMethod) {
            super(null);
            constructedType = aConstructedType;
            delegateMethod = aDelegateMethod;
        }

        @Override
        public Object invokeExact(final Object... args) {
            return newLambdaInterfaceInvocation(constructedType, delegateMethod, args);
        }
    }

    public static class InvokeVirtualCallsite extends ImplementingCallsite {

        private final MethodType constructedType;
        private final MethodHandle delegateMethod;

        public InvokeVirtualCallsite(final MethodType aConstructedType, final MethodHandle aDelegateMethod) {
            super(null);
            constructedType = aConstructedType;
            delegateMethod = aDelegateMethod;
        }

        @Override
        public Object invokeExact(final Object... args) {
            return newLambdaVirtualInvocation(constructedType, delegateMethod, args);
        }
    }

    public static class InvokeSpecialCallsite extends ImplementingCallsite {

        private final MethodType constructedType;
        private final MethodHandle delegateMethod;

        public InvokeSpecialCallsite(final MethodType aConstructedType, final MethodHandle aDelegateMethod) {
            super(null);
            constructedType = aConstructedType;
            delegateMethod = aDelegateMethod;
        }

        @Override
        public Object invokeExact(final Object... args) {
            return newLambdaSpecialInvocation(constructedType, delegateMethod, args);
        }
    }

    public static class LambdaConstructorRefCallsite extends ImplementingCallsite {

        private final MethodType constructedType;
        private final MethodHandle constructorRef;

        public LambdaConstructorRefCallsite(final MethodType aConstructedType, final MethodHandle aConstructorRef) {
            super(null);
            constructedType = aConstructedType;
            constructorRef = aConstructorRef;
        }

        @Override
        public Object invokeExact(final Object... args) {
            return newLambdaConstructorInvocation(constructedType, constructorRef, args);
        }
    }

    public static native Object newLambdaStaticInvocation(final String methodName, final MethodType aConstructedType, final MethodHandle aImplMethod, final Object... staticArguments);

    public static native Object newLambdaConstructorInvocation(final MethodType aConstructedType, final MethodHandle aConstructorRef, final Object... staticArguments);

    public static native Object newLambdaInterfaceInvocation(final MethodType aConstructedType, final MethodHandle aDelegateMethod, final Object... staticArguments);

    public static native Object newLambdaVirtualInvocation(final MethodType aConstructedType, final MethodHandle aDelegateMethod, final Object... staticArguments);

    public static native Object newLambdaSpecialInvocation(final MethodType aConstructedType, final MethodHandle aDelegateMethod, final Object... staticArguments);

    public static Object newInstanceFromDefaultConstructor(final Class clz) {
        return null;
    }

    private static String newStringInternal(final byte[] aData) {
        return null;
    }

    public static Locale defaultLocale() {
        return new Locale("en", "US");
    }

    @EmulatedByRuntime
    public static native boolean isChar(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native boolean isFloat(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native boolean isDouble(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native boolean isBoolean(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native boolean isInteger(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native boolean isLong(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native boolean isShort(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native boolean isByte(final MethodType aType, final int aParamIndex);

    @EmulatedByRuntime
    public static native long arrayEntryAsLong(final Object[] aObject, final int index);

    @EmulatedByRuntime
    public static native int arrayEntryAsInt(final Object[] aObject, final int index);

    @EmulatedByRuntime
    public static native float arrayEntryAsFloat(final Object[] aObject, final int index);

    @EmulatedByRuntime
    public static native double arrayEntryAsDouble(final Object[] aObject, final int index);

    @EmulatedByRuntime
    public static native char arrayEntryAsChar(final Object[] aObject, final int index);

    @EmulatedByRuntime
    public static native char arrayEntryAsBoolean(final Object[] aObject, final int index);

    @EmulatedByRuntime
    public static native short arrayEntryAsShort(final Object[] aObject, final int index);

    @EmulatedByRuntime
    public static native byte arrayEntryAsByte(final Object[] aObject, final int index);

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
}
