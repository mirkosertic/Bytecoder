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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class BreakExpression extends Expression {

    private final Label blockToBreak;
    private final BytecodeOpcodeAddress jumpTarget;
    private boolean silent;
    private boolean setLabelRequired;

    public BreakExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final Label aBlockToBreak, final BytecodeOpcodeAddress aJumpTarget) {
        super(aProgram, aAddress);
        blockToBreak = aBlockToBreak;
        jumpTarget = aJumpTarget;
        silent = false;
        setLabelRequired = true;
    }

    public void noSetRequired() {
        setLabelRequired = false;
    }

    public void silent() {
        silent = true;
    }

    public boolean isSetLabelRequired() {
        return setLabelRequired;
    }

    public boolean isSilent() {
        return silent;
    }

    public Label blockToBreak() {
        return blockToBreak;
    }

    public BytecodeOpcodeAddress jumpTarget() {
        return jumpTarget;
    }

    @Override
    public boolean isTrulyFunctional() {
        return false;
    }
}
