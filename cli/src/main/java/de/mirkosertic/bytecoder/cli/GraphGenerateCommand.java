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
import de.mirkosertic.bytecoder.core.backend.js.GraphExporter;
import de.mirkosertic.bytecoder.core.backend.js.JSIntrinsics;
import de.mirkosertic.bytecoder.core.ir.AnalysisException;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.Loader;
import org.objectweb.asm.Type;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static de.mirkosertic.bytecoder.cli.BytecoderCommand.parseClasspath;
import static picocli.CommandLine.*;

@Command(name = "generate")
public class GraphGenerateCommand implements Callable<Integer> {

    @ParentCommand
    GraphCommand parent;

    @Option(names = "-classpath", required = true, description = "The classpath containing the JVM class files and JARs to be compiled. Format: /path/to/classes,/path/to/some.jar.")
    protected String classpath;

    @Option(names = "-mainclass", required = true, description = "Name of the class that contains the main() method")
    protected String mainClass;

    @Option(names = "-builddirectory", required = false, description = "The directory to output the generated code to. Defaults to '.'")
    protected String buildDirectory = ".";

    @Option(names = "-optimizationlevel", required = false, description = "The optimization level. Can be 'NONE', 'DEFAULT' or 'ALL'. Defaults to 'DEFAULT'.")
    protected String optimizationLevel = "DEFAULT";

    @Option(names = "-matchpattern", required = false, description = "Regex to match the full qualified class and method names for the export. Defaults to all methods of the main class.")
    protected String matchPattern;

    @Override
    public Integer call() throws Exception {

        try {
            final ClassLoader rootClassLoader = BytecoderCLI.class.getClassLoader();
            final URLClassLoader classLoader = new URLClassLoader(parseClasspath(classpath), rootClassLoader);

            final Loader loader = new BytecoderLoader(classLoader);

            final Logger logger = new Slf4JLogger();

            logger.info("Compiling main class {} to directory {}", mainClass, buildDirectory);

            final CompileUnit compileUnit = new CompileUnit(loader, logger, new JSIntrinsics());
            final Type invokedType = Type.getObjectType(mainClass.replace('.','/'));

            compileUnit.resolveMainMethod(invokedType, "main", Type.getMethodType(Type.VOID_TYPE, Type.getType("[Ljava/lang/String;")));

            compileUnit.finalizeLinkingHierarchy();

            compileUnit.logStatistics();

            final Pattern p;
            if (matchPattern != null) {
                p = Pattern.compile(matchPattern);
            } else {
                p = Pattern.compile(mainClass + ".*");
            }

            final GraphExporter.Filter filter = (resolvedClass, resolvedMethod) -> {
                final String fq = resolvedClass.type.getClassName() + "." + resolvedMethod.methodNode.name;
                return p.matcher(fq).matches();
            };

            final GraphExporter exporter = new GraphExporter();
            exporter.export(compileUnit, logger, Optimizations.valueOf(optimizationLevel), filter, new File(buildDirectory));

            return 0;

        } catch (final AnalysisException e) {
            e.getAnalysisStack().dumpAnalysisStack(System.out);
            throw e;
        }
    }
}
