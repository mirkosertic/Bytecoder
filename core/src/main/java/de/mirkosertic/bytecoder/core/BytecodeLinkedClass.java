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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BytecodeLinkedClass extends Node<Node, EdgeType> {

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

    private final int uniqueId;
    private final BytecodeObjectTypeRef className;
    private final BytecodeClass bytecodeClass;
    private final BytecodeLinkerContext linkerContext;
    private BytecodeMethod classInitializer;
    private Boolean opaque;
    private Boolean callback;
    private Boolean event;
    private final BytecodeLinkedClass superClass;

    public BytecodeLinkedClass(final BytecodeLinkedClass aSuperclass, final int aUniqueId, final BytecodeLinkerContext aLinkerContext, final BytecodeObjectTypeRef aClassName, final BytecodeClass aBytecodeClass) {
        uniqueId = aUniqueId;
        className = aClassName;
        bytecodeClass = aBytecodeClass;
        linkerContext = aLinkerContext;
        superClass = aSuperclass;

        if (superClass != null) {
            addEdgeTo(BytecodeSubclassOfEdgeType.instance, superClass);
        }
    }

    public boolean isOpaqueType() {
        if (opaque != null) {
            return opaque;
        }
        final Set<BytecodeLinkedClass> theImplementingTypes = getImplementingTypes();
        for (final BytecodeLinkedClass theClass : theImplementingTypes) {
            if (theClass.getClassName().name().equals(OpaqueReferenceType.class.getName())) {
                opaque = true;
                return opaque;
            }
        }
        opaque = false;
        return opaque;
    }

    public boolean isCallback() {
        if (callback != null) {
            return callback;
        }
        final Set<BytecodeLinkedClass> theImplementingTypes = getImplementingTypes();
        for (final BytecodeLinkedClass theClass : theImplementingTypes) {
            if (theClass.getClassName().name().equals(Callback.class.getName())) {
                callback = true;
                return callback;
            }
        }
        callback = false;
        return callback;
    }

    public boolean isEvent() {
        if (event != null) {
            return event;
        }
        final Set<BytecodeLinkedClass> theImplementingTypes = getImplementingTypes();
        for (final BytecodeLinkedClass theClass : theImplementingTypes) {
            if (theClass.getClassName().name().equals(Event.class.getName())) {
                event = true;
                return event;
            }
        }
        event = false;
        return event;
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

    private final Map<String, Set<BytecodeLinkedClass>> implementingTypesCache = new HashMap<>();

    public Set<BytecodeLinkedClass> getImplementingTypes() {
        return getImplementingTypes(true, true);
    }

    public Set<BytecodeLinkedClass> getImplementingTypes(final boolean aIncludeSuperClass, final boolean aIncludeSelf) {
        final String key = aIncludeSuperClass + "_" + aIncludeSelf;
        return implementingTypesCache.computeIfAbsent(key, aKey -> {
            final Set<BytecodeLinkedClass> theTempResult = new HashSet<>();
            if (aIncludeSelf) {
                theTempResult.add(BytecodeLinkedClass.this);
            }
            outgoingEdges(BytecodeImplementsEdgeType.filter()).forEach(edge -> {
                final BytecodeLinkedClass theLinkedClass = (BytecodeLinkedClass) edge.targetNode();
                theTempResult.addAll(theLinkedClass.getImplementingTypes());
            });
            if (aIncludeSuperClass) {
                final BytecodeLinkedClass theSuperClass = getSuperClass();
                if (theSuperClass != null) {
                    theTempResult.addAll(theSuperClass.getImplementingTypes());
                }
            }
            return theTempResult;
        });
    }

    public BytecodeLinkedClass getSuperClass() {
        return superClass;
    }

    public void resolveClassInitializer(final BytecodeMethod aMethod) {
        classInitializer = aMethod;
        resolveStaticMethod(aMethod.getName().stringValue(), aMethod.getSignature());
    }

    public boolean resolveStaticField(final BytecodeUtf8Constant aName) {
        final String theFieldName = aName.stringValue();

        if (outgoingEdges(BytecodeProvidesFieldEdgeType.filter())
                .map(t -> (BytecodeField) t.targetNode())
                .anyMatch(t -> Objects.equals(t.getName().stringValue(), theFieldName) && t.getAccessFlags().isStatic())) {
            return true;
        }

        final BytecodeField theField = bytecodeClass.fieldByName(theFieldName);
        if (theField != null) {
            if (!theField.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Field " + theFieldName + " is not static in " + className.name());
            }

            addEdgeTo(BytecodeProvidesFieldEdgeType.instance, theField);

            linkerContext.resolveTypeRef(theField.getTypeRef());

            return true;
        }

        // Implementing interfaces can also provide static fields
        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            if (theImplementedInterface.resolveStaticField(aName)) {
                return true;
            }
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.resolveStaticField(aName);
        }

        return false;
    }

    public boolean resolveInstanceField(final BytecodeUtf8Constant aName) {

        final String theFieldName = aName.stringValue();

        // Do we already have a link?
        final Map<String, BytecodeField> theFields = new HashMap<>();
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

            addEdgeTo(BytecodeProvidesFieldEdgeType.instance, theField);

            linkerContext.resolveTypeRef(theField.getTypeRef());

            return true;
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.resolveInstanceField(aName);
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

        outgoingEdges(BytecodeProvidesFieldEdgeType.filter())
                .map(t -> (BytecodeField) t.targetNode()).forEach(aField -> theMap.register(BytecodeLinkedClass.this, aField));
        return theMap;
    }

    public BytecodeVTable resolveVTable() {

        final BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeVTable theTable = theSuperclass != null ? theSuperclass.resolveVTable() : new BytecodeVTable();

        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            theImplementedInterface.outgoingEdges(BytecodeProvidesMethodEdgeType.filter()).forEach(c -> {
                final BytecodeLinkedClass theClass = (BytecodeLinkedClass) c.sourceNode();
                final BytecodeMethod theMethod = (BytecodeMethod) c.targetNode();
                if (!theMethod.isClassInitializer() && !theMethod.isConstructor() && !theMethod.getAccessFlags().isStatic()) {
                    theTable.register(theMethod, theClass);
                }
            });
        }

        outgoingEdges(BytecodeProvidesMethodEdgeType.filter()).forEach(c -> {
            final BytecodeLinkedClass theClass = (BytecodeLinkedClass) c.sourceNode();
            final BytecodeMethod theMethod = (BytecodeMethod) c.targetNode();
            if (!theMethod.isClassInitializer() && !theMethod.isConstructor() && !theMethod.getAccessFlags().isStatic()) {
                theTable.register(theMethod, theClass);
            }
        });

        return theTable;
    }

    public BytecodeResolvedMethods resolvedMethods() {
        final BytecodeLinkedClass theSuperclass = getSuperClass();
        final BytecodeResolvedMethods theMap = theSuperclass != null ? theSuperclass.resolvedMethods() : new BytecodeResolvedMethods();

        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            final BytecodeResolvedMethods theInterfaceMethods = theImplementedInterface.resolvedMethods();
            theMap.merge(theInterfaceMethods);
        }

        outgoingEdges(BytecodeProvidesMethodEdgeType.filter())
                .forEach(aEdge -> theMap.register((BytecodeLinkedClass) aEdge.sourceNode(), (BytecodeMethod) aEdge.targetNode()));
        return theMap;
    }

    private void link(final BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            return;
        }
        if (aTypeRef instanceof BytecodeArrayTypeRef) {
            final BytecodeArrayTypeRef theArrayRef = (BytecodeArrayTypeRef) aTypeRef;
            link(theArrayRef.getType());
            return;
        }
        if (aTypeRef instanceof BytecodeObjectTypeRef) {
            linkerContext.resolveClass((BytecodeObjectTypeRef) aTypeRef);
        }
    }

    public boolean resolveVirtualMethod(final String aMethodName, final BytecodeMethodSignature aSignature) {

        // Do we already have a link?
        if (outgoingEdges(BytecodeProvidesMethodEdgeType.filter())
                .map(t -> (BytecodeMethod) t.targetNode())
                .anyMatch(t -> Objects.equals(t.getName().stringValue(), aMethodName) && t.getSignature().matchesExactlyTo(aSignature))) {
            return true;
        }

        boolean somethingFound = false;

        // Try to find default methods and also mark usage
        // of interface methods
        for (final BytecodeLinkedClass theImplementedInterface : getImplementingTypes(false, false)) {
            if (theImplementedInterface.resolveVirtualMethod(aMethodName, aSignature)) {
                somethingFound = true;
            }
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod != null) {
            if (theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Method " + aMethodName + " is static in " + className.name());
            }

            addEdgeTo(BytecodeProvidesMethodEdgeType.instance, theMethod);

            resolveMethodSignatureAndBody(theMethod);
            somethingFound = true;
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            if (theSuperClass.resolveVirtualMethod(aMethodName, aSignature)) {
                return true;
            }
        }

        return somethingFound;
    }

    public boolean resolveConstructorInvocation(final BytecodeMethodSignature aSignature) {

        // Do we already have a link?
        if (outgoingEdges(BytecodeProvidesMethodEdgeType.filter())
                .map(t -> (BytecodeMethod) t.targetNode())
                .anyMatch(t -> t.isConstructor() && t.getSignature().matchesExactlyTo(aSignature))) {
            return true;
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull("<init>", aSignature);
        if (theMethod != null) {
            if (theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Constructor <init> is static in " + className.name());
            }

            addEdgeTo(BytecodeProvidesMethodEdgeType.instance, theMethod);

            resolveMethodSignatureAndBody(theMethod);

            return true;
        }

        return false;
    }

    public boolean resolvePrivateMethod(final String aMethodName, final BytecodeMethodSignature aSignature) {

        // Do we already have a link?
        if (outgoingEdges(BytecodeProvidesMethodEdgeType.filter())
                .map(t -> (BytecodeMethod) t.targetNode())
                .anyMatch(t -> Objects.equals(t.getName().stringValue(), aMethodName) && t.getSignature().matchesExactlyTo(aSignature))) {
            return true;
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod != null) {
            if (theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Method " + aMethodName + " is static in " + className.name());
            }

            addEdgeTo(BytecodeProvidesMethodEdgeType.instance, theMethod);

            resolveMethodSignatureAndBody(theMethod);

            return true;
        }

        return false;
    }

    public boolean resolveStaticMethod(final String aMethodName, final BytecodeMethodSignature aSignature) {

        // Do we already have a link?
        if (outgoingEdges(BytecodeProvidesMethodEdgeType.filter())
                .map(t -> (BytecodeMethod) t.targetNode())
                .anyMatch(t -> t.getAccessFlags().isStatic() && Objects.equals(t.getName().stringValue(), aMethodName) && t.getSignature().matchesExactlyTo(aSignature))) {
            return true;
        }

        final BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignatureOrNull(aMethodName, aSignature);
        if (theMethod != null) {
            if (!theMethod.getAccessFlags().isStatic()) {
                throw new IllegalStateException("Method " + aMethodName + " is not static in " + className.name());
            }

            addEdgeTo(BytecodeProvidesMethodEdgeType.instance, theMethod);

            resolveMethodSignatureAndBody(theMethod);

            return true;
        }

        final BytecodeLinkedClass theSuperClass = getSuperClass();
        if (theSuperClass != null) {
            return theSuperClass.resolveStaticMethod(aMethodName, aSignature);
        }

        return false;
    }

    private void resolveMethodSignatureAndBody(final BytecodeMethod aMethod) {
        final BytecodeMethodSignature theSignature = aMethod.getSignature();
        link(theSignature.getReturnType());
        for (final BytecodeTypeRef theArgument : theSignature.getArguments()) {
            link(theArgument);
        }

        if (!aMethod.getAccessFlags().isAbstract()) {
            if (aMethod.getAccessFlags().isNative()) {
                if (bytecodeClass.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName()) == null) {
                    // Will be linked dynamically
                    // No need to worry
                }
            } else {
                final BytecodeCodeAttributeInfo theCode = aMethod.getCode(bytecodeClass);
                final BytecodeProgram theProgram = theCode.getProgram();
                for (final BytecodeInstruction theInstruction : theProgram.getInstructions()) {
                    theInstruction.performLinking(bytecodeClass, linkerContext);
                }

                for (final BytecodeExceptionTableEntry theHandler : theProgram.getExceptionHandlers()) {
                    if (!theHandler.isFinally()) {
                        linkerContext.resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theHandler.getCatchType().getConstant()));
                    }
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

    public void resolveInheritedOverriddenMethods() {
        final Set<BytecodeLinkedClass> theHierarchy = getImplementingTypes(true, false);

        // Now we walk the hierarchy up and try to resolve all abstract methods
        for (final BytecodeLinkedClass theClass : theHierarchy) {
            final BytecodeResolvedMethods theResolvedMethods = theClass.resolvedMethods();
            final List<BytecodeMethod> theInstanceMethods = theResolvedMethods.stream().filter(t -> !t.getValue().getAccessFlags().isPrivate() && !t.getValue().getAccessFlags().isStatic()).map(BytecodeResolvedMethods.MethodEntry::getValue).collect(Collectors.toList());
            for (final BytecodeMethod theMethod : theInstanceMethods) {
                if (!BytecodeLinkedClass.this.resolveVirtualMethod(theMethod.getName().stringValue(), theMethod.getSignature())) {
                    throw new IllegalStateException("Cannot find method " + theMethod.getName() + " with signature " + theMethod.getSignature() + " in class " + theClass.getClassName().name());
                }
            }
        }
    }

    @Override
    public String toString() {
        return className.name();
    }
}