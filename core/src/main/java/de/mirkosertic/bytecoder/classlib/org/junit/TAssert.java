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

public class TAssert {

    public static void fail(String message) {
        if(message == null) {
            throw new RuntimeException();
        } else {
            throw new RuntimeException(message);
        }
    }

    public static String format(String message, Object expected, Object actual) {
        String formatted = "";
        if(message != null && message.length() > 0) {
            formatted = message + " ";
        }

        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }

    public static void failNotEquals(String message, Object expected, Object actual) {
        fail(format(message, expected, actual));
    }

    public static void assertEquals(String message, float expected, float actual, float delta) {
        if(Float.compare(expected, actual) != 0) {
            if(Math.abs(expected - actual) > delta) {
                failNotEquals(message, new Float(expected), new Float(actual));
            }
        }
    }

    public static void assertEquals(float expected, float actual, float delta) {
        assertEquals((String)null, expected, actual, delta);
    }
}
