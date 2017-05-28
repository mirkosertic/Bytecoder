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

import de.mirkosertic.bytecoder.annotations.EmulatedByRuntime;
import de.mirkosertic.bytecoder.annotations.Import;
import de.mirkosertic.bytecoder.classlib.java.lang.TClass;

public class BytecodeLinkedClass {

    public static final BytecodeMethodSignature GET_CLASS_SIGNATURE = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TClass.class), new BytecodeTypeRef[0]);
    public static final BytecodeMethod GET_CLASS_PLACEHOLDER = new BytecodeMethod(null, null, null, null) {
        @Override
        public BytecodeUtf8Constant getName() {
            return new BytecodeUtf8Constant("getClass");
        }

        @Override
        public BytecodeMethodSignature getSignature() {
            return GET_CLASS_SIGNATURE;
        }
    };

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
    private BytecodeMethod classInitializer;

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

    public BytecodeObjectTypeRef getClassName() {
        return className;
    }

    public int getUniqueId() {
        return uniqueId;
    }

    public Set<BytecodeLinkedClass> getImplementingTypes() {
        return getImplementingTypes(true, true);
    }

    public Set<BytecodeLinkedClass> getImplementingTypes(boolean aIncludeSuperClass, boolean aIncludeSelf) {
        Set<BytecodeLinkedClass> theResult = new HashSet<>();
        if (aIncludeSelf) {
            theResult.add(this);
        }
        for (BytecodeInterface theInterface : bytecodeClass.getInterfaces()) {
            BytecodeLinkedClass theLinkedOrNull = linkerContext.isLinkedOrNull(theInterface.getClassinfoConstant().getConstant());
            if (theLinkedOrNull != null) {
                theResult.addAll(theLinkedOrNull.getImplementingTypes());
            }
            theInterface.getClassinfoConstant().getConstant().stringValue();
        }
        if (superClass != null && aIncludeSuperClass) {
            theResult.addAll(superClass.getImplementingTypes());
        }
        return theResult;
    }

    public BytecodeAccessFlags getAccessFlags() {
        return bytecodeClass.getAccessFlags();
    }

    public BytecodeLinkedClass getSuperClass() {
        return superClass;
    }

    public void linkClassInitializer(BytecodeMethod aMethod) {
        classInitializer = aMethod;
        linkVirtualMethod(aMethod.getName().stringValue(), aMethod.getSignature());
    }

    public void linkStaticField(BytecodeUtf8Constant aName) {
        String theFieldName = aName.stringValue();
        if (!staticFields.containsKey(theFieldName)) {
            BytecodeField theField = bytecodeClass.fieldByName(aName.stringValue());
            if (theField == null) {
                throw new RuntimeException("No field " + aName.stringValue() + " in " + className.name());
            }
            staticFields.put(theFieldName, theField);

            linkerContext.linkTypeRef(theField.getTypeRef());
        }
    }

    public void linkField(BytecodeUtf8Constant aName) {
        String theFieldName = aName.stringValue();
        if (!memberFields.containsKey(theFieldName)) {
            BytecodeField theField = bytecodeClass.fieldByName(aName.stringValue());
            if (theField == null) {
                if (bytecodeClass.getSuperClass() != BytecodeClassinfoConstant.OBJECT_CLASS) {
                    BytecodeObjectTypeRef theSuperClassName = BytecodeObjectTypeRef.fromUtf8Constant(bytecodeClass.getSuperClass().getConstant());
                    linkerContext.linkClass(theSuperClassName).linkField(aName);
                    return;
                } else {
                    throw new RuntimeException("No field " + aName.stringValue() + " in " + className.name());
                }
            }
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

    private BytecodeMethod findVirtualMethodIncludingInterfaces(BytecodeClass aClass, String aMethodName, BytecodeMethodSignature aSignature) {
        BytecodeMethod theMethod = aClass.methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod != null) {
            return theMethod;
        }
        for (BytecodeLinkedClass theImplementingType : getImplementingTypes(false, false)) {
            theMethod = theImplementingType.findVirtualMethodIncludingInterfaces(theImplementingType.bytecodeClass, aMethodName, aSignature);
            if (theMethod != null) {
                // Method body not found, but part of implementation, hence implicitly marked abstract
                return theMethod;
            }
        }
        return null;
    }

    public void linkVirtualMethod(String aMethodName, BytecodeMethodSignature aSignature) {
        try {
            BytecodeObjectTypeRef theClassName = className;
            BytecodeClass theClass = bytecodeClass;

            if ("getClass".equals(aMethodName) && GET_CLASS_SIGNATURE.metchesExactlyTo(aSignature)) {

                BytecodeVirtualMethodIdentifier theIdentifier = linkerContext.getMethodCollection()
                        .identifierFor("getClass", GET_CLASS_SIGNATURE);
                linkedMethods.put(theIdentifier, new LinkedMethod(theClassName, GET_CLASS_PLACEHOLDER));
                return;
            }

            while(theClass != null) {

                BytecodeMethod theMethod = findVirtualMethodIncludingInterfaces(theClass, aMethodName, aSignature);
                if (theMethod != null) {
                    if (!theMethod.isClassInitializer()) {
                        BytecodeVirtualMethodIdentifier theIdentifier = linkerContext.getMethodCollection()
                                .identifierFor(theMethod);
                        if (linkedMethods.containsKey(theIdentifier)) {
                            // Already linked, nothing to do
                            return;
                        }
                        linkedMethods.put(theIdentifier, new LinkedMethod(theClassName, theMethod));
                    }
                    linkMethodInternal(theMethod, theClass == bytecodeClass);

                    if (theClass != bytecodeClass) {
                        // Superclass methods must also be marked as linked
                        linkerContext.linkVirtualMethod(theClassName, aMethodName, aSignature);
                    }

                    return;
                }

                if (theClass.getSuperClass() != BytecodeClassinfoConstant.OBJECT_CLASS) {
                    theClassName = BytecodeObjectTypeRef.fromUtf8Constant(theClass.getSuperClass().getConstant());

                    theClass = linkerContext.linkClass(theClassName).bytecodeClass;
                } else {
                    theClass = null;
                }
            }
            throw new IllegalArgumentException("No such name : " + aMethodName + " with signature " + aSignature + "in " + className.name());
            // We need to traverse
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while linking virtual method name " + aMethodName + " for " + className.name(), e);
        }
    }

    public void linkConstructorInvocation(BytecodeMethodSignature aMethodSignature) {

        BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull("<init>", aMethodSignature);
        if (theMethod == null) {
            System.out.println("No constructor with signature " + aMethodSignature + " in " + className.name());
            return;
        }

        if (!knownMethods.contains(theMethod)) {
            linkMethodInternal(theMethod, true);
        }
    }

    public void linkPrivateMethod(String aMethodName, BytecodeMethodSignature aMethodSignature) {

        BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aMethodSignature);
        if (theMethod == null) {
            System.out.println("No method " + aMethodName + " with signature " + aMethodSignature + " in " + className.name());
            return;
        }

        if (!knownMethods.contains(theMethod)) {
            linkMethodInternal(theMethod, true);
        }
    }

    public void linkStaticMethod(String aMethodName, BytecodeMethodSignature aMethodSignature) {
        try {
            BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aMethodSignature);
            if (theMethod == null) {
                throw new IllegalArgumentException("No such name : " + aMethodName + " with signature " + aMethodSignature);
            }
            if (!theMethod.isClassInitializer()) {
                BytecodeVirtualMethodIdentifier theIdentifier = linkerContext.getMethodCollection().identifierFor(theMethod);
                linkedMethods.put(theIdentifier, new LinkedMethod(className, theMethod));
            }

            linkMethodInternal(theMethod, true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while linking static name " + aMethodName + " for " + className.name(), e);
        }
    }

    public void linkMethodInternal(BytecodeMethod aMethod, boolean isLocal) {
        BytecodeMethodSignature theSignature = aMethod.getSignature();
        if (isLocal) {
            knownMethods.add(aMethod);
        }

        link(theSignature.getReturnType());
        for (BytecodeTypeRef theArgument : theSignature.getArguments()) {
            link(theArgument);
        }

        if (!aMethod.getAccessFlags().isAbstract()) {
            if (aMethod.getAccessFlags().isNative()) {
                if (bytecodeClass.getAnnotations().getAnnotationByType(EmulatedByRuntime.class.getName()) == null) {
                    if (aMethod.getAnnotations().getAnnotationByType(Import.class.getName()) == null) {
                        throw new IllegalStateException("Method " + aMethod.getName().stringValue()
                                + " declared as native, but no @Import annotation found");
                    }
                }
            } else {
                BytecodeCodeAttributeInfo theCode = aMethod.getCode(bytecodeClass);
                BytecodeProgram theProgram = theCode.getProgramm();
                for (BytecodeInstruction theInstruction : theProgram.getInstructions()) {
                    theInstruction.performLinking(linkerContext);
                }
            }
        }
    }

    public BytecodeConstantPool getConstantPool() {
        return bytecodeClass.getConstantPool();
    }

    public BytecodeClass getBytecodeClass() {
        return bytecodeClass;
    }

    public void forEachVirtualMethod(Consumer<Map.Entry<BytecodeVirtualMethodIdentifier, LinkedMethod>> aConsumer) {
        Map<BytecodeVirtualMethodIdentifier, LinkedMethod> theClone = new HashMap<>(linkedMethods);
        theClone.entrySet().forEach(aConsumer);
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

    public boolean hasClassInitializer() {
        return classInitializer != null;
    }

    public void propagateVirtualMethodsAndFields() {
        if (superClass == null) {
            return;
        }
        superClass.forEachMemberField(new Consumer<Map.Entry<String, BytecodeField>>() {
            @Override
            public void accept(Map.Entry<String, BytecodeField> aEntry) {
                if (!memberFields.containsKey(aEntry.getKey())) {
                    memberFields.put(aEntry.getKey(), aEntry.getValue());
                }
            }
        });

        for (BytecodeLinkedClass theImplementedInterface : getImplementingTypes()) {
            theImplementedInterface.forEachVirtualMethod(aEntry -> {
                if (!linkedMethods.containsKey(aEntry.getKey())) {
                    LinkedMethod theLinktarget = aEntry.getValue();
                    BytecodeMethod theLinkMethod = theLinktarget.getTargetMethod();
                    linkVirtualMethod(theLinkMethod.getName().stringValue(), theLinkMethod.getSignature());
                }
            });
        }

        superClass.forEachVirtualMethod(aEntry -> {
            if (!linkedMethods.containsKey(aEntry.getKey())) {
                LinkedMethod theLinktarget = aEntry.getValue();
                BytecodeMethod theLinkMethod = theLinktarget.getTargetMethod();
                linkVirtualMethod(theLinkMethod.getName().stringValue(), theLinkMethod.getSignature());
            }
        });
    }
}