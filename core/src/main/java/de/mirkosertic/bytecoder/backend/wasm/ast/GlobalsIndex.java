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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GlobalsIndex {

    private final List<Global> globals;

    GlobalsIndex() {
        globals = new ArrayList<>();
    }

    public void add(final Global global) {
        globals.add(global);
    }

    public Global globalByLabel(final String globalName) {
        for (final Global g : globals) {
            if (Objects.equals(globalName, g.getLabel())) {
                return g;
            }
        }
        throw new IllegalArgumentException("No such global : " + globalName);
    }

    public int size() {
        return globals.size();
    }

    public Global get(final int i) {
        return globals.get(i);
    }

    public int indexOf(final Global global) {
        return globals.indexOf(global);
    }
}