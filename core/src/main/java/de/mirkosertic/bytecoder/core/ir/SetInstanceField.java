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

public class SetInstanceField extends ControlTokenConsumer {

    public final ResolvedField field;

    SetInstanceField(final Graph owner, final ResolvedField field) {
        super(owner, NodeType.SetInstanceField);
        this.field = field;
    }

    @Override
    public String additionalDebugInfo() {
        return ": " + field.name;
    }

    @Override
    public boolean hasSideSideEffect() {
        return true;
    }

    @Override
    public SetInstanceField stampInto(final Graph target) {
        return target.newSetInstanceField(field);
    }

    @Override
    public void sanityCheck() {
        super.sanityCheck();
        if (incomingDataFlows.length != 1) {
            throw new IllegalStateException("Invalid number of incoming data flows : " + incomingDataFlows.length);
        }
        final Node[] target = outgoingDataFlows();
        if (target.length != 1) {
            throw new IllegalStateException("Invalid number of targets : " + target.length);
        }
    }
}
