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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class BytecodeLinkedClass {

    private final BytecodeObjectTypeRef className;
    private final BytecodeClass bytecodeClass;
    private final Set<BytecodeMethod> linkedMethods;
    private final BytecodeLinkerContext linkerContext;

    public BytecodeLinkedClass(BytecodeLinkerContext aLinkerContext, BytecodeObjectTypeRef aClassName, BytecodeClass aBytecodeClass) {
        className = aClassName;
        bytecodeClass = aBytecodeClass;
        linkedMethods = new HashSet<>();
        linkerContext = aLinkerContext;
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
    }

    public void linkMethod(String aMethodName, BytecodeMethodSignature aMethodSignature) {
        try {
            BytecodeMethod theMethod = bytecodeClass.methodByNameAndSignature(aMethodName, aMethodSignature);
            linkedMethods.add(theMethod);

            link(aMethodSignature.getReturnType());
            for (BytecodeTypeRef theArgument : aMethodSignature.getArguments()) {
                link(theArgument);
            }

            if ("<init>".equals(aMethodName) && ("java.lang.Object".equals(className.name()))) {
                // Do not try to resolve root constructor of Object() !!
                return;
            }
            if ("<init>".equals(aMethodName) && (TThrowable.class.getName().equals(className.name()))) {
                // Do not try to resolve root constructor of Object() !!
                return;
            }

            BytecodeCodeAttributeInfo theCode = theMethod.attributeByType(BytecodeCodeAttributeInfo.class);
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

    public void forEachMethod(Consumer<BytecodeMethod> aMethod) {
        linkedMethods.forEach(aMethod);
    }
}
