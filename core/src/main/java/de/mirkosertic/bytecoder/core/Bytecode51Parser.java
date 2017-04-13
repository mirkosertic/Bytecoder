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

import java.io.DataInput;
import java.io.IOException;

public class Bytecode51Parser implements BytecodeParser {

    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_Fieldref = 9;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_InterfaceMethodref= 11;
    private static final int CONSTANT_String = 8;
    private static final int CONSTANT_Integer = 3;
    private static final int CONSTANT_Float = 4;
    private static final int CONSTANT_Long = 5;
    private static final int CONSTANT_Double = 6;
    private static final int CONSTANT_NameAndType = 12;
    private static final int CONSTANT_Utf8 = 1;
    private static final int CONSTANT_MethodHandle = 15;
    private static final int CONSTANT_MethodType = 16;
    private static final int CONSTANT_InvokeDynamic = 18;

    @Override
    public BytecodeClass parseBody(DataInput dis) throws IOException {

        BytecodeConstantPool theConstantPool = parseConstantPool(dis);

        BytecodeClass theResult = new BytecodeClass(theConstantPool);
        parseAccessFlags(dis);
        parseThisClass(dis);
        parseSuperClass(dis);
        parseInterfaces(dis);
        parseFields(dis);
        parseMethods(dis);
        parseAttributes(dis);

        return theResult;
    }

    private BytecodeConstantPool parseConstantPool(DataInput aDis) throws IOException {
        BytecodeConstantPool theResult = new BytecodeConstantPool();
        int theConstantPoolCount = aDis.readUnsignedShort();
        for (int i=1;i<theConstantPoolCount;i++) {
            int theTag = aDis.readUnsignedByte();
            switch (theTag) {
                case CONSTANT_Class:
                    parseConstantPool_CONSTANT_Class(aDis, theResult);
                    break;
                case CONSTANT_Fieldref:
                    parseConstantPool_CONSTANT_Fieldref(aDis, theResult);
                    break;
                case CONSTANT_Methodref:
                    parseConstantPool_CONSTANT_Methodref(aDis, theResult);
                    break;
                case CONSTANT_InterfaceMethodref:
                    parseConstantPool_CONSTANT_InterfaceMethodref(aDis, theResult);
                    break;
                case CONSTANT_String:
                    parseConstantPool_CONSTANT_String(aDis, theResult);
                    break;
                case CONSTANT_Integer:
                    parseConstantPool_CONSTANT_Integer(aDis, theResult);
                    break;
                case CONSTANT_Float:
                    parseConstantPool_CONSTANT_Float(aDis, theResult);
                    break;
                case CONSTANT_Long:
                    parseConstantPool_CONSTANT_Long(aDis, theResult);
                    break;
                case CONSTANT_Double:
                    parseConstantPool_CONSTANT_Double(aDis, theResult);
                    break;
                case CONSTANT_NameAndType:
                    parseConstantPool_CONSTANT_NameAndType(aDis, theResult);
                    break;
                case CONSTANT_Utf8:
                    parseConstantPool_CONSTANT_Utf8(aDis, theResult);
                    break;

                case CONSTANT_MethodHandle:
                    parseConstantPool_CONSTANT_MethodHandle(aDis);
                    break;
                case CONSTANT_MethodType:
                    parseConstantPool_CONSTANT_MethodType(aDis);
                    break;
                case CONSTANT_InvokeDynamic:
                    parseConstantPool_CONSTANT_InvokeDynamic(aDis);
                    break;
                default:
                    throw new IllegalStateException("Unknown constant pool tag : " + theTag + " for index " + i + " of " + theConstantPoolCount);
            }
        }

        return theResult;
    }

