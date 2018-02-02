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
package de.mirkosertic.bytecoder.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.google.common.io.Files;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.backend.wasm.WASMCompileResult;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.ssa.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

/**
 * Plugin to run Bytecoder using Maven.
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BytecoderMavenMojo extends AbstractMojo {

    @Component
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classFiles;

    /**
     * Classname with the main class to be compiled.
     */
    @Parameter(required = true)
    protected String mainClass;

    /**
     * Backend to be used.
     */
    @Parameter(required = true, defaultValue = "js")
    protected String backend;

    /**
     * The build target directory.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected String buldDirectory;

    /**
     * Shall the JavaScript be optimized with the Google closure compiler?
     */
    @Parameter(required = false, defaultValue = "false")
    protected boolean optimizeWithGoogleClosure;

    /**
     * Shall debug output be generated?
     */
    @Parameter(required = false, defaultValue = "false")
    protected boolean debugOutput;

    /**
     * The closure optimization level.
     */
    @Parameter(required = false, defaultValue = "SIMPLE_OPTIMIZATIONS")
    protected String closureOptimizationLevel;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File theBaseDirectory = new File(buldDirectory);
        File theBytecoderDirectory = new File(theBaseDirectory, "bytecoder");
        theBytecoderDirectory.mkdirs();

        try {
            ClassLoader theLoader = prepareClassLoader();
            Class theTargetClass = theLoader.loadClass(mainClass);

            CompileTarget theCompileTarget = new CompileTarget(theLoader, CompileTarget.BackendType.valueOf(backend));
            File theBytecoderFileName = new File(theBytecoderDirectory, theCompileTarget.generatedFileName());

            BytecodeMethodSignature theSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                    new BytecodeTypeRef[] { new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(TString.class), 1) });

            CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), debugOutput, KnownOptimizer.ALL);
            CompileResult theCode = theCompileTarget.compileToJS(theOptions, theTargetClass, "main", theSignature);
            try (PrintWriter theWriter = new PrintWriter(new FileWriter(theBytecoderFileName))) {
                theWriter.println(theCode.getData());
            }

            if (optimizeWithGoogleClosure) {
                Compiler theCompiler = new Compiler();
                CompilerOptions theClosureOptions = new CompilerOptions();
                theClosureOptions.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT);
                theClosureOptions.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT);

                CompilationLevel.valueOf(closureOptimizationLevel).setOptionsForCompilationLevel(theClosureOptions);

                List<SourceFile> theSourceFiles = CommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.BROWSER);
                theSourceFiles.add(SourceFile.fromCode("bytecoder.js", (String) theCode.getData()));
                theCompiler.compile(new ArrayList<>(), theSourceFiles, theClosureOptions);
                String theClosureCode = theCompiler.toSource();

                File theBytecoderClosureFileName = new File(theBytecoderDirectory, "bytecoder-closure.js");

                try (PrintWriter theWriter = new PrintWriter(new FileWriter(theBytecoderClosureFileName))) {
                    theWriter.println(theClosureCode);
                }
            }

            if (theCode instanceof WASMCompileResult) {
                WASMCompileResult theWASMCompileResult = (WASMCompileResult) theCode;
                int[] theWASM = wat2wasm(theWASMCompileResult);
                File theBytecoderWASMFileName = new File(theBytecoderDirectory, "bytecoder.wasm");
                try (FileOutputStream theFos = new FileOutputStream(theBytecoderWASMFileName)) {
                    for (int aTheWASM : theWASM) {
                        theFos.write(aTheWASM);
                    }
                }

            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error running bytecoder", e);
        }
    }

    private int[] wat2wasm(WASMCompileResult aResult) throws IOException {
        String theChromeDriverBinary = System.getenv("CHROMEDRIVER_BINARY");
        if (theChromeDriverBinary == null || theChromeDriverBinary.isEmpty()) {
            throw new RuntimeException("No chromedriver binary found! Please set CHROMEDRIVER_BINARY environment variable!");
        }

        ChromeDriverService.Builder theDriverServiceBuilder = new ChromeDriverService.Builder();
        theDriverServiceBuilder = theDriverServiceBuilder.withVerbose(false);
        theDriverServiceBuilder = theDriverServiceBuilder.usingDriverExecutable(new File(theChromeDriverBinary));

        ChromeDriverService theDriverService = theDriverServiceBuilder.build();
        theDriverService.start();

        File theTempDirectory = Files.createTempDir();
        File theGeneratedFile = new File(theTempDirectory, "compile.html");

        // Copy WABT Tools
        File theWABTFile = new File(theTempDirectory, "libwabt.js");
        try (FileOutputStream theOS = new FileOutputStream(theWABTFile)) {
            IOUtils.copy(getClass().getResourceAsStream("/libwabt.js"), theOS);
        }

        PrintWriter theWriter = new PrintWriter(theGeneratedFile);
        theWriter.println("<html>");
        theWriter.println("    <body>");
        theWriter.println("        <h1>Module code</h1>");
        theWriter.println("        <pre id=\"modulecode\">");
        theWriter.println(aResult.getData());
        theWriter.println("        </pre>");
        theWriter.println("        <h1>Compilation result</h1>");
        theWriter.println("        <pre id=\"compileresult\">");
        theWriter.println("        </pre>");
        theWriter.println("        <script src=\"libwabt.js\">");
        theWriter.println("        </script>");
        theWriter.println("        <script>");
        theWriter.println("            function compile() {");
        theWriter.println("                console.log('Compilation started');");
        theWriter.println("                try {");
        theWriter.println("                    var module = wabt.parseWat('test.wast', document.getElementById(\"modulecode\").innerText);");
        theWriter.println("                    module.resolveNames();");
        theWriter.println("                    module.validate();");
        theWriter.println("                    var binaryOutput = module.toBinary({log: true, write_debug_names:true});");
        theWriter.println("                    document.getElementById(\"compileresult\").innerText = binaryOutput.log;");
        theWriter.println("                    return binaryOutput.buffer;");
        theWriter.println("                } catch (e) {");
        theWriter.println("                    document.getElementById(\"compileresult\").innerText = e.toString();");
        theWriter.println("                    console.log(e.toString());");
        theWriter.println("                    console.log(e.stack);");
        theWriter.println("                }");
        theWriter.println("            }");
        theWriter.println("        </script>");
        theWriter.println("    </body>");
        theWriter.println("</html>");

        theWriter.flush();
        theWriter.close();

        ChromeOptions theOptions = new ChromeOptions();
        theOptions.addArguments("headless");
        theOptions.addArguments("disable-gpu");

        LoggingPreferences theLoggingPreferences = new LoggingPreferences();
        theLoggingPreferences.enable(LogType.BROWSER, Level.ALL);
        theOptions.setCapability(CapabilityType.LOGGING_PREFS, theLoggingPreferences);

        DesiredCapabilities theCapabilities = DesiredCapabilities.chrome();
        theCapabilities.setCapability(ChromeOptions.CAPABILITY, theOptions);

        RemoteWebDriver theDriver = new RemoteWebDriver(theDriverService.getUrl(), theCapabilities);

        theDriver.get(theGeneratedFile.toURI().toURL().toString());

        ArrayList<Long> theResult = (ArrayList<Long>) theDriver.executeScript("return compile();");
        int[] theBinaryDara = new int[theResult.size()];
        for (int i=0;i<theResult.size();i++) {
            long theLongValue = theResult.get(i);
            theBinaryDara[i] = (int) (theLongValue);
        }
        List<LogEntry> theAll = theDriver.manage().logs().get(LogType.BROWSER).getAll();
        for (LogEntry theEntry : theAll) {
            System.out.println(theEntry.getMessage());
        }

        theDriver.close();
        theDriverService.stop();

        return theBinaryDara;
    }

    protected boolean isSupportedScope(String scope) {
        switch (scope) {
        case Artifact.SCOPE_COMPILE:
        case Artifact.SCOPE_PROVIDED:
        case Artifact.SCOPE_SYSTEM:
            return true;
        default:
            return false;
        }
    }

    protected final ClassLoader prepareClassLoader() throws MojoExecutionException {
        try {
            List<URL> theURLs = new ArrayList<>();
            StringBuilder theClassPath = new StringBuilder();
            for (Artifact artifact : project.getArtifacts()) {
                if (!isSupportedScope(artifact.getScope())) {
                    continue;
                }
                File file = artifact.getFile();
                if (theClassPath.length() > 0) {
                    theClassPath.append(':');
                }
                theClassPath.append(file.getPath());
                theURLs.add(file.toURI().toURL());
            }
            if (theClassPath.length() > 0) {
                theClassPath.append(':');
            }
            theClassPath.append(classFiles.getPath());
            theURLs.add(classFiles.toURI().toURL());

            return new URLClassLoader(theURLs.toArray(new URL[theURLs.size()]),
                    getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Cannot create classloader", e);
        }
    }
}