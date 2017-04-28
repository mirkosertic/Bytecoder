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

public class Bytecode5xProgrammParser implements BytecodeProgrammParser {

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
                case 1: { // aconst_null = 1 (0x1)
                    theResult.addInstruction(new BytecodeInstructionACONSTNULL());
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
                case 9: { // lconst_0 = 9 (0x9)
                    theResult.addInstruction(new BytecodeInstructionLCONST(0l));
                    break;
                }
                case 10: { // lconst_1 = 10 (0xa)
                    theResult.addInstruction(new BytecodeInstructionLCONST(1l));
                    break;
                }
                case 11: { // fconst_0 = 11 (0xb)
                    theResult.addInstruction(new BytecodeInstructionFCONST(0f));
                    break;
                }
                case 12: { // fconst_1 = 12 (0xc)
                    theResult.addInstruction(new BytecodeInstructionFCONST(1f));
                    break;
                }
                case 13: { // fconst_2 = 13 (0xd)
                    theResult.addInstruction(new BytecodeInstructionFCONST(2f));
                    break;
                }
                case 14: { // i2b = 145 (0x91)
                    theResult.addInstruction(new BytecodeInstructionI2B());
                    break;
                }
                case 16: { // bipush = 16 (0x10)
                    byte theValue = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionBIPUSH(theValue));
                    break;
                }
                case 17: { // sipush = 17 (0x11)
                    byte theByte1 = aBytecodes[offset++];
                    byte theByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionSIPUSH(theByte1, theByte2));
                    break;
                }
                case 18: { //ldc = 18 (0x12)
                    byte theValue = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLDC(theValue));
                    break;
                }
                case 20: { //ldc2_w = 20 (0x14)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLDC2W(theIndexByte1, theIndexByte2));
                    break;
                }
                case 21: { // iload = 21 (0x15)
                    byte theIndexByte1 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionILOAD(theIndexByte1));
                    break;
                }
                case 22: { // lload = 22 (0x16)
                    byte theIndexByte1 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLLOAD(theIndexByte1));
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
                case 30: { // lload_0 = 30 (0x1e)
                    theResult.addInstruction(new BytecodeInstructionLLOAD((byte) 0));
                    break;
                }
                case 31: { // lload_1 = 31 (0x1f)
                    theResult.addInstruction(new BytecodeInstructionLLOAD((byte) 1));
                    break;
                }
                case 32: { // lload_2 = 32 (0x20)
                    theResult.addInstruction(new BytecodeInstructionLLOAD((byte) 2));
                    break;
                }
                case 33: { // lload_3 = 33 (0x21)
                    theResult.addInstruction(new BytecodeInstructionLLOAD((byte) 3));
                    break;
                }
                case 34: { // fload_0 = 34 (0x22)
                    theResult.addInstruction(new BytecodeInstructionFLOAD((byte) 0));
                    break;
                }
                case 35: { // fload_1 = 35 (0x23)
                    theResult.addInstruction(new BytecodeInstructionFLOAD((byte) 1));
                    break;
                }
                case 36: { // fload_2 = 36 (0x24)
                    theResult.addInstruction(new BytecodeInstructionFLOAD((byte) 2));
                    break;
                }
                case 37: { // fload_3 = 37 (0x25)
                    theResult.addInstruction(new BytecodeInstructionFLOAD((byte) 3));
                    break;
                }
                case 38: { // dload_0 = 38 (0x26)
                    theResult.addInstruction(new BytecodeInstructionDLOAD((byte) 0));
                    break;
                }
                case 39: { // dload_1 = 39 (0x27)
                    theResult.addInstruction(new BytecodeInstructionDLOAD((byte) 1));
                    break;
                }
                case 40: { // dload_2 = 40 (0x28)
                    theResult.addInstruction(new BytecodeInstructionDLOAD((byte) 2));
                    break;
                }
                case 41: { // dload_3 = 41 (0x29)
                    theResult.addInstruction(new BytecodeInstructionDLOAD((byte) 3));
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
                case 46: {// iaload = 46 (0x2e)
                    theResult.addInstruction(new BytecodeInstructionIALOAD());
                    break;
                }
                case 50: { // aaload = 50 (0x32)
                    theResult.addInstruction(new BytecodeInstructionAALOAD());
                    break;
                }
                case 51: { //baload = 51 (0x33)
                    theResult.addInstruction(new BytecodeInstructionBALOAD());
                    break;
                }
                case 52: { // caload = 52 (0x34)
                    theResult.addInstruction(new BytecodeInstructionCALOAD());
                    break;
                }
                case 54: { // istore = 54 (0x36)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionISTORE(theIndex));
                    break;
                }
                case 55: { // lstore = 55 (0x37)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLSTORE(theIndex));
                    break;
                }
                case 57: { // dstore = 57 (0x39)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionDSTORE(theIndex));
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
                case 63: { // lstore_0 = 63 (0x3f)
                    theResult.addInstruction(new BytecodeInstructionLSTORE((byte) 0));
                    break;
                }
                case 64: { // lstore_1 = 64 (0x40)
                    theResult.addInstruction(new BytecodeInstructionLSTORE((byte) 1));
                    break;
                }
                case 65: { // lstore_2 = 65 (0x41)
                    theResult.addInstruction(new BytecodeInstructionLSTORE((byte) 2));
                    break;
                }
                case 66: { // lstore_3 = 66 (0x42)
                    theResult.addInstruction(new BytecodeInstructionLSTORE((byte) 3));
                    break;
                }
                case 71: { // dstore_0 = 71 (0x47)
                    theResult.addInstruction(new BytecodeInstructionDSTORE((byte) 0));
                    break;
                }
                case 72: { // dstore_1 = 72 (0x48)
                    theResult.addInstruction(new BytecodeInstructionDSTORE((byte) 1));
                    break;
                }
                case 73: { // dstore_2 = 73 (0x49)
                    theResult.addInstruction(new BytecodeInstructionDSTORE((byte) 2));
                    break;
                }
                case 74: { // dstore_3 = 74 (0x4a)
                    theResult.addInstruction(new BytecodeInstructionDSTORE((byte) 2));
                    break;
                }
                case 75: { // astore_0 = 75 (0x4b)
                    theResult.addInstruction(new BytecodeInstructionASTORE((byte) 0));
                    break;
                }
                case 76: { // astore_1 = 76 (0x4c)
                    theResult.addInstruction(new BytecodeInstructionASTORE((byte) 1));
                    break;
                }
                case 77: { // astore_2 = 77 (0x4d)
                    theResult.addInstruction(new BytecodeInstructionASTORE((byte) 2));
                    break;
                }
                case 78: { // astore_3 = 78 (0x4e)
                    theResult.addInstruction(new BytecodeInstructionASTORE((byte) 3));
                    break;
                }
                case 83: { // aastore = 83 (0x53)
                    theResult.addInstruction(new BytecodeInstructionAASTORE());
                    break;
                }
                case 85: { // castore = 85 (0x55)
                    theResult.addInstruction(new BytecodeInstructionCASTORE());
                    break;
                }
                case 84: { // bastore = 84 (0x54)
                    theResult.addInstruction(new BytecodeInstructionBASTORE());
                    break;
                }
                case 87: { // pop = 87 (0x57)
                    theResult.addInstruction(new BytecodeInstructionPOP());
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
                case 97: { // ladd = 97 (0x61)
                    theResult.addInstruction(new BytecodeInstructionLADD());
                    break;
                }
                case 100: { // isub = 100 (0x64)
                    theResult.addInstruction(new BytecodeInstructionISUB());
                    break;
                }
                case 101: { // lsub = 101 (0x65)
                    theResult.addInstruction(new BytecodeInstructionLSUB());
                    break;
                }
                case 102: { // fsub = 102 (0x66)
                    theResult.addInstruction(new BytecodeInstructionFSUB());
                    break;
                }
                case 103: { // dsub = 103 (0x67)
                    theResult.addInstruction(new BytecodeInstructionDSUB());
                    break;
                }
                case 104: { // imul = 104 (0x68)
                    theResult.addInstruction(new BytecodeInstructionIMUL());
                    break;
                }
                case 105: { // lmul = 105 (0x69)
                    theResult.addInstruction(new BytecodeInstructionLMUL());
                    break;
                }
                case 107: { // dmul = 107 (0x6b)
                    theResult.addInstruction(new BytecodeInstructionDMUL());
                    break;
                }
                case 108: { // idiv = 108 (0x6c)
                    theResult.addInstruction(new BytecodeInstructionIDIV());
                    break;
                }
                case 109: { // ldiv = 109 (0x6d)
                    theResult.addInstruction(new BytecodeInstructionLDIV());
                    break;
                }
                case 111: {// ddiv = 111 (0x6f)
                    theResult.addInstruction(new BytecodeInstructionDDIV());
                    break;
                }
                case 116: {// ineg = 116 (0x74)
                    theResult.addInstruction(new BytecodeInstructionINEG());
                    break;
                }
                case 117: {// lneg = 117 (0x75)
                    theResult.addInstruction(new BytecodeInstructionLNEG());
                    break;
                }
                case 120: { // ishl = 120 (0x78)
                    theResult.addInstruction(new BytecodeInstructionISHL());
                    break;
                }
                case 122: { // ishr = 122 (0x7a)
                    theResult.addInstruction(new BytecodeInstructionISHR());
                    break;
                }
                case 123: { // lshr = 123 (0x7b)
                    theResult.addInstruction(new BytecodeInstructionLSHR());
                    break;
                }
                case 125: { // lushr = 125 (0x7d)
                    theResult.addInstruction(new BytecodeInstructionLUSHR());
                    break;
                }
                case 126: { // iand = 126 (0x7e)
                    theResult.addInstruction(new BytecodeInstructionIAND());
                    break;
                }
                case 127: { // land = 127 (0x7f)
                    theResult.addInstruction(new BytecodeInstructionLAND());
                    break;
                }
                case 128: { // ior = 128 (0x80)
                    theResult.addInstruction(new BytecodeInstructionIOR());
                    break;
                }
                case 129: { // lor = 129 (0x81)
                    theResult.addInstruction(new BytecodeInstructionLOR());
                    break;
                }
                case 130: { // ixor = 130 (0x82)
                    theResult.addInstruction(new BytecodeInstructionIXOR());
                    break;
                }
                case 131: { // lxor = 131 (0x83)
                    theResult.addInstruction(new BytecodeInstructionLXOR());
                    break;
                }
                case 132: { // iinc = 132 (0x84)
                    byte theIndex = aBytecodes[offset++];
                    byte theConstant = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIINC(theIndex, theConstant));
                    break;
                }
                case 133: { // i2l = 133 (0x85)
                    theResult.addInstruction(new BytecodeInstructionI2L());
                    break;
                }
                case 136: { // l2i = 136 (0x88)
                    theResult.addInstruction(new BytecodeInstructionL2I());
                    break;
                }
                case 139: { // f2i = 139 (0x8b)
                    theResult.addInstruction(new BytecodeInstructionF2I());
                    break;
                }
                case 143: { // d2l = 143 (0x8f)
                    theResult.addInstruction(new BytecodeInstructionD2L());
                    break;
                }
                case 145: { // i2b = 145 (0x91)
                    theResult.addInstruction(new BytecodeInstructionI2B());
                    break;
                }
                case 146: { // i2c = 146 (0x92)
                    theResult.addInstruction(new BytecodeInstructionI2C());
                    break;
                }
                case 148: { // lcmp = 148 (0x94)
                    theResult.addInstruction(new BytecodeInstructionLCMP());
                    break;
                }
                case 149: { // fcmpl = 149 (0x95)
                    theResult.addInstruction(new BytecodeInstructionFCMP(BytecodeInstructionFCMP.Type.l));
                    break;
                }
                case 150: { // fcmpg = 150 (0x96)
                    theResult.addInstruction(new BytecodeInstructionFCMP(BytecodeInstructionFCMP.Type.g));
                    break;
                }
                case 151: { // dcmpl = 151 (0x97)
                    theResult.addInstruction(new BytecodeInstructionDCMP(BytecodeInstructionDCMP.Type.l));
                    break;
                }
                case 152: { // dcmpg = 152 (0x98)
                    theResult.addInstruction(new BytecodeInstructionDCMP(BytecodeInstructionDCMP.Type.g));
                    break;
                }
                case 153: { // ifeq = 153 (0x99)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFCOND(BytecodeInstructionIFCOND.Type.eq, theBranchByte1, theBranchByte2));
                    break;
                }
                case 154: { // ifne = 154 (0x9a)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFCOND(BytecodeInstructionIFCOND.Type.ne, theBranchByte1, theBranchByte2));
                    break;
                }
                case 155: { // iflt = 155 (0x9b)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFCOND(BytecodeInstructionIFCOND.Type.lt, theBranchByte1, theBranchByte2));
                    break;
                }
                case 156: { // ifge = 156 (0x9c)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFCOND(BytecodeInstructionIFCOND.Type.ge, theBranchByte1, theBranchByte2));
                    break;
                }
                case 157: { // ifgt = 157 (0x9d)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFCOND(BytecodeInstructionIFCOND.Type.gt, theBranchByte1, theBranchByte2));
                    break;
                }
                case 158: { // ifle = 158 (0x9e)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFCOND(BytecodeInstructionIFCOND.Type.le, theBranchByte1, theBranchByte2));
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
                case 165: { // if_acmpeq = 165 (0xa5)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFACMP(BytecodeInstructionIFACMP.Type.eq, theBranchByte1, theBranchByte2));
                    break;
                }
                case 166: { // if_acmpne = 166 (0xa6)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFACMP(BytecodeInstructionIFACMP.Type.ne, theBranchByte1, theBranchByte2));
                    break;
                }
                case 167: { // goto = 167 (0xa7)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGOTO(theBranchByte1, theBranchByte2));
                    break;
                }
                case 172: { // ireturn = 172 (0xac)
                    theResult.addInstruction(new BytecodeInstructionIRETURN());
                    break;
                }
                case 173: {// lreturn = 173 (0xad)
                    theResult.addInstruction(new BytecodeInstructionLRETURN());
                    break;
                }
                case 174: {// freturn = 174 (0xae)
                    theResult.addInstruction(new BytecodeInstructionFRETURN());
                    break;
                }
                case 175: {// dreturn = 175 (0xaf)
                    theResult.addInstruction(new BytecodeInstructionDRETURN());
                    break;
                }
                case 176: {// areturn = 176 (0xb0)
                    theResult.addInstruction(new BytecodeInstructionARETURN());
                    break;
                }
                case 177: {// return = 177 (0xb1)
                    theResult.addInstruction(new BytecodeInstructionRETURN());
                    break;
                }
                case 178: { // getstatic = 178 (0xb2)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGETSTATIC(theIndexByte1, theIndexByte2));
                    break;
                }
                case 179: { // getstatic = 178 (0xb2)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionPUTSTATIC(theIndexByte1, theIndexByte2));
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
                case 184: {//invokestatic = 184 (0xb8)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKESTATIC(theIndexByte1, theIndexByte2));
                    break;
                }
                case 185: { // invokeinterface = 185 (0xb9)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    byte theCount = aBytecodes[offset++];
                    byte theNull = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKEINTERFACE(theIndexByte1, theIndexByte2, theCount));
                    break;
                }
                case 187: { // new = 187 (0xbb)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionNEW(theIndexByte1, theIndexByte2));
                    break;
                }
                case 188: { // newarray = 188 (0xbc)
                    byte theType = aBytecodes[offset++];
                    switch (theType) {
                        case 4:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_BOOLEAN));
                            break;
                        case 5:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_CHAR));
                            break;
                        case 6:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_FLOAT));
                            break;
                        case 7:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_DOUBLE));
                            break;
                        case 8:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_BYTE));
                            break;
                        case 9:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_SHORT));
                            break;
                        case 10:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_INT));
                            break;
                        case 11:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(BytecodeInstructionNEWARRAY.Type.T_LONG));
                            break;
                        default:
                            throw new IllegalStateException("Unknown array type : " + theType);
                    }
                    break;
                }
                case 189: { // anewarray = 189 (0xbd)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionANEWARRAY(theIndexByte1, theIndexByte2));
                    break;
                }
                case 190: { // arraylength = 190 (0xbe)
                    theResult.addInstruction(new BytecodeInstructionARRAYLENGTH());
                    break;
                }
                case 191: { // athrow = 191 (0xbf)
                    theResult.addInstruction(new BytecodeInstructionATHROW());
                    break;
                }
                case 192: { // checkcast = 192 (0xc0)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionCHECKCAST(theIndexByte1, theIndexByte2));
                    break;
                }
                case 193: { // instanceof = 193 (0xc1)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINSTANCEOF(theIndexByte1, theIndexByte2));
                    break;
                }
                case 194: { // monitorenter = 194 (0xc2)
                    theResult.addInstruction(new BytecodeInstructionMONITORENTER());
                    break;
                }
                case 195: { // monitorenter = 194 (0xc2)
                    theResult.addInstruction(new BytecodeInstructionMONITOREXIT());
                    break;
                }
                case 198: { // ifnull = 198 (0xc6)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFNULL(theIndexByte1, theIndexByte2));
                    break;
                }
                case 199: { // ifnonnull = 199 (0xc7)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFNONNULL(theIndexByte1, theIndexByte2));
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown opcode : " + theOpcode);
            }
        }
        return theResult;
    }
}