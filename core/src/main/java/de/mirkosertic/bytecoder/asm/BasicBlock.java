/*
 * Copyright 2021 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BasicBlock {

    enum EdgeType {
        UNKNOWN, FORWARD, BACKWARD
    }

    enum Tag {
        EXCEPTION_HANDLER, FINALLY_BLOCK
    }

    final AbstractInsnNode start;
    final Map<BasicBlock, EdgeType> successors;
    final Set<BasicBlock> preds;
    final Set<Tag> tags;

    public BasicBlock(final AbstractInsnNode start) {
        this.start = start;
        this.successors = new HashMap<>();
        this.preds = new HashSet<>();
        this.tags = new HashSet<>();
    }

    public void tag(final Tag tag) {
        tags.add(tag);
    }

    public void addSuccessor(final BasicBlock successor) {
        if (!successors.containsKey(successor)) {
            successors.put(successor, EdgeType.UNKNOWN);
        }
        successor.preds.add(this);
    }
}
