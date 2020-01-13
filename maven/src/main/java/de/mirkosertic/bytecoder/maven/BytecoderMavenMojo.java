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

import de.mirkosertic.bytecoder.allocator.Allocator;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
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
     * Shall Exception-Handling be activated?
     */
    @Parameter(required = false, defaultValue = "false")
    protected boolean enableExceptionHandling;

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
     * Minimum number of pages for WASM memory.
     */
    @Parameter(required = false, defaultValue = "512")
    protected int wasmInitialPages;

    /**
     * Maximum number of pages for WASM memory.
     */
    @Parameter(required = false, defaultValue = "1024")
    protected int wasmMaximumPages;

    /**
     * Shall the compile result be minified?
     */
    @Parameter(required = false, defaultValue = "true")
    protected boolean minifyCompileResult;

    /**
     * Shall the Stackifier be used and the Relooper as fallback?
     */
    @Parameter(required = false, defaultValue = "false")
    protected boolean preferStackifier;

    /**
     * Which register allocator should be used? Can be linear or passthru.
     */
    @Parameter(required = false, defaultValue = "linear")
    protected String registerAllocator;

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
        final File theBaseDirectory = new File(buildDirectory);
        final File theBytecoderDirectory = new File(theBaseDirectory, "bytecoder");
        theBytecoderDirectory.mkdirs();

        try {
            final ClassLoader theLoader = prepareClassLoader();
            final Class theTargetClass = theLoader.loadClass(mainClass);

            final CompileTarget theCompileTarget = new CompileTarget(theLoader, CompileTarget.BackendType.valueOf(backend));

            final BytecodeMethodSignature theSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                    new BytecodeTypeRef[] { new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(String.class), 1) });

            final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), debugOutput, KnownOptimizer.valueOf(optimizationLevel), enableExceptionHandling, filenamePrefix, wasmInitialPages, wasmMaximumPages, minifyCompileResult, preferStackifier, Allocator.valueOf(registerAllocator), additionalClassesToLink, additionalResources);
            final CompileResult theCode = theCompileTarget.compile(theOptions, theTargetClass, "main", theSignature);
            for (final CompileResult.Content content : theCode.getContent()) {
                final File theBytecoderFileName = new File(theBytecoderDirectory, content.getFileName());
                try (final FileOutputStream theFos = new FileOutputStream(theBytecoderFileName)) {
                    content.writeTo(theFos);
                }
            }

        } catch (final Exception e) {
            throw new MojoExecutionException("Error running bytecoder", e);
        }
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
