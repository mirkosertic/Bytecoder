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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

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

import de.mirkosertic.bytecoder.backend.js.JSCompileTarget;
import de.mirkosertic.bytecoder.classlib.java.lang.TString;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

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
    @Parameter(required = true, defaultValue = "ssacompiler")
    protected String backend;

    /**
     * The build target directory.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected String buldDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File theBaseDirectory = new File(buldDirectory);
        File theBytecoderDirectory = new File(theBaseDirectory, "bytecoder");
        theBytecoderDirectory.mkdirs();

        File theBytecoderFileName = new File(theBytecoderDirectory, "bytecoder.js");

        try {
            ClassLoader theLoader = prepareClassLoader();
            Class theTargetClass = theLoader.loadClass(mainClass);

            JSCompileTarget theCompileTarget = new JSCompileTarget(theLoader, JSCompileTarget.BackendType.valueOf(backend));

            BytecodeMethodSignature theSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                    new BytecodeTypeRef[] { new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(TString.class), 1) });

            String theCode = theCompileTarget.compileToJS(theTargetClass, "main", theSignature);
            try (PrintWriter theWriter = new PrintWriter(new FileWriter(theBytecoderFileName))) {
                theWriter.println(theCode);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error running bytecoder", e);
        }
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
                    this.getClass().getClassLoader());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Cannot create classloader", e);
        }
    }

}