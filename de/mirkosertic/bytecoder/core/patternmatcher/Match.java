/*
 * Copyright 2024 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.patternmatcher;

import de.mirkosertic.bytecoder.core.ir.Node;

import java.util.Map;

public class Match {

    private final Node root;
    private final Map<Node, Node> mappings;

    Match(final Node root, final Map<Node, Node> mappings) {
        this.root = root;
        this.mappings = mappings;
    }

    public Node root() {
        return root;
    }

    public <T extends Node> T mappingFor(final T node) {
        return (T) mappings.get(node);
    }
}
