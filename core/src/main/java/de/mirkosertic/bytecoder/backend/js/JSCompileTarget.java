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
package de.mirkosertic.bytecoder.backend.js;

import de.mirkosertic.bytecoder.classlib.java.lang.TClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePackageReplacer;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

import java.lang.reflect.Method;

public class JSCompileTarget {

    private final JSBackend backend;
    private final BytecodePackageReplacer packageReplacer;
    private final BytecodeLoader bytecodeLoader;

    public JSCompileTarget(JSBackend.CodeType aCodeType) {
        backend = new JSBackend(aCodeType);
        packageReplacer = new BytecodePackageReplacer();
        bytecodeLoader = new BytecodeLoader(packageReplacer);
    }

    public String compileToJS(Class aClass, String aMethodName, BytecodeMethodSignature aSignature) {
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(bytecodeLoader);

        // We need these intrinsics
        BytecodeLinkedClass theClassLinkedCass = theLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(TClass.class));
        theClassLinkedCass.linkConstructorInvocation(new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {}));

        BytecodeObjectTypeRef theTypeRef = BytecodeObjectTypeRef.fromRuntimeClass(aClass);

        theLinkerContext.linkClassMethod(theTypeRef, aMethodName, aSignature);
        theLinkerContext.propagateVirtualMethodsAndFields();

        return backend.generateCodeFor(theLinkerContext);
    }

    public String toClassName(BytecodeObjectTypeRef aTypeRef) {
        return backend.toClassName(aTypeRef);
    }

    public String toMethodName(String aName, BytecodeMethodSignature aSignature) {
        return backend.toMethodName(aName, aSignature);
    }

    public BytecodeMethodSignature toMethodSignature(Method aMethod) {
        return bytecodeLoader.getSignatureParser().toMethodSignature(aMethod);
    }
}