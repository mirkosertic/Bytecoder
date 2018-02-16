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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.classlib.java.lang.TClass;
import de.mirkosertic.bytecoder.graph.Node;

public class BytecodeLinkedClass extends Node {

    public static final BytecodeMethodSignature GET_CLASS_SIGNATURE = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TClass.class), new BytecodeTypeRef[0]);
    public static final BytecodeMethod GET_CLASS_PLACEHOLDER = new BytecodeMethod(new BytecodeAccessFlags(0x0001), null, null, null) {
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
        private final BytecodeObjectTypeRef declaringType;
        private final BytecodeMethod targetMethod;

        public LinkedMethod(BytecodeObjectTypeRef aDeclaringType, BytecodeMethod aTargetMethod) {
            if (aDeclaringType == null) {
                throw new IllegalStateException();
            }
            if (aTargetMethod == null) {
                throw new IllegalStateException();
            }
            declaringType = aDeclaringType;
            targetMethod = aTargetMethod;
        }

        public BytecodeObjectTypeRef getDeclaringType() {
            return declaringType;
        }

        public BytecodeMethod getTargetMethod() {
            return targetMethod;
        }
    }

    private final int uniqueId;
    private final BytecodeObjectTypeRef className;
    private final BytecodeClass bytecodeClass;
    private final Map<BytecodeVirtualMethodIdentifier, LinkedMethod> linkedMethods;
    private final BytecodeLinkerContext linkerContext;
    private final Set<BytecodeMethod> knownMethods;
    private BytecodeMethod classInitializer;

    public BytecodeLinkedClass(int aUniqueId, BytecodeLinkerContext aLinkerContext, BytecodeObjectTypeRef aClassName, BytecodeClass aBytecodeClass) {
        uniqueId = aUniqueId;
        className = aClassName;
        bytecodeClass = aBytecodeClass;
        linkedMethods = new HashMap<>();
        linkerContext = aLinkerContext;
        knownMethods = new HashSet<>();
    }

    public boolean hasMethod(BytecodeMethod aMethod) {
        for (BytecodeMethod theMethod : knownMethods) {
            if (Objects.equals(aMethod.getName().stringValue(), theMethod.getName().stringValue()) &&
                    aMethod.getSignature().metchesExactlyTo(theMethod.getSignature())) {
                return true;
            }
        }
        return false;
    }

    public boolean emulatedByRuntime() {
        return bytecodeClass.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) != null;
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
        outgoingEdges(BytecodeImplementsEdgeType.filter()).forEach(edge -> {
            BytecodeLinkedClass theLinkedClass = (BytecodeLinkedClass) edge.targetNode();
            theResult.addAll(theLinkedClass.getImplementingTypes());
        });
        BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null && aIncludeSuperClass) {
            theResult.addAll(theSuperClass.getImplementingTypes());
        }
        return theResult;
    }

    public BytecodeLinkedClass getSuperClass() {
        return (BytecodeLinkedClass) singleOutgoingNodeMatching(
                BytecodeSubclassOfEdgeType.filter()).orElse(null);
    }

    public void linkClassInitializer(BytecodeMethod aMethod) {
        classInitializer = aMethod;
        linkStaticMethod(aMethod.getName().stringValue(), aMethod.getSignature());
    }

    public boolean linkStaticField(BytecodeUtf8Constant aName) {
        String theFieldName = aName.stringValue();

        // Do we already have a link?
        Map<String, BytecodeField> theFields = new HashMap<>();
        outgoingEdges(BytecodeProvidesFieldEdgeType.filter()).map(t-> (BytecodeField) t.targetNode()).forEach(t -> theFields.put(t.getName().stringValue(), t));

        BytecodeField theField = theFields.get(theFieldName);
        if (theField != null) {
            if (!theField.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Field " + theFieldName + " is not static in " + className.name());
            }
            return true;
        }

        theField = bytecodeClass.fieldByName(theFieldName);
        if (theField != null) {
            if (!theField.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Field " + theFieldName + " is not static in " + className.name());
            }

            addEdgeTo(new BytecodeProvidesFieldEdgeType(), theField);

            linkerContext.linkTypeRef(theField.getTypeRef());

            return true;
        }

        // Implementing interfaces can also provide static fields
        for (BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            if (theImplementedInterface.linkStaticField(aName)) {
                return true;
            }
        }

        BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.linkStaticField(aName);
        }

        return false;
    }

    public boolean linkField(BytecodeUtf8Constant aName) {

        String theFieldName = aName.stringValue();

        // Do we already have a link?
        Map<String, BytecodeField> theFields = new HashMap<>();
        outgoingEdges(BytecodeProvidesFieldEdgeType.filter()).map(t-> (BytecodeField) t.targetNode()).forEach(t -> theFields.put(t.getName().stringValue(), t));

        BytecodeField theField = theFields.get(theFieldName);
        if (theField != null) {
            if (theField.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Field " + theFieldName + " is static in " + className.name());
            }
            return true;
        }

        theField = bytecodeClass.fieldByName(theFieldName);
        if (theField != null) {
            if (theField.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Field " + theFieldName + " is static in " + className.name());
            }

            addEdgeTo(new BytecodeProvidesFieldEdgeType(), theField);

            linkerContext.linkTypeRef(theField.getTypeRef());

            return true;
        }

        BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.linkField(aName);
        }

        return false;
    }

    public BytecodeFieldMap instanceFieldMap() {
        BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeFieldMap theMap = theSuperclass != null ? theSuperclass.instanceFieldMap() : new BytecodeFieldMap();

        outgoingEdges(BytecodeProvidesFieldEdgeType.filter())
                .map(t -> (BytecodeField) t.targetNode()).forEach(aField -> {
            if (!aField.getAccessFlags().isStatic()) {
                theMap.register(BytecodeLinkedClass.this, aField);
            }
        });
        return theMap;
    }

    public BytecodeFieldMap staticFieldMap() {
        BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeFieldMap theMap = theSuperclass != null ? theSuperclass.staticFieldMap() : new BytecodeFieldMap();

        for (BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            BytecodeFieldMap theInterfaceFields = theImplementedInterface.staticFieldMap();
            theMap.merge(theInterfaceFields);
        }

        outgoingEdges(BytecodeProvidesFieldEdgeType.filter())
                .map(t -> (BytecodeField) t.targetNode()).forEach(aField -> {
            if (aField.getAccessFlags().isStatic()) {
                theMap.register(BytecodeLinkedClass.this, aField);
            }
        });
        return theMap;
    }

    public BytecodeMethodMap methodsMap() {
        BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeMethodMap theMap = theSuperclass != null ? theSuperclass.methodsMap() : new BytecodeMethodMap();

        for (BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            BytecodeMethodMap theInterfaceMethods = theImplementedInterface.methodsMap();
            theMap.merge(theInterfaceMethods);
        }

        outgoingEdges(BytecodeProvidesMethodEdgeType.filter())
                .map(t -> (BytecodeMethod) t.targetNode()).forEach(aMethod -> {
            theMap.register(BytecodeLinkedClass.this, aMethod);
        });
        return theMap;
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

            if (Objects.equals("getClass", aMethodName) && GET_CLASS_SIGNATURE.metchesExactlyTo(aSignature)) {

                BytecodeVirtualMethodIdentifier theIdentifier = linkerContext.getMethodCollection()
                        .identifierFor("getClass", GET_CLASS_SIGNATURE);

                if (!linkedMethods.containsKey(theIdentifier)) {

                    linkedMethods.put(theIdentifier, new LinkedMethod(theClassName, GET_CLASS_PLACEHOLDER));
                    // We also have to propagate this to the subclasses
                    for (BytecodeLinkedClass theSubClass : linkerContext.getSubclassesOf(this)) {
                        theSubClass.linkVirtualMethod(aMethodName, aSignature);
                    }
                }
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
                        linkerContext.linkClass(theClassName).linkVirtualMethod(aMethodName, aSignature);
                    }

                    // Ok, now we know this method is virtual
                    // We have to propagate it over all known subclasses to make it also reachable
                    for (BytecodeLinkedClass theSubClass : linkerContext.getSubclassesOf(this)) {
                        theSubClass.linkVirtualMethod(aMethodName, aSignature);
                    }

                    // In case this is in interface, we also have to link implementing classes of this interface
                    if (bytecodeClass.getAccessFlags().isInterface()) {
                        for (BytecodeLinkedClass theSubClass : linkerContext.getImplementingClassesOf(this)) {
                            theSubClass.linkVirtualMethod(aMethodName, aSignature);
                        }
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
            throw new IllegalArgumentException("No such name : " + aMethodName + " with signature " + aSignature + " in " + className.name());
            // We need to traverse
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while linking virtual method name " + aMethodName + " for " + className.name(), e);
        }
    }

    public void linkConstructorInvocation(BytecodeMethodSignature aMethodSignature) {

        BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull("<init>", aMethodSignature);
        if (theMethod == null) {
            linkerContext.getLogger().warn("No constructor with signature {} in  {}", aMethodSignature , className.name());
            return;
        }

        if (!knownMethods.contains(theMethod)) {
            linkMethodInternal(theMethod, true);
        }
    }

    public void linkPrivateMethod(String aMethodName, BytecodeMethodSignature aMethodSignature) {

        BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aMethodSignature);
        if (theMethod == null) {
            linkerContext.getLogger().warn("No method {} with signature {} in {}", aMethodName, aMethodSignature, className.name());
            return;
        }

        if (!knownMethods.contains(theMethod)) {
            linkMethodInternal(theMethod, true);
        }
    }

    public BytecodeLinkedClass linkStaticMethod(String aMethodName, BytecodeMethodSignature aMethodSignature) {

        BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aMethodSignature);
        if (theMethod != null) {
            if (!theMethod.isClassInitializer()) {
                BytecodeVirtualMethodIdentifier theIdentifier = linkerContext.getMethodCollection().identifierFor(theMethod);
                linkedMethods.put(theIdentifier, new LinkedMethod(className, theMethod));
            }

            linkMethodInternal(theMethod, true);
            return this;
        }

        if (bytecodeClass.getSuperClass() != BytecodeClassinfoConstant.OBJECT_CLASS) {
            return linkerContext.linkClass( BytecodeObjectTypeRef.fromUtf8Constant(bytecodeClass.getSuperClass().getConstant())).linkStaticMethod(aMethodName, aMethodSignature);
        }

        throw new IllegalArgumentException("No such name : " + aMethodName + " with signature " + aMethodSignature);
    }

    private void linkMethodInternal(BytecodeMethod aMethod, boolean isLocal) {
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
                if (bytecodeClass.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) == null) {
                    // Will be linked dynamically
                    // No need to worry
                }
            } else {
                BytecodeCodeAttributeInfo theCode = aMethod.getCode(bytecodeClass);
                BytecodeProgram theProgram = theCode.getProgramm();
                for (BytecodeInstruction theInstruction : theProgram.getInstructions()) {
                    theInstruction.performLinking(bytecodeClass, linkerContext);
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

    public boolean hasClassInitializer() {
        return classInitializer != null;
    }

    public boolean containsVirtualMethod(BytecodeVirtualMethodIdentifier aIdentifier) {
        return linkedMethods.containsKey(aIdentifier);
    }

    public BytecodeImportedLink linkfor(BytecodeMethod aMethod) {
        BytecodeAnnotation theImportAnnotation = aMethod.getAttributes().getAnnotationByType(Import.class.getName());
        if (theImportAnnotation == null) {
            String theClassName = className.name();
            int p = theClassName.lastIndexOf('.');
            if (p>=0) {
                theClassName = theClassName.substring(p + 1);
            }
            // No import declaration found
            // By default we derive the module name from the classname to lower case
            // the link name is the method name
            return new BytecodeImportedLink(
                theClassName.toLowerCase(),
                aMethod.getName().stringValue()
            );
        }
        return new BytecodeImportedLink(theImportAnnotation.getElementValueByName("module").stringValue(),
                theImportAnnotation.getElementValueByName("name").stringValue());
    }
}