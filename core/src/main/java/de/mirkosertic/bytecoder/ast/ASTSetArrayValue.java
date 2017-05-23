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
package de.mirkosertic.bytecoder.ast;

public class ASTSetArrayValue extends ASTValue {

    private final ASTValue array;
    private final ASTValue index;
    private final ASTValue value;

    public ASTSetArrayValue(ASTValue aValue, ASTValue aIndex, ASTValue aArray) {
        array = aArray;
        index = aIndex;
        value = aValue;
    }

    public ASTValue getArray() {
        return array;
    }

    public ASTValue getIndex() {
        return index;
    }

    public ASTValue getValue() {
        return value;
    }
}
