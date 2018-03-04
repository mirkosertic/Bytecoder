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

import de.mirkosertic.bytecoder.api.DelegatesTo;
import de.mirkosertic.bytecoder.graph.Node;

import java.util.Objects;

public class BytecodeMethod extends Node {

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

    public BytecodeMethod replaceAndFlagsFrom(BytecodeMethod aOtherMethod) {
        return new BytecodeMethod(aOtherMethod.accessFlags, name, signature, aOtherMethod.attributes);
    }

    public BytecodeAttributes getAttributes() {
        return new BytecodeAttributes(attributes);
    }

    public BytecodeUtf8Constant getName() {
        return name;
    }

    public BytecodeAccessFlags getAccessFlags() {
        return accessFlags;
    }

    public BytecodeCodeAttributeInfo getCode(BytecodeClass aContextClass) {
        BytecodeAnnotation theDelegatesTo = getAttributes().getAnnotationByType(DelegatesTo.class.getName());
        if (theDelegatesTo != null) {
            BytecodeAnnotation.ElementValue theMethodToDelegate = theDelegatesTo.getElementValueByName("methodName");
            String theDelegatingMethod = theMethodToDelegate.stringValue();
            BytecodeMethod theMethod = aContextClass.methodByNameAndSignatureOrNull(theDelegatingMethod, getSignature());
            if (theMethod == null) {
                throw new IllegalStateException("Cannot find method " + theDelegatingMethod + " in " + aContextClass.getThisInfo().getConstant().stringValue());
            }
            return theMethod.getCode(aContextClass);
        }
        return attributeByType(BytecodeCodeAttributeInfo.class);
    }

    public <T extends BytecodeAttributeInfo> T attributeByType(Class<T> aAttributeClass) {
        for (BytecodeAttributeInfo theInfo : attributes) {
            if (Objects.equals(theInfo.getClass(), aAttributeClass)) {
                return (T) theInfo;
            }
        }
        return null;
    }

    public BytecodeMethodSignature getSignature() {
        return signature;
    }

    public boolean isConstructor() {
        return Objects.equals(name.stringValue(), "<init>");
    }

    public boolean isClassInitializer() {
        return Objects.equals(name.stringValue(), "<clinit>");
    }
}