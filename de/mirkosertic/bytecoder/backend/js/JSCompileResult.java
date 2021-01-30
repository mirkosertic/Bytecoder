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
package de.mirkosertic.bytecoder.backend.js;

import de.mirkosertic.bytecoder.backend.CompileResult;

public class JSCompileResult extends CompileResult<String> {

    private final JSMinifier minifier;

    public JSCompileResult(final JSMinifier aMinifier, final StringContent... content) {
        this.minifier = aMinifier;
        for (final StringContent c : content) {
            add(c);
        }
    }

    public JSMinifier getMinifier() {
        return minifier;
    }
}