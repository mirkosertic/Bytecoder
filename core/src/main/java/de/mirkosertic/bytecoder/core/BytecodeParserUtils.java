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

public class BytecodeParserUtils {

    public static int integerFromByteArray(byte[] aData, int aOffset) {
        int theByte1 = aData[aOffset++];
        int theByte2 = aData[aOffset];
        return (short)( ((theByte1 & 0xFF) << 8) | (theByte2 & 0xFF) );
    }

    public static short shortFromByteArray(byte[] aData, int aOffset) {
        byte theByte1 = aData[aOffset++];
        byte theByte2 = aData[aOffset];
        return (short)( ((theByte1 & 0xFF) << 8) | (theByte2 & 0xFF) );
    }

    public static int byteFromByteArray(byte[] aData, int aOffset) {
        return aData[aOffset] & 0xFF;
    }

    public static long longFromByteArray(byte[] aData, int aOffset) {
        int theByte1 = aData[aOffset++];
        int theByte2 = aData[aOffset++];
        int theByte3 = aData[aOffset++];
        int theByte4 = aData[aOffset];

        return ((theByte1 << 24) | (theByte2 << 16) | (theByte3 << 8) | theByte4);
    }

    public static float intToFloat(int aIntValue) {
        if (aIntValue ==  0x7f800000) {
            return Float.POSITIVE_INFINITY;
        }
        if (aIntValue == 0xff800000) {
            return Float.NEGATIVE_INFINITY;
        }

        int s = ((aIntValue >> 31) == 0) ? 1 : -1;
        int e = ((aIntValue >> 23) & 0xff);
        int m = (e == 0) ?
                (aIntValue & 0x7fffff) << 1 :
                (aIntValue & 0x7fffff) | 0x800000;

        return (float) (s * m * Math.pow(2, e-150));
    }

    public static double intToDouble(long aLowBytes, long aHighBytes) {
        long theValue = aHighBytes << 32  + aLowBytes;
        if (theValue == 0x7ff0000000000000L) {
            return Double.POSITIVE_INFINITY;
        }
        if (theValue == 0xfff0000000000000L) {
            return Double.NEGATIVE_INFINITY;
        }

        int s = ((theValue >> 63) == 0) ? 1 : -1;
        int e = (int)((theValue >> 52) & 0x7ffL);
        long m = (e == 0) ?
                (theValue & 0xfffffffffffffL) << 1 :
                (theValue & 0xfffffffffffffL) | 0x10000000000000L;

        return (double) (s * m * Math.pow(2, e-1075));
    }
}