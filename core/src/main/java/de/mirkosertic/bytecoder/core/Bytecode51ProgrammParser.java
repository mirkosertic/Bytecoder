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

public class Bytecode51ProgrammParser implements BytecodeProgrammParser {

    @Override
    public BytecodeProgramm parse(byte[] aBytecodes) {
        BytecodeProgramm theResult = new BytecodeProgramm();
        int offset = 0;
        while(offset < aBytecodes.length) {
            int theOpcode = aBytecodes[offset++]  & 0xFF;
            switch (theOpcode) {
                case 0: { // nop = 0 (0x0)
                    break;
                }
                case 16: { // bipush = 16 (0x10)
                    byte theValue = aBytecodes[offset++];
                    break;
                }
                case 18: { //ldc = 18 (0x12)
                    byte theValue = aBytecodes[offset++];
                    break;
                }
                case 26: { // iload_0 = 26 (0x1a)
                    break;
                }
                case 27: { //iload_1 = 27 (0x1b)
                    break;
                }
                case 28: { // iload_2 = 28 (0x1c)
                    break;
                }
                case 29: { // iload_3 = 29 (0x1d)
                    break;
                }
                case 42: {// aload_0 (0x2a)
                    break;
                }
                case 43: {// aload_1 = 43 (0x2b)
                    break;
                }
                case 44: { // aload_2 = 44 (0x2c)
                    break;
                }
                case 45: {// aload_3 = 45 (0x2d)
                    break;
                }
                case 59: { //istore_0 = 59 (0x3b)
                    break;
                }
                case 60: { // istore_1 = 60 (0x3c)
                    break;
                }
                case 61: { //istore_2 = 61 (0x3d)
                    break;
                }
                case 62: { // istore_3 = 62 (0x3e)
                    break;
                }
                case 96: { // iadd = 96 (0x60)
                    break;
                }
                case 177: {// return = 177 (0xb1)
                    break;
                }
                case 181: { // putfield = 181 (0xb5)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    break;
                }
                case 183: {//invokespecial = 183 (0xb7)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown opcode : " + theOpcode);
            }
        }
        return theResult;
    }
}
