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
import java.util.stream.Collectors;

public class LocalIndex {

    private final List<Local> locals;

    public LocalIndex() {
        locals = new ArrayList<>();
    }

    public LocalIndex(final List<Param> params) {
        locals = new ArrayList<>(params);
    }

    public void add(final Local local) {
        locals.add(local);
    }

    public List<Local> localsExcludingParams() {
        return locals.stream().filter(t -> !(t instanceof Param)).collect(Collectors.toList());
    }

    public Local localByLabel(final String name) {
        for (final Local local : locals) {
            if (Objects.equals(name, local.getLabel())) {
                return local;
            }
        }
        return null;
    }

    public int indexOf(final Local local) {
        return locals.indexOf(local);
    }
}
