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

import java.util.Objects;

public class BytecodeAnnotation {

    public interface ElementValue {

        String stringValue();
    }

    public static class StringElementValue implements ElementValue {
        private final int constValueIndex;
        private final BytecodeConstantPool constantPool;

        public StringElementValue(final int aConstValueIndex, final BytecodeConstantPool aConstantPool) {
            constValueIndex = aConstValueIndex;
            constantPool = aConstantPool;
        }

        @Override
        public String stringValue() {
            final BytecodeUtf8Constant theString = (BytecodeUtf8Constant) constantPool.constantByIndex(constValueIndex - 1);
            return theString.stringValue();
        }
    }

    public static class BooleanElementValue implements ElementValue {
        private final int constValueIndex;
        private final BytecodeConstantPool constantPool;

        public BooleanElementValue(final int aConstValueIndex, final BytecodeConstantPool aConstantPool) {
            constValueIndex = aConstValueIndex;
            constantPool = aConstantPool;
        }

        @Override
        public String stringValue() {
            final BytecodeIntegerConstant theConstant = (BytecodeIntegerConstant) constantPool.constantByIndex(constValueIndex - 1);
            if (theConstant.getIntegerValue() == 1) {
                return "true";
            }
            return "false";
        }
    }

    public static class IntegerElementValue implements ElementValue {
        private final int constValueIndex;
        private final BytecodeConstantPool constantPool;

        public IntegerElementValue(final int aConstValueIndex, final BytecodeConstantPool aConstantPool) {
            constValueIndex = aConstValueIndex;
            constantPool = aConstantPool;
        }

        @Override
        public String stringValue() {
            return Integer.toString(intVakue());
        }

        public int intVakue() {
            final BytecodeIntegerConstant theInt = (BytecodeIntegerConstant) constantPool.constantByIndex(constValueIndex - 1);
            return theInt.getIntegerValue();
        }
    }


    public static class ClassElementValue implements ElementValue {
        private final int classInfoIndex;
        private final BytecodeConstantPool constantPool;
        private final BytecodeSignatureParser signatureParser;

        public ClassElementValue(final int aClassInfoIndex, final BytecodeConstantPool aConstantPool, final BytecodeSignatureParser aSignatureParser) {
            classInfoIndex = aClassInfoIndex;
            constantPool = aConstantPool;
            signatureParser = aSignatureParser;
        }

        @Override
        public String stringValue() {
            final BytecodeUtf8Constant theConstant = (BytecodeUtf8Constant) constantPool.constantByIndex(classInfoIndex - 1);
            final BytecodeTypeRef[] theTypes = signatureParser.toTypes(theConstant.stringValue());
            return theTypes[0].name();
        }
    }

    public static class EnumElementValue implements ElementValue {
        public EnumElementValue(final BytecodeConstantPool aConstantPool,
                                final int aTypeNameIndex, final int aConstNameIndex) {
        }

        @Override
        public String stringValue() {
            throw new IllegalStateException("Not implemented yet");
        }
    }

    public static class ArrayElementValue implements ElementValue {
        public ArrayElementValue(final BytecodeConstantPool aConstantPool,
                                 final ElementValue[] aValues) {
        }

        @Override
        public String stringValue() {
            throw new IllegalStateException("Not implemented yet");
        }
    }

    public static class AnnotationElementValueElementValue implements ElementValue {
        public AnnotationElementValueElementValue(final BytecodeConstantPool aConstantPool,
                                                  final BytecodeAnnotation annotation) {
        }

        @Override
        public String stringValue() {
            throw new IllegalStateException("Not implemented yet");
        }
    }

    public static class ElementValuePair {

        private final int elementNameIndex;
        private final ElementValue elementValue;
        private final BytecodeConstantPool constantPool;

        public ElementValuePair(final int aElementNameIndex, final ElementValue aElementValue, final BytecodeConstantPool aConstantPool) {
            elementNameIndex = aElementNameIndex;
            elementValue = aElementValue;
            constantPool = aConstantPool;
        }

        public BytecodeUtf8Constant getName() {
            return (BytecodeUtf8Constant) constantPool.constantByIndex(elementNameIndex - 1);
        }

        public ElementValue getValue() {
            return elementValue;
        }
    }

    private final int typeIndex;
    private final ElementValuePair[] elementValuePairs;
    private final BytecodeConstantPool constantPool;
    private final BytecodeSignatureParser signatureParser;

    public BytecodeAnnotation(final int aTypeIndex, final ElementValuePair[] aElementValuePairs, final BytecodeConstantPool aConstantPool, final BytecodeSignatureParser aSignatureParser) {
        typeIndex = aTypeIndex;
        elementValuePairs = aElementValuePairs;
        constantPool = aConstantPool;
        signatureParser = aSignatureParser;
    }

    public BytecodeTypeRef getType() {
        final BytecodeUtf8Constant theConstant = (BytecodeUtf8Constant) constantPool.constantByIndex(typeIndex - 1);
        final BytecodeTypeRef[] theRefs = signatureParser.toTypes(theConstant.stringValue());
        return theRefs[0];
    }

    public ElementValue getElementValueByName(final String aAttributeName) {
        for (final ElementValuePair thePair : elementValuePairs) {
            if (Objects.equals(thePair.getName().stringValue(), aAttributeName)) {
                return thePair.getValue();
            }
        }
        return null;
    }
}
