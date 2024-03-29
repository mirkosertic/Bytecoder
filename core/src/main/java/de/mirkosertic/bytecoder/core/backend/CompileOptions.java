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
package de.mirkosertic.bytecoder.core.backend;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.optimizer.Optimizer;

public class CompileOptions {

    private final Logger logger;

    private final Optimizer optimizer;

    private final String[] additionalResources;

    private final String filenamePrefix;

    private final boolean debugOutput;

    public CompileOptions(final Logger logger, final Optimizer optimizer, final String[] additionalResources, final String filenamePrefix, final boolean debugOutput) {
        this.logger = logger;
        this.optimizer = optimizer;
        this.additionalResources = additionalResources;
        this.filenamePrefix = filenamePrefix;
        this.debugOutput = debugOutput;
    }

    public Logger getLogger() {
        return logger;
    }

    public Optimizer getOptimizer() {
        return optimizer;
    }

    public String[] getAdditionalResources() {
        return additionalResources;
    }

    public String getFilenamePrefix() {
        return filenamePrefix;
    }

    public boolean isDebugOutput() {
        return debugOutput;
    }
}
