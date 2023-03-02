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
import de.mirkosertic.bytecoder.asm.backend.CompileOptions;
import de.mirkosertic.bytecoder.asm.backend.js.JSBackend;
import de.mirkosertic.bytecoder.asm.backend.js.JSCompileResult;
import de.mirkosertic.bytecoder.asm.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.asm.ir.AnalysisException;
import de.mirkosertic.bytecoder.asm.ir.AnalysisStack;
import de.mirkosertic.bytecoder.asm.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.Loader;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "js")
public class CompileJSCommand implements Callable<Integer> {

    @ParentCommand
    CompileCommand parent;

    @Option(names = "-classpath", required = true, description = "Die Directory containing the JVM class files to be compiled.")
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

    @Override
    public Integer call() throws Exception {

        try {
            final ClassLoader rootClassLoader = BytecoderCLI.class.getClassLoader();
            final URLClassLoader classLoader = new URLClassLoader(new URL[] {new File(classpath).toURI().toURL()}, rootClassLoader);

            final Loader loader = new BytecoderLoader(classLoader);

            final Logger logger = new Slf4JLogger();

            logger.info("Compiling main class {} to directory {}", mainClass, buildDirectory);

            final CompileUnit compileUnit = new CompileUnit(loader, logger, new JSIntrinsics());
            final Type invokedType = Type.getObjectType(mainClass.replace('.','/'));

            compileUnit.resolveMainMethod(invokedType, "main", Type.getMethodType(Type.VOID_TYPE, Type.getType("[Ljava/lang/String;")));

            for (final String className : additionalClassesToLink) {
                compileUnit.resolveClass(Type.getObjectType(className.replace('.', '/')), new AnalysisStack());
            }

            compileUnit.finalizeLinkingHierarchy();

            compileUnit.logStatistics();

            final CompileOptions compileOptions =
                    new CompileOptions(logger, Optimizations.valueOf(optimizationLevel), additionalResources, filenamePrefix, false);

            final JSBackend backend = new JSBackend();
            final JSCompileResult result = backend.generateCodeFor(compileUnit, compileOptions);

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