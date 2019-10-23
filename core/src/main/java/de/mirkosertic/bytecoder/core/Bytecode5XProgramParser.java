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

public class Bytecode5XProgramParser implements BytecodeProgramParser {

    @Override
    public BytecodeProgram parse(final byte[] aBytecodes, final BytecodeConstantPool aConstantPool) {
        final BytecodeProgram theResult = new BytecodeProgram();
        int offset = 0;
        boolean wide = false;
        boolean isWideOperator;
        while(offset < aBytecodes.length) {
            final BytecodeOpcodeAddress theOpcodeIndex = new BytecodeOpcodeAddress(offset);
            final int theOpcode = aBytecodes[offset++]  & 0xFF;
            isWideOperator = wide;
            wide = false;
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
                    final byte theValue = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionBIPUSH(theOpcodeIndex,theValue));
                    break;
                }
                case 17: { // sipush = 17 (0x11)
                    final short theShortValue = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionSIPUSH(theOpcodeIndex, theShortValue));
                    break;
                }
                case 18: { //ldc = 18 (0x12)
                    final int theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                    offset++;
                    theResult.addInstruction(new BytecodeInstructionGenericLDC(theOpcodeIndex,theIndex, aConstantPool));
                    break;
                }
                case 19: { //ldc_w = 19 (0x13)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionGenericLDC(theOpcodeIndex,theIndex, aConstantPool));
                    break;
                }
                case 20: { //ldc2_w = 20 (0x14)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionGenericLDC(theOpcodeIndex,theIndex, aConstantPool));
                    break;
                }
                case 21: { // iload = 21 (0x15)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1) ,BytecodePrimitiveTypeRef.INT, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex,BytecodePrimitiveTypeRef.INT, theIndex));
                    }
                    break;
                }
                case 22: { // lload = 22 (0x16)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1),BytecodePrimitiveTypeRef.LONG, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex,BytecodePrimitiveTypeRef.LONG, theIndex));
                    }
                    break;
                }
                case 23: { // fload = 23 (0x17)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    }
                    break;
                }
                case 24: { // dload = 24 (0x18)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), BytecodePrimitiveTypeRef.DOUBLE, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, theIndex));
                    }
                    break;
                }
                case 25: { // aload = 25 (0x19)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionALOAD(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, theIndex));
                    }
                    break;
                }
                case 26: { // iload_0 = 26 (0x1a)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 0));
                    break;
                }
                case 27: { //iload_1 = 27 (0x1b)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 1));
                    break;
                }
                case 28: { // iload_2 = 28 (0x1c)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 2));
                    break;
                }
                case 29: { // iload_3 = 29 (0x1d)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 3));
                    break;
                }
                case 30: { // lload_0 = 30 (0x1e)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 0));
                    break;
                }
                case 31: { // lload_1 = 31 (0x1f)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 1));
                    break;
                }
                case 32: { // lload_2 = 32 (0x20)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 2));
                    break;
                }
                case 33: { // lload_3 = 33 (0x21)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 3));
                    break;
                }
                case 34: { // fload_0 = 34 (0x22)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 0));
                    break;
                }
                case 35: { // fload_1 = 35 (0x23)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 1));
                    break;
                }
                case 36: { // fload_2 = 36 (0x24)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 2));
                    break;
                }
                case 37: { // fload_3 = 37 (0x25)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 3));
                    break;
                }
                case 38: { // dload_0 = 38 (0x26)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 0));
                    break;
                }
                case 39: { // dload_1 = 39 (0x27)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 1));
                    break;
                }
                case 40: { // dload_2 = 40 (0x28)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 2));
                    break;
                }
                case 41: { // dload_3 = 41 (0x29)
                    theResult.addInstruction(new BytecodeInstructionGenericLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 3));
                    break;
                }
                case 42: {// aload_0 (0x2a)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, 0));
                    break;
                }
                case 43: {// aload_1 = 43 (0x2b)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, 1));
                    break;
                }
                case 44: { // aload_2 = 44 (0x2c)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, 2));
                    break;
                }
                case 45: {// aload_3 = 45 (0x2d)
                    theResult.addInstruction(new BytecodeInstructionALOAD(theOpcodeIndex, 3));
                    break;
                }
                case 46: {// iaload = 46 (0x2e)
                    theResult.addInstruction(new BytecodeInstructionGenericArrayLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 47: {// laload = 47 (0x2f)
                    theResult.addInstruction(new BytecodeInstructionGenericArrayLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 48: {// faload = 48 (0x30)
                    theResult.addInstruction(new BytecodeInstructionGenericArrayLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 49: {// daload = 49 (0x31)
                    theResult.addInstruction(new BytecodeInstructionGenericArrayLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 50: { // aaload = 50 (0x32)
                    theResult.addInstruction(new BytecodeInstructionObjectArrayLOAD(theOpcodeIndex));
                    break;
                }
                case 51: { //baload = 51 (0x33)
                    theResult.addInstruction(new BytecodeInstructionGenericArrayLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.BYTE));
                    break;
                }
                case 52: { // caload = 52 (0x34)
                    theResult.addInstruction(new BytecodeInstructionGenericArrayLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.CHAR));
                    break;
                }
                case 53: { // saload = 53 (0x35)
                    theResult.addInstruction(new BytecodeInstructionGenericArrayLOAD(theOpcodeIndex, BytecodePrimitiveTypeRef.SHORT));
                    break;
                }
                case 54: { // istore = 54 (0x36)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress()), BytecodePrimitiveTypeRef.INT, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, theIndex));
                    }
                    break;
                }
                case 55: { // lstore = 55 (0x37)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), BytecodePrimitiveTypeRef.LONG, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, theIndex));
                    }
                    break;
                }
                case 56: { // fstore = 56 (0x38)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    }
                    break;
                }
                case 57: { // dstore = 57 (0x39)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, theIndex));
                    }
                    break;
                }
                case 58: { // astore = 58 (0x3a)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;
                        theResult.addInstruction(new BytecodeInstructionASTORE(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;
                        theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, theIndex));
                    }
                    break;
                }
                case 59: { //istore_0 = 59 (0x3b)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 0));
                    break;
                }
                case 60: { // istore_1 = 60 (0x3c)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 1));
                    break;
                }
                case 61: { //istore_2 = 61 (0x3d)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 2));
                    break;
                }
                case 62: { // istore_3 = 62 (0x3e)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT, 3));
                    break;
                }
                case 63: { // lstore_0 = 63 (0x3f)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 0));
                    break;
                }
                case 64: { // lstore_1 = 64 (0x40)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 1));
                    break;
                }
                case 65: { // lstore_2 = 65 (0x41)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 2));
                    break;
                }
                case 66: { // lstore_3 = 66 (0x42)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG, 3));
                    break;
                }
                case 67: { // fstore_0 = 67 (0x43)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 0));
                    break;
                }
                case 68: { // fstore_1 = 68 (0x44)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 1));
                    break;
                }
                case 69: { // fstore_2 = 69 (0x45)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 2));
                    break;
                }
                case 70: { // fstore_3 = 70 (0x46)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT, 3));
                    break;
                }
                case 71: { // dstore_0 = 71 (0x47)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 0));
                    break;
                }
                case 72: { // dstore_1 = 72 (0x48)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 1));
                    break;
                }
                case 73: { // dstore_2 = 73 (0x49)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 2));
                    break;
                }
                case 74: { // dstore_3 = 74 (0x4a)
                    theResult.addInstruction(new BytecodeInstructionGenericSTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE, 3));
                    break;
                }
                case 75: { // astore_0 = 75 (0x4b)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, 0));
                    break;
                }
                case 76: { // astore_1 = 76 (0x4c)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, 1));
                    break;
                }
                case 77: { // astore_2 = 77 (0x4d)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, 2));
                    break;
                }
                case 78: { // astore_3 = 78 (0x4e)
                    theResult.addInstruction(new BytecodeInstructionASTORE(theOpcodeIndex, 3));
                    break;
                }
                case 79: { // iastore = 79 (0x4f)
                    theResult.addInstruction(new BytecodeInstructionGenericArraySTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 80: { // lastore = 80 (0x50)
                    theResult.addInstruction(new BytecodeInstructionGenericArraySTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 81: { // fastore = 81 (0x51)
                    theResult.addInstruction(new BytecodeInstructionGenericArraySTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 82: { // dastore = 82 (0x52)
                    theResult.addInstruction(new BytecodeInstructionGenericArraySTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 83: { // aastore = 83 (0x53)
                    theResult.addInstruction(new BytecodeInstructionObjectArraySTORE(theOpcodeIndex));
                    break;
                }
                case 84: { // bastore = 84 (0x54)
                    theResult.addInstruction(new BytecodeInstructionGenericArraySTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.BYTE));
                    break;
                }
                case 85: { // castore = 85 (0x55)
                    theResult.addInstruction(new BytecodeInstructionGenericArraySTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.CHAR));
                    break;
                }
                case 86: { // sastore = 86 (0x56)
                    theResult.addInstruction(new BytecodeInstructionGenericArraySTORE(theOpcodeIndex, BytecodePrimitiveTypeRef.SHORT));
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
                    theResult.addInstruction(new BytecodeInstructionGenericREM(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 113: {// lrem = 113 (0x71)
                    theResult.addInstruction(new BytecodeInstructionGenericREM(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 114: {// frem = 114 (0x72)
                    theResult.addInstruction(new BytecodeInstructionGenericREM(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 115: {// drem = 115 (0x73)
                    theResult.addInstruction(new BytecodeInstructionGenericREM(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 116: {// ineg = 116 (0x74)
                    theResult.addInstruction(new BytecodeInstructionGenericNEG(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 117: {// lneg = 117 (0x75)
                    theResult.addInstruction(new BytecodeInstructionGenericNEG(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 118: {// fneg = 118 (0x76)
                    theResult.addInstruction(new BytecodeInstructionGenericNEG(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 119: {// dneg = 119 (0x77)
                    theResult.addInstruction(new BytecodeInstructionGenericNEG(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 120: { // ishl = 120 (0x78)
                    theResult.addInstruction(new BytecodeInstructionGenericSHL(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 121: { // lshl = 121 (0x79)
                    theResult.addInstruction(new BytecodeInstructionGenericSHL(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 122: { // ishr = 122 (0x7a)
                    theResult.addInstruction(new BytecodeInstructionGenericSHR(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 123: { // lshr = 123 (0x7b)
                    theResult.addInstruction(new BytecodeInstructionGenericSHR(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 124: { // iushr = 124 (0x7c)
                    theResult.addInstruction(new BytecodeInstructionGenericUSHR(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 125: { // lushr = 125 (0x7d)
                    theResult.addInstruction(new BytecodeInstructionGenericUSHR(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 126: { // iand = 126 (0x7e)
                    theResult.addInstruction(new BytecodeInstructionGenericAND(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 127: { // land = 127 (0x7f)
                    theResult.addInstruction(new BytecodeInstructionGenericAND(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 128: { // ior = 128 (0x80)
                    theResult.addInstruction(new BytecodeInstructionGenericOR(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 129: { // lor = 129 (0x81)
                    theResult.addInstruction(new BytecodeInstructionGenericOR(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 130: { // ixor = 130 (0x82)
                    theResult.addInstruction(new BytecodeInstructionGenericXOR(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 131: { // lxor = 131 (0x83)
                    theResult.addInstruction(new BytecodeInstructionGenericXOR(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 132: { // iinc = 132 (0x84)
                    final int theIndex;
                    final int theConstant;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;

                        theConstant = BytecodeParserUtils.signedShortFromByteArray(aBytecodes, offset);
                        offset+=2;

                        theResult.addInstruction(new BytecodeInstructionIINC(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), theIndex, theConstant));

                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;

                        theConstant = BytecodeParserUtils.signedByteFromByteArray(aBytecodes, offset);
                        offset++;

                        theResult.addInstruction(new BytecodeInstructionIINC(theOpcodeIndex, theIndex, theConstant));
                    }

                    break;
                }
                case 133: { // i2l = 133 (0x85)
                    theResult.addInstruction(new BytecodeInstructionI2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 134: { // i2f = 134 (0x86)
                    theResult.addInstruction(new BytecodeInstructionI2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 135: { // i2d = 135 (0x87)
                    theResult.addInstruction(new BytecodeInstructionI2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 136: { // l2i = 136 (0x88)
                    theResult.addInstruction(new BytecodeInstructionL2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 137: { // l2f = 137 (0x89)
                    theResult.addInstruction(new BytecodeInstructionL2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 138: { // l2d = 138 (0x8a)
                    theResult.addInstruction(new BytecodeInstructionL2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 139: { // f2i = 139 (0x8b)
                    theResult.addInstruction(new BytecodeInstructionF2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 140: { // f2l = 140 (0x8c)
                    theResult.addInstruction(new BytecodeInstructionF2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 141: { // f2d = 141 (0x8d)
                    theResult.addInstruction(new BytecodeInstructionF2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 142: { // d2i = 142 (0x8e)
                    theResult.addInstruction(new BytecodeInstructionD2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                    break;
                }
                case 143: { // d2l = 143 (0x8f)
                    theResult.addInstruction(new BytecodeInstructionD2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                    break;
                }
                case 144: { // d2f = 144 (0x90)
                    theResult.addInstruction(new BytecodeInstructionD2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 145: { // i2b = 145 (0x91)
                    theResult.addInstruction(new BytecodeInstructionI2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.BYTE));
                    break;
                }
                case 146: { // i2c = 146 (0x92)
                    theResult.addInstruction(new BytecodeInstructionI2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.CHAR));
                    break;
                }
                case 147: { // i2s = 147 (0x93)
                    theResult.addInstruction(new BytecodeInstructionI2Generic(theOpcodeIndex, BytecodePrimitiveTypeRef.SHORT));
                    break;
                }
                case 148: { // lcmp = 148 (0x94)
                    theResult.addInstruction(new BytecodeInstructionLCMP(theOpcodeIndex));
                    break;
                }
                case 149: { // fcmpl = 149 (0x95)
                    theResult.addInstruction(new BytecodeInstructionGenericCMP(theOpcodeIndex, BytecodeInstructionGenericCMP.Type.l, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 150: { // fcmpg = 150 (0x96)
                    theResult.addInstruction(new BytecodeInstructionGenericCMP(theOpcodeIndex, BytecodeInstructionGenericCMP.Type.g, BytecodePrimitiveTypeRef.FLOAT));
                    break;
                }
                case 151: { // dcmpl = 151 (0x97)
                    theResult.addInstruction(new BytecodeInstructionGenericCMP(theOpcodeIndex, BytecodeInstructionGenericCMP.Type.l, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 152: { // dcmpg = 152 (0x98)
                    theResult.addInstruction(new BytecodeInstructionGenericCMP(theOpcodeIndex, BytecodeInstructionGenericCMP.Type.g, BytecodePrimitiveTypeRef.DOUBLE));
                    break;
                }
                case 153: { // ifeq = 153 (0x99)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.eq, theIndex));
                    break;
                }
                case 154: { // ifne = 154 (0x9a)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.ne, theIndex));
                    break;
                }
                case 155: { // iflt = 155 (0x9b)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.lt, theIndex));
                    break;
                }
                case 156: { // ifge = 156 (0x9c)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.ge, theIndex));
                    break;
                }
                case 157: { // ifgt = 157 (0x9d)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.gt, theIndex));
                    break;
                }
                case 158: { // ifle = 158 (0x9e)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFCOND(theOpcodeIndex, BytecodeInstructionIFCOND.Type.le, theIndex));
                    break;
                }
                case 159: { // if_icmpeq = 159 (0x9f)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFICMP(theOpcodeIndex, BytecodeInstructionIFICMP.Type.eq, theIndex));
                    break;
                }
                case 160: { // if_icmpne = 160 (0xa0)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFICMP(theOpcodeIndex, BytecodeInstructionIFICMP.Type.ne, theIndex));
                    break;
                }
                case 161: { // if_icmplt = 161 (0xa1)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    theResult.addInstruction(new BytecodeInstructionIFICMP(theOpcodeIndex, BytecodeInstructionIFICMP.Type.lt, theIndex));
                    break;
                }
                case 162: { // if_icmpge = 162 (0xa2)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFICMP(theOpcodeIndex, BytecodeInstructionIFICMP.Type.ge, theIndex));
                    break;
                }
                case 163: { // if_icmpgt = 163 (0xa3)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFICMP(theOpcodeIndex, BytecodeInstructionIFICMP.Type.gt, theIndex));
                    break;
                }
                case 164: { // if_icmple = 164 (0xa4)
                    final int theOffset = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFICMP(theOpcodeIndex, BytecodeInstructionIFICMP.Type.le, theOffset));
                    break;
                }
                case 165: { // if_acmpeq = 165 (0xa5)
                    final int theOffset = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFACMP(theOpcodeIndex, BytecodeInstructionIFACMP.Type.eq, theOffset));
                    break;
                }
                case 166: { // if_acmpne = 166 (0xa6)
                    final int theOffset = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFACMP(theOpcodeIndex, BytecodeInstructionIFACMP.Type.ne, theOffset));
                    break;
                }
                case 167: { // goto = 167 (0xa7)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionGOTO(theOpcodeIndex, theIndex));
                    break;
                }
                case 168: { // jsr = 168 (0xa8)
                    final byte theBranchByte1 = aBytecodes[offset++];
                    final byte theBranchByte2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionJSR(theOpcodeIndex, theBranchByte1, theBranchByte2));
                    break;
                }
                case 169: { // ret = 169 (0xa9)
                    final int theIndex;
                    if (isWideOperator) {
                        theIndex = BytecodeParserUtils.shortFromByteArray(aBytecodes, offset);
                        offset+=2;

                        theResult.addInstruction(new BytecodeInstructionRET(new BytecodeOpcodeAddress(theOpcodeIndex.getAddress() - 1), theIndex));
                    } else {
                        theIndex = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                        offset++;

                        theResult.addInstruction(new BytecodeInstructionRET(theOpcodeIndex, theIndex));
                    }
                    break;
                }
                case 170: { // tableswitch = 170 (0xaa)
                    final int theRemainder = offset % 4;
                    if (theRemainder > 0) {
                        offset += 4 - theRemainder;
                    }

                    final long theDefault = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    final long theLow = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    final long theHigh = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    final long theNumOffsets = theHigh - theLow + 1;
                    final long[] theOffsets = new long[(int) theNumOffsets];
                    for (int i=0;i<theNumOffsets;i++) {

                        final long theOffset = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                        offset+=4;

                        theOffsets[i] = theOffset;
                    }

                    theResult.addInstruction(new BytecodeInstructionTABLESWITCH(theOpcodeIndex, theDefault, theLow, theHigh, theOffsets));
                    break;
                }
                case 171: { // lookupswitch = 171 (0xab)
                    final int theRemainder = offset % 4;
                    if (theRemainder > 0) {
                        offset += 4 - theRemainder;
                    }

                    final long theDefault = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    final long theNumPairs = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                    offset+=4;

                    final BytecodeInstructionLOOKUPSWITCH.Pair[] thePairs = new BytecodeInstructionLOOKUPSWITCH.Pair[(int) theNumPairs];

                    for (long i=0; i<theNumPairs; i++) {
                        final long theMatch = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
                        offset+=4;

                        final long theOffset = BytecodeParserUtils.longFromByteArray(aBytecodes, offset);
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
                    theResult.addInstruction(new BytecodeInstructionObjectRETURN(theOpcodeIndex));
                    break;
                }
                case 177: {// return = 177 (0xb1)
                    theResult.addInstruction(new BytecodeInstructionRETURN(theOpcodeIndex));
                    break;
                }
                case 178: { // getstatic = 178 (0xb2)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionGETSTATIC(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 179: { // getstatic = 178 (0xb2)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionPUTSTATIC(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 180: { // getfield = 180 (0xb4)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionGETFIELD(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 181: { // putfield = 181 (0xb5)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionPUTFIELD(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 182: { // invokevirtual = 182 (0xb6)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionINVOKEVIRTUAL(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 183: {//invokespecial = 183 (0xb7)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionINVOKESPECIAL(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 184: {//invokestatic = 184 (0xb8)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionINVOKESTATIC(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 185: { // invokeinterface = 185 (0xb9)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    final int theCount = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                    offset++;

                    final byte theNull = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKEINTERFACE(theOpcodeIndex, theIndex, theCount, aConstantPool));
                    break;
                }
                case 186: { // invokedynamic = 186 (0xba)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    final byte theNull1 = aBytecodes[offset++];
                    final byte theNull2 = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionINVOKEDYNAMIC(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 187: { // new = 187 (0xbb)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionNEW(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 188: { // newarray = 188 (0xbc)
                    final int theType = BytecodeParserUtils.byteFromByteArray(aBytecodes, offset);
                    offset++;

                    switch (theType) {
                        case 4:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.BOOLEAN));
                            break;
                        case 5:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.CHAR));
                            break;
                        case 6:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.FLOAT));
                            break;
                        case 7:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.DOUBLE));
                            break;
                        case 8:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.BYTE));
                            break;
                        case 9:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.SHORT));
                            break;
                        case 10:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.INT));
                            break;
                        case 11:
                            theResult.addInstruction(new BytecodeInstructionNEWARRAY(theOpcodeIndex, BytecodePrimitiveTypeRef.LONG));
                            break;
                        default:
                            throw new IllegalStateException("Unknown array type : " + theType);
                    }
                    break;
                }
                case 189: { // anewarray = 189 (0xbd)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionANEWARRAY(theOpcodeIndex, theIndex, aConstantPool));
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
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionCHECKCAST(theOpcodeIndex, theIndex, aConstantPool));
                    break;
                }
                case 193: { // instanceof = 193 (0xc1)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionINSTANCEOF(theOpcodeIndex, theIndex, aConstantPool));
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
                case 196: { // wide = 196 (0xc4)
                    wide = true;
                    break;
                }
                case 197: { // multianewarray = 197 (0xc5)
                    final int theIndex = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;
                    final byte theDimensions = aBytecodes[offset++];
                    theResult.addInstruction(new BytecodeInstructionNEWMULTIARRAY(theOpcodeIndex, theIndex, theDimensions, aConstantPool));
                    break;
                }
                case 198: { // ifnull = 198 (0xc6)
                    final int theOffset = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
                    offset+=2;

                    theResult.addInstruction(new BytecodeInstructionIFNULL(theOpcodeIndex, theOffset));
                    break;
                }
                case 199: { // ifnonnull = 199 (0xc7)
                    final int theOffset = BytecodeParserUtils.integerFromByteArray(aBytecodes, offset);
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