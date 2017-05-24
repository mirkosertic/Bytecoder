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

import de.mirkosertic.bytecoder.backend.js.JSBackend;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

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
@Mojo(name = "compile", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class BytecoderMavenMojo extends AbstractMojo {

    /**
     * Classname with the main class to be compiled.
     */
    @Parameter(required = true)
    String mainClass;

    /**
     * The build target directory.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    String buldDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File theBaseDirectory = new File(buldDirectory);
        File theBytecoderDirectory = new File(theBaseDirectory, "bytecoder");
        theBytecoderDirectory.mkdirs();

        File theBytecoderFileName = new File(theBytecoderDirectory, "bytecoder.js");

        JSCompileTarget theCompileTarget = new JSCompileTarget(JSBackend.CodeType.STACK);

        try {
            Class theTargetClass = getClass().getClassLoader().loadClass(mainClass);

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
}