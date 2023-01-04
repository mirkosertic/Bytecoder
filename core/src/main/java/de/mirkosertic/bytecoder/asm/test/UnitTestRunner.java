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
package de.mirkosertic.bytecoder.asm.test;

import com.sun.net.httpserver.HttpServer;
import de.mirkosertic.bytecoder.asm.AnalysisStack;
import de.mirkosertic.bytecoder.asm.CompileUnit;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.backend.js.JSBackend;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
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
            } else {
                for (final BytecoderTestOption o : declaredOptions.value()) {
                    testOptions.add(new TestOption(o.backend().name(), o.minify()));
                }
            }
            additionalClassesToLink = declaredOptions.additionalClassesToLink();
            additionalResources = declaredOptions.additionalResources();
        } else {
            testOptions.add(new TestOption(null, false));
            testOptions.add(new TestOption("js", false));

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

                final AnalysisStack analysisStack = new AnalysisStack();

                final ClassLoader cl = testClass.getJavaClass().getClassLoader();
                final CompileUnit compileUnit = new CompileUnit(cl);
                final Type invokedType = Type.getType(testClass.getJavaClass());
                final ResolvedClass resolvedClass = compileUnit.resolveClass(invokedType, analysisStack);
                final ResolvedMethod method = resolvedClass.resolveMethod(aFrameworkMethod.getName(), Type.getMethodType(Type.VOID_TYPE), analysisStack);

                final ByteArrayOutputStream compiledCode = new ByteArrayOutputStream();

                final JSBackend backend = new JSBackend();
                backend.generateCodeFor(compileUnit, compiledCode);


                final StringWriter theStrWriter = new StringWriter();
                final PrintWriter theCodeWriter = new PrintWriter(theStrWriter);

                theCodeWriter.println(compiledCode);

                final String theClassName = backend.generateClassName(invokedType);
                final String theMethodName = backend.generateMethodName(method.methodNode.name);

                final String theFilename = theClassName + "." + theMethodName + "_" + aTestOption.toFilePrefix() + ".html";

                theCodeWriter.println();

                theCodeWriter.println("console.log(\"Starting test\");");
                theCodeWriter.println("var theTestInstance = new " + theClassName + "();");
                theCodeWriter.println("try {");
                theCodeWriter.println("     theTestInstance." + theMethodName + "();");
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

                final File theMavenTargetDir = new File(theWorkingDirectory, "target");
                final File theGeneratedFilesDir = new File(theMavenTargetDir, "bytecoder_js_new");
                theGeneratedFilesDir.mkdirs();

                final File theGeneratedFile = new File(theGeneratedFilesDir, theFilename);
                final PrintWriter theWriter = new PrintWriter(theGeneratedFile);
                theWriter.println("<html><body><script>");
                theWriter.println(theStrWriter);
                theWriter.println("</script></body></html>");
                theWriter.flush();
                theWriter.close();

                initializeTestWebServer();

                final BrowserWebDriverContainer theContainer = initializeSeleniumContainer();

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
                default:
                    throw new IllegalStateException("Unsupported backend :" + o.getBackendType());
            }
        }
    }
}
