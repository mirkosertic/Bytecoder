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

import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public abstract class AbstractJSBackend {

    protected final BytecodeMethodSignature theRegisterExceptionOutcomeSignature;
    protected final BytecodeMethodSignature theGetLastExceptionOutcomeSignature;
    protected final JSModules modules;

    public AbstractJSBackend() {
        theRegisterExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class)});
        theGetLastExceptionOutcomeSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(TThrowable.class), new BytecodeTypeRef[0]);
        modules = new JSModules();

        JSModule theMathModule = new JSModule();
        theMathModule.registerFunction("ceil", new JSFunction("return Math.ceil(p1);"));
        theMathModule.registerFunction("floor", new JSFunction("return Math.floor(p1);"));
        theMathModule.registerFunction("sin", new JSFunction("return Math.sin(p1);"));
        theMathModule.registerFunction("cos", new JSFunction("return Math.cos(p1);"));
        theMathModule.registerFunction("sqrt", new JSFunction("return Math.sqrt(p1);"));
        theMathModule.registerFunction("round", new JSFunction("return Math.round(p1);"));
        theMathModule.registerFunction("NaN", new JSFunction("return NaN;"));
        theMathModule.registerFunction("atan2", new JSFunction("return Math.atan2(p1, p2);"));
        theMathModule.registerFunction("max", new JSFunction("return Math.max(p1, p2);"));
        theMathModule.registerFunction("random", new JSFunction("return Math.random();"));

        JSModule theSystemModule = new JSModule();
        theSystemModule.registerFunction("currentTimeMillis", new JSFunction("return Date.now();"));
        theSystemModule.registerFunction("nanoTime", new JSFunction("return Date.now() * 1000000;"));
        theSystemModule.registerFunction("logByteArrayAsString", new JSFunction("bytecoder.logByteArrayAsString(p1);"));
        theSystemModule.registerFunction("logDebug", new JSFunction("bytecoder.logDebug(p1);"));

        modules.register("math", theMathModule);
        modules.register("system", theSystemModule);
    }

    private String typeRefToString(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) aTypeRef;
            return thePrimitive.name();
        }
        if (aTypeRef.isArray()) {
            BytecodeArrayTypeRef theRef = (BytecodeArrayTypeRef) aTypeRef;
            return "A" + theRef.getDepth() + typeRefToString(theRef.getType());
        }
        BytecodeObjectTypeRef theObjectRef = (BytecodeObjectTypeRef) aTypeRef;
        return toClassName(theObjectRef);
    }

    public String toMethodName(String aMethodName, BytecodeMethodSignature aSignature) {
        String theName = typeRefToString(aSignature.getReturnType());
        theName += aMethodName.replace("<", "").replace(">", "");

        for (BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName += typeRefToString(theTypeRef);
        }
        return theName;
    }

    private String toClassNameInternal(String aClassName) {
        int p = aClassName.lastIndexOf(".");
        return aClassName.substring(p + 1);
    }

    public String toClassName(BytecodeObjectTypeRef aTypeRef) {
        return toClassNameInternal(aTypeRef.name());
    }

    public String toClassName(BytecodeClassinfoConstant aTypeRef) {
        return toClassNameInternal(aTypeRef.getConstant().stringValue().replace("/","."));
    }

    public String toArray(byte[] aData) {
        StringBuilder theResult = new StringBuilder("[");
        for (int i=0;i<aData.length;i++) {
            if (i>0) {
                theResult.append(",");
            }
            theResult.append(aData[i]);
        }
        theResult.append("]");
        return theResult.toString();
    }

    public abstract String generateCodeFor(BytecodeLinkerContext aLinkerContext);
}
