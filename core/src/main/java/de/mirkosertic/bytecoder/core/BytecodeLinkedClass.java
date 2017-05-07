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

import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;

import java.util.*;
import java.util.function.Consumer;

public class BytecodeLinkedClass {

    public static class LinkingInfo {
        private final BytecodeMethodSignature methodSignature;
        private final BytecodeClass targetType;

        public LinkingInfo(BytecodeMethodSignature aMethodSignature, BytecodeClass aTargetType) {
            this.methodSignature = aMethodSignature;
            this.targetType = aTargetType;
        }
    }

    public static class LinkingCollection {
        private final List<LinkingInfo> linkingInfos;
        private final Map<String, BytecodeClass> typeLinks;

        public LinkingCollection() {
            linkingInfos = new ArrayList<>();
            typeLinks = new HashMap<>();
        }

        public void linkSignatureAndType(BytecodeMethodSignature aSignature, BytecodeClass aBytecodeClass) {
            typeLinks.put(aSignature.toString(), aBytecodeClass);
        }
    }

    private final BytecodeObjectTypeRef className;
    private final BytecodeClass bytecodeClass;
    private final Map<String, LinkingCollection> linkedMethods;
    private final BytecodeLinkerContext linkerContext;
    private final Set<BytecodeMethod> knownMethods;

    public BytecodeLinkedClass(BytecodeLinkerContext aLinkerContext, BytecodeObjectTypeRef aClassName, BytecodeClass aBytecodeClass) {
        className = aClassName;
        bytecodeClass = aBytecodeClass;
        linkedMethods = new HashMap<>();
        linkerContext = aLinkerContext;
        knownMethods = new HashSet<>();
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

    private LinkingCollection getOrCreateLinkingCollection(String aMethodName) {
        LinkingCollection theCollection = linkedMethods.get(aMethodName);
        if (theCollection == null) {
            theCollection = new LinkingCollection();
            linkedMethods.put(aMethodName, theCollection);
        }
        return theCollection;
    }

    public void linkVirtualMethod(String aMethodName, BytecodeMethodSignature aSignature) {
        linkMethod(aMethodName, aSignature);
    }

    public void linkMethod(String aMethodName, BytecodeMethodSignature aMethodSignature) {
        try {
            LinkingCollection theCollection = getOrCreateLinkingCollection(aMethodName);
            BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignature(aMethodName, aMethodSignature);
            theCollection.linkSignatureAndType(theMethod.getSignature(), bytecodeClass);

            knownMethods.add(theMethod);

            link(aMethodSignature.getReturnType());
            for (BytecodeTypeRef theArgument : aMethodSignature.getArguments()) {
                link(theArgument);
            }

            if ("<init>".equals(aMethodName) && (TThrowable.class.getName().equals(className.name()))) {
                // Do not try to resolve root constructor of TThrowable() !!
                //return;
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

    public void forEachMethod(Consumer<BytecodeMethod> aMethod) {
        knownMethods.forEach(aMethod);
    }
}