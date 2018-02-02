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
package de.mirkosertic.bytecoder.backend;

import de.mirkosertic.bytecoder.core.Logger;
import de.mirkosertic.bytecoder.ssa.optimizer.Optimizer;

public class CompileOptions {

    private final Logger logger;
    private final boolean debugOutput;
    private final Optimizer optimizer;

    public CompileOptions(Logger aLogger, boolean aDebugOutput, Optimizer aOptimizer) {
        logger = aLogger;
        debugOutput = aDebugOutput;
        optimizer = aOptimizer;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isDebugOutput() {
        return debugOutput;
    }

    public Optimizer getOptimizer() {
        return optimizer;
    }
}
