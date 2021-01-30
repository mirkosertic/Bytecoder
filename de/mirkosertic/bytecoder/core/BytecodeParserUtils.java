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

    public static short signedShortFromByteArray(byte[] aData, int aOffset) {
        byte theByte1 = aData[aOffset++];
        byte theByte2 = aData[aOffset];
        return (short)( (theByte1 << 8) | theByte2);
    }

    public static int byteFromByteArray(byte[] aData, int aOffset) {
        return aData[aOffset] & 0xFF;
    }

    public static int signedByteFromByteArray(byte[] aData, int aOffset) {
        return aData[aOffset];
    }

    public static long longFromByteArray(byte[] aData, int aOffset) {
        int theByte1 = aData[aOffset++] & 0xFF;
        int theByte2 = aData[aOffset++] & 0xFF;
        int theByte3 = aData[aOffset++] & 0xFF;
        int theByte4 = aData[aOffset] & 0xFF;

        return ((theByte1 << 24) | (theByte2 << 16) | (theByte3 << 8) | theByte4);
    }
}