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
package de.mirkosertic.bytecoder.asm.optimizer;

import de.mirkosertic.bytecoder.asm.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.ir.Projection;

import java.util.List;

public class NodeContext {

    public static class ControlFlowEdge {

        public final Projection projection;
        public final ControlTokenConsumer controlTokenConsumer;

        public ControlFlowEdge(final Projection projection, final ControlTokenConsumer controlTokenConsumer) {
            this.projection = projection;
            this.controlTokenConsumer = controlTokenConsumer;
        }
    }

    public List<ControlFlowEdge> predsToSucc;
    public List<ControlFlowEdge> nodeToSucc;
}
