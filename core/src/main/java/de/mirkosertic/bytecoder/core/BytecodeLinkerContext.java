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
import java.util.Map;
import java.util.function.Consumer;

public class BytecodeLinkerContext {

    private final Map<BytecodeObjectTypeRef, BytecodeLinkedClass> linkedClasses;
    private final BytecodeLoader loader;

    public BytecodeLinkerContext(BytecodeLoader aLoader) {
        linkedClasses = new HashMap<>();
        loader = aLoader;
    }

    public BytecodeLinkedClass linkClass(BytecodeObjectTypeRef aTypeRef) {
        BytecodeLinkedClass theLinkedClass = linkedClasses.get(aTypeRef);
        if (theLinkedClass != null) {
            return theLinkedClass;
        }

        try {
            BytecodeClass theLoadedClass = loader.loadByteCode(aTypeRef);
            theLinkedClass = new BytecodeLinkedClass(this, aTypeRef, theLoadedClass);
            linkedClasses.put(aTypeRef, theLinkedClass);

            BytecodeClassinfoConstant theClass = theLoadedClass.getSuperClass();
            if (theClass != BytecodeClassinfoConstant.OBJECT_CLASS) {
                BytecodeUtf8Constant theSuperClassName = theClass.getConstant();
                linkClass(new BytecodeObjectTypeRef(theSuperClassName.stringValue().replace("/", ".")));
            }
            for (BytecodeInterface theInterface : theLoadedClass.getInterfaces()) {
                BytecodeUtf8Constant theSuperClassName = theInterface.getClassinfoConstant().getConstant();
                linkClass(new BytecodeObjectTypeRef(theSuperClassName.stringValue().replace("/", ".")));
            }

            return theLinkedClass;
        } catch (Exception e) {
            throw new RuntimeException("Error linking class " + aTypeRef.name(), e);
        }
    }

    public void linkClassMethod(BytecodeObjectTypeRef aTypeRef, String aMethodName, BytecodeMethodSignature aSignature) {
        linkClass(aTypeRef).linkMethod(aMethodName, aSignature);
    }

    public void forEachClass(Consumer<Map.Entry<BytecodeObjectTypeRef, BytecodeLinkedClass>> aConsumer) {
        linkedClasses.entrySet().forEach(aConsumer);
    }
}