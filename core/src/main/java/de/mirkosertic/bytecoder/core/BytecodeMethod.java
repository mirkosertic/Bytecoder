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

import de.mirkosertic.bytecoder.annotations.DelegatesTo;

public class BytecodeMethod {

    private final BytecodeAccessFlags accessFlags;
    private final BytecodeUtf8Constant name;
    private final BytecodeAttributeInfo[] attributes;
    private final BytecodeMethodSignature signature;

    public BytecodeMethod(BytecodeAccessFlags aAccessFlags, BytecodeUtf8Constant aName, BytecodeMethodSignature aSignature, BytecodeAttributeInfo[] aAttributes) {
        accessFlags = aAccessFlags;
        name = aName;
        signature = aSignature;
        attributes = aAttributes;
    }

    public BytecodeAnnotations getAnnotations() {
        return new BytecodeAnnotations(attributes);
    }

    public BytecodeUtf8Constant getName() {
        return name;
    }

    public BytecodeAccessFlags getAccessFlags() {
        return accessFlags;
    }

    public BytecodeCodeAttributeInfo getCode(BytecodeClass aContextClass) {
        BytecodeAnnotation theDelegatesTo = getAnnotations().getAnnotationByType(DelegatesTo.class.getName());
        if (theDelegatesTo != null) {
            BytecodeAnnotation.ElementValue theMethodToDelegate = theDelegatesTo.getElementValueByName("methodName");
            String theDelegatingMethod = theMethodToDelegate.stringValue();
            return aContextClass.methodByNameAndSignature(theDelegatingMethod, getSignature()).getCode(aContextClass);
        }
        return attributeByType(BytecodeCodeAttributeInfo.class);
    }

    private <T extends BytecodeAttributeInfo> T attributeByType(Class<T> aAttributeClass) {
        for (BytecodeAttributeInfo theInfo : attributes) {
            if (theInfo.getClass().equals(aAttributeClass)) {
                return (T) theInfo;
            }
        }
        return null;
    }

    public BytecodeMethodSignature getSignature() {
        return signature;
    }

    public boolean isConstructor() {
        return name.stringValue().equals("<init>");
    }

    public boolean isClassInitializer() {
        return name.stringValue().equals("<clinit>");
    }
}