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
package de.mirkosertic.bytecoder.unittest;

import de.mirkosertic.bytecoder.backend.js.JSBackend;
import de.mirkosertic.bytecoder.core.*;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.TestClass;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class BytecoderUnitTestRunner extends Runner {

    private static final ScriptEngineManager SCRIPTENGINEFACTORY = new ScriptEngineManager();

    private final List<Method> testMethods;
    private final TestClass testClass;

    public BytecoderUnitTestRunner(java.lang.Class aClass) {
        testClass = new TestClass(aClass);
        testMethods = new ArrayList<>();

        Method[] classMethods = aClass.getDeclaredMethods();
        for (int i = 0; i < classMethods.length; i++) {
            Method classMethod = classMethods[i];
            Class retClass = classMethod.getReturnType();
            int length = classMethod.getParameterTypes().length;
            int modifiers = classMethod.getModifiers();
            if (retClass == null || length != 0 || Modifier.isStatic(modifiers)
                    || !Modifier.isPublic(modifiers) || Modifier.isInterface(modifiers)
                    || Modifier.isAbstract(modifiers)) {
                continue;
            }
            String methodName = classMethod.getName();
            if (methodName.toUpperCase().startsWith("TEST")
                    || classMethod.getAnnotation(Test.class) != null) {
                testMethods.add(classMethod);
            }
            if (classMethod.getAnnotation(Ignore.class) != null) {
                testMethods.remove(classMethod);
            }
        }
    }

    @Override
    public Description getDescription() {
        Description spec = Description.createSuiteDescription(this.testClass.getName(),
                this.testClass.getJavaClass().getAnnotations());
        return spec;
    }

    @Override
    public void run(RunNotifier aRunNotifier) {
        for (Method theMethod : testMethods) {

            Description theDescription = Description.createTestDescription(theMethod.getClass(), theMethod.getName());
            aRunNotifier.fireTestStarted(theDescription);

            try {
                BytecodeLoader theLoader = new BytecodeLoader();
                BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);

                BytecodeSignatureParser theParser = new BytecodeSignatureParser();
                BytecodeMethodSignature theSignature = theParser.toMethodSignature(theMethod);

                BytecodeObjectTypeRef theTypeRef = new BytecodeObjectTypeRef(testClass.getName());

                theLinkerContext.linkClassMethod(theTypeRef, theMethod.getName(), theSignature);

                JSBackend theBackend = new JSBackend();
                String theCode = theBackend.generateCodeFor(theLinkerContext);
                theCode += "\n";
                theCode += theBackend.toClassName(theTypeRef) + "." + theBackend.toMethodName(theMethod.getName(), theSignature) + "({})";

                System.out.println(theCode);

                ScriptEngine theEngine = SCRIPTENGINEFACTORY.getEngineByName("JavaScript");
                Object theResult = theEngine.eval(theCode);

                aRunNotifier.fireTestFinished(theDescription);
            } catch (Exception e) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            }
        }
    }
}