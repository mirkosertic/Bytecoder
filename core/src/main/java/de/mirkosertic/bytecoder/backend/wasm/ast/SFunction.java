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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import java.util.List;

public class SFunction extends SExpression implements SImportable {

    public SFunction(final SLabel label, final List<SParam> params, final SResult result) {
        super("func");
        addChildInternal(label);
        for (final SParam param : params) {
            addChildInternal(param);
        }
        addChildInternal(result);
    }

    public SFunction(final SLabel label, final List<SParam> params) {
        super("func");
        addChildInternal(label);
        for (final SParam param : params) {
            addChildInternal(param);
        }
    }

    public SFunction(final SLabel label, final SResult result) {
        super("func");
        addChildInternal(label);
        addChildInternal(result);
    }

    public void addChild(final SExpression expression) {
        addChildInternal(expression);
    }
}