    private void parseConstantPool_CONSTANT_Class(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theNameIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeClassinfoConstant(new BytecodeNameIndex(theNameIndex)));
    }

    private void parseConstantPool_CONSTANT_Fieldref(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeFieldRefConstant(new BytecodeClassIndex(theClassIndex), new BytecodeNameAndTypeIndex(theNameAndTypeIndex)));
    }

    private void parseConstantPool_CONSTANT_Methodref(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeMethodRefConstant(new BytecodeClassIndex(theClassIndex), new BytecodeNameAndTypeIndex(theNameAndTypeIndex)));
    }

    private void parseConstantPool_CONSTANT_InterfaceMethodref(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeInterfaceRefConstant(new BytecodeClassIndex(theClassIndex), new BytecodeNameAndTypeIndex(theNameAndTypeIndex)));
    }

    private void parseConstantPool_CONSTANT_String(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theStringIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeStringConstant(new BytecodeStringIndex(theStringIndex)));
    }

    private void parseConstantPool_CONSTANT_Integer(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        long theBytes = aDis.readLong();
        aConstantPool.registerConstant(new BytecodeIntegerConstant(theBytes));
    }

    private void parseConstantPool_CONSTANT_Float(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        long theBytes = aDis.readLong();
        aConstantPool.registerConstant(new BytecodeFloatConstant(theBytes));
    }

    private void parseConstantPool_CONSTANT_Long(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        long theHighBytes = aDis.readLong();
        long theLowBytes = aDis.readLong();
        aConstantPool.registerConstant(new BytecodeLongConstant(theHighBytes, theLowBytes));
    }

    private void parseConstantPool_CONSTANT_Double(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        long theHighBytes = aDis.readLong();
        long theLowBytes = aDis.readLong();
        aConstantPool.registerConstant(new BytecodeDoubleConstant(theHighBytes, theLowBytes));
    }

    private void parseConstantPool_CONSTANT_NameAndType(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theNameIndex = aDis.readUnsignedShort();
        int theDescriptorIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeNameAndTypeConstant(new BytecodeNameIndex(theNameIndex), new BytecodeDescriptorIndex(theDescriptorIndex)));
    }

    private void parseConstantPool_CONSTANT_Utf8(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theLength = aDis.readUnsignedShort();
        byte[] theData = new byte[theLength];
        aDis.readFully(theData);
        aConstantPool.registerConstant(new BytecodeUtf8Constant(new String(theData, "UTF-8")));
    }

    private void parseConstantPool_CONSTANT_MethodHandle(DataInput aDis) throws IOException {
        int theReferenceKind = aDis.readUnsignedByte();
        int theReferenceIndex = aDis.readUnsignedShort();
    }

    private void parseConstantPool_CONSTANT_MethodType(DataInput aDis) throws IOException {
        int theDescriptorIndex = aDis.readUnsignedShort();
    }

    private void parseConstantPool_CONSTANT_InvokeDynamic(DataInput aDis) throws IOException {
        int theBootstrapMethodAttrIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
    }

    private void parseAccessFlags(DataInput aDis) throws IOException {
        int theAccessFlags = aDis.readUnsignedShort();
    }

    private void parseThisClass(DataInput aDis) throws IOException {
        int theThisClass = aDis.readUnsignedShort();
    }

    private void parseSuperClass(DataInput aDis) throws IOException {
        int theSuperClass = aDis.readUnsignedShort();
    }

    private void parseInterfaces(DataInput aDis) throws IOException {
        int theInterfaceCount = aDis.readUnsignedShort();
        for (int i=0;i<theInterfaceCount;i++) {
            int theNameIndex = aDis.readUnsignedShort();
        }
    }

    private void parseFields(DataInput aDis) throws IOException {
        int theFieldCount = aDis.readUnsignedShort();
        for (int i=0;i<theFieldCount;i++) {
            int theAccessFlags = aDis.readUnsignedShort();
            int theNameIndex = aDis.readUnsignedShort();
            int theDescriptorIndex = aDis.readUnsignedShort();
            int theAttributesCount = aDis.readUnsignedShort();
            for (int j=0;j<theAttributesCount;j++) {
                int theAttributeNameIndex = aDis.readUnsignedShort();
                long theAttributeLength = aDis.readLong();
                for (int k=0;k<theAttributeLength;k++) {
                    int b = aDis.readUnsignedByte();
                }
            }
        }
    }

    private void parseMethods(DataInput aDis) throws IOException {
    /*    int theMethodCount = aDis.readUnsignedShort();
        for (int i=0;i<theMethodCount;i++) {
            int theAccessFlags = aDis.readUnsignedShort();
            int theNameIndex = aDis.readUnsignedShort();
            int theDescriptorIndex = aDis.readUnsignedShort();
            int theAttributesCount = aDis.readUnsignedShort();
            for (int j=0;j<theAttributesCount;j++) {
                int theAttributeNameIndex = aDis.readUnsignedShort();
                long theAttributeLength = aDis.readLong();
                for (int k=0;k<theAttributeLength;k++) {
                    int b = aDis.readUnsignedByte();
                }
            }
        }*/
    }

    private void parseAttributes(DataInput aDis) {
    }
}