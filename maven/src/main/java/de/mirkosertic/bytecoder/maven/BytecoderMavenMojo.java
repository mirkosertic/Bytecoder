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

import de.mirkosertic.bytecoder.core.backend.js.JSBackend;
import de.mirkosertic.bytecoder.core.backend.js.JSCompileResult;
import de.mirkosertic.bytecoder.core.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmBackend;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmCompileResult;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmIntrinsics;
import de.mirkosertic.bytecoder.core.ir.AnalysisException;
import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.Loader;
import de.mirkosertic.bytecoder.core.backend.CompileResult;
import de.mirkosertic.bytecoder.core.Slf4JLogger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin to run Bytecoder using Maven.
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
    requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BytecoderMavenMojo extends AbstractMojo {

    @Parameter( defaultValue = "${project}", readonly = true )
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File classFiles;

    /**
     * Classname with the main class to be compiled.
     */
    @Parameter(required = true)
    protected String mainClass;

    /**
     * Backend to be used, can be either js,wasm or wasm_llvm.
     */
    @Parameter(required = true, defaultValue = "js")
    protected String backend;

    /**
     * The build target directory.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected String buildDirectory;

    /**
     * Shall debug output be generated?
     */
    @Parameter(required = false, defaultValue = "false")
    protected boolean debugOutput;

    /**
     * Which kind of optimization should be applied? Can be NONE, ALL or EXPERIMENTAL.
     */
    @Parameter(required = false, defaultValue = "ALL")
    protected String optimizationLevel;

    /**
     * Prefix of the generated files.
     */
    @Parameter(required = false, defaultValue = "bytecoder")
    protected String filenamePrefix;

    /**
     * List of full qualified class names to be linked beside the statically referenced ones.
     */
    @Parameter(required = false)
    protected String[] additionalClassesToLink = new String[0];

    /**
     * A list of classpath resources to be included into the build.
     */
    @Parameter(required = false)
    protected String[] additionalResources = new String[0];

    @Override
    public void execute() throws MojoExecutionException {
        final File baseDirectory = new File(buildDirectory);
        final File bytecoderDirectory = new File(baseDirectory, "bytecoder");
        bytecoderDirectory.mkdirs();

        try {
            final ClassLoader classLoader = prepareClassLoader();
            final Loader loader = new BytecoderLoader(classLoader);

            if ("js".equals(backend)) {

                final CompileUnit compileUnit = new CompileUnit(loader, new Slf4JLogger(), new JSIntrinsics());
                final Type invokedType = Type.getObjectType(mainClass.replace('.', '/'));

                compileUnit.resolveMainMethod(invokedType, "main", Type.getMethodType(Type.VOID_TYPE, Type.getType("[Ljava/lang/String;")));

                for (final String className : additionalClassesToLink) {
                    compileUnit.resolveClass(Type.getObjectType(className.replace('.', '/')), new AnalysisStack());
                }

                compileUnit.finalizeLinkingHierarchy();

                compileUnit.logStatistics();

                final de.mirkosertic.bytecoder.core.backend.CompileOptions compileOptions =
                        new de.mirkosertic.bytecoder.core.backend.CompileOptions(new Slf4JLogger(), Optimizations.valueOf(optimizationLevel), additionalResources, filenamePrefix, debugOutput);

                final JSBackend backend = new JSBackend();
                final JSCompileResult result = backend.generateCodeFor(compileUnit, compileOptions);

                for (final CompileResult.Content content : result.getContent()) {
                    final File theBytecoderFileName = new File(bytecoderDirectory, content.getFileName());
                    try (final FileOutputStream theFos = new FileOutputStream(theBytecoderFileName)) {
                        content.writeTo(theFos);
                    }
                }

            } else {
                final CompileUnit compileUnit = new CompileUnit(loader, new Slf4JLogger(), new WasmIntrinsics());
                final Type invokedType = Type.getObjectType(mainClass.replace('.', '/'));

                compileUnit.resolveMainMethod(invokedType, "main", Type.getMethodType(Type.VOID_TYPE, Type.getType("[Ljava/lang/String;")));

                for (final String className : additionalClassesToLink) {
                    compileUnit.resolveClass(Type.getObjectType(className.replace('.', '/')), new AnalysisStack());
                }

                compileUnit.finalizeLinkingHierarchy();

                compileUnit.logStatistics();

                final de.mirkosertic.bytecoder.core.backend.CompileOptions compileOptions =
                        new de.mirkosertic.bytecoder.core.backend.CompileOptions(new Slf4JLogger(), Optimizations.valueOf(optimizationLevel), additionalResources, filenamePrefix, debugOutput);

                final WasmBackend backend = new WasmBackend();
                final WasmCompileResult result = backend.generateCodeFor(compileUnit, compileOptions);

                for (final CompileResult.Content content : result.getContent()) {
                    final File theBytecoderFileName = new File(bytecoderDirectory, content.getFileName());
                    try (final FileOutputStream theFos = new FileOutputStream(theBytecoderFileName)) {
                        content.writeTo(theFos);
                    }
                }
            }
        } catch (final AnalysisException e) {
            throw new MojoExecutionException("Error running Bytecoder : " + printStackTrace(e.getAnalysisStack()), e);
        } catch (final Exception e) {
            throw new MojoExecutionException("Error running Bytecoder", e);
        }
    }

    private String printStackTrace(final AnalysisStack analysisStack) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintStream stringStream = new PrintStream(bos);
        analysisStack.dumpAnalysisStack(stringStream);
        return "\n" + bos;
    }

    protected boolean isSupportedScope(final String scope) {
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
            final List<URL> theURLs = new ArrayList<>();
            for (final Artifact artifact : project.getArtifacts()) {
                if (!isSupportedScope(artifact.getScope())) {
                    continue;
                }
                final File file = artifact.getFile();
                theURLs.add(file.toURI().toURL());
            }
            theURLs.add(classFiles.toURI().toURL());

            return new URLClassLoader(theURLs.toArray(new URL[0]),
                    getClass().getClassLoader());
        } catch (final MalformedURLException e) {
            throw new MojoExecutionException("Cannot create classloader", e);
        }
    }
}
