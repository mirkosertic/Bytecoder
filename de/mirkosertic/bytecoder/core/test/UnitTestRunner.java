/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.test;

import com.sun.net.httpserver.HttpServer;
import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.backend.CodeGenerationFailure;
import de.mirkosertic.bytecoder.core.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.backend.CompileResult;
import de.mirkosertic.bytecoder.core.backend.js.JSBackend;
import de.mirkosertic.bytecoder.core.backend.js.JSCompileResult;
import de.mirkosertic.bytecoder.core.backend.js.JSHelpers;
import de.mirkosertic.bytecoder.core.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmBackend;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmCompileResult;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmHelpers;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmIntrinsics;
import de.mirkosertic.bytecoder.core.ir.AnalysisException;
import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.Loader;
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
import org.objectweb.asm.Type;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

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

public class UnitTestRunner extends ParentRunner<FrameworkMethodWithTestOption> {

    private static final Slf4JLogger LOGGER = new Slf4JLogger();

    private final List<TestOption> testOptions;
    private final String[] additionalClassesToLink;
    private final String[] additionalResources;

    private static HttpServer TESTSERVER;
    private static BrowserWebDriverContainer SELENIUMCONTAINER;
    private static final AtomicReference<File> HTTPFILESDIR = new AtomicReference<>();

    public UnitTestRunner(final Class aClass) throws InitializationError {
        super(aClass);

        testOptions = new ArrayList<>();
        final BytecoderTestOptions declaredOptions = getTestClass().getJavaClass().getAnnotation(BytecoderTestOptions.class);
        if (declaredOptions != null) {
            if (declaredOptions.includeJVM()) {
                testOptions.add(new TestOption(null, false));
            }
            if (declaredOptions.value().length == 0 && declaredOptions.includeTestPermutations()) {
                testOptions.add(new TestOption("js", false));
                testOptions.add(new TestOption("wasm", false));
            } else {
                for (final BytecoderTestOption o : declaredOptions.value()) {
                    testOptions.add(new TestOption(o.backend(), o.minify()));
                }
            }
            additionalClassesToLink = declaredOptions.additionalClassesToLink();
            additionalResources = declaredOptions.additionalResources();
        } else {
            testOptions.add(new TestOption(null, false));
            testOptions.add(new TestOption("js", false));
            testOptions.add(new TestOption("wasm", false));

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

            theOptions.addArguments("--js-flags=--experimental-wasm-gc");
            theOptions.addArguments("--enable---experimental-wasm-gc");

            theOptions.addArguments("disable-infobars"); // disabling infobars
            theOptions.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
            theOptions.addArguments("--no-sandbox"); // Bypass OS security model
            theOptions.setExperimentalOption("useAutomationExtension", false);

            final LoggingPreferences theLoggingPreferences = new LoggingPreferences();
            theLoggingPreferences.enable(LogType.BROWSER, Level.ALL);
            theOptions.setCapability("goog:loggingPrefs", theLoggingPreferences);

            Testcontainers.exposeHostPorts(getTestWebServerPort());

            SELENIUMCONTAINER = new BrowserWebDriverContainer("selenium/standalone-chrome:beta")
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

            final File workingDirectory = new File(".");

            final File mavenTargetDir = new File(workingDirectory, "target");
            final File generatedFilesDir = new File(mavenTargetDir, "bytecoder_js_new");
            generatedFilesDir.mkdirs();

            try {

                final ClassLoader cl = testClass.getJavaClass().getClassLoader();
                final Loader loader = new BytecoderLoader(cl);

                final CompileUnit compileUnit = new CompileUnit(loader, LOGGER, new JSIntrinsics());
                final Type invokedType = Type.getType(testClass.getJavaClass());

                final ResolvedMethod method = compileUnit.resolveMainMethod(invokedType, aFrameworkMethod.getName(), Type.getMethodType(Type.VOID_TYPE));

                for (final String className : additionalClassesToLink) {
                    compileUnit.resolveClass(Type.getObjectType(className.replace('.', '/')), new AnalysisStack());
                }

                compileUnit.finalizeLinkingHierarchy();

                compileUnit.logStatistics();

                final StringWriter strWriter = new StringWriter();
                final PrintWriter codeWriter = new PrintWriter(strWriter);

                final String className = JSHelpers.generateClassName(invokedType);
                final String methodName = JSHelpers.generateMethodName(method.methodNode.name, Type.getMethodType(method.methodNode.desc));
                final String filenamePrefix = className + "." + methodName + "_" + aTestOption.toFilePrefix();

                final CompileOptions compileOptions = new CompileOptions(LOGGER, Optimizations.DISABLED, additionalResources, filenamePrefix, true);

                final JSBackend backend = new JSBackend();
                final JSCompileResult result = backend.generateCodeFor(compileUnit, compileOptions);

                for (final CompileResult.Content c : result.getContent()) {
                    if (c instanceof CompileResult.StringContent) {
                        codeWriter.println(c.asString());
                    }
                }

                final String filename = className + "." + methodName + "_" + aTestOption.toFilePrefix() + ".html";

                codeWriter.println("console.log(\"Starting test\");");
                codeWriter.println("var theTestInstance = new " + className + "();");
                codeWriter.println("try {");
                codeWriter.println("     theTestInstance." + methodName + "();");
                codeWriter.println("     console.log(\"Test finished OK\");");
                codeWriter.println("} catch (e) {");
                codeWriter.println("     if (e instanceof java$lang$Throwable) {");
                codeWriter.println("         console.log(\"Test finished with exception \" + e.constructor.name + \". Message = \" + bytecoder.toJSString(e.message));");
                codeWriter.println("     } else {");
                codeWriter.println("         console.log(\"Test finished with exception.\");");
                codeWriter.println("     }");
                codeWriter.println("     console.log(e.stack);");
                codeWriter.println("}");

                codeWriter.flush();

                for (final CompileResult.Content c : result.getContent()) {
                    if (!(c instanceof CompileResult.StringContent)) {
                        final File output = new File(generatedFilesDir, c.getFileName());
                        try (final FileOutputStream fos = new FileOutputStream(output)) {
                            c.writeTo(fos);
                        }
                    }
                }

                final File generatedFile = new File(generatedFilesDir, filename);
                final PrintWriter writer = new PrintWriter(generatedFile);
                writer.println("<html><body><script>");
                writer.println(strWriter);
                writer.println("</script></body></html>");
                writer.flush();
                writer.close();

                initializeTestWebServer();

                final BrowserWebDriverContainer browserContainer = initializeSeleniumContainer();

                initializeWebRoot(generatedFile.getParentFile());

                final URL testURL = getTestFileUrl(generatedFile);
                final WebDriver driver = browserContainer.getWebDriver();
                driver.get(testURL.toString());

                List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
                if (1 > logs.size()) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("No console output from browser")));
                }
                LogEntry lastLogEntry = logs.get(logs.size() - 1);

