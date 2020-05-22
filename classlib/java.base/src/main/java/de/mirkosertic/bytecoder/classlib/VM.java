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
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class VM {

    public static final DecimalFormatSymbols FORMAT_SYMBOLS = DecimalFormatSymbols.getInstance();

    public static final ClassLoader SYSTEM_LOADER = new ClassLoader() {
    };

    public static void appendInternal(final StringBuilder sb, final double aValue, final long aMultiplier) {
        final long theA;
        final long theB;
        if (aValue < 0) {
            theA = (long) Math.ceil(aValue);
            theB = - (long) Math.ceil((aValue % 1) * 10000);
        } else {
            theA = (long) Math.floor(aValue);
            theB = (long) Math.floor((aValue % 1) * 10000);
        }
        sb.append(theA);

        final StringBuilder theTemp = new StringBuilder();
        theTemp.append(theB);

        for (int i=theTemp.length()-1;i>=0;i--) {
            final char theChar = theTemp.charAt(i);
            if (theChar != '0') {
                sb.append(VM.FORMAT_SYMBOLS.getDecimalSeparator());
                for (int j=0;j<=i;j++) {
                    sb.append(theTemp.charAt(j));
                }
                return;
            }
        }

        sb.append(VM.FORMAT_SYMBOLS.getDecimalSeparator());
        sb.append('0');
    }

    public static final byte[] DigitTens = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    public static final byte[] DigitOnes = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    } ;

    public static abstract class ImplementingCallsite extends ConstantCallSite {

        public ImplementingCallsite(final MethodHandle target) {
            super(target);
        }

        public abstract Object invokeExact(Object... args) throws Throwable;
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

    public static void setClassMember(final Class clz, final String name, final Object value) {
    }

    public static final char NEWLINE = '\n';

    public static long stringToLong(final String aString) {
        return stringToLong(aString,10);
    }

    public static long stringToLong(final String aString, final int aRadix) {
        long theResult = 0;
        long theMultiplier = 1;
        for (int k=aString.length()-1;k>=0;k--) {
            final char theCharAt = aString.charAt(k);
            if (k==0 && theCharAt == '-') {
                theResult=-theResult;
            } else {
                theResult += Character.getNumericValue(theCharAt) * theMultiplier;
                theMultiplier *= aRadix;
            }
        }
        return theResult;
    }

    public static String longToHex(long aValue) {
        if (aValue == 0) {
            return "0";
        }
        final StringBuilder theResult = new StringBuilder();
        while(aValue > 0) {
            final int theModulo = (int) (aValue % 16);
            theResult.append(Character.forDigit(theModulo, 16));
            aValue = aValue >> 4;
        }
        return theResult.reverse().toString();
    }

    private static String newStringInternal(final byte[] aData) {
        return null;
    }

    @Export("newStringUTF8")
    public static String newStringUTF8(final byte[] aData) {
        // This method invocation will be replaced by an intrinsic
        // calling the package private constructor (byte[],coder)
        return newStringInternal(aData);
    }

    @Export("newByteArray")
    public static byte[] newByteArray(final int length) {
        return new byte[length];
    }

    @Export("setByteArrayEntry")
    public static void setByteArrayEntry(final byte[] aArray, final int aIndex, final byte aValue) {
        aArray[aIndex] = aValue;
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
}
