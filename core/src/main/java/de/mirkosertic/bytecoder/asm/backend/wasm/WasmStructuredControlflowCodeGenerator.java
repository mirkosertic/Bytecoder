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
package de.mirkosertic.bytecoder.asm.backend.wasm;

import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmType;
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
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import de.mirkosertic.bytecoder.asm.sequencer.StructuredControlflowCodeGenerator;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Function;

public class WasmStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    private final ExportableFunction exportableFunction;

    private final Function<Type, WasmType> typeConverter;

    public WasmStructuredControlflowCodeGenerator(final ExportableFunction exportableFunction, final Function<Type, WasmType> typeConverter) {
        this.exportableFunction = exportableFunction;
        this.typeConverter = typeConverter;
    }

    @Override
    public void registerVariables(final List<AbstractVar> variables) {
    }

    @Override
    public void write(final InstanceMethodInvocation node) {

    }

    @Override
    public void write(final VirtualMethodInvocation node) {

    }

    @Override
    public void write(final StaticMethodInvocation node) {

    }

    @Override
    public void write(final InterfaceMethodInvocation node) {

    }

    @Override
    public void write(final Copy node) {

    }

    @Override
    public void writeIfAndStartTrueBlock(final If node) {

    }

    @Override
    public void startIfElseBlock(final If node) {

    }

    @Override
    public void finishBlock() {

    }

    @Override
    public void startBlock(final Sequencer.Block node) {

    }

    @Override
    public void write(final LineNumberDebugInfo node) {

    }

    @Override
    public void write(final FrameDebugInfo node) {

    }

    @Override
    public void write(final Goto node) {

    }

    @Override
    public void write(final MonitorEnter node) {

    }

    @Override
    public void write(final MonitorExit node) {

    }

    @Override
    public void write(final Unwind node) {

    }

    @Override
    public void write(final Return node) {

    }

    @Override
    public void write(final ReturnValue node) {

    }

    @Override
    public void write(final SetInstanceField node) {

    }

    @Override
    public void write(final SetClassField node) {

    }

    @Override
    public void write(final ArrayStore node) {

    }

    @Override
    public void write(final CheckCast node) {

    }

    @Override
    public void writeBreakTo(final String label) {

    }

    @Override
    public void writeContinueTo(final String label) {

    }

    @Override
    public void startTryCatch(final String label) {

    }

    @Override
    public void startCatchBlock() {

    }

    @Override
    public void startCatchHandler(final Type type) {

    }

    @Override
    public void endCatchHandler() {

    }

    @Override
    public void writeRethrowException() {

    }

    @Override
    public void startFinallyBlock() {

    }

    @Override
    public void writeSwitch(final TableSwitch node) {

    }

    @Override
    public void startTableSwitchDefaultBlock() {

    }

    @Override
    public void writeSwitch(final LookupSwitch node) {

    }

    @Override
    public void writeSwitchCase(final int index) {

    }

    @Override
    public void writeSwitchDefaultCase() {

    }

    @Override
    public void finishSwitchCase() {

    }

    @Override
    public void writeDebugNote(final String message) {
    }
}
