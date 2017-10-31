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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import de.mirkosertic.bytecoder.annotations.Export;

public class BytecodeLinkerContext {

    private final Map<BytecodeObjectTypeRef, BytecodeLinkedClass> linkedClasses;
    private final BytecodeLoader loader;
    private final BytecodeMethodCollection methodCollection;

    public BytecodeLinkerContext(BytecodeLoader aLoader) {
        linkedClasses = new HashMap<>();
        loader = aLoader;
        methodCollection = new BytecodeMethodCollection();
    }

    public BytecodeSignatureParser getSignatureParser() {
        return loader.getSignatureParser();
    }

    public BytecodeMethodCollection getMethodCollection() {
        return methodCollection;
    }

    public BytecodeLinkedClass isLinkedOrNull(BytecodeUtf8Constant aConstant) {
        BytecodeObjectTypeRef theTypeRef = BytecodeObjectTypeRef.fromUtf8Constant(aConstant);
        return linkedClasses.get(theTypeRef);
    }

    public BytecodeLinkedClass linkClass(BytecodeObjectTypeRef aTypeRef) {

        BytecodeLinkedClass theLinkedClass = linkedClasses.get(aTypeRef);
        if (theLinkedClass != null) {
            return theLinkedClass;
        }

        try {
            BytecodeLinkedClass theParentClass = null;
            BytecodeClass theLoadedClass = loader.loadByteCode(aTypeRef);
            BytecodeClassinfoConstant theSuperClass = theLoadedClass.getSuperClass();
            if (theSuperClass != BytecodeClassinfoConstant.OBJECT_CLASS) {
                BytecodeUtf8Constant theSuperClassName = theSuperClass.getConstant();
                theParentClass = linkClass(BytecodeObjectTypeRef.fromUtf8Constant(theSuperClassName));
            }

            theLinkedClass = new BytecodeLinkedClass(linkedClasses.size(), theParentClass, this, aTypeRef, theLoadedClass);
            linkedClasses.put(aTypeRef, theLinkedClass);

            for (BytecodeMethod theMethod : theLoadedClass.getMethods()) {
                BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                if (theAnnotation != null) {
                    // The method should be exported
                    if (theMethod.getAccessFlags().isStatic()) {
                        theLinkedClass.linkStaticMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                    } else {
                        theLinkedClass.linkVirtualMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                    }
                }
            }

            BytecodeMethod theMethod = theLoadedClass.classInitializerOrNull();
            if (theMethod != null) {
                theLinkedClass.linkClassInitializer(theMethod);
            }

            for (BytecodeInterface theInterface : theLoadedClass.getInterfaces()) {
                BytecodeUtf8Constant theSuperClassName = theInterface.getClassinfoConstant().getConstant();
                linkClass(BytecodeObjectTypeRef.fromUtf8Constant(theSuperClassName));
            }

            // Ok, we know that this class is newly linked
            // We automatically link every virtual method for the superclasses and implementing interfaces
            final BytecodeLinkedClass theFinalClass = theLinkedClass;
            for (BytecodeLinkedClass theSuperClassOrType : theLinkedClass.getImplementingTypes(true, false)) {
                // Makre sure all fields are known
                theSuperClassOrType.forEachMemberField(
                        aEntry -> theFinalClass.linkField(new BytecodeUtf8Constant(aEntry.getKey())));

                // Also link the virtual methods
                theSuperClassOrType.forEachVirtualMethod(
                        aEntry -> {
                            BytecodeMethod theMethod1 = aEntry.getValue().getTargetMethod();
                            theFinalClass.linkVirtualMethod(theMethod1.getName().stringValue(), theMethod1.getSignature());
                        });
            }

            System.out.println("Linked " + theLinkedClass.getClassName().name());

            return theLinkedClass;
        } catch (Exception e) {
            throw new RuntimeException("Error linking class " + aTypeRef.name(), e);
        }
    }

    public void forEachClass(Consumer<Map.Entry<BytecodeObjectTypeRef, BytecodeLinkedClass>> aConsumer) {
        linkedClasses.entrySet().forEach(aConsumer);
    }

    private List<BytecodeLinkedClass> findLinkedClassWithParent(BytecodeLinkedClass aParent) {
        List<BytecodeLinkedClass> theResult = new ArrayList<>();
        linkedClasses.entrySet().forEach(aEntry -> {
            if (aEntry.getValue().getSuperClass() == aParent) {
                theResult.add(aEntry.getValue());
            }
        });
        return theResult;
    }

    public void linkTypeRef(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isVoid()) {
            return;
        }
        if (aTypeRef.isPrimitive()) {
            return;
        }
        if (aTypeRef.isArray()) {
            BytecodeArrayTypeRef theArray = (BytecodeArrayTypeRef) aTypeRef;
            linkTypeRef(theArray.getType());
            return;
        }
        BytecodeObjectTypeRef theTypeRef = (BytecodeObjectTypeRef) aTypeRef;
        linkClass(theTypeRef);
    }

    public void addSubclassesOfToSet(Set<BytecodeLinkedClass> aTarget, BytecodeLinkedClass aSuperClass) {
        for (BytecodeLinkedClass theClass : linkedClasses.values()) {
            if (theClass.getSuperClass() == aSuperClass) {
                aTarget.add(theClass);
                aTarget.addAll(getSubclassesOf(theClass));
            }
        }
    }

    public Set<BytecodeLinkedClass> getSubclassesOf(BytecodeLinkedClass aParentClass) {
        Set<BytecodeLinkedClass> theClasses = new HashSet<>();
        addSubclassesOfToSet(theClasses, aParentClass);
        return theClasses;
    }

    public Set<BytecodeLinkedClass> getImplementingClassesOf(BytecodeLinkedClass aInterface) {
        Set<BytecodeLinkedClass> theClasses = new HashSet<>();
        for (BytecodeLinkedClass theClass : linkedClasses.values()) {
            if (theClass.getImplementingTypes(true, false).contains(aInterface)) {
                theClasses.add(theClass);
            }
        }
        return theClasses;
    }

    public List<BytecodeLinkedClass> getClassesImplementingVirtualMethod(BytecodeVirtualMethodIdentifier aIdentifier) {
        List<BytecodeLinkedClass> theResult = new ArrayList<>();
        for (BytecodeLinkedClass theClass : linkedClasses.values()) {
            if (theClass.containsVirtualMethod(aIdentifier) && !theClass.getBytecodeClass().getAccessFlags().isInterface()) {
                theResult.add(theClass);
            }
        }
        return theResult;
    }
}