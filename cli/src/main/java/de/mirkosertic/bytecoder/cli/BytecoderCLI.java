/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.cli;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class BytecoderCLI {

    private static final Logger LOGGER = LoggerFactory.getLogger(BytecoderCLI.class);

    @Command(name = "Bytecoder")
    public static class CLIOptions {

        @Option(names = "-classpath", required = true, description = "Die Directory containing the JVM class files to be compiled.")
        protected String classpath;

        @Option(names = "-mainclass", required = true, description = "Name of the class that contains the main() method")
        protected String mainClass;

        @Option(names = "-backend", required = true, description = "The compiler backend to use. Can be 'wasm' or 'js'.")
        protected String backend;

        @Option(names = "-builddirectory", required = false, description = "The directory to output the generated code to. Defaults to '.'")
        protected String buldDirectory = ".";

        @Option(names = "-debug", required = false, description = "Shall debugging information be embedded in the code? Results in larger file sizes. Defaults to 'false'.")
        protected boolean debugOutput;

        @Option(names = "-enableexceptions", required = false, description = "Shall exception handling be enabled? Defaults to 'false'.")
        protected boolean enableExceptionHandling;

        @Option(names = "-optimizationlevel", required = false, description = "The optimization level. Can be 'NONE' or 'ALL'. Defaults to 'ALL'.")
        protected String optimizationLevel = "ALL";

        @Option(names = "-filenameprefix", required = false, description = "The prefix for generated files. Defaults to 'bytecoder'")
        protected String filenamePrefix = "bytecoder";

        @Option(names = "-initialwasmpages", required = false, description = "The initial wasm page size. Defaults to '512'.")
        protected int wasmInitialPages = 512;

        @Option(names = "-maximumwasmpages", required = false, description = "The maximum of wasm pages. Defaults to '1024'")
        protected int wasmMaximumPages = 1024;

        @Option(names = "-minify", required = false, description = "Shall the generated code be minified? Defaults to 'true'.")
        protected boolean minifyCompileResult = true;

        @Option(names = "-preferStackifier", required = false, description = "Shall the Stackifier be used in favor to Relooper? Defaults to 'false'.")
        protected boolean preferStackifier = false;

        @Option(names = "-registerallocator", required = false, description = "Which register allocator should be used? Can be linear or passthru. Defaults to 'linear'.")
        protected String registerAllocator = "linear";

    }

    public static void main(final String[] args) throws IOException, ClassNotFoundException {

        final CLIOptions theCLIOptions = new CLIOptions();
        try {
            CommandLine.populateCommand(theCLIOptions, args);
        } catch (final CommandLine.ExecutionException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                LOGGER.error(e.getCause().getMessage());
            }
            CommandLine.usage(theCLIOptions, System.out);
            return;
        } catch (final Exception e) {
            CommandLine.usage(theCLIOptions, System.out);
            return;
        }

        final File theBytecoderDirectory = new File(theCLIOptions.buldDirectory);
        theBytecoderDirectory.mkdirs();

        final ClassLoader theLoader = BytecoderCLI.class.getClassLoader();
        final URLClassLoader classLoader = new URLClassLoader(new URL[] {new File(theCLIOptions.classpath).toURI().toURL()}, theLoader);

        final Class theTargetClass = classLoader.loadClass(theCLIOptions.mainClass);

        final CompileTarget theCompileTarget = new CompileTarget(classLoader, CompileTarget.BackendType.valueOf(theCLIOptions.backend));

        final BytecodeMethodSignature theSignature = new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                new BytecodeTypeRef[] { new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(String.class), 1) });

        final CompileOptions theOptions = new CompileOptions(new Slf4JLogger(), theCLIOptions.debugOutput, KnownOptimizer.valueOf(theCLIOptions.optimizationLevel), theCLIOptions.enableExceptionHandling, theCLIOptions.filenamePrefix, theCLIOptions.wasmInitialPages, theCLIOptions.wasmMaximumPages, theCLIOptions.minifyCompileResult, theCLIOptions.preferStackifier, Allocator.valueOf(theCLIOptions.registerAllocator));
        final CompileResult theCode = theCompileTarget.compile(theOptions, theTargetClass, "main", theSignature);
        for (final CompileResult.Content content : theCode.getContent()) {
            final File theBytecoderFileName = new File(theBytecoderDirectory, content.getFileName());
            try (final FileOutputStream theFos = new FileOutputStream(theBytecoderFileName)) {
                content.writeTo(theFos);
            }
        }
    }
}