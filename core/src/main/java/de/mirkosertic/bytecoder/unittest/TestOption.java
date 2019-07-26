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
package de.mirkosertic.bytecoder.unittest;

import de.mirkosertic.bytecoder.backend.CompileTarget;

public class TestOption {

    private final CompileTarget.BackendType backendType;
    private final boolean preferStackifier;
    private final boolean exceptionsEnabled;
    private final boolean minify;

    public TestOption(final CompileTarget.BackendType backendType, final boolean preferStackifier, final boolean exceptionsEnabled, final boolean minify) {
        this.backendType = backendType;
        this.preferStackifier = preferStackifier;
        this.exceptionsEnabled = exceptionsEnabled;
        this.minify = minify;
    }

    public String toDescription() {
        return "backend=" +
                backendType.toString() +
                " preferStackifier=" +
                preferStackifier +
                " minify=" +
                minify +
                " exceptionsEnabled=" +
                exceptionsEnabled;
    }

    public String toFilePrefix() {
        final StringBuilder builder = new StringBuilder();
        builder.append(backendType.toString());
        if (preferStackifier) {
            builder.append("_stackifier");
        }
        if (minify) {
            builder.append("_minify");
        }
        if (exceptionsEnabled) {
            builder.append("_exceptionsEnabled");
        }
        return builder.toString();
    }

    public CompileTarget.BackendType getBackendType() {
        return backendType;
    }

    public boolean isPreferStackifier() {
        return preferStackifier;
    }

    public boolean isExceptionsEnabled() {
        return exceptionsEnabled;
    }

    public boolean isMinify() {
        return minify;
    }
}
