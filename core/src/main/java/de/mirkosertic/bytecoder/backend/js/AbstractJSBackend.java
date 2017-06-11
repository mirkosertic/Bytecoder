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

import de.mirkosertic.bytecoder.annotations.OverrideParentClass;
import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeClass;
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
        theMathModule.registerFunction("tan", new JSFunction("return Math.tan(p1);"));
        theMathModule.registerFunction("toRadians", new JSFunction("return Math.toRadians(p1);"));
        theMathModule.registerFunction("toDegrees", new JSFunction("return Math.toDegrees(p1);"));
        theMathModule.registerFunction("min", new JSFunction("return Math.min(p1, p2);"));

        JSModule theSystemModule = new JSModule();
        theSystemModule.registerFunction("currentTimeMillis", new JSFunction("return Date.now();"));
        theSystemModule.registerFunction("nanoTime", new JSFunction("return Date.now() * 1000000;"));
        theSystemModule.registerFunction("logByteArrayAsString", new JSFunction("bytecoder.logByteArrayAsString(p1);"));
        theSystemModule.registerFunction("logDebug", new JSFunction("bytecoder.logDebug(p1);"));

        modules.register("math", theMathModule);
        modules.register("system", theSystemModule);
    }

    protected String getOverriddenParentClassFor(BytecodeClass aBytecodeClass) {
        BytecodeAnnotation theDelegatesTo = aBytecodeClass.getAttributes().getAnnotationByType(OverrideParentClass.class.getName());
        if (theDelegatesTo != null) {
            BytecodeAnnotation.ElementValue theParentOverride = (BytecodeAnnotation.ClassElementValue) theDelegatesTo.getElementValueByName("parentClass");
            return theParentOverride.stringValue().replace("/",".");
        }
        return null;
    }

    public abstract String generateCodeFor(BytecodeLinkerContext aLinkerContext);
}
