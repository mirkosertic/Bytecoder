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

import de.mirkosertic.bytecoder.allocator.Allocator;
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
    private final boolean preferStackifier;
    private final Allocator allocator;
    private final String[] additionalClassesToLink;
    private final String[] additionalResources;
    private final LLVMOptimizationLevel llvmOptimizationLevel;
    private final boolean escapeAnalysisEnabled;

    public CompileOptions(final Logger aLogger, final boolean aDebugOutput, final Optimizer aOptimizer, final boolean aEnableExceptions,
                          final String aFilenamePrefix, final int aWasmMinimumPageSize, final int aWasmMaximumPageSize,
                          final boolean aMinify,
                          final boolean aPreferStackifier,
                          final Allocator aAllocator,
                          final String[] aAdditionalClassesToLink,
                          final String[] aAdditionalResources,
                          final LLVMOptimizationLevel aLlvmOptimizationLevel,
                          final boolean aEscapeAnalysisEnabled) {
        logger = aLogger;
        debugOutput = aDebugOutput;
        optimizer = aOptimizer;
        enableExceptions = aEnableExceptions;
        filenamePrefix = aFilenamePrefix;
        wasmMinimumPageSize = aWasmMinimumPageSize;
        wasmMaximumPageSize = aWasmMaximumPageSize;
        minify = aMinify;
        preferStackifier = aPreferStackifier;
        allocator = aAllocator;
        additionalClassesToLink = aAdditionalClassesToLink;
        additionalResources = aAdditionalResources;
        llvmOptimizationLevel = aLlvmOptimizationLevel;
        escapeAnalysisEnabled = aEscapeAnalysisEnabled;
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

    public boolean isPreferStackifier() {
        return preferStackifier;
    }

    public Allocator getAllocator() {
        return allocator;
    }

    public String[] getAdditionalClassesToLink() {
        return additionalClassesToLink;
    }

    public String[] getAdditionalResources() {
        return additionalResources;
    }

    public LLVMOptimizationLevel getLlvmOptimizationLevel() {
        return llvmOptimizationLevel;
    }

    public boolean isEscapeAnalysisEnabled() {
        return escapeAnalysisEnabled;
    }
}
