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
package de.mirkosertic.bytecoder.cli;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.backend.CompileResult;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmBackend;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmCompileResult;
import de.mirkosertic.bytecoder.core.backend.wasm.WasmIntrinsics;
import de.mirkosertic.bytecoder.core.ir.AnalysisException;
import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.Loader;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

import static de.mirkosertic.bytecoder.cli.BytecoderCommand.parseClasspath;
import static picocli.CommandLine.*;

@Command(name = "wasm")
public class CompileWasmCommand implements Callable<Integer> {

    @ParentCommand
    CompileCommand parent;

    @Option(names = "-classpath", required = true, description = "The classpath containing the JVM class files and JARs to be compiled. Format: /path/to/classes,/path/to/some.jar.")
    protected String classpath;

    @Option(names = "-mainclass", required = true, description = "Name of the class that contains the main() method")
    protected String mainClass;

    @Option(names = "-builddirectory", required = false, description = "The directory to output the generated code to. Defaults to '.'")
    protected String buildDirectory = ".";

    @Option(names = "-optimizationlevel", required = false, description = "The optimization level. Can be 'NONE', 'DEFAULT' or 'ALL'. Defaults to 'DEFAULT'.")
    protected String optimizationLevel = "DEFAULT";

    @Option(names = "-additionalClassesToLink", required = false, description = "List of full qualified class names to be linked beside the statically referenced ones.")
    protected String[] additionalClassesToLink = new String[0];

    @Option(names = "-additionalResources", required = false, description = "A list of classpath resources to be included into the build.")
    protected String[] additionalResources = new String[0];

    @Option(names = "-filenameprefix", required = false, description = "The prefix for generated files. Defaults to 'bytecoder'")
    protected String filenamePrefix = "bytecoder";

    @Option(names = "-debugoutput", required = false, description = "Shall debug information be included in the output? Defaults to 'false'")
    protected boolean debugoutput = false;

    @Override
    public Integer call() throws Exception {

        try {
            final ClassLoader rootClassLoader = BytecoderCLI.class.getClassLoader();
            final URLClassLoader classLoader = new URLClassLoader(parseClasspath(classpath), rootClassLoader);

            final Loader loader = new BytecoderLoader(classLoader);

            final Logger logger = new Slf4JLogger();

            logger.info("Compiling main class {} to directory {}", mainClass, buildDirectory);

            final CompileUnit compileUnit = new CompileUnit(loader, logger, new WasmIntrinsics());
            final Type invokedType = Type.getObjectType(mainClass.replace('.', '/'));

            compileUnit.resolveMainMethod(invokedType, "main", Type.getMethodType(Type.VOID_TYPE, Type.getType("[Ljava/lang/String;")));

            for (final String className : additionalClassesToLink) {
                compileUnit.resolveClass(Type.getObjectType(className.replace('.', '/')), new AnalysisStack());
            }

            compileUnit.finalizeLinkingHierarchy();

            compileUnit.logStatistics();

            final CompileOptions compileOptions =
                    new CompileOptions(logger, Optimizations.valueOf(optimizationLevel), additionalResources, filenamePrefix, debugoutput);

            final WasmBackend backend = new WasmBackend();
            final WasmCompileResult result = backend.generateCodeFor(compileUnit, compileOptions);

            for (final CompileResult.Content content : result.getContent()) {
                final File outputFile = new File(buildDirectory, content.getFileName());
                try (final FileOutputStream theFos = new FileOutputStream(outputFile)) {
                    content.writeTo(theFos);
                }
            }

            return 0;
        } catch (final AnalysisException e) {
            e.getAnalysisStack().dumpAnalysisStack(System.out);
            throw e;
        }
    }
}
