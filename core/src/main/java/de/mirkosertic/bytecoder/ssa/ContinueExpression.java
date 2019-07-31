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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class ContinueExpression extends Expression {

    private final Label labelToReturnTo;
    private final BytecodeOpcodeAddress jumpTarget;
    private boolean jumpLabelRequired;

    public ContinueExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final Label aLabelToReturnTo, final BytecodeOpcodeAddress aJumpTarget) {
        super(aProgram, aAddress);
        labelToReturnTo = aLabelToReturnTo;
        jumpTarget = aJumpTarget;
        jumpLabelRequired = true;
    }

    public Label labelToReturnTo() {
        return labelToReturnTo;
    }

    public boolean isJumpLabelRequired() {
        return jumpLabelRequired;
    }

    public void noJumpLabelRequired() {
        jumpLabelRequired = false;
    }

    public BytecodeOpcodeAddress jumpTarget() {
        return jumpTarget;
    }
}
