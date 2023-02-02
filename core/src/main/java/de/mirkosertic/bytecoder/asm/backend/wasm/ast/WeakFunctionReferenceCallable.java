/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.backend.wasm.ast;

public class WeakFunctionReferenceCallable implements Callable {

    private final String label;

    WeakFunctionReferenceCallable(final String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public PrimitiveType resolveResultType(final WasmValue.ExportContext context) {
        final Function f = context.functionIndex().firstByLabel(label);
        return f.getResultType();
    }

    @Override
    public int resolveIndex(final WasmValue.ExportContext context) {
        final Function f = context.functionIndex().firstByLabel(label);
        return context.functionIndex().indexOf(f);
    }
}
