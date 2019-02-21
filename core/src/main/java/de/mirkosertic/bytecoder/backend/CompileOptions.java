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

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.optimizer.Optimizer;

public class CompileOptions {

    private final Logger logger;
    private final boolean debugOutput;
    private final Optimizer optimizer;
    private final boolean enableExceptions;
    private final String filenamePrefix;
    private final int wasmMinimumPageSize;
    private final int wasmMaximumPageSize;
    private final boolean minify;

    public CompileOptions(final Logger aLogger, final boolean aDebugOutput, final Optimizer aOptimizer, final boolean aEnableExceptions,
                          final String aFilenamePrefix, final int aWasmMinimumPageSize, final int aWasmMaximumPageSize,
                          final boolean aMinify) {
        logger = aLogger;
        debugOutput = aDebugOutput;
        optimizer = aOptimizer;
        enableExceptions = aEnableExceptions;
        filenamePrefix = aFilenamePrefix;
        wasmMinimumPageSize = aWasmMinimumPageSize;
        wasmMaximumPageSize = aWasmMaximumPageSize;
        minify = aMinify;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isDebugOutput() {
        return debugOutput && (!minify);
    }

    public Optimizer getOptimizer() {
        return optimizer;
    }

    public boolean isEnableExceptions() {
        return enableExceptions;
    }

    public String getFilenamePrefix() {
        return filenamePrefix;
    }

    public int getWasmMinimumPageSize() {
        return wasmMinimumPageSize;
    }

    public int getWasmMaximumPageSize() {
        return wasmMaximumPageSize;
    }

    public boolean isMinify() {
        return minify;
    }
}
