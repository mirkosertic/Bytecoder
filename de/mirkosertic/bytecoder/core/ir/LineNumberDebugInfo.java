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
package de.mirkosertic.bytecoder.core.ir;

public class LineNumberDebugInfo extends ControlTokenConsumer {

    public final String sourceFile;
    public final int lineNumber;

    LineNumberDebugInfo(final Graph owner, final String sourceFile, final int lineNumber) {
        super(owner, NodeType.LineNumberDebugInfo);
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
    }

    @Override
    public String additionalDebugInfo() {
        return ": " + sourceFile + "#" + lineNumber;
    }

    @Override
    public LineNumberDebugInfo stampInto(final Graph target) {
        return target.newLineNumberDebugInfo(sourceFile, lineNumber);
    }

    public void deleteFromControlFlow() {
        owner.deleteFromControlFlowInternally(this);
    }
}
