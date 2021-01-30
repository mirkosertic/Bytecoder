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

import de.mirkosertic.bytecoder.api.IsObject;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7
public class Bytecode5xClassParser implements BytecodeClassParser {

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

    private final BytecodeProgramParser programmParser;
    private final BytecodeSignatureParser signatureParser;
    private final BytecodeReplacer bytecodeReplacer;

    public Bytecode5xClassParser(final BytecodeProgramParser aParser,
                                 final BytecodeSignatureParser aSignatureParser,
                                 final BytecodeReplacer aReplacer) {
        programmParser = aParser;
        signatureParser = aSignatureParser;
        bytecodeReplacer = aReplacer;
    }

    @Override
    public BytecodeClass parseBody(final DataInput dis) throws IOException {

        final BytecodeConstantPool theConstantPool = parseConstantPool(dis);

        final BytecodeAccessFlags theAccessFlags = parseAccessFlags(dis);
        final BytecodeClassinfoConstant theThisClass = parseThisClass(dis, theConstantPool);
        BytecodeClassinfoConstant theSuperClass = parseSuperClass(dis, theConstantPool);

        final BytecodeInterface[] theInterfaces = parseInterfaces(dis, theConstantPool);
        final BytecodeField[] theFields = parseFields(dis, theConstantPool);
        final BytecodeMethod[] theMethods = parseMethods(dis, theConstantPool);

        final BytecodeAttributeInfo[] theClassAttributes = parseAttributes(dis, theConstantPool);

        final BytecodeAttributes theAttributes = new BytecodeAttributes(theClassAttributes);
        if (theAttributes.getAnnotationByType(IsObject.class.getName()) != null) {
            theSuperClass = BytecodeClassinfoConstant.OBJECT_CLASS;
        }

        final BytecodeReplacer.MergeResult theResult = bytecodeReplacer.replace(theThisClass,
                theMethods,
                theFields,
                theSuperClass,
                theInterfaces,
                theClassAttributes);

        return new BytecodeClass(theConstantPool,
                theAccessFlags,
                theThisClass,
                theSuperClass,
                theInterfaces,
                theResult.getFields(),
                theResult.getMethods(),
                theResult.getClassAttributes());
    }

    private BytecodeConstantPool parseConstantPool(final DataInput aDis) throws IOException {
        final BytecodeConstantPool theResult = new BytecodeConstantPool();
        final int theConstantPoolCount = aDis.readUnsignedShort();
        for (int i=1;i<theConstantPoolCount;i++) {
            final int theTag = aDis.readUnsignedByte();
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
                theResult.registerConstant(new BytecodeUnusedConstant());
                i++;
                break;
            case CONSTANT_Double:
                parseConstantPool_CONSTANT_Double(aDis, theResult);
                theResult.registerConstant(new BytecodeUnusedConstant());
                i++;
                break;
            case CONSTANT_NameAndType:
                parseConstantPool_CONSTANT_NameAndType(aDis, theResult);
                break;
            case CONSTANT_Utf8:
                parseConstantPool_CONSTANT_Utf8(aDis, theResult);
                break;
            case CONSTANT_MethodHandle:
                parseConstantPool_CONSTANT_MethodHandle(aDis, theResult);
                break;
            case CONSTANT_MethodType:
                parseConstantPool_CONSTANT_MethodType(aDis, theResult);
                break;
            case CONSTANT_InvokeDynamic:
                parseConstantPool_CONSTANT_InvokeDynamic(aDis, theResult);
                break;
            default:
                throw new IllegalStateException("Unknown constant pool tag : " + theTag + " for index " + i + " of " + theConstantPoolCount);
            }
        }

