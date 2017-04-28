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
package de.mirkosertic.bytecoder.core;

public class SimpleClass implements SimpleInterface {

    protected String unknownString;
    protected double doubleValue = 10d;
    protected int intValue = 10;
    protected float floatValue = 14f;
    protected String[] stringArray;
    protected byte[] byteArray;
    protected long longValue = 44L;
    protected int anotherIntValue;

    public static byte[] createArray() {
        return new byte[] {(byte) 10};
    }

    public int sum(int a, int b) {
        return a + b;
    }

    public static void main(String args) {

        int a = 10;
        int b = 20;
        int c = a + b;

        byte x1= -1;
        byte x2 = 0;
        byte x3 = 1;
        byte x4 = 2;
        byte x5 = 3;
        byte x6 = 4;
        byte x7 = 5;

        boolean[] booleans = new boolean[10];
        booleans[0] = false;
        char[] chars = {};
        float[] floats = {};
        double[] doubles = {};
        byte[] bytes = {};
        short[] shorts = {};
        int[] ints = {};
        long[] longs = {};

        int z = createArray().length;

        Object[] objects = new Object[10];
        objects[0] = null;
        if (objects[1] instanceof Object) {
            return;
        }

        double dw = 11d;

        if (c > 20) {
            return;
        }

        SimpleClass theSimpleMe = new SimpleClass();
        int theSum = theSimpleMe.sum(10, 20);
        int x = theSimpleMe.intValue;
    }
}
