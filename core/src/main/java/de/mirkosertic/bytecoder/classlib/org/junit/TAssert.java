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
package de.mirkosertic.bytecoder.classlib.org.junit;

import de.mirkosertic.bytecoder.classlib.java.lang.TDouble;
import de.mirkosertic.bytecoder.classlib.java.lang.TFloat;
import de.mirkosertic.bytecoder.classlib.java.lang.TMath;
import de.mirkosertic.bytecoder.classlib.java.lang.TRuntimeException;

public class TAssert {

    public static void fail(String message) throws TRuntimeException {
        if (message != null) {
            throw new TRuntimeException(message);
        } else {
            throw new TRuntimeException();
        }
    }

    public static String format(String message, Object expected, Object actual) {
        String formatted = "";
        if(message != null && message.length() > 0) {
            formatted = message + " ";
        }

        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }

    public static void failNotEquals(String message, Object expected, Object actual) throws TRuntimeException {
        fail(format(message, expected, actual));
    }

    public static void assertEquals(String message, float expected, float actual, float delta) throws TRuntimeException {
        if  (TFloat.compare(expected, actual) != 0) {
            if (TMath.abs(expected - actual) > delta) {
                failNotEquals(message, new TFloat(expected), new TFloat(actual));
            }
        }
    }

    public static void assertEquals(String message, double expected, double actual, double delta) throws TRuntimeException {
        if  (TDouble.compare(expected, actual) != 0) {
            if (TMath.abs(expected - actual) > delta) {
                failNotEquals(message, new TDouble(expected), new TDouble(actual));
            }
        }
    }

    public static void assertEquals(float expected, float actual, float delta) throws TRuntimeException {
        assertEquals((String)null, expected, actual, delta);
    }

    public static void assertEquals(double expected, double actual, double delta) throws TRuntimeException {
        assertEquals(null, expected, actual, delta);
    }

    public static void assertTrue(boolean aValue) throws TRuntimeException {
        if (!aValue) {
            throw new TRuntimeException();
        }
    }

    public static void assertFalse(boolean aValue) throws TRuntimeException {
        if (aValue) {
            throw new TRuntimeException();
        }
    }

    public static void assertNotNull(Object aValue) throws TRuntimeException {
        if (aValue == null) {
            throw new TRuntimeException();
        }
    }

    public static void assertNull(Object aValue) throws TRuntimeException {
        if (aValue != null) {
            throw new TRuntimeException();
        }
    }

    public static void assertEquals(Object aValue1, Object aValue2) throws TRuntimeException {
        if (!aValue1.equals(aValue2)) {
            throw new TRuntimeException();
        }
    }

    public static void assertSame(Object aValue1, Object aValue2) throws TRuntimeException {
        if (!(aValue1 == aValue2)) {
            throw new TRuntimeException();
        }
    }

    public static void assertNotSame(Object aValue1, Object aValue2) throws TRuntimeException {
        if (aValue1 == aValue2) {
            throw new TRuntimeException();
        }
    }
}