        return theResult;
    }

    private void parseConstantPool_CONSTANT_Class(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theNameIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeClassinfoConstant(theNameIndex, aConstantPool, bytecodeReplacer));
    }

    private void parseConstantPool_CONSTANT_Fieldref(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theClassIndex = aDis.readUnsignedShort();
        final int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeFieldRefConstant(new BytecodeClassIndex(theClassIndex, aConstantPool), new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private void parseConstantPool_CONSTANT_Methodref(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theClassIndex = aDis.readUnsignedShort();
        final int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeMethodRefConstant(new BytecodeClassIndex(theClassIndex, aConstantPool), new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private void parseConstantPool_CONSTANT_InterfaceMethodref(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theClassIndex = aDis.readUnsignedShort();
        final int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeInterfaceRefConstant(new BytecodeClassIndex(theClassIndex, aConstantPool), new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private void parseConstantPool_CONSTANT_String(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theStringIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeStringConstant(new BytecodeStringIndex(theStringIndex), aConstantPool));
    }

    private void parseConstantPool_CONSTANT_Integer(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theBytes = aDis.readInt();
        aConstantPool.registerConstant(new BytecodeIntegerConstant(theBytes));
    }

    private void parseConstantPool_CONSTANT_Float(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final float theFloat = aDis.readFloat();
        aConstantPool.registerConstant(new BytecodeFloatConstant(theFloat));
    }

    private void parseConstantPool_CONSTANT_Long(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final long theLowBytes = aDis.readInt() & 0xFFFFFFFFL;
        final long theHighBytes = aDis.readInt() & 0xFFFFFFFFL;
        aConstantPool.registerConstant(new BytecodeLongConstant(theHighBytes, theLowBytes));
    }

    private void parseConstantPool_CONSTANT_Double(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final double theDouble = aDis.readDouble();
        aConstantPool.registerConstant(new BytecodeDoubleConstant(theDouble));
    }

    private void parseConstantPool_CONSTANT_NameAndType(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theNameIndex = aDis.readUnsignedShort();
        final int theDescriptorIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeNameAndTypeConstant(new BytecodeNameIndex(theNameIndex, aConstantPool), new BytecodeDescriptorIndex(theDescriptorIndex, aConstantPool, signatureParser)));
    }

    private void parseConstantPool_CONSTANT_Utf8(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theLength = aDis.readUnsignedShort();
        final byte[] theData = new byte[theLength];
        aDis.readFully(theData);
        aConstantPool.registerConstant(new BytecodeUtf8Constant(new String(theData, StandardCharsets.UTF_8)));
    }

    private void parseConstantPool_CONSTANT_MethodHandle(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theReferenceKind = aDis.readUnsignedByte();
        final int theReferenceIndex = aDis.readUnsignedShort();
        switch (theReferenceKind) {
        case 1:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_getField, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 2:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_getStatic, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 3:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_putField, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 4:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_putStatic, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 5:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeVirtual, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 6:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeStatic, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 7:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeSpecial, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 8:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_newInvokeSpecial, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 9:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeInterface, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        default:
            throw new IllegalStateException("Unknown reference kind : " + theReferenceKind);
        }
    }

    private void parseConstantPool_CONSTANT_MethodType(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theDescriptorIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeMethodTypeConstant(new BytecodeDescriptorIndex(theDescriptorIndex, aConstantPool, signatureParser)));
    }

    private void parseConstantPool_CONSTANT_InvokeDynamic(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theBootstrapMethodAttrIndex = aDis.readUnsignedShort();
        final int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeInvokeDynamicConstant(new BytecodeMethodAttributeIndex(theBootstrapMethodAttrIndex),
                new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private BytecodeAccessFlags parseAccessFlags(final DataInput aDis) throws IOException {
        final int theAccessFlags = aDis.readUnsignedShort();
        return new BytecodeAccessFlags(theAccessFlags);
    }

    private BytecodeClassinfoConstant parseThisClass(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theThisClass = aDis.readUnsignedShort();
        final BytecodeConstant theConstant = aConstantPool.constantByIndex(theThisClass - 1);
        if (!(theConstant instanceof BytecodeClassinfoConstant)) {
            throw new IllegalStateException("Invalid this constant reference : got type " + theConstant.getClass().getName());
        }
        return (BytecodeClassinfoConstant) theConstant;
    }

    private BytecodeClassinfoConstant parseSuperClass(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theSuperClass = aDis.readUnsignedShort();
        if (theSuperClass == 0) {
            return BytecodeClassinfoConstant.OBJECT_CLASS;
        }
        final BytecodeConstant theConstant = aConstantPool.constantByIndex(theSuperClass - 1);
        if (!(theConstant instanceof BytecodeClassinfoConstant)) {
            throw new IllegalStateException("Invalid super_class constant reference : got type " + theConstant.getClass().getName());
        }
        return (BytecodeClassinfoConstant) theConstant;
    }

    private BytecodeInterface[] parseInterfaces(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final List<BytecodeInterface> theInterfaces = new ArrayList<>();
        final int theInterfaceCount = aDis.readUnsignedShort();
        for (int i=0;i<theInterfaceCount;i++) {
            final int theNameIndex = aDis.readUnsignedShort();
            final BytecodeConstant theConstant = aConstantPool.constantByIndex(theNameIndex - 1);
            if (!(theConstant instanceof BytecodeClassinfoConstant)) {
                throw new IllegalStateException("Invalid constant reference : got type " + theConstant.getClass().getName());
            }
            theInterfaces.add(new BytecodeInterface((BytecodeClassinfoConstant) theConstant));
        }
        return theInterfaces.toArray(new BytecodeInterface[theInterfaces.size()]);
    }

    private BytecodeBootstrapMethodsAttributeInfo parseBootstrapAttribute(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theNumMethods = aDis.readUnsignedShort();
        final List<BytecodeBootstrapMethod> theMethods = new ArrayList<>();
        for (int i=0;i<theNumMethods;i++) {
            final int theMethodRef = aDis.readUnsignedShort();
            final int theNumArguments = aDis.readUnsignedShort();
            final int[] theArguments = new int[theNumArguments];
            for (int j=0;j<theNumArguments;j++) {
                theArguments[j] = aDis.readUnsignedShort();
            }
            theMethods.add(new BytecodeBootstrapMethod(theMethodRef, theArguments, aConstantPool));
        }

        return new BytecodeBootstrapMethodsAttributeInfo(theMethods.toArray(new BytecodeBootstrapMethod[theMethods.size()]));
    }

    private BytecodeSourceFileAttributeInfo parseSourceFileAttribute(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theNameIndex = aDis.readUnsignedShort();
        return new BytecodeSourceFileAttributeInfo(aConstantPool, theNameIndex);
    }

    private BytecodeLineNumberTableAttributeInfo parseLineNumberTableAttribute(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theNumEntries = aDis.readUnsignedShort();
        final BytecodeLineNumberTableAttributeInfo.Entry[] theEntries = new BytecodeLineNumberTableAttributeInfo.Entry[theNumEntries];
        for (int i=0;i<theNumEntries;i++) {
            final int theStartPC = aDis.readUnsignedShort();
            final int theLineNum = aDis.readUnsignedShort();
            theEntries[i] = new BytecodeLineNumberTableAttributeInfo.Entry(theStartPC, theLineNum);
        }
        return new BytecodeLineNumberTableAttributeInfo(theEntries);
    }

    private BytecodeLocalVariableTableAttributeInfo parseLocalVariableTableAttribute(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theNumTableEntries = aDis.readUnsignedShort();
        final List<BytecodeLocalVariableTableEntry> theEntries = new ArrayList<>();
        for (int i=0;i<theNumTableEntries;i++) {
            final int theStartPC = aDis.readUnsignedShort();
            final int theLength = aDis.readUnsignedShort();
            final int theNameIndex = aDis.readUnsignedShort();
            final int theDescriptorIndex = aDis.readUnsignedShort();
            final int theIndex = aDis.readUnsignedShort();

            final BytecodeTypeRef theTypeRef = signatureParser.toFieldType(((BytecodeUtf8Constant) aConstantPool.constantByIndex(theDescriptorIndex - 1)));

            theEntries.add(new BytecodeLocalVariableTableEntry(theStartPC, theLength, theNameIndex, theTypeRef, theIndex));
        }

        return new BytecodeLocalVariableTableAttributeInfo(aConstantPool, theEntries.toArray(new BytecodeLocalVariableTableEntry[theEntries.size()]));
    }

    private BytecodeAnnotation.ElementValue readAnnotationElementValueFrom(final DataInput aDis, final BytecodeConstantPool aConstantPool)
            throws IOException {
        final char theTag = (char) aDis.readUnsignedByte();
        switch (theTag) {
            case 's': {
                final int theConstValueIndex = aDis.readUnsignedShort();
                return new BytecodeAnnotation.StringElementValue(theConstValueIndex, aConstantPool);
            }
            case 'I': {
                final int theConstValueIndex = aDis.readUnsignedShort();
                return new BytecodeAnnotation.IntegerElementValue(theConstValueIndex, aConstantPool);
            }
            case 'c': {
                final int theClassInfoIndex = aDis.readUnsignedShort();
                return new BytecodeAnnotation.ClassElementValue(theClassInfoIndex, aConstantPool, signatureParser);
            }
            case 'Z': {
                final int theClassInfoIndex = aDis.readUnsignedShort();
                return new BytecodeAnnotation.BooleanElementValue(theClassInfoIndex, aConstantPool);
            }
            case 'e': {
                final int theTypeNameIndex = aDis.readUnsignedShort();
                final int theConstNameIndex = aDis.readUnsignedShort();
                return new BytecodeAnnotation.EnumElementValue(aConstantPool,
                        theTypeNameIndex, theConstNameIndex);
            }
            case '[': {
                final int theLength = aDis.readUnsignedShort();
                final BytecodeAnnotation.ElementValue[] theValues = new BytecodeAnnotation.ElementValue[theLength];
                for (int i=0;i<theLength;i++) {
                    theValues[i] = readAnnotationElementValueFrom(aDis, aConstantPool);
                }
                return new BytecodeAnnotation.ArrayElementValue(aConstantPool, theValues);
            }
            case '@': {
                final BytecodeAnnotation theAnnotation = readSingleAnnotation(aDis, aConstantPool);
                return new BytecodeAnnotation.AnnotationElementValueElementValue(aConstantPool, theAnnotation);
            }
        }
        throw new IllegalArgumentException("Not supported annotation value type : " + theTag);
    }

    private BytecodeAnnotationAttributeInfo parseAnnotationAttribute(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theAnnotationCount = aDis.readUnsignedShort();
        final List<BytecodeAnnotation> theAnnotations = new ArrayList<>();
        for (int i=0;i<theAnnotationCount;i++) {
            theAnnotations.add(readSingleAnnotation(aDis, aConstantPool));
        }

        return new BytecodeAnnotationAttributeInfo(theAnnotations.toArray(new BytecodeAnnotation[theAnnotations.size()]));
    }

    private BytecodeAnnotation readSingleAnnotation(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final int theTypeIndex = aDis.readUnsignedShort();
        final int theNumElementValuePairs = aDis.readUnsignedShort();

        final List<BytecodeAnnotation.ElementValuePair> theElementValuePairs = new ArrayList<>();
        for (int j=0;j<theNumElementValuePairs;j++) {
            final int theElementNameIndex = aDis.readUnsignedShort();
            final BytecodeAnnotation.ElementValue theAnnotationValue =  readAnnotationElementValueFrom(aDis, aConstantPool);
            theElementValuePairs.add(new BytecodeAnnotation.ElementValuePair(theElementNameIndex,
                    theAnnotationValue,
                    aConstantPool));
        }

        return new BytecodeAnnotation(theTypeIndex, theElementValuePairs.toArray(new BytecodeAnnotation.ElementValuePair[theElementValuePairs.size()]), aConstantPool, signatureParser);
    }

    private BytecodeCodeAttributeInfo parseCodeAttribute(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {

        final int theMaxStack = aDis.readUnsignedShort();
        final int theMaxLocals = aDis.readUnsignedShort();
        final int theCodeLength = aDis.readInt();
        final byte[] theCode = new byte[theCodeLength];
        aDis.readFully(theCode);

        final BytecodeProgram theProgramm = programmParser.parse(theCode, aConstantPool);

        final int theExceptionTableLength = aDis.readUnsignedShort();
        for (int i=0;i<theExceptionTableLength;i++) {
            final BytecodeOpcodeAddress theStartPC = new BytecodeOpcodeAddress(aDis.readUnsignedShort());
            final BytecodeOpcodeAddress theEndPc = new BytecodeOpcodeAddress(aDis.readUnsignedShort());
            final BytecodeOpcodeAddress theHandlerPc = new BytecodeOpcodeAddress(aDis.readUnsignedShort());
            final int theCatchType = aDis.readUnsignedShort();
            theProgramm.addExceptionHandler(new BytecodeExceptionTableEntry(theStartPC, theEndPc, theHandlerPc, theCatchType, aConstantPool));
        }
        final BytecodeAttributeInfo[] theAttributes = parseAttributes(aDis, aConstantPool);

        return new BytecodeCodeAttributeInfo(theMaxStack, theMaxLocals, theProgramm, theAttributes);
    }

    private BytecodeAttributeInfo[] parseAttributes(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final List<BytecodeAttributeInfo> theAttributes = new ArrayList<>();
        final int theAttributesCount = aDis.readUnsignedShort();
        for (int j=0;j<theAttributesCount;j++) {
            final int theAttributeNameIndex = aDis.readUnsignedShort();

            final BytecodeConstant theAttributeNameConstant = aConstantPool.constantByIndex(theAttributeNameIndex - 1);
            if (!(theAttributeNameConstant instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid constant reference : got type " + theAttributeNameConstant.getClass().getName());
            }

            final int theAttributeLength = aDis.readInt();
            switch (((BytecodeUtf8Constant) theAttributeNameConstant).stringValue()) {
            case "Code":
                theAttributes.add(parseCodeAttribute(aDis, aConstantPool));
                break;
            case "RuntimeVisibleAnnotations":
                theAttributes.add(parseAnnotationAttribute(aDis, aConstantPool));
                break;
            case "BootstrapMethods":
                theAttributes.add(parseBootstrapAttribute(aDis, aConstantPool));
                break;
            case "LocalVariableTable":
                theAttributes.add(parseLocalVariableTableAttribute(aDis, aConstantPool));
                break;
            case "SourceFile":
                theAttributes.add(parseSourceFileAttribute(aDis, aConstantPool));
                break;
            case "LineNumberTable":
                theAttributes.add(parseLineNumberTableAttribute(aDis, aConstantPool));
                break;
            default:
                final byte[] theAttributeData = new byte[theAttributeLength];
                aDis.readFully(theAttributeData);

                theAttributes.add(new BytecodeUnknownAttributeInfo((BytecodeUtf8Constant) theAttributeNameConstant, theAttributeData));
                break;
            }
        }
        return theAttributes.toArray(new BytecodeAttributeInfo[theAttributes.size()]);
    }

    private BytecodeField[] parseFields(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final List<BytecodeField> theFields = new ArrayList<>();
        final int theFieldCount = aDis.readUnsignedShort();
        for (int i=0;i<theFieldCount;i++) {
            final int theAccessFlags = aDis.readUnsignedShort();
            final int theNameIndex = aDis.readUnsignedShort();
            final BytecodeConstant theNameConstant = aConstantPool.constantByIndex(theNameIndex - 1);
            if (!(theNameConstant instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theNameConstant.getClass().getName());
            }

            final int theDescriptorIndex = aDis.readUnsignedShort();
            final BytecodeConstant theDescriptorConstant = aConstantPool.constantByIndex(theDescriptorIndex - 1);
            if (!(theDescriptorConstant instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theDescriptorConstant.getClass().getName());
            }

            final BytecodeAttributeInfo[] theAttributes = parseAttributes(aDis, aConstantPool);

            final BytecodeTypeRef theTypeRef = signatureParser.toFieldType(((BytecodeUtf8Constant) theDescriptorConstant));

            theFields.add(new BytecodeField(
                    new BytecodeAccessFlags(theAccessFlags),
                    (BytecodeUtf8Constant) theNameConstant,
                    theTypeRef,
                    theAttributes));
        }
        return theFields.toArray(new BytecodeField[theFields.size()]);
    }

    private BytecodeMethod[] parseMethods(final DataInput aDis, final BytecodeConstantPool aConstantPool) throws IOException {
        final List<BytecodeMethod> theMethods = new ArrayList<>();
        final int theMethodCount = aDis.readUnsignedShort();
        for (int i=0;i<theMethodCount;i++) {
            final int theAccessFlags = aDis.readUnsignedShort();
            final int theNameIndex = aDis.readUnsignedShort();

            final BytecodeConstant theName = aConstantPool.constantByIndex(theNameIndex - 1);
            if (!(theName instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theName.getClass().getName());
            }

            final int theDescriptorIndex = aDis.readUnsignedShort();

            final BytecodeConstant theDescriptor = aConstantPool.constantByIndex(theDescriptorIndex - 1);
            if (!(theDescriptor instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theDescriptor.getClass().getName());
            }

            final BytecodeAttributeInfo[] theAttributes = parseAttributes(aDis, aConstantPool);

            theMethods.add(new BytecodeMethod(new BytecodeAccessFlags(theAccessFlags),
                    (BytecodeUtf8Constant) theName,
                    signatureParser.toMethodSignature((BytecodeUtf8Constant) theDescriptor),
                    theAttributes));
        }
        return theMethods.toArray(new BytecodeMethod[theMethods.size()]);
    }
}