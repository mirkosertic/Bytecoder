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
package de.mirkosertic.bytecoder.asm.backend.sequencer;

import de.mirkosertic.bytecoder.asm.ir.AbstractVar;
import de.mirkosertic.bytecoder.asm.ir.ArrayStore;
import de.mirkosertic.bytecoder.asm.ir.CheckCast;
import de.mirkosertic.bytecoder.asm.ir.Copy;
import de.mirkosertic.bytecoder.asm.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.asm.ir.Goto;
import de.mirkosertic.bytecoder.asm.ir.If;
import de.mirkosertic.bytecoder.asm.ir.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.InterfaceMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.asm.ir.LookupSwitch;
import de.mirkosertic.bytecoder.asm.ir.MonitorEnter;
import de.mirkosertic.bytecoder.asm.ir.MonitorExit;
import de.mirkosertic.bytecoder.asm.ir.Return;
import de.mirkosertic.bytecoder.asm.ir.ReturnValue;
import de.mirkosertic.bytecoder.asm.ir.SetClassField;
import de.mirkosertic.bytecoder.asm.ir.SetInstanceField;
import de.mirkosertic.bytecoder.asm.ir.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.TableSwitch;
import de.mirkosertic.bytecoder.asm.ir.Unwind;
import de.mirkosertic.bytecoder.asm.ir.VirtualMethodInvocation;
import org.objectweb.asm.Type;

import java.util.List;

public interface StructuredControlflowCodeGenerator {

    void registerVariables(List<AbstractVar> variables);

    void write(InstanceMethodInvocation node);

    void write(VirtualMethodInvocation node);

    void write(StaticMethodInvocation node);

    void write(InterfaceMethodInvocation node);

    void write(Copy node);

    void writeIfAndStartTrueBlock(If node);

    void startIfElseBlock(If node);

    void finishBlock();

    void startBlock(Sequencer.Block node);

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

    void write(CheckCast node);

    void writeBreakTo(String label);

    void writeContinueTo(String label);

    void startTryCatch(String label);

    void startCatchBlock();

    void startCatchHandler(Type type);

    void endCatchHandler();

    void writeRethrowException();

    void startFinallyBlock();

    void writeSwitch(final TableSwitch node);

    void startTableSwitchDefaultBlock();

    void writeSwitch(final LookupSwitch node);

    void writeSwitchCase(int index);

    void writeSwitchDefaultCase();

    void finishSwitchCase();

    void writeDebugNote(String message);

}
