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

import java.util.HashSet;
import java.util.Set;

public class BytecodePackageReplacer {

    private final Set<String> shadowPackages;

    public BytecodePackageReplacer() {
        shadowPackages = new HashSet<>();
        shadowPackages.add("java.");
        shadowPackages.add("org.junit.");
        shadowPackages.add("junit.");
    }

    public BytecodeObjectTypeRef replaceTypeIn(BytecodeObjectTypeRef aTypeRef) {
        StringBuilder theTypeName = new StringBuilder(aTypeRef.name());

        for (String thePrefix : shadowPackages) {
            if (theTypeName.indexOf(thePrefix) == 0) {
                theTypeName.insert(0, "de.mirkosertic.bytecoder.classlib.");
                int p = theTypeName.lastIndexOf(".");
                theTypeName.insert(p+1, "T");
            }
        }

        return new BytecodeObjectTypeRef(theTypeName.toString());
    }

    public BytecodeUtf8Constant replaceTypeIn(BytecodeUtf8Constant aClassNameConstant) {
        StringBuilder theTypeName = new StringBuilder(aClassNameConstant.stringValue().replace("/","."));
        for (String thePrefix : shadowPackages) {
            if (theTypeName.indexOf(thePrefix) == 0) {
                theTypeName.insert(0, "de.mirkosertic.bytecoder.classlib.");
                int p = theTypeName.lastIndexOf(".");
                theTypeName.insert(p+1, "T");
            }
        }
        return new BytecodeUtf8Constant(theTypeName.toString().replace(".","/"));
    }
}
