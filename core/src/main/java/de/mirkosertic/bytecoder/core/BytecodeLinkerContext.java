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

import de.mirkosertic.bytecoder.annotations.Export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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
                BytecodeAnnotation theAnnotation = theMethod.getAnnotations().getAnnotationByType(Export.class.getName());
                if (theAnnotation != null) {
                    // The method should be exported
                    if (theMethod.getAccessFlags().isStatic()) {
                        theLinkedClass.linkStaticMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                    } else {
                        theLinkedClass.linkVirtualMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                    }
                } else {
                    // Brute force: link everything
                   if (!theMethod.isClassInitializer()) {
                        if (theMethod.isConstructor()) {
                            theLinkedClass.linkConstructorInvocation(theMethod.getSignature());
                        } else if (theMethod.getAccessFlags().isStatic()) {
                            theLinkedClass.linkStaticMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                        } else {
                            theLinkedClass.linkVirtualMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                        }
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

            return theLinkedClass;
        } catch (Exception e) {
            throw new RuntimeException("Error linking class " + aTypeRef.name(), e);
        }
    }

    public void linkClassMethod(BytecodeObjectTypeRef aTypeRef, String aMethodName, BytecodeMethodSignature aSignature) {
        linkClass(aTypeRef).linkStaticMethod(aMethodName, aSignature);
    }

    public void linkConstructorInvocation(BytecodeObjectTypeRef aTypeRef, BytecodeMethodSignature aSignature) {
        linkClass(aTypeRef).linkConstructorInvocation(aSignature);
    }

    public void linkVirtualMethod(BytecodeObjectTypeRef aTypeRef, String aMethodName, BytecodeMethodSignature aSignature) {
        linkClass(aTypeRef).linkVirtualMethod(aMethodName, aSignature);
    }

    public void linkPrivateMethod(BytecodeObjectTypeRef aTypeRef, String aMethodName, BytecodeMethodSignature aSignature) {
        linkClass(aTypeRef).linkPrivateMethod(aMethodName, aSignature);
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

    private void propagateVirtualMethodsAndFields(BytecodeLinkedClass aClass) {

        aClass.propagateVirtualMethodsAndFields();

        List<BytecodeLinkedClass> theClasses = findLinkedClassWithParent(aClass);
        for (BytecodeLinkedClass theEntry : theClasses) {
            propagateVirtualMethodsAndFields(theEntry);
        }
    }

    public void propagateVirtualMethodsAndFields() {

        Set<BytecodeLinkedClass> theKnownClasses = new HashSet<>(linkedClasses.values());

        // First, we search for used interface and link the used methods to all classes implementing this interface
        for (BytecodeLinkedClass theLinkedClass : theKnownClasses) {
            if (theLinkedClass.getAccessFlags().isInterface()) {
                for (BytecodeLinkedClass theOtherClass : theKnownClasses) {
                    if (theOtherClass.getImplementingTypes().contains(theLinkedClass)) {
                        theLinkedClass.forEachMethod(bytecodeMethod -> theOtherClass.linkVirtualMethod(bytecodeMethod.getName().stringValue(), bytecodeMethod.getSignature()));
                    }
                }
            }
        }

        List<BytecodeLinkedClass> theClasses = findLinkedClassWithParent(null);
        for (BytecodeLinkedClass theEntry : theClasses) {
            propagateVirtualMethodsAndFields(theEntry);
        }
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
}