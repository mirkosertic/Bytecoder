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
package de.mirkosertic.bytecoder.core.test;

import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.optimizer.Optimizer;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

public class FocusOptimizer implements Optimizer {

    private final ResolvedClass focus;
    private final Optimizer optimizer;

    public FocusOptimizer(final ResolvedClass focus, final Optimizer optimizer) {
        this.focus = focus;
        this.optimizer = optimizer;
    }

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {
        if (method.owner == focus) {
            return optimizer.optimize(backendType, compileUnit, method);
        }
        return false;
    }
}
