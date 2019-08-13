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

public class BytecodeDescriptorIndex {

    private final int index;
    private final BytecodeConstantPool constantPool;
    private final BytecodeSignatureParser signatureParser;
    private BytecodeMethodSignature signature;
    private BytecodeTypeRef typeRef;

    public BytecodeDescriptorIndex(final int aIndex, final BytecodeConstantPool aConstantPool, final BytecodeSignatureParser aSignatureParser) {
        index = aIndex;
        constantPool = aConstantPool;
        signatureParser = aSignatureParser;
    }

    public BytecodeMethodSignature methodSignature() {
        if (signature == null) {
            signature = signatureParser.toMethodSignature((BytecodeUtf8Constant) constantPool.constantByIndex(index - 1));
        }
        return signature;
    }

    public BytecodeTypeRef fieldType() {
        if (typeRef == null) {
            typeRef = signatureParser.toFieldType((BytecodeUtf8Constant) constantPool.constantByIndex(index - 1));
        }
        return typeRef;
    }
}
