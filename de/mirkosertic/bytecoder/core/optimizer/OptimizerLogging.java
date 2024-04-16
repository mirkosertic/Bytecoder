/*
 * Copyright 2024 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OptimizerLogging {

    final List<File> graphSteps = new ArrayList<>();
    final List<File> dominatorTreeSteps = new ArrayList<>();

    private final ResolvedMethod rm;

    public OptimizerLogging(final ResolvedMethod rm) {
        this.rm = rm;
        logStep(0);
    }

    public void logStep(final int step) {
        if ("true".equalsIgnoreCase(System.getProperty("bytecoder.logoptimizations", "false"))) {
            try {
                final String prefix = rm.owner.type.getClassName() + "_" + rm.methodNode.name.replace("<", "$").replace(">", "$") + "_";

                final File generatedFilesDir = new File(".");
                final String filenameDg = prefix + "optimization_debuggraph_" + step + ".dot";
                final File unoptdbf = new File(generatedFilesDir, filenameDg);
                try (final FileOutputStream fos = new FileOutputStream(unoptdbf)) {
                    rm.methodBody.writeDebugTo(fos);
                }
                graphSteps.add(unoptdbf);

                final String filenameDt = prefix + "optimiztation_dominatortree_" + step + ".dot";
                final File unoptdtf = new File(generatedFilesDir, filenameDt);
                try (final FileOutputStream fos = new FileOutputStream(unoptdtf)) {
                    final DominatorTree dt = new DominatorTree(rm.methodBody);
                    dt.writeDebugTo(fos);
                }
                dominatorTreeSteps.add(unoptdtf);
            } catch (final IOException e) {
                throw new RuntimeException("Cannot write debug data", e);
            }
        }
    }
    public void finished() {
        //if (!rm.owner.type.getClassName().contains("TailcallVarargs") && (!"eval".equals(rm.methodNode.name))) {
            graphSteps.stream().forEach(File::delete);
            dominatorTreeSteps.stream().forEach(File::delete);
        //}
    }
}
