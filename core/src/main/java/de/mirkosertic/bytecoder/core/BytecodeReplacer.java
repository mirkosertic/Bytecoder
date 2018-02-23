/*
 * Copyright 2018 Mirko Sertic
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

public class BytecodeReplacer {

    public static class MergeResult {

        private final BytecodeMethod[] methods;
        private final BytecodeField[] fields;

        MergeResult(BytecodeMethod[] aMethods, BytecodeField[] aFields) {
            methods = aMethods;
            fields = aFields;
        }

        public BytecodeMethod[] getMethods() {
            return methods;
        }

        public BytecodeField[] getFields() {
            return fields;
        }
    }

    protected final BytecodeLoader loader;

    public BytecodeReplacer(BytecodeLoader aLoader) {
        loader = aLoader;
    }

    public MergeResult replace(
            BytecodeClassinfoConstant aClass, BytecodeMethod[] aMethods, BytecodeField[] aFields) {
        return new MergeResult(aMethods, aFields);
    }

    public BytecodeTypeRef replaceTypeIn(BytecodeObjectTypeRef aType) {
        return aType;
    }

    public BytecodeUtf8Constant replaceTypeIn(BytecodeUtf8Constant aType) {
        return aType;
    }

}