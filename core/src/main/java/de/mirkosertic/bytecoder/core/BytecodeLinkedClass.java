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

import de.mirkosertic.bytecoder.api.Callback;
import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;
import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.graph.EdgeType;
import de.mirkosertic.bytecoder.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BytecodeLinkedClass extends Node<Node, EdgeType> {

    public enum Tag {
        INVOKEINTERFACE_TARGET,
        INVOKESTATIC_TARGET,
        INVOKEVIRTUAL_TARGET,
        INVOKESPECIAL_TARGET,
        INSTANCEOF_CHECKED,
        STATIC_READ_WRITE_ACCESS,
        INSTANTIATED,
        REFERENCED_AS_CONSTANT,
        POSSIBLE_USE_IN_LAMBDA,
        HAS_CLASS_INITIALIZER,
        PROVIDES_DEFAULT_IMPLEMENTATION
    }

    public static final BytecodeMethodSignature GET_CLASS_SIGNATURE = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Class.class), new BytecodeTypeRef[0]);
    public static final BytecodeMethodSignature GET_CLASSLOADER_SIGNATURE = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(ClassLoader.class), new BytecodeTypeRef[0]);
    public static final BytecodeMethodSignature DESIRED_ASSERTION_STATUS_SIGNATURE = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.BOOLEAN, new BytecodeTypeRef[0]);
    public static final BytecodeMethodSignature GET_ENUM_CONSTANTS_SIGNATURE = new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), 1), new BytecodeTypeRef[0]);
    public static final BytecodeMethodSignature CLASS_FOR_NAME_SIGNATURE = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Class.class), new BytecodeTypeRef[] {
            BytecodeObjectTypeRef.fromRuntimeClass(String.class),
            BytecodePrimitiveTypeRef.BOOLEAN,
            BytecodeObjectTypeRef.fromRuntimeClass(ClassLoader.class),
    });
    public static final BytecodeMethodSignature GET_SUPERCLASS_SIGNATURE = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Class.class), new BytecodeTypeRef[0]);

    private static class MethodLink {
        final BytecodeLinkedClass providingClass;
        final BytecodeMethod method;

        public MethodLink(final BytecodeLinkedClass providingClass, final BytecodeMethod method) {
            this.providingClass = providingClass;
            this.method = method;
        }
    }

    private final int uniqueId;
    private final BytecodeObjectTypeRef className;
    private final BytecodeClass bytecodeClass;
    private final BytecodeLinkerContext linkerContext;
    private BytecodeMethod classInitializer;
    private Boolean opaque;
    private Boolean callback;
    private Boolean event;
    private final BytecodeLinkedClass superClass;
    private final Set<BytecodeLinkedClass> implementedInterfaces;
    private final Set<BytecodeLinkedClass> subclasses;
    private final Set<Tag> tags;
    private final List<MethodLink> resolvedMethods;
    private final List<BytecodeField> resolvedFields;

    public BytecodeLinkedClass(final BytecodeLinkedClass aSuperclass, final int aUniqueId, final BytecodeLinkerContext aLinkerContext, final BytecodeObjectTypeRef aClassName, final BytecodeClass aBytecodeClass) {
        uniqueId = aUniqueId;
        className = aClassName;
        bytecodeClass = aBytecodeClass;
        linkerContext = aLinkerContext;
        superClass = aSuperclass;
        tags = new HashSet<>();
        subclasses = new HashSet<>();
        implementedInterfaces = new HashSet<>();
        resolvedMethods = new ArrayList<>();
        resolvedFields = new ArrayList<>();

        if (superClass != null) {
            subclasses.add(this);
        }
    }

    public void addImplementedInterface(final BytecodeLinkedClass implementedInterface) {
        implementedInterfaces.add(implementedInterface);
        implementedInterface.subclasses.add(this);
    }

    public BytecodeLinkedClass tagWith(final Tag tag) {
        tags.add(tag);
        return this;
    }

    public boolean hasTag(final Tag tag) {
        return tags.contains(tag);
    }

    public boolean hasTags() {
        return !tags.isEmpty();
    }

    public boolean isOpaqueType() {
        if (opaque != null) {
            return opaque;
        }
        final Set<BytecodeLinkedClass> theImplementingTypes = getImplementingTypes();
        for (final BytecodeLinkedClass theClass : theImplementingTypes) {
            if (theClass.getClassName().name().equals(OpaqueReferenceType.class.getName())) {
                opaque = true;
                return true;
            }
        }
        opaque = false;
        return false;
    }

    public boolean isCallback() {
        if (callback != null) {
            return callback;
        }
        final Set<BytecodeLinkedClass> theImplementingTypes = getImplementingTypes();
        for (final BytecodeLinkedClass theClass : theImplementingTypes) {
            if (theClass.getClassName().name().equals(Callback.class.getName())) {
                callback = true;
                return true;
            }
        }
        callback = false;
        return false;
    }

    public boolean isEvent() {
        if (event != null) {
            return event;
        }
        final Set<BytecodeLinkedClass> theImplementingTypes = getImplementingTypes();
        for (final BytecodeLinkedClass theClass : theImplementingTypes) {
            if (theClass.getClassName().name().equals(Event.class.getName())) {
                event = true;
                return true;
            }
        }
        event = false;
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

    public Set<BytecodeLinkedClass> getImplementingTypes(final boolean aIncludeSuperClass, final boolean aIncludeSelf) {

        final Set<BytecodeLinkedClass> theTempResult = new HashSet<>();
        if (aIncludeSelf) {
            theTempResult.add(BytecodeLinkedClass.this);
        }
        for (final BytecodeLinkedClass implementedInterface : implementedInterfaces) {
            theTempResult.addAll(implementedInterface.getImplementingTypes());
        }
        if (aIncludeSuperClass) {
            final BytecodeLinkedClass theSuperClass = getSuperClass();
            if (theSuperClass != null) {
                theTempResult.addAll(theSuperClass.getImplementingTypes());
            }
        }
        return theTempResult;
    }

    public BytecodeLinkedClass getSuperClass() {
        return superClass;
    }

    public void resolveClassInitializer(final BytecodeMethod aMethod, final AnalysisStack analysisStack) {
        classInitializer = aMethod;
        resolveStaticMethod(aMethod.getName().stringValue(), aMethod.getSignature(), analysisStack);
    }

    public boolean resolveStaticField(final BytecodeUtf8Constant aName, final AnalysisStack analysisStack) {
        final String theFieldName = aName.stringValue();

        for (final BytecodeField field : resolvedFields) {
            if (Objects.equals(field.getName().stringValue(), theFieldName) && field.getAccessFlags().isStatic()) {
                return true;
            }
        }

        final BytecodeField theField = bytecodeClass.fieldByName(theFieldName);
        if (theField != null) {
            if (!theField.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Field " + theFieldName + " is not static in " + className.name());
            }

            resolvedFields.add(theField);

            linkerContext.resolveTypeRef(theField.getTypeRef(), analysisStack);

            return true;
        }

        // Implementing interfaces can also provide static fields
        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            if (theImplementedInterface.resolveStaticField(aName, analysisStack)) {
                return true;
            }
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.resolveStaticField(aName, analysisStack);
        }

        return false;
    }

    public boolean resolveInstanceField(final BytecodeUtf8Constant aName, final AnalysisStack analysisStack) {

        final String theFieldName = aName.stringValue();

        // Do we already have a link?
        final Map<String, BytecodeField> theFields = new HashMap<>();

        for (final BytecodeField field : resolvedFields) {
            theFields.put(field.getName().stringValue(), field);
        }

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

            resolvedFields.add(theField);

            linkerContext.resolveTypeRef(theField.getTypeRef(), analysisStack);

            return true;
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.resolveInstanceField(aName, analysisStack);
        }

        return false;
    }

    public BytecodeResolvedFields resolvedFields() {
        final BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeResolvedFields theMap = theSuperclass != null ? theSuperclass.resolvedFields() : new BytecodeResolvedFields();

        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            final BytecodeResolvedFields theInterfaceFields = theImplementedInterface.resolvedFields();
            theMap.merge(theInterfaceFields);
        }

        for (final BytecodeField field : resolvedFields) {
            theMap.register(this, field);
        }
        return theMap;
    }

    public BytecodeVTable resolveVTable() {

        final BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeVTable theTable = theSuperclass != null ? theSuperclass.resolveVTable() : new BytecodeVTable();

        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            for (final MethodLink methodLink : theImplementedInterface.resolvedMethods) {
                final BytecodeLinkedClass providingClass = methodLink.providingClass;
                final BytecodeMethod method = methodLink.method;
                if (!method.isClassInitializer() && !method.isConstructor() && !method.getAccessFlags().isStatic()) {
                    theTable.register(method, providingClass);
                }
            }
        }

        for (final MethodLink link : resolvedMethods) {
            final BytecodeLinkedClass theClass = link.providingClass;
            final BytecodeMethod theMethod = link.method;
            if (!theMethod.isClassInitializer() && !theMethod.isConstructor() && !theMethod.getAccessFlags().isStatic()) {
                theTable.register(theMethod, theClass);
            }
        }

        return theTable;
    }

    public BytecodeResolvedMethods resolvedMethods() {
        final BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeResolvedMethods theMap = theSuperclass != null ? theSuperclass.resolvedMethods() : new BytecodeResolvedMethods();

        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            final BytecodeResolvedMethods theInterfaceMethods = theImplementedInterface.resolvedMethods();
            theMap.merge(theInterfaceMethods);
        }

        for (final MethodLink link : resolvedMethods) {
            theMap.register(link.providingClass, link.method);
        }
        return theMap;
    }

    public boolean resolveVirtualMethod(final String aMethodName, final BytecodeMethodSignature aSignature, final AnalysisStack analysisStack) {

        // Do we already have a link?
        for (final MethodLink link : resolvedMethods) {
            if (Objects.equals(link.method.getName().stringValue(), aMethodName) && link.method.getSignature().matchesExactlyTo(aSignature)) {
                return true;
            }
        }

        boolean somethingFound = false;

        // Try to find default methods and also mark usage
        // of interface methods
        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            if (theImplementedInterface.resolveVirtualMethod(aMethodName, aSignature, analysisStack)) {
                somethingFound = true;
            }
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod != null) {
            if (theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Method " + aMethodName + " is static in " + className.name());
            }

            resolvedMethods.add(new MethodLink(this, theMethod));

            resolveMethodSignatureAndBody(theMethod, analysisStack);
            somethingFound = true;
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            if (theSuperClass.resolveVirtualMethod(aMethodName, aSignature, analysisStack)) {
                return true;
            }
        }

        return somethingFound;
    }

    public boolean resolveConstructorInvocation(final BytecodeMethodSignature aSignature, final AnalysisStack analysisStack) {

        // Do we already have a link?
        for (final MethodLink link : resolvedMethods) {
            if (link.method.isConstructor() && link.method.getSignature().matchesExactlyTo(aSignature)) {
                return true;
            }
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull("<init>", aSignature);
        if (theMethod != null) {
            if (theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Constructor <init> is static in " + className.name());
            }

            resolvedMethods.add(new MethodLink(this, theMethod));

            resolveMethodSignatureAndBody(theMethod, analysisStack);

            return true;
        }

        return false;
    }

    public boolean resolvePrivateMethod(final String aMethodName, final BytecodeMethodSignature aSignature, final AnalysisStack analysisStack) {

        // Do we already have a link?
        for (final MethodLink link : resolvedMethods) {
            if (Objects.equals(link.method.getName().stringValue(), aMethodName) && link.method.getSignature().matchesExactlyTo(aSignature)) {
                return true;
            }
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod != null) {
            if (theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Method " + aMethodName + " is static in " + className.name());
            }

            resolvedMethods.add(new MethodLink(this, theMethod));

            resolveMethodSignatureAndBody(theMethod, analysisStack);

            return true;
        }

        return false;
    }

    public boolean resolveStaticMethod(final String aMethodName, final BytecodeMethodSignature aSignature, final AnalysisStack analysisStack) {

        // Do we already have a link?
        for (final MethodLink link : resolvedMethods) {
            if (link.method.getAccessFlags().isStatic() && Objects.equals(link.method.getName().stringValue(), aMethodName) && link.method.getSignature().matchesExactlyTo(aSignature)) {
                return true;
            }
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod != null) {
            if (!theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Method " + aMethodName + " is not static in " + className.name());
            }

            theMethod.tagWith(BytecodeMethod.Tag.IMPLEMENTATION_USED);
            resolvedMethods.add(new MethodLink(this, theMethod));

            resolveMethodSignatureAndBody(theMethod, analysisStack);

            return true;
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.resolveStaticMethod(aMethodName, aSignature, analysisStack);
        }

        return false;
    }

    private void resolveMethodSignatureAndBody(final BytecodeMethod aMethod, final AnalysisStack analysisStack) {
        final BytecodeMethodSignature theSignature = aMethod.getSignature();
        linkerContext.resolveTypeRef(theSignature.getReturnType(), analysisStack);
        for (final BytecodeTypeRef argumentType : theSignature.getArguments()) {
            linkerContext.resolveTypeRef(argumentType, analysisStack);
        }

        if (!aMethod.getAccessFlags().isAbstract()) {
            if (aMethod.getAccessFlags().isNative()) {
                if (bytecodeClass.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) == null) {
                    // Will be linked dynamically
                    // No need to worry
                }
            } else {

                final AnalysisStack.Frame methodInvocationFrame = analysisStack.startMethodInvocation(this.className, aMethod.getName().stringValue(), aMethod.getSignature());
                try {

                    final BytecodeCodeAttributeInfo theCode = aMethod.getCode(bytecodeClass);
                    final BytecodeProgram theProgram = theCode.getProgram();
                    for (final BytecodeInstruction theInstruction : theProgram.getInstructions()) {
                        theInstruction.performLinking(bytecodeClass, linkerContext, analysisStack);
                    }

                    for (final BytecodeExceptionTableEntry theHandler : theProgram.getExceptionHandlers()) {
                        if (!theHandler.isFinally()) {
                            linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theHandler.getCatchType().getConstant()), analysisStack);
                        }
                    }
                } finally {
                    methodInvocationFrame.close();
                }
            }
        }
    }

    public BytecodeClass getBytecodeClass() {
        return bytecodeClass;
    }

    public boolean hasClassInitializer() {
        return classInitializer != null;
    }

    private String simpleClassNameOf(final String aFullQualifiedClassName) {
        final int p = aFullQualifiedClassName.lastIndexOf('.');
        if (p>=0) {
            return aFullQualifiedClassName.substring(p + 1);
        }
        return aFullQualifiedClassName;
    }

    public BytecodeImportedLink linkFor(final BytecodeMethod aMethod) {
        final BytecodeAnnotation theImportAnnotation = aMethod.getAttributes().getAnnotationByType(Import.class.getName());
        if (theImportAnnotation == null) {
            final String theClassName = simpleClassNameOf(className.name());
            final StringBuilder theMethodName = new StringBuilder(aMethod.getName().stringValue());
            for (final BytecodeTypeRef theArgument : aMethod.getSignature().getArguments()) {
                theMethodName.append(simpleClassNameOf(theArgument.name()));
            }

            // No import declaration found
            // By default we derive the module name from the classname to lower case
            // the link name is the method name
            return new BytecodeImportedLink(
                theClassName.toLowerCase(),
                    theMethodName.toString()
            );
        }
        return new BytecodeImportedLink(theImportAnnotation.getElementValueByName("module").stringValue(),
                theImportAnnotation.getElementValueByName("name").stringValue());
    }

    public void resolveInheritedOverriddenMethods(final AnalysisStack analysisStack) {
        final Set<BytecodeLinkedClass> theHierarchy = getImplementingTypes(true, false);

        // Now we walk the hierarchy up and try to resolve all abstract methods
        for (final BytecodeLinkedClass theClass : theHierarchy) {
            final BytecodeResolvedMethods theResolvedMethods = theClass.resolvedMethods();
            final List<BytecodeMethod> theInstanceMethods = theResolvedMethods.stream().filter(t -> !t.getValue().getAccessFlags().isPrivate() && !t.getValue().getAccessFlags().isStatic()).map(BytecodeResolvedMethods.MethodEntry::getValue).collect(Collectors.toList());
            for (final BytecodeMethod theMethod : theInstanceMethods) {
                if (!BytecodeLinkedClass.this.resolveVirtualMethod(theMethod.getName().stringValue(), theMethod.getSignature(), analysisStack)) {
                    throw new IllegalStateException("Cannot find method " + theMethod.getName() + " with signature " + theMethod.getSignature() + " in class " + theClass.getClassName().name());
                }
            }
        }
    }

    @Override
    public String toString() {
        return className.name();
    }

    public ReflectionConfiguration.ReflectiveClass reflectiveClass() {
        return linkerContext.reflectionConfiguration().resolve(getClassName().name());
    }
}