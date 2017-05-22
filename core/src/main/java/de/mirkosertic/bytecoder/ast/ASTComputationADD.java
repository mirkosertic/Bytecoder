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

public class ASTComputationADD extends ASTComputationResult {

    private final ASTValue value1;
    private final ASTValue value2;

    public ASTComputationADD(ASTValue aValue1, ASTValue aValue2) {
        value1 = aValue1;
        value2 = aValue2;
    }

    public ASTValue getValue1() {
        return value1;
    }

    public ASTValue getValue2() {
        return value2;
    }
}