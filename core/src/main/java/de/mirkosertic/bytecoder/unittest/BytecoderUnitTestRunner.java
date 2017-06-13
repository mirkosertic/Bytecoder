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

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class BytecoderUnitTestRunner extends ParentRunner<FrameworkMethod> {

    private static PhantomJSDriver SINGLETONDRIVER;

    private final List<FrameworkMethod> testMethods;
    private final TestClass testClass;

    public BytecoderUnitTestRunner(java.lang.Class aClass) throws InitializationError {
        super(aClass);
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
                testMethods.add(new FrameworkMethod(classMethod));
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
    protected List<FrameworkMethod> getChildren() {
        return testMethods;
    }

    @Override
    protected Description describeChild(FrameworkMethod frameworkMethod) {
        return Description.createTestDescription(testClass.getJavaClass(), frameworkMethod.getName());
    }

    private void testJSJVMBackendFrameworkMethod(FrameworkMethod aFrameworkMethod, RunNotifier aRunNotifier) {
        Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " JVM Target");
        aRunNotifier.fireTestStarted(theDescription);
        try {
            // Simply invoke using reflection
            Object theInstance = testClass.getJavaClass().newInstance();
            Method theMethod = aFrameworkMethod.getMethod();
            theMethod.invoke(theInstance);

            aRunNotifier.fireTestFinished(theDescription);
        } catch (Exception e) {
            aRunNotifier.fireTestFailure(new Failure(theDescription, e));
        }
    }

    private void testJSBackendFrameworkMethod(CompileTarget.BackendType aBackendType, FrameworkMethod aFrameworkMethod, RunNotifier aRunNotifier) {
        Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " JS Backend " + aBackendType);
        aRunNotifier.fireTestStarted(theDescription);

        try {

            CompileTarget theCompileTarget = new CompileTarget(testClass.getJavaClass().getClassLoader(), aBackendType);

            BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(aFrameworkMethod.getMethod());
            BytecodeMethodSignature theGetLastExceptionSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                    TThrowable.class), new BytecodeTypeRef[0]);

            BytecodeObjectTypeRef theTypeRef = new BytecodeObjectTypeRef(testClass.getName());

            String theCode = theCompileTarget.compileToJS(testClass.getJavaClass(), aFrameworkMethod.getName(), theSignature);

            String theJSFileName = theCompileTarget.toClassName(theTypeRef) + "." + theCompileTarget.toMethodName(aFrameworkMethod.getName(), theSignature) + "_" + aBackendType + ".html";

            theCode += "\nconsole.log(\"Starting test\");\n";
            theCode += theCompileTarget.toClassName(theTypeRef) + "." + theCompileTarget.toMethodName(aFrameworkMethod.getName(), theSignature) + "(" + theCompileTarget.toClassName(theTypeRef) + ".emptyInstance());\n";
            theCode += "var theLastException = " + theCompileTarget.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(
                    ExceptionRethrower.class)) + "." + theCompileTarget.toMethodName("getLastOutcomeOrNullAndReset", theGetLastExceptionSignature) + "();\n";
            theCode += "if (theLastException) {\n";

            theCode += "var theStringData = theLastException.data.message.data.data.data;\n"
                    + "   var theMessage = \"\";\n"
                    + "   for (var i=0;i<theStringData.length;i++) {\n"
                    + "     theMessage += String.fromCharCode(theStringData[i]);\n"
                    + "   }\n"
                    + "   console.log(\"Test finished with exception. Message = \" + theMessage);\n";
            theCode += "  throw theLastException;\n";
            theCode += "}\n";
            theCode += "\nconsole.log(\"Test finished\");\n";
            theCode += "\nreturn('OK');\n";

            File theWorkingDirectory = new File(".");

            File theMavenTargetDir = new File(theWorkingDirectory, "target");
            File theGeneratedFilesDir = new File(theMavenTargetDir, "bytecoderjs");
            theGeneratedFilesDir.mkdirs();
            File theGeneratedFile = new File(theGeneratedFilesDir, theJSFileName);
            PrintWriter theWriter = new PrintWriter(theGeneratedFile);
            theWriter.println("<script>");
            theWriter.println(theCode);
            theWriter.println("</script>");
            theWriter.flush();
            theWriter.close();

            if (SINGLETONDRIVER == null) {
                Properties theProperties = new Properties(System.getProperties());
                File theConfigFile = new File(theWorkingDirectory, "phantomjs.properties");
                if (theConfigFile.exists()) {
                    try (FileInputStream theStream = new FileInputStream(theConfigFile)) {
                        theProperties.load(theStream);
                    }
                }

                String thePhantomBinary = theProperties.getProperty("phantomjs.binary");
                if (thePhantomBinary == null || thePhantomBinary.length() == 0) {
                    throw new RuntimeException("No phantomjs binary found!");
                }

                DesiredCapabilities theCapabilities = new DesiredCapabilities();
                theCapabilities.setJavascriptEnabled(true);
                theCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                        thePhantomBinary);
                SINGLETONDRIVER = new PhantomJSDriver(theCapabilities);
            }

            Object theResult = SINGLETONDRIVER.executePhantomJS(theCode);
            if (!"OK".equals(theResult)) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException(theResult.toString())));
            }
        } catch (Exception e) {
            aRunNotifier.fireTestFailure(new Failure(theDescription, e));
        } finally {
            aRunNotifier.fireTestFinished(theDescription);
        }
    }

    @Override
    protected void runChild(FrameworkMethod aFrameworkMethod, RunNotifier aRunNotifier) {
        //testJSBackendFrameworkMethod(JSCompileTarget.BackendType.interpreter, aFrameworkMethod, aRunNotifier);
        testJSBackendFrameworkMethod(CompileTarget.BackendType.jsssacompiler, aFrameworkMethod, aRunNotifier);
        testJSJVMBackendFrameworkMethod(aFrameworkMethod, aRunNotifier);
    }
}