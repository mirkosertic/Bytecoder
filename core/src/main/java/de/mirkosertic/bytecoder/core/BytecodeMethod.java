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
import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;

import java.util.Objects;

public class BytecodeMethod extends Node<Node, EdgeType> {

    private final BytecodeAccessFlags accessFlags;
    private final BytecodeUtf8Constant name;
    private final BytecodeAttributeInfo[] attributes;
    private final BytecodeMethodSignature signature;
    private final BytecodeAttributes mappedAttributes;

    public BytecodeMethod(final BytecodeAccessFlags aAccessFlags, final BytecodeUtf8Constant aName, final BytecodeMethodSignature aSignature, final BytecodeAttributeInfo[] aAttributes) {
        accessFlags = aAccessFlags;
        name = aName;
        signature = aSignature;
        attributes = aAttributes;
        mappedAttributes = new BytecodeAttributes(attributes);
    }

    public BytecodeMethod replaceAndFlagsFrom(final BytecodeMethod aOtherMethod) {
        return new BytecodeMethod(aOtherMethod.accessFlags, name, signature, aOtherMethod.attributes);
    }

    public BytecodeMethod replaceSignature(final BytecodeMethod aOtherMethod) {
        return new BytecodeMethod(aOtherMethod.accessFlags, name, aOtherMethod.getSignature(), attributes);
    }

    public BytecodeAttributes getAttributes() {
        return mappedAttributes;
    }

    public BytecodeUtf8Constant getName() {
        return name;
    }

    public BytecodeAccessFlags getAccessFlags() {
        return accessFlags;
    }

    public BytecodeCodeAttributeInfo getCode(final BytecodeClass aContextClass) {
        final BytecodeAnnotation theDelegatesTo = getAttributes().getAnnotationByType(DelegatesTo.class.getName());
        if (theDelegatesTo != null) {
            final BytecodeAnnotation.ElementValue theMethodToDelegate = theDelegatesTo.getElementValueByName("methodName");
            final String theDelegatingMethod = theMethodToDelegate.stringValue();
            final BytecodeMethod theMethod = aContextClass.methodByNameAndSignatureOrNull(theDelegatingMethod, getSignature());
            if (theMethod == null) {
                throw new IllegalStateException("Cannot find method " + theDelegatingMethod + " in " + aContextClass.getThisInfo().getConstant().stringValue());
            }
            return theMethod.getCode(aContextClass);
        }
        return attributeByType(BytecodeCodeAttributeInfo.class);
    }

    public <T extends BytecodeAttributeInfo> T attributeByType(final Class<T> aAttributeClass) {
        for (final BytecodeAttributeInfo theInfo : attributes) {
            if (Objects.equals(theInfo.getClass(), aAttributeClass)) {
                return (T) theInfo;
            }
        }
        return null;
    }

    public boolean emulatedByRuntime() {
        return getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null;
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