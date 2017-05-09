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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class BytecodeLinkedClass {

    public static class LinkedMethod {
        private final BytecodeObjectTypeRef targetType;
        private final BytecodeMethod targetMethod;

        public LinkedMethod(BytecodeObjectTypeRef aTargetType, BytecodeMethod aTargetMethod) {
            targetType = aTargetType;
            targetMethod = aTargetMethod;
        }

        public BytecodeObjectTypeRef getTargetType() {
            return targetType;
        }

        public BytecodeMethod getTargetMethod() {
            return targetMethod;
        }
    }

    private final int uniqueId;
    private final BytecodeObjectTypeRef className;
    private final BytecodeClass bytecodeClass;
    private final Map<BytecodeVirtualMethodIdentifier, LinkedMethod> linkedMethods;
    private final Map<String, BytecodeField> staticFields;
    private final Map<String, BytecodeField> memberFields;
    private final BytecodeLinkerContext linkerContext;
    private final Set<BytecodeMethod> knownMethods;
    private final BytecodeLinkedClass superClass;

    public BytecodeLinkedClass(int aUniqueId, BytecodeLinkedClass aSuperClass, BytecodeLinkerContext aLinkerContext, BytecodeObjectTypeRef aClassName, BytecodeClass aBytecodeClass) {
        uniqueId = aUniqueId;
        className = aClassName;
        bytecodeClass = aBytecodeClass;
        linkedMethods = new HashMap<>();
        linkerContext = aLinkerContext;
        knownMethods = new HashSet<>();
        superClass = aSuperClass;
        staticFields = new HashMap<>();
        memberFields = new HashMap<>();
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public Set<BytecodeLinkedClass> getImplementingTypes() {
        Set<BytecodeLinkedClass> theResult = new HashSet<>();
        theResult.add(this);
        for (BytecodeInterface theInterface : bytecodeClass.getInterfaces()) {
            BytecodeLinkedClass theLinkedOrNull = linkerContext.isLinkedOrNull(theInterface.getClassinfoConstant().getConstant());
            if (theLinkedOrNull != null) {
                theResult.addAll(theLinkedOrNull.getImplementingTypes());
            }
            theInterface.getClassinfoConstant().getConstant().stringValue();
        }
        if (superClass != null) {
            theResult.addAll(superClass.getImplementingTypes());
        }
        return theResult;
    }

    public BytecodeLinkedClass getSuperClass() {
        return superClass;
    }

    public void linkStaticField(BytecodeUtf8Constant aName) {
        String theFieldName = aName.stringValue();
        if (!staticFields.containsKey(theFieldName)) {
            BytecodeField theField = bytecodeClass.fieldByName(aName.stringValue());
            staticFields.put(theFieldName, theField);

            linkerContext.linkTypeRef(theField.getTypeRef());
        }
    }

    public void linkField(BytecodeUtf8Constant aName) {
        String theFieldName = aName.stringValue();
        if (!memberFields.containsKey(theFieldName)) {
            BytecodeField theField = bytecodeClass.fieldByName(aName.stringValue());
            memberFields.put(theFieldName, theField);

            linkerContext.linkTypeRef(theField.getTypeRef());
        }
    }

    private void link(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            return;
        }
        if (aTypeRef instanceof BytecodeArrayTypeRef) {
            BytecodeArrayTypeRef theArrayRef = (BytecodeArrayTypeRef) aTypeRef;
            link(theArrayRef.getType());
            return;
        }
        if (aTypeRef instanceof BytecodeObjectTypeRef) {
            linkerContext.linkClass((BytecodeObjectTypeRef) aTypeRef);
        }
    }

    public void linkVirtualMethod(String aMethodName, BytecodeMethodSignature aSignature) {
        linkStaticMethod(aMethodName, aSignature);
    }

    public void linkStaticMethod(String aMethodName, BytecodeMethodSignature aMethodSignature) {
        try {
            BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignature(aMethodName, aMethodSignature);
            // Constructors are not virtual
            if (!theMethod.isConstructor()) {
                BytecodeVirtualMethodIdentifier theIdentifier = linkerContext.getMethodCollection().identifierFor(theMethod);
                linkedMethods.put(theIdentifier, new LinkedMethod(className, theMethod));
            }

            knownMethods.add(theMethod);

            link(aMethodSignature.getReturnType());
            for (BytecodeTypeRef theArgument : aMethodSignature.getArguments()) {
                link(theArgument);
            }

            BytecodeCodeAttributeInfo theCode = theMethod.getCode(bytecodeClass);
            BytecodeProgram theProgram = theCode.getProgramm();
            for (BytecodeInstruction theInstruction : theProgram.getInstructions()) {
                theInstruction.performLinking(linkerContext);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while linking method for " + className.name(), e);
        }
    }

    public BytecodeConstantPool getConstantPool() {
        return bytecodeClass.getConstantPool();
    }

    public BytecodeClass getBytecodeClass() {
        return bytecodeClass;
    }

    public void forEachVirtualMethod(Consumer<Map.Entry<BytecodeVirtualMethodIdentifier, LinkedMethod>> aConsumer) {
        linkedMethods.entrySet().forEach(aConsumer);
    }

    public void forEachMethod(Consumer<BytecodeMethod> aMethod) {
        knownMethods.forEach(aMethod);
    }

    public void forEachStaticField(Consumer<Map.Entry<String, BytecodeField>> aConsumer) {
        staticFields.entrySet().forEach(aConsumer);
    }

    public void forEachMemberField(Consumer<Map.Entry<String, BytecodeField>> aConsumer) {
        memberFields.entrySet().forEach(aConsumer);
    }

    public void propagateVirtualMethodsAndFields() {
        if (superClass == null) {
            return;
        }
        superClass.forEachMemberField(new Consumer<Map.Entry<String, BytecodeField>>() {
            @Override
            public void accept(Map.Entry<String, BytecodeField> aEntry) {
                if (memberFields.containsKey(aEntry.getKey())) {
                    return;
                }

                memberFields.put(aEntry.getKey(), aEntry.getValue());
            }
        });
        superClass.forEachVirtualMethod(aEntry -> {
            if (linkedMethods.containsKey(aEntry.getKey())) {
                // return because already linked
                return;
            }

            LinkedMethod theLinktarget = aEntry.getValue();
            BytecodeMethod theLinkMethod = theLinktarget.getTargetMethod();

            linkVirtualMethod(theLinkMethod.getName().stringValue(), theLinkMethod.getSignature());
        });
    }
}