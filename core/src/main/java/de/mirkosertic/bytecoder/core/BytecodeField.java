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

import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;

public class BytecodeField extends Node<Node, EdgeType> {

    private final BytecodeAccessFlags accessFlags;
    private final BytecodeUtf8Constant name;
    private final BytecodeTypeRef typeRef;
    private final BytecodeAttributeInfo[] attributeInfo;

    public BytecodeField(final BytecodeAccessFlags aAccessFlags, final BytecodeUtf8Constant aName, final BytecodeTypeRef aTypeRef, final BytecodeAttributeInfo[] aAttributeInfo) {
        accessFlags = aAccessFlags;
        name = aName;
        typeRef = aTypeRef;
        attributeInfo = aAttributeInfo;
    }

    public BytecodeAccessFlags getAccessFlags() {
        return accessFlags;
    }

    public BytecodeUtf8Constant getName() {
        return name;
    }

    public BytecodeTypeRef getTypeRef() {
        return typeRef;
    }

    public BytecodeAttributeInfo[] getAttributeInfo() {
        return attributeInfo;
    }
}
