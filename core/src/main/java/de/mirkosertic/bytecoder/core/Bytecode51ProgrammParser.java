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
                    theResult.addInstruction(new BytecodeInstructionNOP());
                    break;
                }
                case 2: { // iconst_m1 = 2 (0x2)
                    theResult.addInstruction(new BytecodeInstructionICONST(-1));
                    break;
                }
                case 3: { // iconst_0 = 3 (0x3)
                    theResult.addInstruction(new BytecodeInstructionICONST(0));
                    break;
                }
                case 4: { // iconst_1 = 4 (0x4)
                    theResult.addInstruction(new BytecodeInstructionICONST(1));
                    break;
                }
                case 5: { // iconst_2 = 5 (0x5)
                    theResult.addInstruction(new BytecodeInstructionICONST(2));
                    break;
                }
                case 6: { // iconst_3 = 6 (0x6)
                    theResult.addInstruction(new BytecodeInstructionICONST(3));
                    break;
                }
                case 7: { // iconst_4 = 7 (0x7)
                    theResult.addInstruction(new BytecodeInstructionICONST(4));
                    break;
                }
                case 8: { // iconst_5 = 8 (0x8)
                    theResult.addInstruction(new BytecodeInstructionICONST(5));
                    break;
                }
                case 16: { // bipush = 16 (0x10)
                    byte theValue = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionBIPUSH(theValue));
                    break;
                }
                case 18: { //ldc = 18 (0x12)
                    byte theValue = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLDC(theValue));
                    break;
                }
                case 25: { // aload = 25 (0x19)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionALOAD(theIndex));
                    break;
                }
                case 26: { // iload_0 = 26 (0x1a)
                    theResult.addInstruction(new BytecodeInstructionILOAD((byte) 0));
                    break;
                }
                case 27: { //iload_1 = 27 (0x1b)
                    theResult.addInstruction(new BytecodeInstructionILOAD((byte) 1));
                    break;
                }
                case 28: { // iload_2 = 28 (0x1c)
                    theResult.addInstruction(new BytecodeInstructionILOAD((byte) 2));
                    break;
                }
                case 29: { // iload_3 = 29 (0x1d)
                    theResult.addInstruction(new BytecodeInstructionILOAD((byte) 3));
                    break;
                }
                case 42: {// aload_0 (0x2a)
                    theResult.addInstruction(new BytecodeInstructionALOAD((byte) 0));
                    break;
                }
                case 43: {// aload_1 = 43 (0x2b)
                    theResult.addInstruction(new BytecodeInstructionALOAD((byte) 1));
                    break;
                }
                case 44: { // aload_2 = 44 (0x2c)
                    theResult.addInstruction(new BytecodeInstructionALOAD((byte) 2));
                    break;
                }
                case 45: {// aload_3 = 45 (0x2d)
                    theResult.addInstruction(new BytecodeInstructionALOAD((byte) 3));
                    break;
                }
                case 54: { // istore = 54 (0x36)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionISTORE(theIndex));
                    break;
                }
                case 58: {
                     // astore = 58 (0x3a)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionASTORE(theIndex));
                    break;
                }
                case 59: { //istore_0 = 59 (0x3b)
                    theResult.addInstruction(new BytecodeInstructionISTORE((byte) 0));
                    break;
                }
                case 60: { // istore_1 = 60 (0x3c)
                    theResult.addInstruction(new BytecodeInstructionISTORE((byte) 1));
                    break;
                }
                case 61: { //istore_2 = 61 (0x3d)
                    theResult.addInstruction(new BytecodeInstructionISTORE((byte) 2));
                    break;
                }
                case 62: { // istore_3 = 62 (0x3e)
                    theResult.addInstruction(new BytecodeInstructionISTORE((byte) 3));
                    break;
                }
                case 89: { // dup = 89 (0x59)
                    theResult.addInstruction(new BytecodeInstructionDUP());
                    break;
                }
                case 96: { // iadd = 96 (0x60)
                    theResult.addInstruction(new BytecodeInstructionIADD());
                    break;
                }
                case 159: { // if_icmpeq = 159 (0x9f)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionICMP(BytecodeInstructionICMP.Type.eq, theBranchByte1, theBranchByte2));
                    break;
                }
                case 160: { // if_icmpne = 160 (0xa0)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionICMP(BytecodeInstructionICMP.Type.ne, theBranchByte1, theBranchByte1));
                    break;
                }
                case 161: { // if_icmplt = 161 (0xa1)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionICMP(BytecodeInstructionICMP.Type.lt, theBranchByte1, theBranchByte2));
                    break;
                }
                case 162: { // if_icmpge = 162 (0xa2)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionICMP(BytecodeInstructionICMP.Type.ge, theBranchByte1, theBranchByte2));
                    break;
                }
                case 163: { // if_icmpgt = 163 (0xa3)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionICMP(BytecodeInstructionICMP.Type.gt, theBranchByte1, theBranchByte2));
                    break;
                }
                case 164: { // if_icmple = 164 (0xa4)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionICMP(BytecodeInstructionICMP.Type.le, theBranchByte1, theBranchByte2));
                    break;
                }
                case 172: { // ireturn = 172 (0xac)
                    theResult.addInstruction(new BytecodeInstructionIRETURN());
                    break;
                }
                case 177: {// return = 177 (0xb1)
                    theResult.addInstruction(new BytecodeInstructionRETURN());
                    break;
                }
                case 180: { // getfield = 180 (0xb4)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGETFIELD(theIndexByte1, theIndexByte2));
                    break;
                }
                case 181: { // putfield = 181 (0xb5)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionPUTFIELD(theIndexByte1, theIndexByte2));
                    break;
                }
                case 182: { // invokevirtual = 182 (0xb6)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKEVIRTUAL(theIndexByte1, theIndexByte2));
                    break;
                }
                case 183: {//invokespecial = 183 (0xb7)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKESPECIAL(theIndexByte1, theIndexByte2));
                    break;
                }
                case 187: { // new = 187 (0xbb)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionNEW(theIndexByte1, theIndexByte2));
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown opcode : " + theOpcode);
            }
        }
        return theResult;
    }
}