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

import java.util.Objects;

import de.mirkosertic.bytecoder.api.OverrideParentClass;
import de.mirkosertic.bytecoder.api.Substitutes;

public class BytecodeClass {

    private final BytecodeConstantPool constantPool;
    private final BytecodeAccessFlags accessFlags;
    private final BytecodeClassinfoConstant thisClass;
    private final BytecodeClassinfoConstant superClass;
    private final BytecodeInterface[] interfaces;
    private final BytecodeField[] fields;
    private final BytecodeMethod[] methods;
    private final BytecodeAttributeInfo[] classAttributes;

    public BytecodeClass(final BytecodeConstantPool aConstantPool, final BytecodeAccessFlags aAccessFlags, final BytecodeClassinfoConstant aThisClass, final BytecodeClassinfoConstant aSuperClass, final BytecodeInterface[] aInterfaces, final BytecodeField[] aFields, final BytecodeMethod[] aMethods, final BytecodeAttributeInfo[] aClassAttributes) {
        constantPool = aConstantPool;
        accessFlags = aAccessFlags;
        thisClass = aThisClass;
        superClass = aSuperClass;
        interfaces = aInterfaces;
        fields = aFields;
        methods = aMethods;
        classAttributes = aClassAttributes;
    }

    public BytecodeClassinfoConstant getThisInfo() {
        return thisClass;
    }

    public BytecodeAccessFlags getAccessFlags() {
        return accessFlags;
    }

    public BytecodeAttributes getAttributes() {
        return new BytecodeAttributes(classAttributes);
    }

    public BytecodeAttributeInfo[] getAttributesRaw() {
        return classAttributes;
    }

    public BytecodeConstantPool getConstantPool() {
        return constantPool;
    }

    public BytecodeField[] fields() {
        return fields;
    }

    public BytecodeField fieldByName(final String aName) {
        for (final BytecodeField theField : fields) {
            if (Objects.equals(theField.getName().stringValue(), aName)) {
                return theField;
            }
        }
        return null;
    }

    public BytecodeMethod methodByNameAndSignatureOrNull(final String aMethodName, final BytecodeMethodSignature aSignature) {
        for (final BytecodeMethod theMethod : methods) {
            String theMethodName = theMethod.getName().stringValue();
            final BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(Substitutes.class.getName());
            if (theAnnotation != null) {
                theMethodName = theAnnotation.getElementValueByName("value").stringValue();
            }
            if (Objects.equals(aMethodName, theMethodName) && theMethod.getSignature().matchesExactlyTo(aSignature)) {
                return theMethod;
            }
        }
        return null;
    }

    public BytecodeClassinfoConstant getSuperClass() {
        final BytecodeAnnotation theDelegatesTo = getAttributes().getAnnotationByType(OverrideParentClass.class.getName());
        if (theDelegatesTo != null) {
            final BytecodeAnnotation.ElementValue theParentOverride = theDelegatesTo.getElementValueByName("parentClass");
            return new BytecodeClassinfoConstant(-1, null, null) {
                @Override
                public BytecodeUtf8Constant getConstant() {
                    return new BytecodeUtf8Constant(theParentOverride.stringValue().replace(".","/"));
                }
            };
        }
        return superClass;
    }

    public BytecodeMethod[] getMethods() {
        return methods;
    }

    public BytecodeInterface[] getInterfaces() {
        return interfaces;
    }

    public BytecodeMethod classInitializerOrNull() {
        for (final BytecodeMethod theMethod : methods) {
            if ("<clinit>".equalsIgnoreCase(theMethod.getName().stringValue())) {
                return theMethod;
            }
        }
        return null;
    }
}