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

    public static final int CONSTANT_Class = 7;
    public static final int CONSTANT_Fieldref = 9;
    public static final int CONSTANT_Methodref = 10;
    public static final int CONSTANT_InterfaceMethodref= 11;
    public static final int CONSTANT_String = 8;
    public static final int CONSTANT_Integer = 3;
    public static final int CONSTANT_Float = 4;
    public static final int CONSTANT_Long = 5;
    public static final int CONSTANT_Double = 6;
    public static final int CONSTANT_NameAndType = 12;
    public static final int CONSTANT_Utf8 = 1;
    public static final int CONSTANT_MethodHandle = 15;
    public static final int CONSTANT_MethodType = 16;
    public static final int CONSTANT_InvokeDynamic = 18;

    @Override
    public void parseBody(DataInput dis) throws IOException {
        parseConstantPool(dis);
        parseAccessFlags(dis);
        parseThisClass(dis);
        parseSuperClass(dis);
        parseInterfaces(dis);
        parseFields(dis);
        parseMethods(dis);
        parseAttributes(dis);
    }

    private void parseConstantPool(DataInput dis) throws IOException {
        int theConstantPoolCount = dis.readUnsignedShort();
        for (int i=1;i<theConstantPoolCount;i++) {
            int theTag = dis.readUnsignedByte();
            switch (theTag) {
                case CONSTANT_Class:
                    parseConstantPool_CONSTANT_Class(dis);
                    break;
                case CONSTANT_Fieldref:
                    parseConstantPool_CONSTANT_Fieldref(dis);
                    break;
                case CONSTANT_Methodref:
                    parseConstantPool_CONSTANT_Methodref(dis);
                    break;
                case CONSTANT_InterfaceMethodref:
                    parseConstantPool_CONSTANT_InterfaceMethodref(dis);
                    break;
                case CONSTANT_String:
                    parseConstantPool_CONSTANT_String(dis);
                    break;
                case CONSTANT_Integer:
                    parseConstantPool_CONSTANT_Integer(dis);
                    break;
                case CONSTANT_Float:
                    parseConstantPool_CONSTANT_Float(dis);
                    break;
                case CONSTANT_Long:
                    parseConstantPool_CONSTANT_Long(dis);
                    break;
                case CONSTANT_Double:
                    parseConstantPool_CONSTANT_Double(dis);
                    break;
                case CONSTANT_NameAndType:
                    parseConstantPool_CONSTANT_NameAndType(dis);
                    break;
                case CONSTANT_Utf8:
                    parseConstantPool_CONSTANT_Utf8(dis);
                    break;
                case CONSTANT_MethodHandle:
                    parseConstantPool_CONSTANT_MethodHandle(dis);
                    break;
                case CONSTANT_MethodType:
                    parseConstantPool_CONSTANT_MethodType(dis);
                    break;
                case CONSTANT_InvokeDynamic:
                    parseConstantPool_CONSTANT_InvokeDynamic(dis);
                    break;
                default:
                    throw new IllegalStateException("Unknown constant pool tag : " + theTag + " for index " + i + " of " + theConstantPoolCount);
            }
        }
    }

    private void parseConstantPool_CONSTANT_Class(DataInput aDis) throws IOException {
        int theNameIndex = aDis.readUnsignedShort();
    }

    private void parseConstantPool_CONSTANT_Fieldref(DataInput aDis) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
    }

    private void parseConstantPool_CONSTANT_Methodref(DataInput aDis) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
    }

    private void parseConstantPool_CONSTANT_InterfaceMethodref(DataInput aDis) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
    }

    private void parseConstantPool_CONSTANT_String(DataInput aDis) throws IOException {
        int theStringIndex = aDis.readUnsignedShort();

    }

    private void parseConstantPool_CONSTANT_Integer(DataInput aDis) throws IOException {
        long theBytes = aDis.readLong();
    }

    private void parseConstantPool_CONSTANT_Float(DataInput aDis) throws IOException {
        long theBytes = aDis.readLong();

    }

    private void parseConstantPool_CONSTANT_Long(DataInput aDis) throws IOException {
        long theHighBytes = aDis.readLong();
        long theLowBytes = aDis.readLong();
    }

    private void parseConstantPool_CONSTANT_Double(DataInput aDis) throws IOException {
        long theHighBytes = aDis.readLong();
        long theLowBytes = aDis.readLong();
    }

    private void parseConstantPool_CONSTANT_NameAndType(DataInput aDis) throws IOException {
        int theNameIndex = aDis.readUnsignedShort();
        int theDescriptorIndex = aDis.readUnsignedShort();
    }

    private void parseConstantPool_CONSTANT_Utf8(DataInput aDis) throws IOException {
        int theLength = aDis.readUnsignedShort();
        for (int i=0;i<theLength;i++) {
            aDis.readUnsignedByte();
        }
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

    private void parseInterfaces(DataInput aDis) {
    }

    private void parseFields(DataInput aDis) {
    }

    private void parseMethods(DataInput aDis) {
    }

    private void parseAttributes(DataInput aDis) {
    }
}
