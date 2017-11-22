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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.classlib.ExceptionRethrower;
import de.mirkosertic.bytecoder.classlib.java.lang.TThrowable;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class BytecoderUnitTestRunner extends ParentRunner<FrameworkMethod> {

    private static final Slf4JLogger LOGGER = new Slf4JLogger();

    private static WebDriver SINGLETONDRIVER;

    private final List<FrameworkMethod> testMethods;
    private final TestClass testClass;

    public BytecoderUnitTestRunner(java.lang.Class aClass) throws InitializationError {
        super(aClass);
        testClass = new TestClass(aClass);
        testMethods = new ArrayList<>();

        Method[] classMethods = aClass.getDeclaredMethods();
        for (Method classMethod : classMethods) {
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

    private static void initializeSeleniumDriver(File aWorkingDirectory) throws IOException {
        if (SINGLETONDRIVER == null) {

            Properties theProperties = new Properties(System.getProperties());
            File theConfigFile = new File(aWorkingDirectory, "testrunner.properties");
            if (theConfigFile.exists()) {
                try (FileInputStream theStream = new FileInputStream(theConfigFile)) {
                    theProperties.load(theStream);
                }
            }

            String theChromeDriverBinary = theProperties.getProperty("chromedriver.binary");
            if (theChromeDriverBinary == null || theChromeDriverBinary.length() == 0) {
                throw new RuntimeException("No chromedriver binary found!");
            }

            ChromeDriverService.Builder theDriverService = new ChromeDriverService.Builder();
            theDriverService = theDriverService.withVerbose(false);
            theDriverService = theDriverService.usingDriverExecutable(new File(theChromeDriverBinary));

            ChromeOptions theOptions = new ChromeOptions();
            theOptions.addArguments("headless");
            LoggingPreferences theLoggingPreferences = new LoggingPreferences();
            theLoggingPreferences.enable(LogType.BROWSER, Level.ALL);
            theOptions.setCapability(CapabilityType.LOGGING_PREFS, theLoggingPreferences);
            SINGLETONDRIVER = new ChromeDriver(theDriverService.build(), theOptions);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> SINGLETONDRIVER.quit()));
        }
    }

    private void testJSBackendFrameworkMethod(FrameworkMethod aFrameworkMethod, RunNotifier aRunNotifier) {
        Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " JS Backend ");
        aRunNotifier.fireTestStarted(theDescription);

        try {
            CompileTarget theCompileTarget = new CompileTarget(testClass.getJavaClass().getClassLoader(), CompileTarget.BackendType.js);

            BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(aFrameworkMethod.getMethod());
            BytecodeMethodSignature theGetLastExceptionSignature = new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                    TThrowable.class), new BytecodeTypeRef[0]);

            BytecodeObjectTypeRef theTypeRef = new BytecodeObjectTypeRef(testClass.getName());

            StringWriter theStrWriter = new StringWriter();
            PrintWriter theCodeWriter = new PrintWriter(theStrWriter);

            theCodeWriter.println(theCompileTarget.compileToJS(LOGGER, testClass.getJavaClass(), aFrameworkMethod.getName(), theSignature));

            String theFilename = theCompileTarget.toClassName(theTypeRef) + "." + theCompileTarget.toMethodName(aFrameworkMethod.getName(), theSignature) + "_js.html";

            theCodeWriter.println("console.log(\"Starting test\");");
            theCodeWriter.println("bytecoder.bootstrap();");
            theCodeWriter.println(theCompileTarget.toClassName(theTypeRef) + "." + theCompileTarget.toMethodName(aFrameworkMethod.getName(), theSignature) + "(" + theCompileTarget.toClassName(theTypeRef) + ".emptyInstance());");
            theCodeWriter.println("var theLastException = " + theCompileTarget.toClassName(BytecodeObjectTypeRef.fromRuntimeClass(
                    ExceptionRethrower.class)) + "." + theCompileTarget.toMethodName("getLastOutcomeOrNullAndReset", theGetLastExceptionSignature) + "();");
            theCodeWriter.println("if (theLastException) {");
            theCodeWriter.println("var theStringData = theLastException.message.data.data;");
            theCodeWriter.println("   var theMessage = \"\";");
            theCodeWriter.println("   for (var i=0;i<theStringData.length;i++) {");
            theCodeWriter.println("     theMessage += String.fromCharCode(theStringData[i]);");
            theCodeWriter.println("   }");
            theCodeWriter.println("   console.log(\"Test finished with exception. Message = \" + theMessage);");
            theCodeWriter.println("  throw theLastException;");
            theCodeWriter.println("}");
            theCodeWriter.println("console.log(\"Test finished OK\");");

            theCodeWriter.flush();

            File theWorkingDirectory = new File(".");

            initializeSeleniumDriver(theWorkingDirectory);

            File theMavenTargetDir = new File(theWorkingDirectory, "target");
            File theGeneratedFilesDir = new File(theMavenTargetDir, "bytecoderjs");
            theGeneratedFilesDir.mkdirs();
            File theGeneratedFile = new File(theGeneratedFilesDir, theFilename);
            PrintWriter theWriter = new PrintWriter(theGeneratedFile);
            theWriter.println("<html><body><script>");
            theWriter.println(theStrWriter.toString());
            theWriter.println("</script></body></html>");
            theWriter.flush();
            theWriter.close();

            SINGLETONDRIVER.get(theGeneratedFile.toURI().toURL().toString());

            List<LogEntry> theAll = SINGLETONDRIVER.manage().logs().get(LogType.BROWSER).getAll();
            if (theAll.size() < 1) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("No console output from browser")));
            }
            for (LogEntry theEntry : theAll) {
                System.out.println(theEntry.getMessage());
            }
            LogEntry theLast = theAll.get(theAll.size() - 1);

            if (!theLast.getMessage().contains("Test finished OK")) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("Test did not succeed! Got : " + theLast.getMessage())));
            }

        } catch (Exception e) {
            aRunNotifier.fireTestFailure(new Failure(theDescription, e));
        } finally {
            aRunNotifier.fireTestFinished(theDescription);
        }
    }

    private void testWASMBackendFrameworkMethod(FrameworkMethod aFrameworkMethod, RunNotifier aRunNotifier) {
        Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " WASM Backend ");
        aRunNotifier.fireTestStarted(theDescription);

        try {
            CompileTarget theCompileTarget = new CompileTarget(testClass.getJavaClass().getClassLoader(), CompileTarget.BackendType.wasm);

            BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(aFrameworkMethod.getMethod());
            BytecodeObjectTypeRef theTypeRef = new BytecodeObjectTypeRef(testClass.getName());

            String theCode = theCompileTarget.compileToJS(LOGGER, testClass.getJavaClass(), aFrameworkMethod.getName(), theSignature);

            String theFileName = theCompileTarget.toClassName(theTypeRef) + "." + theCompileTarget.toMethodName(aFrameworkMethod.getName(), theSignature) + ".html";

            File theWorkingDirectory = new File(".");

            File theMavenTargetDir = new File(theWorkingDirectory, "target");
            File theGeneratedFilesDir = new File(theMavenTargetDir, "bytecoderwat");
            theGeneratedFilesDir.mkdirs();
            File theGeneratedFile = new File(theGeneratedFilesDir, theFileName);

            // Copy WABT Tools
            File theWABTFile = new File(theGeneratedFilesDir, "libwabt.js");
            try (FileOutputStream theOS = new FileOutputStream(theWABTFile)) {
                IOUtils.copy(getClass().getResourceAsStream("/libwabt.js"), theOS);
            }

            PrintWriter theWriter = new PrintWriter(theGeneratedFile);
            theWriter.println("<html>");
            theWriter.println("    <body>");
            theWriter.println("        <h1>Module code</h1>");
            theWriter.println("        <pre id=\"modulecode\">");
            theWriter.println(theCode);
            theWriter.println("        </pre>");
            theWriter.println("        <h1>Compilation result</h1>");
            theWriter.println("        <pre id=\"compileresult\">");
            theWriter.println("        </pre>");
            theWriter.println("        <script src=\"libwabt.js\">");
            theWriter.println("        </script>");
            theWriter.println("        <script>");
            theWriter.println("            function compile() {");
            theWriter.println("                console.log('Test started');");
            theWriter.println("                try {");
            theWriter.println("                    var module = wabt.parseWat('test.wast', document.getElementById(\"modulecode\").innerText);");
            theWriter.println("                    module.resolveNames();");
            theWriter.println("                    module.validate();");
            theWriter.println("                    var binaryOutput = module.toBinary({log: true, write_debug_names:true});");
            theWriter.println("                    document.getElementById(\"compileresult\").innerText = binaryOutput.log;");
            theWriter.println("                    var binaryBuffer = binaryOutput.buffer;");

            theWriter.println("                    var theInstantiatePromise = WebAssembly.instantiate(binaryBuffer, {");
            theWriter.println("                         math: {");
            theWriter.println("                             foor: function(aValue) {return Math.floor(aValue);},");
            theWriter.println("                             ceil: function(aValue) {return Math.ceil(aValue);},");
            theWriter.println("                         }");
            theWriter.println("                    });");
            theWriter.println("                    theInstantiatePromise.then(");
            theWriter.println("                         function (resolved) {");
            theWriter.println("                             var wasmModule = resolved.module;");
            theWriter.println("                             var wasmInstance = resolved.instance;");
            theWriter.println("                             wasmInstance.exports.initMemory(1024 * 1024);");
            theWriter.println("                             console.log(\"Memory initialized\")");
            theWriter.println("                             wasmInstance.exports.bootstrap();");
            theWriter.println("                             console.log(\"Bootstrapped\")");
            theWriter.println("                             wasmInstance.exports.main();");
            theWriter.println("                             console.log(\"Test finished OK\")");
            theWriter.println("                         },");
            theWriter.println("                         function (rejected) {");
            theWriter.println("                             console.log(\"Error instantiating webassembly\");");
            theWriter.println("                             console.log(rejected);");
            theWriter.println("                         }");
            theWriter.println("                    );");
            theWriter.println("                } catch (e) {");
            theWriter.println("                    document.getElementById(\"compileresult\").innerText = e.toString();");
            theWriter.println("                    console.log(e.toString());");
            theWriter.println("                }");
            theWriter.println("            }");
            theWriter.println("            compile();");
            theWriter.println("        </script>");
            theWriter.println("    </body>");
            theWriter.println("</html>");

            theWriter.flush();
            theWriter.close();

            // Invoke test in browser
            SINGLETONDRIVER.get(theGeneratedFile.toURI().toURL().toString());

            List<LogEntry> theAll = SINGLETONDRIVER.manage().logs().get(LogType.BROWSER).getAll();
            if (theAll.size() < 1) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("No console output from browser")));
            }
            for (LogEntry theEntry : theAll) {
                System.out.println(theEntry.getMessage());
            }
            LogEntry theLast = theAll.get(theAll.size() - 1);

            if (!theLast.getMessage().contains("Test finished OK")) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("Test did not succeed! Got : " + theLast.getMessage())));
            }

        } catch (Exception e) {
            aRunNotifier.fireTestFailure(new Failure(theDescription, e));
        } finally {
            aRunNotifier.fireTestFinished(theDescription);
        }
    }


    @Override
    protected void runChild(FrameworkMethod aFrameworkMethod, RunNotifier aRunNotifier) {
        testJSBackendFrameworkMethod(aFrameworkMethod, aRunNotifier);
        testWASMBackendFrameworkMethod(aFrameworkMethod, aRunNotifier);
        testJSJVMBackendFrameworkMethod(aFrameworkMethod, aRunNotifier);
    }
}