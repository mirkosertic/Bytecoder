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
package de.mirkosertic.bytecoder.core.backend.sequencer;

import de.mirkosertic.bytecoder.core.ir.AbstractVar;
import de.mirkosertic.bytecoder.core.ir.ArrayStore;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.core.ir.Goto;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.core.ir.LookupSwitch;
import de.mirkosertic.bytecoder.core.ir.MethodInvocation;
import de.mirkosertic.bytecoder.core.ir.MonitorEnter;
import de.mirkosertic.bytecoder.core.ir.MonitorExit;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.ReturnValue;
import de.mirkosertic.bytecoder.core.ir.SetClassField;
import de.mirkosertic.bytecoder.core.ir.SetInstanceField;
import de.mirkosertic.bytecoder.core.ir.TableSwitch;
import de.mirkosertic.bytecoder.core.ir.Unwind;
import org.objectweb.asm.Type;

import java.util.List;

public interface StructuredControlflowCodeGenerator {

    void registerVariables(List<AbstractVar> variables);

    void write(MethodInvocation node);

    void write(Copy node);

    void startIfWithTrueBlock(If node);

    void startIfElseBlock(If node);

    void finishIfBlock();

    void startBlock(Sequencer.Block node);

    void finishBlock(Sequencer.Block node, boolean emptyStack);

    void write(LineNumberDebugInfo node);

    void write(FrameDebugInfo node);

    void write(Goto node);

    void write(MonitorEnter node);

    void write(MonitorExit node);

    void write(Unwind node);

    void write(Return node);

    void write(ReturnValue node);

    void write(SetInstanceField node);

    void write(SetClassField node);

    void write(ArrayStore node);

    void writeBreakTo(String label);

    void writeContinueTo(String label);

    void startTryCatch(String label);

    void startCatchBlock();

    void startCatchHandler(Type type);

    void finishCatchHandler();

    void writeRethrowException();

    void finishTryCatch();

    void startTableSwitch(final TableSwitch node);

    void finishLookupSwitch();

    void finishTableSwitch();

    void startTableSwitchDefaultBlock();

    void finishTableSwitchDefaultBlock();

    void startLookupSwitch(final LookupSwitch node);

    void writeSwitchCase(int index);

    void writeSwitchDefaultCase();

    void finishSwitchDefault();

    void finishSwitchCase();
}
