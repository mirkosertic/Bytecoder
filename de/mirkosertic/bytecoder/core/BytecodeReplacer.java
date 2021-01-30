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
        private final BytecodeAttributeInfo[] classAttributes;

        public MergeResult(final BytecodeMethod[] aMethods, final BytecodeField[] aFields,
                           final BytecodeClassinfoConstant aSuperClass, final BytecodeInterface[] aInterfaces, final BytecodeAttributeInfo[] aClassAttributes) {
            methods = aMethods;
            fields = aFields;
            superClass = aSuperClass;
            interfaces = aInterfaces;
            classAttributes = aClassAttributes;
        }

        public BytecodeAttributeInfo[] getClassAttributes() {
            return classAttributes;
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

    public BytecodeReplacer(final BytecodeLoader aLoader) {
        loader = aLoader;
        typeMap = new HashMap<>();
    }

    public void addTypeMap(final String aOld, final String aNew) {
        typeMap.put(aOld, aNew);
    }

    public MergeResult replace(
            final BytecodeClassinfoConstant aClass, final BytecodeMethod[] aMethods, final BytecodeField[] aFields,
            final BytecodeClassinfoConstant aSuperClass,
            final BytecodeInterface[] aInterfaces,
            final BytecodeAttributeInfo[] aClassAttributes) {
        return new MergeResult(aMethods, aFields, aSuperClass, aInterfaces, aClassAttributes);
    }

    public BytecodeTypeRef replaceTypeIn(final BytecodeObjectTypeRef aType) {
        final String theNew = typeMap.get(aType.name());
        if (theNew != null) {
            return new BytecodeObjectTypeRef(theNew);
        }
        return aType;
    }

    public BytecodeUtf8Constant replaceTypeIn(final BytecodeUtf8Constant aType) {
        final String theTypeName = aType.stringValue().replace("/", ".");
        final String theNew = typeMap.get(theTypeName);
        if (theNew != null) {
            return new BytecodeUtf8Constant(theNew.replace(".","/"));
        }
        return aType;
    }

}