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

import com.sun.net.httpserver.HttpServer;
import de.mirkosertic.bytecoder.allocator.Allocator;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.backend.LLVMOptimizationLevel;
import de.mirkosertic.bytecoder.backend.js.JSCompileResult;
import de.mirkosertic.bytecoder.backend.llvm.LLVMCompileResult;
import de.mirkosertic.bytecoder.backend.llvm.LLVMWriterUtils;
import de.mirkosertic.bytecoder.backend.wasm.WASMCompileResult;
import de.mirkosertic.bytecoder.backend.wasm.WASMWriterUtils;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import org.apache.commons.io.FileUtils;
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
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class BytecoderUnitTestRunner extends ParentRunner<FrameworkMethodWithTestOption> {

    private static final Slf4JLogger LOGGER = new Slf4JLogger();
    private final List<TestOption> testOptions;
    private final String[] additionalClassesToLink;
    private final String[] additionalResources;

    private static HttpServer TESTSERVER;
    private static BrowserWebDriverContainer SELENIUMCONTAINER;
    private static final AtomicReference<File> HTTPFILESDIR = new AtomicReference<>();

    public BytecoderUnitTestRunner(final Class aClass) throws InitializationError {
        super(aClass);

        testOptions = new ArrayList<>();
        final BytecoderTestOptions declaredOptions = getTestClass().getJavaClass().getAnnotation(BytecoderTestOptions.class);
        if (declaredOptions != null) {
            if (declaredOptions.includeJVM()) {
                testOptions.add(new TestOption(null, false, false, false, false));
            }
            if (declaredOptions.value().length == 0 && declaredOptions.includeTestPermutations()) {
                testOptions.add(new TestOption(CompileTarget.BackendType.js, false, false, false, false));
                testOptions.add(new TestOption(CompileTarget.BackendType.js, false, false, true, false));
                testOptions.add(new TestOption(CompileTarget.BackendType.js, true, false, false, false));
                testOptions.add(new TestOption(CompileTarget.BackendType.wasm, false, false, false, false));
                testOptions.add(new TestOption(CompileTarget.BackendType.wasm, true, false, false, false));
                //testOptions.add(new TestOption(CompileTarget.BackendType.wasm_llvm, false, false, false, false));
                testOptions.add(new TestOption(CompileTarget.BackendType.wasm_llvm, false, false, false, true));

            } else {
                for (final BytecoderTestOption o : declaredOptions.value()) {
                    testOptions.add(new TestOption(o.backend(), o.preferStackifier(), o.exceptionsEnabled(), o.minify(), o. escapeAnalysisEnabled()));
                }
            }
            additionalClassesToLink = declaredOptions.additionalClassesToLink();
            additionalResources = declaredOptions.additionalResources();
        } else {
            testOptions.add(new TestOption(null, false, false, false, false));
            testOptions.add(new TestOption(CompileTarget.BackendType.js, false, false, false, false));
            testOptions.add(new TestOption(CompileTarget.BackendType.js, false, false, true, false));
            testOptions.add(new TestOption(CompileTarget.BackendType.js, true, false, false, false));
            testOptions.add(new TestOption(CompileTarget.BackendType.wasm, false, false, false, false));
            testOptions.add(new TestOption(CompileTarget.BackendType.wasm, true, false, false, false));
            //testOptions.add(new TestOption(CompileTarget.BackendType.wasm_llvm, false, false, false, false));
            testOptions.add(new TestOption(CompileTarget.BackendType.wasm_llvm, false, false, false, true));

            additionalClassesToLink = new String[0];
            additionalResources = new String[0];
        }
    }

    @Override
    public Description getDescription() {
        final TestClass testClass = getTestClass();
        return Description.createSuiteDescription(testClass.getName(),
                testClass.getJavaClass().getAnnotations());
    }

    @Override
    protected List<FrameworkMethodWithTestOption> getChildren() {
        final List<FrameworkMethodWithTestOption> testMethods = new ArrayList<>();

        final TestClass testClass = getTestClass();
        final Method[] classMethods = testClass.getJavaClass().getDeclaredMethods();
        for (final Method classMethod : classMethods) {
            final Class retClass = classMethod.getReturnType();
            final int length = classMethod.getParameterTypes().length;
            final int modifiers = classMethod.getModifiers();
            if (null == retClass || 0 != length || Modifier.isStatic(modifiers)
                    || !Modifier.isPublic(modifiers) || Modifier.isInterface(modifiers)
                    || Modifier.isAbstract(modifiers)) {
                continue;
            }
            final String methodName = classMethod.getName();
            if (methodName.toUpperCase().startsWith("TEST")
                    || null != classMethod.getAnnotation(Test.class)) {
                if (classMethod.isAnnotationPresent(Ignore.class)) {
                    testMethods.add(new FrameworkMethodWithTestOption(classMethod, testOptions.get(0)));
                } else {
                    for (final TestOption o : testOptions) {
                        testMethods.add(new FrameworkMethodWithTestOption(classMethod, o));
                    }
                }
            }
        }

        return testMethods;
    }

    @Override
    protected Description describeChild(final FrameworkMethodWithTestOption frameworkMethod) {
        final TestClass testClass = getTestClass();
        return Description.createTestDescription(testClass.getJavaClass(), frameworkMethod.getName());
    }

    private void testJVMBackendFrameworkMethod(final FrameworkMethod aFrameworkMethod, final RunNotifier aRunNotifier) {
        if ("".equals(System.getProperty("BYTECODER_DISABLE_JVMTESTS", ""))) {
            final TestClass testClass = getTestClass();
            final Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " JVM Target");
            aRunNotifier.fireTestStarted(theDescription);
            try {
                // Simply invoke using reflection
                final Object theInstance = testClass.getJavaClass().getDeclaredConstructor().newInstance();
                final Method theMethod = aFrameworkMethod.getMethod();
                theMethod.invoke(theInstance);

                aRunNotifier.fireTestFinished(theDescription);
            } catch (final Exception e) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            }
        }
    }

    private static int getTestWebServerPort() {
        return Integer.parseInt(System.getProperty("BYTECODER_TESTSERVERPORT", "10000"));
    }

    private static synchronized BrowserWebDriverContainer initializeSeleniumContainer() {

        if (SELENIUMCONTAINER == null) {
            java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);

            final ChromeOptions theOptions = new ChromeOptions().setHeadless(true);
            theOptions.addArguments("--js-flags=experimental-wasm-eh");
            theOptions.addArguments("--enable-experimental-wasm-eh");
            theOptions.addArguments("disable-infobars"); // disabling infobars
            theOptions.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
            theOptions.addArguments("--no-sandbox"); // Bypass OS security model
            theOptions.setExperimentalOption("useAutomationExtension", false);
            final LoggingPreferences theLoggingPreferences = new LoggingPreferences();
            theLoggingPreferences.enable(LogType.BROWSER, Level.ALL);
            theOptions.setCapability(CapabilityType.LOGGING_PREFS, theLoggingPreferences);
            theOptions.setCapability("goog:loggingPrefs", theLoggingPreferences);

            Testcontainers.exposeHostPorts(getTestWebServerPort());

            SELENIUMCONTAINER = new BrowserWebDriverContainer()
                    .withCapabilities(theOptions)
                    .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, new File("."));
            SELENIUMCONTAINER.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> SELENIUMCONTAINER.stop()));
        }
        return SELENIUMCONTAINER;
    }

    private static void initializeTestWebServer() throws IOException {
        if (TESTSERVER == null) {

            java.util.logging.Logger.getLogger("sun.net.httpserver.ExchangeImpl").setLevel(Level.OFF);

            TESTSERVER = HttpServer.create();
            final int port = getTestWebServerPort();
            TESTSERVER.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 20);
            TESTSERVER.createContext("/", httpExchange -> {
                final File filesDir = HTTPFILESDIR.get();
                final String fileName = httpExchange.getRequestURI().getPath();
                final int lastSlash = fileName.lastIndexOf('/');
                final String requestedFileName = fileName.substring(lastSlash + 1);
                final File requestedFile = new File(filesDir, requestedFileName);
                if (requestedFile.exists()) {
                    if (requestedFileName.endsWith(".html")) {
                        httpExchange.getResponseHeaders().add("Content-Type", "text/html");
                    } else if (requestedFileName.endsWith(".js")) {
                        httpExchange.getResponseHeaders().add("Content-Type", "text/javascript");
                    } else if (requestedFileName.endsWith(".wasm")) {
                        httpExchange.getResponseHeaders().add("Content-Type", "application/wasm");
                    } else {
                        httpExchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                    }
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, requestedFile.length());
                    FileUtils.copyFile(requestedFile, httpExchange.getResponseBody());
                } else {
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0);
                }
                httpExchange.close();
            });
            TESTSERVER.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> TESTSERVER.stop(0)));
        }
    }

    private static void initializeWebRoot(final File aFile) {
        HTTPFILESDIR.set(aFile);
    }

    private static URL getTestFileUrl(final File aFile) throws MalformedURLException {
        final String theFileName = aFile.getName();
        final InetSocketAddress theServerAddress = TESTSERVER.getAddress();
        return new URL(String.format("http://%s:%d/%s", "host.testcontainers.internal", theServerAddress.getPort(), theFileName));
    }

    private void testJSBackendFrameworkMethod(final FrameworkMethod aFrameworkMethod, final RunNotifier aRunNotifier, final TestOption aTestOption) {
        if ("".equals(System.getProperty("BYTECODER_DISABLE_JSTESTS", ""))) {
            final TestClass testClass = getTestClass();
            final Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " " + aTestOption.toDescription());
            aRunNotifier.fireTestStarted(theDescription);

            try {
                final CompileTarget theCompileTarget = new CompileTarget(testClass.getJavaClass().getClassLoader(), CompileTarget.BackendType.js);

                final BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(aFrameworkMethod.getMethod());

                final BytecodeObjectTypeRef theTestClass = new BytecodeObjectTypeRef(testClass.getName());
                final BytecodeMethodSignature theTestClassConstructorSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]);

                final StringWriter theStrWriter = new StringWriter();
                final PrintWriter theCodeWriter = new PrintWriter(theStrWriter);

                final CompileOptions theOptions = new CompileOptions(LOGGER, true, KnownOptimizer.ALL, aTestOption.isExceptionsEnabled(), "bytecoder", 512, 512, aTestOption.isMinify(), aTestOption.isPreferStackifier(), Allocator.linear, additionalClassesToLink, additionalResources, null, aTestOption.isEscapeAnalysisEnabled());
                final JSCompileResult result = (JSCompileResult) theCompileTarget.compile(theOptions, testClass.getJavaClass(), aFrameworkMethod.getName(), theSignature);
                final CompileResult.StringContent content = (CompileResult.StringContent) result.getContent()[0];

                theCodeWriter.println(content.asString());

                final String theFilename = result.getMinifier().toClassName(theTestClass) + "." + result.getMinifier().toMethodName(aFrameworkMethod.getName(), theSignature) + "_" + aTestOption.toFilePrefix() + ".html";

                theCodeWriter.println();

                theCodeWriter.println("console.log(\"Starting test\");");
                theCodeWriter.println("bytecoder.bootstrap();");
                theCodeWriter.println("var theTestInstance = " + result.getMinifier().toClassName(theTestClass) + "." +  result.getMinifier().toSymbol("__runtimeclass") + "." + result.getMinifier().toMethodName("$newInstance", theTestClassConstructorSignature) + "();");
                theCodeWriter.println("try {");
                theCodeWriter.println("     theTestInstance." + result.getMinifier().toMethodName(aFrameworkMethod.getName(), theSignature) + "();");
                theCodeWriter.println("     console.log(\"Test finished OK\");");
                theCodeWriter.println("} catch (e) {");
                theCodeWriter.println("     if (e.exception) {");
                theCodeWriter.println("         console.log(\"Test finished with exception. Message = \" + bytecoder.toJSString(e.exception.message));");
                theCodeWriter.println("     } else {");
                theCodeWriter.println("         console.log(\"Test finished with exception.\");");
                theCodeWriter.println("     }");
                theCodeWriter.println("     console.log(e.stack);");
                theCodeWriter.println("}");

                theCodeWriter.flush();

                final File theWorkingDirectory = new File(".");

                initializeTestWebServer();

                final BrowserWebDriverContainer theContainer = initializeSeleniumContainer();

                final File theMavenTargetDir = new File(theWorkingDirectory, "target");
                final File theGeneratedFilesDir = new File(theMavenTargetDir, "bytecoderjs");
                theGeneratedFilesDir.mkdirs();

                // Copy additional resources
                for (final CompileResult.Content c : result.getContent()) {
                    if (c instanceof CompileResult.URLContent) {
                        try (final FileOutputStream fos = new FileOutputStream(new File(theGeneratedFilesDir, c.getFileName()))) {
                            c.writeTo(fos);
                        }
                    }
                }

                final File theGeneratedFile = new File(theGeneratedFilesDir, theFilename);
                final PrintWriter theWriter = new PrintWriter(theGeneratedFile);
                theWriter.println("<html><body><script>");
                theWriter.println(theStrWriter.toString());
                theWriter.println("</script></body></html>");
                theWriter.flush();
                theWriter.close();

                initializeWebRoot(theGeneratedFile.getParentFile());

                final URL theTestURL = getTestFileUrl(theGeneratedFile);
                final WebDriver theDriver = theContainer.getWebDriver();
                theDriver.get(theTestURL.toString());

                final List<LogEntry> theAll = theDriver.manage().logs().get(LogType.BROWSER).getAll();
                if (1 > theAll.size()) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("No console output from browser")));
                }
                for (final LogEntry theEntry : theAll) {
                    LOGGER.info(theEntry.getMessage());
                }
                final LogEntry theLast = theAll.get(theAll.size() - 1);

                if (!theLast.getMessage().contains("Test finished OK")) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("Test did not succeed! Got : " + theLast.getMessage())));
                }
            } catch (final Exception e) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            } finally {
                aRunNotifier.fireTestFinished(theDescription);
            }
        }
    }

    private void testWASMASTBackendFrameworkMethod(final FrameworkMethod aFrameworkMethod, final RunNotifier aRunNotifier, final TestOption aTestOption) {
        if (!"".equals(System.getProperty("BYTECODER_DISABLE_WASMTESTS_STACKIFY", "")) && aTestOption.isPreferStackifier()) {
            return;
        }
        if (!"".equals(System.getProperty("BYTECODER_DISABLE_WASMTESTS_RELOOP", "")) && !aTestOption.isPreferStackifier()) {
            return;
        }
        if ("".equals(System.getProperty("BYTECODER_DISABLE_WASMTESTS", ""))) {
            final TestClass testClass = getTestClass();
            final Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " " + aTestOption.toDescription());
            aRunNotifier.fireTestStarted(theDescription);


            try {
                final CompileTarget theCompileTarget = new CompileTarget(testClass.getJavaClass().getClassLoader(), CompileTarget.BackendType.wasm);

                final BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(aFrameworkMethod.getMethod());
                final BytecodeObjectTypeRef theTestClassType = new BytecodeObjectTypeRef(testClass.getName());

                final CompileOptions theOptions = new CompileOptions(LOGGER, true, KnownOptimizer.ALL, false, "bytecoder", 512, 512, aTestOption.isMinify(), aTestOption.isPreferStackifier(), Allocator.linear, additionalClassesToLink, additionalResources, null, aTestOption.isEscapeAnalysisEnabled());
                final WASMCompileResult theResult = (WASMCompileResult) theCompileTarget.compile(theOptions, testClass.getJavaClass(), aFrameworkMethod.getName(), theSignature);
                final WASMCompileResult.WASMCompileContent textualContent = (WASMCompileResult.WASMCompileContent) theResult.getContent()[0];
                final WASMCompileResult.WASMCompileContent binaryContent = (WASMCompileResult.WASMCompileContent)theResult.getContent()[1];
                final WASMCompileResult.WASMCompileContent jsContent = (WASMCompileResult.WASMCompileContent)theResult.getContent()[2];
                final WASMCompileResult.WASMCompileContent sourceMapContent = (WASMCompileResult.WASMCompileContent)theResult.getContent()[3];

                final String theFileName = theResult.getMinifier().toClassName(theTestClassType) + "." + theResult.getMinifier().toMethodName(aFrameworkMethod.getName(), theSignature) + "_" + aTestOption.toFilePrefix()+  ".html";

                final File theWorkingDirectory = new File(".");

                initializeTestWebServer();

                final BrowserWebDriverContainer theContainer = initializeSeleniumContainer();

                final File theMavenTargetDir = new File(theWorkingDirectory, "target");
                final File theGeneratedFilesDir = new File(theMavenTargetDir, "bytecoderwasm");
                theGeneratedFilesDir.mkdirs();

                final File theGeneratedFile = new File(theGeneratedFilesDir, theFileName);

                final PrintWriter theWriter = new PrintWriter(theGeneratedFile);
                theWriter.println("<html>");
                theWriter.println("    <body>");
                theWriter.println("        <h1>Module code</h1>");
                theWriter.println("        <h1>Compilation result</h1>");
                theWriter.println("        <pre id=\"compileresult\">");
                theWriter.println("        </pre>");
                theWriter.println("        <script>");

                theWriter.println(jsContent.asString());

                theWriter.println("            function compile() {");
                theWriter.println("                console.log('Test started');");
                theWriter.println("                try {");

                theWriter.println();
                theWriter.print("                    var binaryBuffer = new Uint8Array([");
                try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    binaryContent.writeTo(bos);
                    bos.flush();
                    final byte[] theData = bos.toByteArray();
                    for (int i = 0; i < theData.length; i++) {
                        if (i > 0) {
                            theWriter.print(",");
                        }
                        theWriter.print(theData[i] & 0xFF);
                    }
                }

                theWriter.println("]);");

                theWriter.println("                    console.log('Size of compiled WASM binary is ' + binaryBuffer.length);");
                theWriter.println();
                theWriter.println("                    var theInstantiatePromise = WebAssembly.instantiate(binaryBuffer, bytecoder.imports);");
                theWriter.println("                    theInstantiatePromise.then(");
                theWriter.println("                         function (resolved) {");
                theWriter.println("                             var wasmModule = resolved.module;");
                theWriter.println("                             bytecoder.init(resolved.instance);");
                theWriter.println("                             bytecoder.exports.initMemory(0);");
                theWriter.println("                             console.log(\"Memory initialized\")");
                theWriter.println("                             console.log(\"Used memory in bytes \" + bytecoder.exports.usedMem());");
                theWriter.println("                             console.log(\"Free memory in bytes \" + bytecoder.exports.freeMem());");
                theWriter.println("                             bytecoder.exports.bootstrap(0);");
                theWriter.println("                             bytecoder.initializeFileIO();");
                theWriter.println("                             console.log(\"Used memory after bootstrap in bytes \" + bytecoder.exports.usedMem());");
                theWriter.println("                             console.log(\"Free memory after bootstrap in bytes \" + bytecoder.exports.freeMem());");
                theWriter.println("                             console.log(\"Creating test instance\")");

                theWriter.print("                             var theTest = bytecoder.exports.");
                theWriter.print(WASMWriterUtils.toMethodName(theTestClassType,
                        "$newInstance", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0])));
                theWriter.println("(0);");
                theWriter.println("                             console.log(\"Bootstrapped\")");
                //theWriter.println("                             bytecoder.exports.logAllocations(0);");
                theWriter.println("                             try {");
                theWriter.println("                                 console.log(\"Starting main method\")");
                theWriter.println("                                 bytecoder.exports.main(theTest);");
                theWriter.println("                                 console.log(\"Main finished\")");
                theWriter.println("                                 console.log(\"Test finished OK\")");
                theWriter.println("                             } catch (e) {");
                theWriter.println("                                 console.log(\"Test threw error\")");
                theWriter.println("                                 throw e;");
                theWriter.println("                             }");
                theWriter.println("                         },");
                theWriter.println("                         function (rejected) {");
                theWriter.println("                             console.log(\"Error instantiating webassembly\");");
                theWriter.println("                             console.log(rejected);");
                theWriter.println("                         }");
                theWriter.println("                    );");
                theWriter.println("                } catch (e) {");
                theWriter.println("                    document.getElementById(\"compileresult\").innerText = e.toString();");
                theWriter.println("                    console.log(e.toString());");
                theWriter.println("                    console.log(e.stack);");
                theWriter.println("                    if (bytecoder.runningInstance) {");
                theWriter.println("                    }");
                theWriter.println("                }");
                theWriter.println("            }");
                theWriter.println();

                theWriter.println("            compile();");
                theWriter.println("        </script>");
                theWriter.println("    </body>");
                theWriter.println("</html>");

                theWriter.flush();
                theWriter.close();

                try (final FileOutputStream fos = new FileOutputStream(new File(theGeneratedFilesDir, theResult.getMinifier().toClassName(theTestClassType) + "." + theResult.getMinifier().toMethodName(aFrameworkMethod.getName(), theSignature) + "_" + aTestOption.toFilePrefix() + ".wat"))) {
                    textualContent.writeTo(fos);
                }

                try (final FileOutputStream fos = new FileOutputStream(new File(theGeneratedFilesDir, theResult.getMinifier().toClassName(theTestClassType) + "." + theResult.getMinifier().toMethodName(aFrameworkMethod.getName(), theSignature) + "_" + aTestOption.toFilePrefix() + ".js"))) {
                    jsContent.writeTo(fos);
                }

                try (final FileOutputStream fos = new FileOutputStream(new File(theGeneratedFilesDir, theResult.getMinifier().toClassName(theTestClassType) + "." + theResult.getMinifier().toMethodName(aFrameworkMethod.getName(), theSignature) + "_" + aTestOption.toFilePrefix() + ".wasm"))) {
                    binaryContent.writeTo(fos);
                }

                try (final FileOutputStream fos = new FileOutputStream(new File(theGeneratedFilesDir, theResult.getMinifier().toClassName(theTestClassType) + "." + theResult.getMinifier().toMethodName(aFrameworkMethod.getName(), theSignature) + "_" + aTestOption.toFilePrefix() + ".wasm.map"))) {
                    sourceMapContent.writeTo(fos);
                }

                // Copy additional resources
                for (final CompileResult.Content c : theResult.getContent()) {
                    if (c instanceof CompileResult.URLContent) {
                        try (final FileOutputStream fos = new FileOutputStream(new File(theGeneratedFilesDir, c.getFileName()))) {
                            c.writeTo(fos);
                        }
                    }
                }

                initializeWebRoot(theGeneratedFile.getParentFile());

                // Invoke test in browser
                final WebDriver theDriver = theContainer.getWebDriver();

                final URL theTestURL = getTestFileUrl(theGeneratedFile);

                theDriver.get(theTestURL.toString());

                final long theStart = System.currentTimeMillis();
                boolean theTestSuccedded = false;

                while (!theTestSuccedded && 10 * 1000 > System.currentTimeMillis() - theStart) {
                    final List<LogEntry> theAll = theDriver.manage().logs().get(LogType.BROWSER).getAll();
                    for (final LogEntry theEntry : theAll) {
                        final String theMessage = theEntry.getMessage();
                        System.out.println(theMessage);

                        if (theMessage.contains("Test finished OK")) {
                            theTestSuccedded = true;
                        }
                    }

                    if (!theTestSuccedded) {
                        Thread.sleep(100);
                    }
                }

                if (!theTestSuccedded) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("Test did not succeed!")));
                }
            } catch (final Exception e) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            } finally {
                aRunNotifier.fireTestFinished(theDescription);
            }
        }
    }

    private void testLLVMWASMASTBackendFrameworkMethod(final FrameworkMethod aFrameworkMethod, final RunNotifier aRunNotifier, final TestOption aTestOption) {
        if ("".equals(System.getProperty("BYTECODER_DISABLE_LLVMWASMTESTS", ""))) {
            final TestClass testClass = getTestClass();
            final Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " " + aTestOption.toDescription());
            aRunNotifier.fireTestStarted(theDescription);

            try {
                final CompileTarget theCompileTarget = new CompileTarget(testClass.getJavaClass().getClassLoader(), CompileTarget.BackendType.wasm_llvm);

                final BytecodeMethodSignature theSignature = theCompileTarget.toMethodSignature(aFrameworkMethod.getMethod());
                final BytecodeObjectTypeRef theTypeRef = new BytecodeObjectTypeRef(testClass.getName());

                final CompileOptions theOptions = new CompileOptions(LOGGER, true, KnownOptimizer.ALL, false, "bytecoder", 512, 512, aTestOption.isMinify(), aTestOption.isPreferStackifier(), Allocator.linear, additionalClassesToLink, additionalResources, LLVMOptimizationLevel.defaultValue(), aTestOption.isEscapeAnalysisEnabled());
                final LLVMCompileResult theResult = (LLVMCompileResult) theCompileTarget.compile(theOptions, testClass.getJavaClass(), aFrameworkMethod.getName(), theSignature);
                final CompileResult.StringContent textualContent = (CompileResult.StringContent) theResult.getContent()[0];
                final CompileResult.StringContent jsContent = (CompileResult.StringContent)theResult.getContent()[1];
                final CompileResult.BinaryContent binaryContent = (CompileResult.BinaryContent)theResult.getContent()[3];

                final String theFileName = LLVMWriterUtils.toMethodName(theTypeRef, aFrameworkMethod.getName(), theSignature) + "_" + aTestOption.toFilePrefix()+  ".html";

                final File theWorkingDirectory = new File(".");

                initializeTestWebServer();

                final BrowserWebDriverContainer theContainer = initializeSeleniumContainer();

                final File theMavenTargetDir = new File(theWorkingDirectory, "target");
                final File theGeneratedFilesDir = new File(theMavenTargetDir, "bytecoderllvmwasm");
                theGeneratedFilesDir.mkdirs();

                final File theGeneratedFile = new File(theGeneratedFilesDir, theFileName);

                final PrintWriter theWriter = new PrintWriter(theGeneratedFile);
                theWriter.println("<html>");
                theWriter.println("    <body>");
                theWriter.println("        <h1>Module code</h1>");
                theWriter.println("        <h1>Compilation result</h1>");
                theWriter.println("        <pre id=\"compileresult\">");
                theWriter.println("        </pre>");
                theWriter.println("        <script>");

                theWriter.println(jsContent.asString());

                theWriter.println("            function compile() {");
                theWriter.println("                console.log('Test started');");
                theWriter.println("                try {");

                theWriter.println();
                theWriter.print("                    var binaryBuffer = new Uint8Array([");
                try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    binaryContent.writeTo(bos);
                    bos.flush();
                    final byte[] theData = bos.toByteArray();
                    for (int i = 0; i < theData.length; i++) {
                        if (i > 0) {
                            theWriter.print(",");
                        }
                        theWriter.print(theData[i] & 0xFF);
                    }
                }

                theWriter.println("]);");

                theWriter.println("                    console.log('Size of compiled WASM binary is ' + binaryBuffer.length);");
                theWriter.println();
                theWriter.println("                    var theInstantiatePromise = WebAssembly.instantiate(binaryBuffer, bytecoder.imports);");
                theWriter.println("                    theInstantiatePromise.then(");
                theWriter.println("                         function (resolved) {");
                theWriter.println("                             var wasmModule = resolved.module;");
                theWriter.println("                             bytecoder.init(resolved.instance);");
                theWriter.println("                             bytecoder.exports.initMemory(0);");
                theWriter.println("                             console.log(\"Memory initialized\")");
                theWriter.println("                             console.log(\"Used memory in bytes \" + bytecoder.exports.usedMem());");
                theWriter.println("                             console.log(\"Free memory in bytes \" + bytecoder.exports.freeMem());");
                theWriter.println("                             bytecoder.exports.bootstrap(0);");
                theWriter.println("                             bytecoder.initializeFileIO();");
                theWriter.println("                             console.log(\"Used memory after bootstrap in bytes \" + bytecoder.exports.usedMem());");
                theWriter.println("                             console.log(\"Free memory after bootstrap in bytes \" + bytecoder.exports.freeMem());");
                theWriter.println("                             console.log(\"Creating test instance\")");

                theWriter.print("                             var theClass = bytecoder.exports.");
                theWriter.print(LLVMWriterUtils.toClassName(theTypeRef));
                theWriter.println("__init();");

                theWriter.print("                             var theTest = bytecoder.exports.");
                theWriter.print(LLVMWriterUtils.toClassName(theTypeRef));
                theWriter.println("_VOID$newInstance(theClass);");
                theWriter.println("                             console.log(\"Bootstrapped\")");
                theWriter.println("                             try {");
                theWriter.println("                                 console.log(\"Starting main method\");");
                theWriter.println("                                 bytecoder.exports.main(theTest);");
                theWriter.println("                                 console.log(\"Main finished\");");
                theWriter.println("                                 console.log(\"Test finished OK\");");
                theWriter.println("                             } catch (e) {");
                theWriter.println("                                 console.log(\"Test threw error\");");
                theWriter.println("                                 throw e;");
                theWriter.println("                             }");
                theWriter.println("                         },");
                theWriter.println("                         function (rejected) {");
                theWriter.println("                             console.log(\"Error instantiating webassembly\");");
                theWriter.println("                             console.log(rejected);");
                theWriter.println("                         }");
                theWriter.println("                    );");
                theWriter.println("                } catch (e) {");
                theWriter.println("                    document.getElementById(\"compileresult\").innerText = e.toString();");
                theWriter.println("                    console.log(e.toString());");
                theWriter.println("                    console.log(e.stack);");
                theWriter.println("                    if (bytecoder.runningInstance) {");
                theWriter.println("                    }");
                theWriter.println("                }");
                theWriter.println("            }");
                theWriter.println();

                theWriter.println("            compile();");
                theWriter.println("        </script>");
                theWriter.println("    </body>");
                theWriter.println("</html>");

                theWriter.flush();
                theWriter.close();

                for (final CompileResult.Content theContent : theResult.getContent()) {
                    if (theContent instanceof CompileResult.URLContent) {
                        try (final FileOutputStream fos = new FileOutputStream(new File(theGeneratedFilesDir, theContent.getFileName()))) {
                            theContent.writeTo(fos);
                        }
                    } else {
                        final File targetFile = new File(theGeneratedFilesDir, LLVMWriterUtils.toMethodName(theTypeRef, aFrameworkMethod.getName(), theSignature) + "_" + theContent.getFileName());
                        try (final FileOutputStream fos = new FileOutputStream(targetFile)) {
                            theContent.writeTo(fos);
                        }
                    }
                }

                initializeWebRoot(theGeneratedFile.getParentFile());

                // Invoke test in browser
                final WebDriver theDriver = theContainer.getWebDriver();

                final URL theTestURL = getTestFileUrl(theGeneratedFile);

                final long theStart = System.currentTimeMillis();
                boolean theTestSuccedded = false;

                theDriver.get(theTestURL.toString());

                while (!theTestSuccedded && 10 * 1000 > System.currentTimeMillis() - theStart) {
                    final List<LogEntry> theAll = theDriver.manage().logs().get(LogType.BROWSER).getAll();
                    for (final LogEntry theEntry : theAll) {
                        final String theMessage = theEntry.getMessage();
                        System.out.println(theMessage);

                        if (theMessage.contains("Test finished OK")) {
                            theTestSuccedded = true;
                        }
                    }

                    if (!theTestSuccedded) {
                        Thread.sleep(100);
                    }
                }

                if (!theTestSuccedded) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("Test did not succeed!")));
                }
            } catch (final Exception e) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            } finally {
                aRunNotifier.fireTestFinished(theDescription);
            }
        }
    }

    @Override
    protected void runChild(final FrameworkMethodWithTestOption aFrameworkMethod, final RunNotifier aRunNotifier) {
        // do not execute ignored tests, only report them
        if (aFrameworkMethod.getMethod().isAnnotationPresent(Ignore.class)) {
            aRunNotifier.fireTestIgnored(
                Description.createTestDescription(
                    getTestClass().getJavaClass(),
                    aFrameworkMethod.getName()
                )
            );
            return;
        }

        final TestOption o = aFrameworkMethod.getTestOption();
        if (o.getBackendType() == null) {
            testJVMBackendFrameworkMethod(aFrameworkMethod, aRunNotifier);
        } else {
            switch (o.getBackendType()) {
                case js:
                    testJSBackendFrameworkMethod(aFrameworkMethod, aRunNotifier, o);
                    break;
                case wasm:
                    testWASMASTBackendFrameworkMethod(aFrameworkMethod, aRunNotifier, o);
                    break;
                case wasm_llvm:
                    testLLVMWASMASTBackendFrameworkMethod(aFrameworkMethod, aRunNotifier, o);
                    break;
                default:
                    throw new IllegalStateException("Unsupported backend :" + o.getBackendType());
            }
        }
    }
}