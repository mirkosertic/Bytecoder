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
    public BytecodeProgramm parse(byte[] aBytecodes, BytecodeConstantPool aConstantPool) {
        BytecodeProgramm theResult = new BytecodeProgramm();
        int offset = 0;
        while(offset < aBytecodes.length) {
            BytecodeOpcodeAddress theOpcodeIndex = new BytecodeOpcodeAddress(offset);
            int theOpcode = aBytecodes[offset++]  & 0xFF;
            switch (theOpcode) {
                case 0: { // nop = 0 (0x0)
                    theResult.addInstruction(new BytecodeInstructionNOP(theOpcodeIndex));
                    break;
                }
                case 1: { // aconst_null = 1 (0x1)
                    theResult.addInstruction(new BytecodeInstructionACONSTNULL(theOpcodeIndex));
                    break;
                }
                case 2: { // iconst_m1 = 2 (0x2)
                    theResult.addInstruction(new BytecodeInstructionICONST(theOpcodeIndex,-1));
                    break;
                }
                case 3: { // iconst_0 = 3 (0x3)
                    theResult.addInstruction(new BytecodeInstructionICONST(theOpcodeIndex,0));
                    break;
                }
                case 4: { // iconst_1 = 4 (0x4)
                    theResult.addInstruction(new BytecodeInstructionICONST(theOpcodeIndex,1));
                    break;
                }
                case 5: { // iconst_2 = 5 (0x5)
                    theResult.addInstruction(new BytecodeInstructionICONST(theOpcodeIndex,2));
                    break;
                }
                case 6: { // iconst_3 = 6 (0x6)
                    theResult.addInstruction(new BytecodeInstructionICONST(theOpcodeIndex,3));
                    break;
                }
                case 7: { // iconst_4 = 7 (0x7)
                    theResult.addInstruction(new BytecodeInstructionICONST(theOpcodeIndex,4));
                    break;
                }
                case 8: { // iconst_5 = 8 (0x8)
                    theResult.addInstruction(new BytecodeInstructionICONST(theOpcodeIndex,5));
                    break;
                }
                case 9: { // lconst_0 = 9 (0x9)
                    theResult.addInstruction(new BytecodeInstructionLCONST(theOpcodeIndex,0l));
                    break;
                }
                case 10: { // lconst_1 = 10 (0xa)
                    theResult.addInstruction(new BytecodeInstructionLCONST(theOpcodeIndex,1l));
                    break;
                }
                case 11: { // fconst_0 = 11 (0xb)
                    theResult.addInstruction(new BytecodeInstructionFCONST(theOpcodeIndex,0f));
                    break;
                }
                case 12: { // fconst_1 = 12 (0xc)
                    theResult.addInstruction(new BytecodeInstructionFCONST(theOpcodeIndex,1f));
                    break;
                }
                case 13: { // fconst_2 = 13 (0xd)
                    theResult.addInstruction(new BytecodeInstructionFCONST(theOpcodeIndex,2f));
                    break;
                }
                case 14: { // dconst_0 = 14 (0xe)
                    theResult.addInstruction(new BytecodeInstructionDCONST(theOpcodeIndex,0d));
                    break;
                }
                case 15: { // dconst_1 = 15 (0xf)
                    theResult.addInstruction(new BytecodeInstructionDCONST(theOpcodeIndex,1d));
                    break;
                }
                case 16: { // bipush = 16 (0x10)
                    byte theValue = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionBIPUSH(theOpcodeIndex,theValue));
                    break;
                }
                case 17: { // sipush = 17 (0x11)
                    byte theByte1 = aBytecodes[offset++];
                    byte theByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionSIPUSH(theOpcodeIndex,theByte1, theByte2));
                    break;
                }
                case 18: { //ldc = 18 (0x12)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLDC(theOpcodeIndex,theIndex, aConstantPool));
                    break;
                }
                case 19: { //ldc_w = 19 (0x13)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLDCW(theOpcodeIndex,theIndexByte1, theIndexByte2));
                    break;
                }
                case 20: { //ldc2_w = 20 (0x14)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionLDC2W(theOpcodeIndex,theIndexByte1, theIndexByte2));
                    break;
                }
                case 21: { // iload = 21 (0x15)
                    byte theIndexByte1 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex,BytecodePrimitiveTypeRef.INT, theIndexByte1));
                    break;
                }
                case 22: { // lload = 22 (0x16)
                    byte theIndexByte1 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex,BytecodePrimitiveTypeRef.LONG, theIndexByte1));
                    break;
                }
                case 23: { // fload = 23 (0x17)
                    byte theIndexByte1 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, theIndexByte1));
                    break;
                }
                case 24: { // dload = 24 (0x18)
                    byte theIndexByte1 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, theIndexByte1));
                    break;
                }
                case 25: { // aload = 25 (0x19)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, theIndex));
                    break;
                }
                case 26: { // iload_0 = 26 (0x1a)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 0));
                    break;
                }
                case 27: { //iload_1 = 27 (0x1b)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 1));
                    break;
                }
                case 28: { // iload_2 = 28 (0x1c)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 2));
                    break;
                }
                case 29: { // iload_3 = 29 (0x1d)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 3));
                    break;
                }
                case 30: { // lload_0 = 30 (0x1e)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 0));
                    break;
                }
                case 31: { // lload_1 = 31 (0x1f)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 1));
                    break;
                }
                case 32: { // lload_2 = 32 (0x20)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 2));
                    break;
                }
                case 33: { // lload_3 = 33 (0x21)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 3));
                    break;
                }
                case 34: { // fload_0 = 34 (0x22)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 0));
                    break;
                }
                case 35: { // fload_1 = 35 (0x23)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 1));
                    break;
                }
                case 36: { // fload_2 = 36 (0x24)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 2));
                    break;
                }
                case 37: { // fload_3 = 37 (0x25)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 3));
                    break;
                }
                case 38: { // dload_0 = 38 (0x26)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 0));
                    break;
                }
                case 39: { // dload_1 = 39 (0x27)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 1));
                    break;
                }
                case 40: { // dload_2 = 40 (0x28)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 2));
                    break;
                }
                case 41: { // dload_3 = 41 (0x29)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 3));
                    break;
                }
                case 42: {// aload_0 (0x2a)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, (byte) 0));
                    break;
                }
                case 43: {// aload_1 = 43 (0x2b)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, (byte) 1));
                    break;
                }
                case 44: { // aload_2 = 44 (0x2c)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, (byte) 2));
                    break;
                }
                case 45: {// aload_3 = 45 (0x2d)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, (byte) 3));
                    break;
                }
                case 46: {// iaload = 46 (0x2e)
                    theResult.addInstruction(new BytecodeInstructionIALOAD(theOpcodeIndex));
                    break;
                }
                case 47: {// laload = 47 (0x2f)
                    theResult.addInstruction(new BytecodeInstructionLALOAD(theOpcodeIndex));
                    break;
                }
                case 48: {// faload = 48 (0x30)
                    theResult.addInstruction(new BytecodeInstructionFALOAD(theOpcodeIndex));
                    break;
                }
                case 49: {// daload = 49 (0x31)
                    theResult.addInstruction(new BytecodeInstructionDALOAD(theOpcodeIndex));
                    break;
                }
                case 50: { // aaload = 50 (0x32)
                    theResult.addInstruction(new BytecodeInstructionAALOAD(theOpcodeIndex));
                    break;
                }
                case 51: { //baload = 51 (0x33)
                    theResult.addInstruction(new BytecodeInstructionBALOAD(theOpcodeIndex));
                    break;
                }
                case 52: { // caload = 52 (0x34)
                    theResult.addInstruction(new BytecodeInstructionCALOAD(theOpcodeIndex));
                    break;
                }
                case 53: { // saload = 53 (0x35)
                    theResult.addInstruction(new BytecodeInstructionSALOAD(theOpcodeIndex));
                    break;
                }
                case 54: { // istore = 54 (0x36)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, theIndex));
                    break;
                }
                case 55: { // lstore = 55 (0x37)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, theIndex));
                    break;
                }
                case 56: { // fstore = 56 (0x38)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    break;
                }
                case 57: { // dstore = 57 (0x39)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    break;
                }
                case 58: { // astore = 58 (0x3a)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, theIndex));
                    break;
                }
                case 59: { //istore_0 = 59 (0x3b)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 0));
                    break;
                }
                case 60: { // istore_1 = 60 (0x3c)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 1));
                    break;
                }
                case 61: { //istore_2 = 61 (0x3d)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 2));
                    break;
                }
                case 62: { // istore_3 = 62 (0x3e)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, (byte) 3));
                    break;
                }
                case 63: { // lstore_0 = 63 (0x3f)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 0));
                    break;
                }
                case 64: { // lstore_1 = 64 (0x40)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 1));
                    break;
                }
                case 65: { // lstore_2 = 65 (0x41)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 2));
                    break;
                }
                case 66: { // lstore_3 = 66 (0x42)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, (byte) 3));
                    break;
                }
                case 67: { // fstore_0 = 67 (0x43)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 0));
                    break;
                }
                case 68: { // fstore_1 = 68 (0x44)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 1));
                    break;
                }
                case 69: { // fstore_2 = 69 (0x45)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 2));
                    break;
                }
                case 70: { // fstore_3 = 70 (0x46)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, (byte) 3));
                    break;
                }
                case 71: { // dstore_0 = 71 (0x47)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 0));
                    break;
                }
                case 72: { // dstore_1 = 72 (0x48)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 1));
                    break;
                }
                case 73: { // dstore_2 = 73 (0x49)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 2));
                    break;
                }
                case 74: { // dstore_3 = 74 (0x4a)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, (byte) 2));
                    break;
                }
                case 75: { // astore_0 = 75 (0x4b)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, (byte) 0));
                    break;
                }
                case 76: { // astore_1 = 76 (0x4c)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, (byte) 1));
                    break;
                }
                case 77: { // astore_2 = 77 (0x4d)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, (byte) 2));
                    break;
                }
                case 78: { // astore_3 = 78 (0x4e)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, (byte) 3));
                    break;
                }
                case 79: { // iastore = 79 (0x4f)
                    theResult.addInstruction(new BytecodeInstructionIASTORE(theOpcodeIndex));
                    break;
                }
                case 80: { // lastore = 80 (0x50)
                    theResult.addInstruction(new BytecodeInstructionLASTORE(theOpcodeIndex));
                    break;
                }
                case 81: { // fastore = 81 (0x51)
                    theResult.addInstruction(new BytecodeInstructionFASTORE(theOpcodeIndex));
                    break;
                }
                case 82: { // dastore = 82 (0x52)
                    theResult.addInstruction(new BytecodeInstructionDASTORE(theOpcodeIndex));
                    break;
                }
                case 83: { // aastore = 83 (0x53)
                    theResult.addInstruction(new BytecodeInstructionAASTORE(theOpcodeIndex));
                    break;
                }
                case 84: { // bastore = 84 (0x54)
                    theResult.addInstruction(new BytecodeInstructionBASTORE(theOpcodeIndex));
                    break;
                }
                case 85: { // castore = 85 (0x55)
                    theResult.addInstruction(new BytecodeInstructionCASTORE(theOpcodeIndex));
                    break;
                }
                case 86: { // sastore = 86 (0x56)
                    theResult.addInstruction(new BytecodeInstructionSASTORE(theOpcodeIndex));
                    break;
                }
                case 87: { // pop = 87 (0x57)
                    theResult.addInstruction(new BytecodeInstructionPOP(theOpcodeIndex));
                    break;
                }
                case 88: { // pop2 = 88 (0x58)
                    theResult.addInstruction(new BytecodeInstructionPOP2(theOpcodeIndex));
                    break;
                }
                case 89: { // dup = 89 (0x59)
                    theResult.addInstruction(new BytecodeInstructionDUP(theOpcodeIndex));
                    break;
                }
                case 90: { // dup_x1 = 90 (0x5a)
                    theResult.addInstruction(new BytecodeInstructionDUPX1(theOpcodeIndex));
                    break;
                }
                case 91: { // dup_x2 = 91 (0x5b)
                    theResult.addInstruction(new BytecodeInstructionDUPX2(theOpcodeIndex));
                    break;
                }
                case 92: { // dup2 = 92 (0x5c)
                    theResult.addInstruction(new BytecodeInstructionDUP2(theOpcodeIndex));
                    break;
                }
                case 93: { // dup2_x1 = 93 (0x5d)
                    theResult.addInstruction(new BytecodeInstructionDUP2X1(theOpcodeIndex));
                    break;
                }
                case 94: { // dup2_x2 = 94 (0x5e)
                    theResult.addInstruction(new BytecodeInstructionDUP2X2(theOpcodeIndex));
                    break;
                }
                case 95: { // swap = 95 (0x5f)
                    theResult.addInstruction(new BytecodeInstructionSWAP(theOpcodeIndex));
                    break;
                }
                case 96: { // iadd = 96 (0x60)
                    theResult.addInstruction(new BytecodeInstructionGenericADD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 97: { // ladd = 97 (0x61)
                    theResult.addInstruction(new BytecodeInstructionGenericADD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 98: { // fadd = 98 (0x62)
                    theResult.addInstruction(new BytecodeInstructionGenericADD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 99: { // ladd = 97 (0x61)
                    theResult.addInstruction(new BytecodeInstructionGenericADD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 100: { // isub = 100 (0x64)
                    theResult.addInstruction(new BytecodeInstructionGenericSUB(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 101: { // lsub = 101 (0x65)
                    theResult.addInstruction(new BytecodeInstructionGenericSUB(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 102: { // fsub = 102 (0x66)
                    theResult.addInstruction(new BytecodeInstructionGenericSUB(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 103: { // dsub = 103 (0x67)
                    theResult.addInstruction(new BytecodeInstructionGenericSUB(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 104: { // imul = 104 (0x68)
                    theResult.addInstruction(new BytecodeInstructionGenericMUL(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 105: { // lmul = 105 (0x69)
                    theResult.addInstruction(new BytecodeInstructionGenericMUL(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 106: { // fmul = 106 (0x6a)
                    theResult.addInstruction(new BytecodeInstructionGenericMUL(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 107: { // dmul = 107 (0x6b)
                    theResult.addInstruction(new BytecodeInstructionGenericMUL(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 108: { // idiv = 108 (0x6c)
                    theResult.addInstruction(new BytecodeInstructionGenericDIV(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 109: { // ldiv = 109 (0x6d)
                    theResult.addInstruction(new BytecodeInstructionGenericDIV(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 110: { // fdiv = 110 (0x6e)
                    theResult.addInstruction(new BytecodeInstructionGenericDIV(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 111: {// ddiv = 111 (0x6f)
                    theResult.addInstruction(new BytecodeInstructionGenericDIV(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 112: {// irem = 112 (0x70)
                    theResult.addInstruction(new BytecodeInstructionIREM(theOpcodeIndex));
                    break;
                }
                case 113: {// lrem = 113 (0x71)
                    theResult.addInstruction(new BytecodeInstructionLREM(theOpcodeIndex));
                    break;
                }
                case 114: {// frem = 114 (0x72)
                    theResult.addInstruction(new BytecodeInstructionFREM(theOpcodeIndex));
                    break;
                }
                case 115: {// drem = 115 (0x73)
                    theResult.addInstruction(new BytecodeInstructionDREM(theOpcodeIndex));
                    break;
                }
                case 116: {// ineg = 116 (0x74)
                    theResult.addInstruction(new BytecodeInstructionINEG(theOpcodeIndex));
                    break;
                }
                case 117: {// lneg = 117 (0x75)
                    theResult.addInstruction(new BytecodeInstructionLNEG(theOpcodeIndex));
                    break;
                }
                case 118: {// fneg = 118 (0x76)
                    theResult.addInstruction(new BytecodeInstructionFNEG(theOpcodeIndex));
                    break;
                }
                case 119: {// dneg = 119 (0x77)
                    theResult.addInstruction(new BytecodeInstructionDNEG(theOpcodeIndex));
                    break;
                }
                case 120: { // ishl = 120 (0x78)
                    theResult.addInstruction(new BytecodeInstructionISHL(theOpcodeIndex));
                    break;
                }
                case 121: { // lshl = 121 (0x79)
                    theResult.addInstruction(new BytecodeInstructionISHL(theOpcodeIndex));
                    break;
                }
                case 122: { // ishr = 122 (0x7a)
                    theResult.addInstruction(new BytecodeInstructionISHR(theOpcodeIndex));
                    break;
                }
                case 123: { // lshr = 123 (0x7b)
                    theResult.addInstruction(new BytecodeInstructionLSHR(theOpcodeIndex));
                    break;
                }
                case 124: { // iushr = 124 (0x7c)
                    theResult.addInstruction(new BytecodeInstructionIUSHR(theOpcodeIndex));
                    break;
                }
                case 125: { // lushr = 125 (0x7d)
                    theResult.addInstruction(new BytecodeInstructionLUSHR(theOpcodeIndex));
                    break;
                }
                case 126: { // iand = 126 (0x7e)
                    theResult.addInstruction(new BytecodeInstructionIAND(theOpcodeIndex));
                    break;
                }
                case 127: { // land = 127 (0x7f)
                    theResult.addInstruction(new BytecodeInstructionLAND(theOpcodeIndex));
                    break;
                }
                case 128: { // ior = 128 (0x80)
                    theResult.addInstruction(new BytecodeInstructionIOR(theOpcodeIndex));
                    break;
                }
                case 129: { // lor = 129 (0x81)
                    theResult.addInstruction(new BytecodeInstructionLOR(theOpcodeIndex));
                    break;
                }
                case 130: { // ixor = 130 (0x82)
                    theResult.addInstruction(new BytecodeInstructionIXOR(theOpcodeIndex));
                    break;
                }
                case 131: { // lxor = 131 (0x83)
                    theResult.addInstruction(new BytecodeInstructionLXOR(theOpcodeIndex));
                    break;
                }
                case 132: { // iinc = 132 (0x84)
                    byte theIndex = aBytecodes[offset++];
                    byte theConstant = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIINC(theOpcodeIndex, theIndex, theConstant));
                    break;
                }
                case 133: { // i2l = 133 (0x85)
                    theResult.addInstruction(new BytecodeInstructionI2L(theOpcodeIndex));
                    break;
                }
                case 134: { // i2f = 134 (0x86)
                    theResult.addInstruction(new BytecodeInstructionI2F(theOpcodeIndex));
                    break;
                }
                case 135: { // i2d = 135 (0x87)
                    theResult.addInstruction(new BytecodeInstructionI2D(theOpcodeIndex));
                    break;
                }
                case 136: { // l2i = 136 (0x88)
                    theResult.addInstruction(new BytecodeInstructionL2I(theOpcodeIndex));
                    break;
                }
                case 137: { // l2f = 137 (0x89)
                    theResult.addInstruction(new BytecodeInstructionL2F(theOpcodeIndex));
                    break;
                }
                case 138: { // l2d = 138 (0x8a)
                    theResult.addInstruction(new BytecodeInstructionL2D(theOpcodeIndex));
                    break;
                }
                case 139: { // f2i = 139 (0x8b)
                    theResult.addInstruction(new BytecodeInstructionF2I(theOpcodeIndex));
                    break;
                }
                case 140: { // f2l = 140 (0x8c)
                    theResult.addInstruction(new BytecodeInstructionF2L(theOpcodeIndex));
                    break;
                }
                case 141: { // f2d = 141 (0x8d)
                    theResult.addInstruction(new BytecodeInstructionF2D(theOpcodeIndex));
                    break;
                }
                case 142: { // d2i = 142 (0x8e)
                    theResult.addInstruction(new BytecodeInstructionF2I(theOpcodeIndex));
                    break;
                }
                case 143: { // d2l = 143 (0x8f)
                    theResult.addInstruction(new BytecodeInstructionD2L(theOpcodeIndex));
                    break;
                }
                case 144: { // d2f = 144 (0x90)
                    theResult.addInstruction(new BytecodeInstructionD2F(theOpcodeIndex));
                    break;
                }
                case 145: { // i2b = 145 (0x91)
                    theResult.addInstruction(new BytecodeInstructionI2B(theOpcodeIndex));
                    break;
                }
                case 146: { // i2c = 146 (0x92)
                    theResult.addInstruction(new BytecodeInstructionI2C(theOpcodeIndex));
                    break;
                }
                case 147: { // i2s = 147 (0x93)
                    theResult.addInstruction(new BytecodeInstructionI2S(theOpcodeIndex));
                    break;
                }
                case 148: { // lcmp = 148 (0x94)
                    theResult.addInstruction(new BytecodeInstructionLCMP(theOpcodeIndex));
                    break;
                }
                case 149: { // fcmpl = 149 (0x95)
                    theResult.addInstruction(new BytecodeInstructionFCMP(theOpcodeIndex, BytecodeInstructionFCMP.Type.l));
                    break;
                }
                case 150: { // fcmpg = 150 (0x96)
                    theResult.addInstruction(new BytecodeInstructionFCMP(theOpcodeIndex, BytecodeInstructionFCMP.Type.g));
                    break;
                }
                case 151: { // dcmpl = 151 (0x97)
                    theResult.addInstruction(new BytecodeInstructionDCMP(theOpcodeIndex, BytecodeInstructionDCMP.Type.l));
                    break;
                }
                case 152: { // dcmpg = 152 (0x98)
                    theResult.addInstruction(new BytecodeInstructionDCMP(theOpcodeIndex, BytecodeInstructionDCMP.Type.g));
                    break;
                }
                case 153: { // ifeq = 153 (0x99)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.eq, theIndex));
                    break;
                }
                case 154: { // ifne = 154 (0x9a)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.ne, theIndex));
                    break;
                }
                case 155: { // iflt = 155 (0x9b)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.lt, theIndex));
                    break;
                }
                case 156: { // ifge = 156 (0x9c)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.ge, theIndex));
                    break;
                }
                case 157: { // ifgt = 157 (0x9d)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.gt, theIndex));
                    break;
                }
                case 158: { // ifle = 158 (0x9e)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.le, theIndex));
                    break;
                }
                case 159: { // if_icmpeq = 159 (0x9f)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionICMP(theOpcodeIndex, BytecodeInstructionICMP.Type.eq, theIndex));
                    break;
                }
                case 160: { // if_icmpne = 160 (0xa0)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionICMP(theOpcodeIndex, BytecodeInstructionICMP.Type.ne, theIndex));
                    break;
                }
                case 161: { // if_icmplt = 161 (0xa1)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionICMP(theOpcodeIndex, BytecodeInstructionICMP.Type.lt, theIndex));
                    break;
                }
                case 162: { // if_icmpge = 162 (0xa2)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionICMP(theOpcodeIndex, BytecodeInstructionICMP.Type.ge, theIndex));
                    break;
                }
                case 163: { // if_icmpgt = 163 (0xa3)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionICMP(theOpcodeIndex, BytecodeInstructionICMP.Type.gt, theIndex));
                    break;
                }
                case 164: { // if_icmple = 164 (0xa4)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionICMP(theOpcodeIndex, BytecodeInstructionICMP.Type.le, theIndex));
                    break;
                }
                case 165: { // if_acmpeq = 165 (0xa5)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFACMP(theOpcodeIndex, BytecodeInstructionIFACMP.Type.eq, theBranchByte1, theBranchByte2));
                    break;
                }
                case 166: { // if_acmpne = 166 (0xa6)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionIFACMP(theOpcodeIndex, BytecodeInstructionIFACMP.Type.ne, theBranchByte1, theBranchByte2));
                    break;
                }
                case 167: { // goto = 167 (0xa7)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionGOTO(theOpcodeIndex, theIndex));
                    break;
                }
                case 168: { // jsr = 168 (0xa8)
                    byte theBranchByte1 = aBytecodes[offset++];
                    byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionJSR(theOpcodeIndex, theBranchByte1, theBranchByte2));
                    break;
                }
                case 169: { // ret = 169 (0xa9)
                    byte theIndex = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionRET(theOpcodeIndex, theIndex));
                    break;
                }
                case 170: { // tableswitch = 170 (0xaa)
                    offset +=  4 - offset % 4;

                    long theDefault = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    long theLow = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    long theHigh = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    long[] theOffsets = new long[(int)(theHigh - theLow + 1)];
                    long theNumOffsets = theHigh - theLow + 1;
                    for (int i=0;i<theNumOffsets;i++) {

                        long theOffset = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                        offset+=4;

                        theOffsets[i] = theOffset;
                    }

                    theResult.addInstruction(new BytecodeInstructionTABLESWITCH(theOpcodeIndex, theDefault, theLow, theHigh, theOffsets));
                    break;
                }
                case 171: { // lookupswitch = 171 (0xab)

                    // Skip padding
                    offset +=  4 - offset % 4;

                    long theDefault = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    long theNumPairs = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    BytecodeInstructionLOOKUPSWITCH.Pair thePairs[] = new BytecodeInstructionLOOKUPSWITCH.Pair[(int) theNumPairs];

                    for (long i=0; i<theNumPairs; i++) {
                        int theMatch = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                        offset+=2;

                        long theOffset = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                        offset+=4;

                        thePairs[(int) i] = new BytecodeInstructionLOOKUPSWITCH.Pair(theMatch, theOffset);
                    }

                    theResult.addInstruction(new BytecodeInstructionLOOKUPSWITCH(theOpcodeIndex, theDefault, thePairs));
                    break;
                }
                case 172: { // ireturn = 172 (0xac)
                    theResult.addInstruction(new BytecodeInstructionGenericRETURN(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 173: {// lreturn = 173 (0xad)
                    theResult.addInstruction(new BytecodeInstructionGenericRETURN(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 174: {// freturn = 174 (0xae)
                    theResult.addInstruction(new BytecodeInstructionGenericRETURN(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 175: {// dreturn = 175 (0xaf)
                    theResult.addInstruction(new BytecodeInstructionGenericRETURN(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 176: {// areturn = 176 (0xb0)
                    theResult.addInstruction(new BytecodeInstructionARETURN(theOpcodeIndex));
                    break;
                }
                case 177: {// return = 177 (0xb1)
                    theResult.addInstruction(new BytecodeInstructionRETURN(theOpcodeIndex));
                    break;
                }
                case 178: { // getstatic = 178 (0xb2)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGETSTATIC(theOpcodeIndex, theIndexByte1, theIndexByte2));
                    break;
                }
                case 179: { // getstatic = 178 (0xb2)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionPUTSTATIC(theOpcodeIndex, theIndexByte1, theIndexByte2));
                    break;
                }
                case 180: { // getfield = 180 (0xb4)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionGETFIELD(theOpcodeIndex, theIndexByte1, theIndexByte2));
                    break;
                }
                case 181: { // putfield = 181 (0xb5)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionPUTFIELD(theOpcodeIndex, theIndexByte1, theIndexByte2));
                    break;
                }
                case 182: { // invokevirtual = 182 (0xb6)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionINVOKEVIRTUAL(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 183: {//invokespecial = 183 (0xb7)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionINVOKESPECIAL(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 184: {//invokestatic = 184 (0xb8)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionINVOKESTATIC(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 185: { // invokeinterface = 185 (0xb9)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    byte theCount = aBytecodes[offset++];
                    byte theNull = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKEINTERFACE(theOpcodeIndex, theIndexByte1, theIndexByte2, theCount));
                    break;
                }
                case 186: { // invokedynamic = 186 (0xba)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    byte theNull1 = aBytecodes[offset++];
                    byte theNull2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKEDYNAMIC(theOpcodeIndex, theIndexByte1, theIndexByte2));
                    break;
                }
                case 187: { // new = 187 (0xbb)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionNEW(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 188: { // newarray = 188 (0xbc)
                    byte theType = aBytecodes[offset++];
                    switch (theType) {
                        case 4:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_BOOLEAN));
                            break;
                        case 5:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_CHAR));
                            break;
                        case 6:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_FLOAT));
                            break;
                        case 7:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_DOUBLE));
                            break;
                        case 8:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_BYTE));
                            break;
                        case 9:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_SHORT));
                            break;
                        case 10:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_INT));
                            break;
                        case 11:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodeInstructionNEWARRAY.Type.T_LONG));
                            break;
                        default:
                            throw new IllegalStateException("Unknown array type : " + theType);
                    }
                    break;
                }
                case 189: { // anewarray = 189 (0xbd)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionANEWARRAY(theOpcodeIndex, theIndexByte1, theIndexByte2));
                    break;
                }
                case 190: { // arraylength = 190 (0xbe)
                    theResult.addInstruction(new BytecodeInstructionARRAYLENGTH(theOpcodeIndex));
                    break;
                }
                case 191: { // athrow = 191 (0xbf)
                    theResult.addInstruction(new BytecodeInstructionATHROW(theOpcodeIndex));
                    break;
                }
                case 192: { // checkcast = 192 (0xc0)
                    int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionCHECKCAST(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 193: { // instanceof = 193 (0xc1)
                    byte theIndexByte1 = aBytecodes[offset++];
                    byte theIndexByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINSTANCEOF(theOpcodeIndex, theIndexByte1, theIndexByte2));
                    break;
                }
                case 194: { // monitorenter = 194 (0xc2)
                    theResult.addInstruction(new BytecodeInstructionMONITORENTER(theOpcodeIndex));
                    break;
                }
                case 195: { // monitorenter = 194 (0xc2)
                    theResult.addInstruction(new BytecodeInstructionMONITOREXIT(theOpcodeIndex));
                    break;
                }
                // TODO: 196
                // TODO: 197
                case 198: { // ifnull = 198 (0xc6)
                    int theOffset = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFNULL(theOpcodeIndex, theOffset));
                    break;
                }
                case 199: { // ifnonnull = 199 (0xc7)
                    int theOffset = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFNONNULL(theOpcodeIndex, theOffset));
                    break;
                }
                // TODO: 200
                // TODO: 201
                default:
                    throw new IllegalStateException("Unknown opcode : " + theOpcode);
            }
        }
        return theResult;
    }
}