                int waitCounter = 0;
                while (!lastLogEntry.getMessage().contains("Test finished OK") && waitCounter < 10) {
                    waitCounter++;
                    Thread.sleep(100);

                    logs = driver.manage().logs().get(LogType.BROWSER).getAll();
                    if (logs.size() > 0) {
                        lastLogEntry = logs.get(logs.size() - 1);
                    }
                }

                for (final LogEntry entry : logs) {
                    LOGGER.info(entry.getMessage());
                }

                if (!lastLogEntry.getMessage().contains("Test finished OK")) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("Test did not succeed! Got : " + lastLogEntry.getMessage())));
                }

            } catch (final CodeGenerationFailure e) {

                final ResolvedMethod rm = e.getMethod();

                final String className = JSHelpers.generateClassName(rm.owner.type);
                final String methodName = JSHelpers.generateMethodName(rm.methodNode.name, Type.getMethodType(rm.methodNode.desc));

                try {
                    final String filenameDg = className + "." + methodName + "_" + aTestOption.toFilePrefix() + "_debuggraph.dot";
                    try (final FileOutputStream fos = new FileOutputStream(new File(generatedFilesDir, filenameDg))) {
                        rm.methodBody.writeDebugTo(fos);
                    }

                    final String filenameDt = className + "." + methodName + "_" + aTestOption.toFilePrefix() + "_dominatortree.dot";
                    try (final FileOutputStream fos = new FileOutputStream(new File(generatedFilesDir, filenameDt))) {
                        e.getDominatorTree().writeDebugTo(fos);
                    }

                } catch (final IOException ex) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, ex));
                }

                aRunNotifier.fireTestFailure(new Failure(theDescription, e));

            } catch (final AnalysisException e) {
                e.getAnalysisStack().dumpAnalysisStack(System.out);
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            } catch (final Exception e) {
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            } finally {
                aRunNotifier.fireTestFinished(theDescription);
            }
        }
    }

    private void testWasmBackendFrameworkMethod(final FrameworkMethod aFrameworkMethod, final RunNotifier aRunNotifier, final TestOption aTestOption) {
        if ("".equals(System.getProperty("BYTECODER_DISABLE_WASMTESTS", ""))) {
            final TestClass testClass = getTestClass();
            final Description theDescription = Description.createTestDescription(testClass.getJavaClass(), aFrameworkMethod.getName() + " " + aTestOption.toDescription());
            aRunNotifier.fireTestStarted(theDescription);

            final File workingDirectory = new File(".");

            final File mavenTargetDir = new File(workingDirectory, "target");
            final File generatedFilesDir = new File(mavenTargetDir, "bytecoder_wasm_new");
            generatedFilesDir.mkdirs();

            try {

                final ClassLoader cl = testClass.getJavaClass().getClassLoader();
                final Loader loader = new BytecoderLoader(cl);

                final CompileUnit compileUnit = new CompileUnit(loader, LOGGER, new WasmIntrinsics());
                final Type invokedType = Type.getType(testClass.getJavaClass());

                final ResolvedMethod method = compileUnit.resolveMainMethod(invokedType, aFrameworkMethod.getName(), Type.getMethodType(Type.VOID_TYPE));

                for (final String className : additionalClassesToLink) {
                    compileUnit.resolveClass(Type.getObjectType(className.replace('.', '/')), new AnalysisStack());
                }

                compileUnit.finalizeLinkingHierarchy();

                compileUnit.logStatistics();

                final StringWriter strWriter = new StringWriter();
                final PrintWriter codeWriter = new PrintWriter(strWriter);

                final String className = WasmHelpers.generateClassName(invokedType);
                final String methodName = WasmHelpers.generateMethodName(method.methodNode.name, Type.getMethodType(method.methodNode.desc));
                final String filenamePrefix = className + "." + methodName + "_" + aTestOption.toFilePrefix();

                final CompileOptions compileOptions = new CompileOptions(LOGGER, Optimizations.DISABLED, additionalResources, filenamePrefix, true);

                final WasmBackend backend = new WasmBackend();
                final WasmCompileResult result = backend.generateCodeFor(compileUnit, compileOptions);

                for (final CompileResult.Content c : result.getContent()) {
                    if (c instanceof CompileResult.StringContent && c.getFileName().endsWith(".js")) {
                        codeWriter.println(c.asString());
                    }
                }

                final String filename = className + "." + methodName + "_" + aTestOption.toFilePrefix() + ".html";

                codeWriter.println("console.log(\"Starting test\");");
                codeWriter.print("bytecoder.instantiate('");
                codeWriter.print(filenamePrefix);
                codeWriter.println("wasmclasses.wasm').then(function() {");
                codeWriter.println("    try {");
                codeWriter.println("        bytecoder.instance.exports.main(null);");
                codeWriter.println("        console.log(\"Test finished OK\");");
                codeWriter.println("    } catch (e) {");
                codeWriter.println("        console.log(\"Test finished with exception.\");");
                codeWriter.println("        console.log(e);");
                codeWriter.println("    }");
                codeWriter.println("});");

                codeWriter.flush();

                for (final CompileResult.Content c : result.getContent()) {
                    if (!c.getFileName().endsWith(".js")) {
                        final File output = new File(generatedFilesDir, c.getFileName());
                        try (final FileOutputStream fos = new FileOutputStream(output)) {
                            c.writeTo(fos);
                        }
                    }
                }

                final File generatedFile = new File(generatedFilesDir, filename);
                final PrintWriter writer = new PrintWriter(generatedFile);
                writer.println("<html><body><script>");
                writer.println(strWriter);
                writer.println("</script></body></html>");
                writer.flush();
                writer.close();

                initializeTestWebServer();

                final BrowserWebDriverContainer browserContainer = initializeSeleniumContainer();

                initializeWebRoot(generatedFile.getParentFile());

                final URL testURL = getTestFileUrl(generatedFile);
                final WebDriver driver = browserContainer.getWebDriver();
                driver.get(testURL.toString());

                List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
                if (1 > logs.size()) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("No console output from browser")));
                }
                LogEntry lastLogEntry = logs.get(logs.size() - 1);

                int waitCounter = 0;
                while (!lastLogEntry.getMessage().contains("Test finished OK") && waitCounter < 10) {
                    waitCounter++;
                    Thread.sleep(100);

                    logs = driver.manage().logs().get(LogType.BROWSER).getAll();
                    if (logs.size() > 0) {
                        lastLogEntry = logs.get(logs.size() - 1);
                    }
                }

                for (final LogEntry entry : logs) {
                    LOGGER.info(entry.getMessage());
                }

                if (!lastLogEntry.getMessage().contains("Test finished OK")) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, new RuntimeException("Test did not succeed! Got : " + lastLogEntry.getMessage())));
                }

            } catch (final CodeGenerationFailure e) {

                final ResolvedMethod rm = e.getMethod();

                final String className = WasmHelpers.generateClassName(rm.owner.type);
                final String methodName = WasmHelpers.generateMethodName(rm.methodNode.name, Type.getMethodType(rm.methodNode.desc));

                try {
                    final String filenameDg = className + "." + methodName + "_" + aTestOption.toFilePrefix() + "_debuggraph.dot";
                    try (final FileOutputStream fos = new FileOutputStream(new File(generatedFilesDir, filenameDg))) {
                        rm.methodBody.writeDebugTo(fos);
                    }

                    final String filenameDt = className + "." + methodName + "_" + aTestOption.toFilePrefix() + "_dominatortree.dot";
                    try (final FileOutputStream fos = new FileOutputStream(new File(generatedFilesDir, filenameDt))) {
                        e.getDominatorTree().writeDebugTo(fos);
                    }

                } catch (final IOException ex) {
                    aRunNotifier.fireTestFailure(new Failure(theDescription, ex));
                }

                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
            } catch (final AnalysisException e) {
                e.getAnalysisStack().dumpAnalysisStack(System.out);
                aRunNotifier.fireTestFailure(new Failure(theDescription, e));
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
                case "js":
                    testJSBackendFrameworkMethod(aFrameworkMethod, aRunNotifier, o);
                    break;
                case "wasm":
                    testWasmBackendFrameworkMethod(aFrameworkMethod, aRunNotifier, o);
                    break;
                default:
                    throw new IllegalStateException("Unsupported backend :" + o.getBackendType());
            }
        }
    }
}
