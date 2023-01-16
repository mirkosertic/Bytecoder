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
package de.mirkosertic.bytecoder.asm.parser;

import de.mirkosertic.bytecoder.asm.StringConstant;

import java.util.ArrayList;
import java.util.List;

public class ConstantPool {

    private final List<String> pooledStrings;

    public ConstantPool() {
        pooledStrings = new ArrayList<>();
    }

    public StringConstant resolveFromPool(final String constant) {
        final int p = pooledStrings.indexOf(constant);
        if (p >= 0) {
            return new StringConstant(p);
        }
        pooledStrings.add(constant);
        return new StringConstant(pooledStrings.size() - 1);
    }

    public List<String> getPooledStrings() {
        return pooledStrings;
    }
}
