/*
 * Copyright 2022 Mirko Sertic
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

public class GraphParserState {

    final Frame frame;

    final ControlTokenConsumerNode lastControlTokenConsumer;

    final Projection projection;

    final int lineNumber;

    public GraphParserState(final Frame frame, final ControlTokenConsumerNode lastControlTokenConsumer, final Projection projection, final int lineNumber) {
        this.frame = frame;
        this.lastControlTokenConsumer = lastControlTokenConsumer;
        this.projection = projection;
        this.lineNumber = lineNumber;
    }

    public GraphParserState controlFlowsTo(final ControlTokenConsumerNode node) {
        if (lastControlTokenConsumer != null) {
            lastControlTokenConsumer.addControlFlowTo(projection, node);
        }
        return new GraphParserState(frame, node, StandardProjections.DEFAULT, lineNumber);
    }

    public GraphParserState projection(final Projection newProjection) {
        return new GraphParserState(frame, lastControlTokenConsumer, newProjection, lineNumber);
    }

    public GraphParserState withLineNumber(final int line) {
        return new GraphParserState(frame, lastControlTokenConsumer, projection, line);
    }

    public GraphParserState withFrame(final Frame frame) {
        return new GraphParserState(frame, lastControlTokenConsumer, projection, lineNumber);
    }
}
