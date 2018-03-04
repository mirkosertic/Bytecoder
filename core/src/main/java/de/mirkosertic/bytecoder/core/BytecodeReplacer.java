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

import java.util.HashMap;
import java.util.Map;

public class BytecodeReplacer {

    public static class MergeResult {

        private final BytecodeMethod[] methods;
        private final BytecodeField[] fields;
        private final BytecodeClassinfoConstant superClass;
        private final BytecodeInterface[] interfaces;

        public MergeResult(BytecodeMethod[] aMethods, BytecodeField[] aFields,
                BytecodeClassinfoConstant aSuperClass, BytecodeInterface[] aInterfaces) {
            methods = aMethods;
            fields = aFields;
            superClass = aSuperClass;
            interfaces = aInterfaces;
        }

        public BytecodeMethod[] getMethods() {
            return methods;
        }

        public BytecodeField[] getFields() {
            return fields;
        }

        public BytecodeClassinfoConstant getSuperClass() {
            return superClass;
        }

        public BytecodeInterface[] getInterfaces() {
            return interfaces;
        }
    }

    protected final BytecodeLoader loader;
    private final Map<String, String> typeMap;

    public BytecodeReplacer(BytecodeLoader aLoader) {
        loader = aLoader;
        typeMap = new HashMap<>();
    }

    public void addTypeMap(String aOld, String aNew) {
        typeMap.put(aOld, aNew);
    }

    public MergeResult replace(
            BytecodeClassinfoConstant aClass, BytecodeMethod[] aMethods, BytecodeField[] aFields,
            BytecodeClassinfoConstant aSuperClass,
            BytecodeInterface[] aInterfaces) {
        return new MergeResult(aMethods, aFields, aSuperClass, aInterfaces);
    }

    public BytecodeTypeRef replaceTypeIn(BytecodeObjectTypeRef aType) {
        String theNew = typeMap.get(aType.name());
        if (theNew != null) {
            return new BytecodeObjectTypeRef(theNew);
        }
        return aType;
    }

    public BytecodeUtf8Constant replaceTypeIn(BytecodeUtf8Constant aType) {
        String theTypeName = aType.stringValue().replace("/", ".");
        String theNew = typeMap.get(theTypeName);
        if (theNew != null) {
            return new BytecodeUtf8Constant(theNew.replace(".","/"));
        }
        return aType;
    }

}