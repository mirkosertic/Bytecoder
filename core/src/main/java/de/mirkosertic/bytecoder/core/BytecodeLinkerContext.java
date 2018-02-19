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

import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.graph.Edge;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BytecodeLinkerContext {

    private final RootNode rootNode;
    private final BytecodeLoader loader;
    private final BytecodeMethodCollection methodCollection;
    private final Logger logger;
    private int classIdCounter;

    public BytecodeLinkerContext(BytecodeLoader aLoader, Logger aLogger) {
        rootNode = new RootNode();
        loader = aLoader;
        methodCollection = new BytecodeMethodCollection();
        logger = aLogger;
        classIdCounter = 0;
    }

    public Logger getLogger() {
        return logger;
    }

    public BytecodeSignatureParser getSignatureParser() {
        return loader.getSignatureParser();
    }

    public BytecodeMethodCollection getMethodCollection() {
        return methodCollection;
    }

    public BytecodeLinkedClass isLinkedOrNull(BytecodeUtf8Constant aConstant) {
        BytecodeObjectTypeRef theTypeRef = BytecodeObjectTypeRef.fromUtf8Constant(aConstant);
        Optional<BytecodeLinkedClass> theFoundLink = rootNode.singleOutgoingNodeMatching(BytecodeLinkedClassEdgeType.filter(theTypeRef));
        return theFoundLink.orElse(null);
    }

    public BytecodeLinkedClass resolveClass(BytecodeObjectTypeRef aTypeRef) {

        Optional<BytecodeLinkedClass> theFoundLink = rootNode.singleOutgoingNodeMatching(BytecodeLinkedClassEdgeType.filter(aTypeRef));
        if (theFoundLink.isPresent()) {
            return theFoundLink.get();
        }

        try {
            BytecodeLinkedClass theParentClass = null;
            BytecodeClass theLoadedClass = loader.loadByteCode(aTypeRef);
            BytecodeClassinfoConstant theSuperClass = theLoadedClass.getSuperClass();
            if (theSuperClass != BytecodeClassinfoConstant.OBJECT_CLASS) {
                BytecodeUtf8Constant theSuperClassName = theSuperClass.getConstant();
                theParentClass = resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theSuperClassName));
            }

            BytecodeLinkedClass theLinkedClass = new BytecodeLinkedClass(classIdCounter++, this, aTypeRef, theLoadedClass);
            rootNode.addEdgeTo(new BytecodeLinkedClassEdgeType(aTypeRef), theLinkedClass);
            if (theParentClass != null) {
                theLinkedClass.addEdgeTo(new BytecodeSubclassOfEdgeType(), theParentClass);
            }

            for (BytecodeMethod theMethod : theLoadedClass.getMethods()) {
                BytecodeAnnotation theAnnotation = theMethod.getAttributes().getAnnotationByType(Export.class.getName());
                if (theAnnotation != null) {
                    // The method should be exported
                    if (theMethod.getAccessFlags().isStatic()) {
                        theLinkedClass.resolveStaticMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                    } else {
                        theLinkedClass.resolveVirtualMethod(theMethod.getName().stringValue(), theMethod.getSignature());
                    }
                }
            }

            BytecodeMethod theMethod = theLoadedClass.classInitializerOrNull();
            if (theMethod != null) {
                theLinkedClass.resolveClassInitializer(theMethod);
            }

            for (BytecodeInterface theInterface : theLoadedClass.getInterfaces()) {
                BytecodeUtf8Constant theSuperClassName = theInterface.getClassinfoConstant().getConstant();
                BytecodeLinkedClass theImplementedClass = resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(theSuperClassName));
                theLinkedClass.addEdgeTo(new BytecodeImplementsEdgeType(), theImplementedClass);
            }

            logger.info("Linked  {}" ,theLinkedClass.getClassName().name());

            return theLinkedClass;
        } catch (Exception e) {
            throw new RuntimeException("Error linking class " + aTypeRef.name(), e);
        }
    }

    public Stream<Edge<BytecodeLinkedClassEdgeType, BytecodeLinkedClass>> linkedClasses() {
        return rootNode.outgoingEdges(BytecodeLinkedClassEdgeType.filter());
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
        resolveClass(theTypeRef);
    }

    public void resolveAbstractMethodsInSubclasses() {
        List<BytecodeLinkedClass> theLinkedClasses = linkedClasses().map(Edge::targetNode).collect(Collectors.toList());
        for (BytecodeLinkedClass theLinked : theLinkedClasses) {
            theLinked.resolveInheritedAbstractMethods();
        }
        if (linkedClasses().count() != theLinkedClasses.size()) {
            // New classes were added, we maybe have to resolve them as well
            resolveAbstractMethodsInSubclasses();
        }
    }

    public List<BytecodeLinkedClass> getClassesImplementingVirtualMethod(BytecodeVirtualMethodIdentifier aIdentifier) {
        return linkedClasses()
                .map(Edge::targetNode)
                .filter(t -> !t.getBytecodeClass().getAccessFlags().isInterface())
                .filter(t -> t.implementsMethod(aIdentifier))
                .collect(Collectors.toList());
    }
}