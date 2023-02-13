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
package de.mirkosertic.bytecoder.asm.backend.js;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.backend.sequencer.DominatorTree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.BiPredicate;

import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateClassName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateMethodName;

public class GraphExporter {

    public interface Filter extends BiPredicate<ResolvedClass, ResolvedMethod> {
    }

    public void export(final CompileUnit compileUnit, final Logger logger, final Optimizer optimizer, final Filter filer, final File targetDirectory) throws IOException {
        for (final ResolvedClass cl : compileUnit.computeClassDependencies()) {
            for (final ResolvedMethod rm : cl.resolvedMethods) {
                if (rm.methodBody != null && filer.test(cl, rm)) {

                    logger.info("Exporting method {}.{}{}", cl.type.getClassName(), rm.methodNode.name, rm.methodType);

                    final String className = generateClassName(cl.type);
                    final String methodName = generateMethodName(rm.methodNode.name, rm.methodType);

                    try (final FileOutputStream fos = new FileOutputStream(new File(targetDirectory, className + "." + methodName + "_debug.dot"))) {
                        rm.methodBody.writeDebugTo(fos);
                    }

                    while (optimizer.optimize(rm)) {
                        //
                    }

                    try (final FileOutputStream fos = new FileOutputStream(new File(targetDirectory, className + "." + methodName + "_debug_optimized.dot"))) {
                        rm.methodBody.writeDebugTo(fos);
                    }

                    final DominatorTree dt = new DominatorTree(rm.methodBody);

                    try (final FileOutputStream fos = new FileOutputStream(new File(targetDirectory, className + "." + methodName + "_dominatortree.dot"))) {
                        dt.writeDebugTo(fos);
                    }
                }
            }
        }
    }
}
