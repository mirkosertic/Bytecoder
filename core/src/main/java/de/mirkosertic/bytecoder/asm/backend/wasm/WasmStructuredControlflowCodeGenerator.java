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

import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Callable;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Container;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.HostType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.RefType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.StructType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmType;
import de.mirkosertic.bytecoder.asm.backend.wasm.ast.WasmValue;
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
import de.mirkosertic.bytecoder.asm.ir.MethodArgument;
import de.mirkosertic.bytecoder.asm.ir.MonitorEnter;
import de.mirkosertic.bytecoder.asm.ir.MonitorExit;
import de.mirkosertic.bytecoder.asm.ir.New;
import de.mirkosertic.bytecoder.asm.ir.Node;
import de.mirkosertic.bytecoder.asm.ir.NullReference;
import de.mirkosertic.bytecoder.asm.ir.ObjectString;
import de.mirkosertic.bytecoder.asm.ir.PHI;
import de.mirkosertic.bytecoder.asm.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.asm.ir.ReadInstanceField;
import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.ir.Return;
import de.mirkosertic.bytecoder.asm.ir.ReturnValue;
import de.mirkosertic.bytecoder.asm.ir.SetClassField;
import de.mirkosertic.bytecoder.asm.ir.SetInstanceField;
import de.mirkosertic.bytecoder.asm.ir.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.ir.StaticMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.ir.TableSwitch;
import de.mirkosertic.bytecoder.asm.ir.This;
import de.mirkosertic.bytecoder.asm.ir.TypeReference;
import de.mirkosertic.bytecoder.asm.ir.Unwind;
import de.mirkosertic.bytecoder.asm.ir.Value;
import de.mirkosertic.bytecoder.asm.ir.VirtualMethodInvocation;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import de.mirkosertic.bytecoder.asm.sequencer.StructuredControlflowCodeGenerator;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class WasmStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    private final CompileUnit compileUnit;

    private final Module module;

    private final Map<ResolvedClass, StructType> objectTypeMappings;

    private final ExportableFunction exportableFunction;

    private final Function<Type, WasmType> typeConverter;

    private final Map<AbstractVar, Local> varLocalMap;

    private final Container targetContainer;

    public WasmStructuredControlflowCodeGenerator(final CompileUnit compileUnit, final Module module, final Map<ResolvedClass, StructType> objectTypeMappings, final ExportableFunction exportableFunction, final Function<Type, WasmType> typeConverter) {
        this.compileUnit = compileUnit;
        this.module = module;
        this.exportableFunction = exportableFunction;
        this.objectTypeMappings = objectTypeMappings;
        this.typeConverter = typeConverter;
        this.varLocalMap = new HashMap<>();
        this.targetContainer = exportableFunction;
    }

    @Override
    public void registerVariables(final List<AbstractVar> variables) {
        for (int i = 0; i < variables.size(); i++) {
            final AbstractVar v = variables.get(i);
            final String varName;
            if (v instanceof PHI) {
                varName = "phi" + i;
            } else {
                varName = "var" + i;
            }
            final WasmType type = typeConverter.apply(v.type);
            final Local local = exportableFunction.newLocal(varName, type);

            varLocalMap.put(v, local);
        }
    }

    @Override
    public void write(final InstanceMethodInvocation node) {
        targetContainer.flow.comment("InstanceMethodInvocation");
    }

    @Override
    public void write(final VirtualMethodInvocation node) {
        targetContainer.flow.comment("VirtualMethodInvocation");
    }

    @Override
    public void write(final StaticMethodInvocation node) {
        targetContainer.flow.comment("StaticMethodInvocation");
    }

    @Override
    public void write(final InterfaceMethodInvocation node) {
        targetContainer.flow.comment("InterfaceMethodInvocation");
    }

    private WasmValue toWasmValue(final This thisRef) {
        final Local local = exportableFunction.localByLabel("thisref");
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final ObjectString objectString) {
        final int index = objectString.value.index;
        final Global global = module.getGlobals().globalsIndex().globalByLabel("stringpool_" + index);
        return ConstExpressions.getGlobal(global);
    }

    private WasmValue toWasmValue(final PrimitiveInt primitiveInt) {
        return ConstExpressions.i32.c(primitiveInt.value);
    }

    private WasmValue toWasmValue(final MethodArgument methodArgument) {
        final String localName = "arg" + methodArgument.index;
        final Local local = exportableFunction.localByLabel(localName);
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final AbstractVar abstractVar) {
        final Local local = varLocalMap.get(abstractVar);
        if (local == null) {
            throw new IllegalArgumentException("Cannot find Wasm local for variable " + abstractVar);
        }
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final NullReference methodArgument) {
        final ResolvedClass cl = compileUnit.findClass(methodArgument.type);
        final StructType type = objectTypeMappings.get(cl);
        return ConstExpressions.ref.nullRef(type);
    }

    private WasmValue toWasmValue(final New newInstruction) {
        final ResolvedClass cl = compileUnit.findClass(newInstruction.type);
        final StructType type = objectTypeMappings.get(cl);
        final List<WasmValue> initArgs = new ArrayList<>();

        final String className = WasmHelpers.generateClassName(cl.type);
        if (cl.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            initArgs.add(ConstExpressions.call(initFunction, Collections.emptyList()));
        } else {
            final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
            initArgs.add(ConstExpressions.getGlobal(global));
        }
        for (int i = 1; i < type.getFields().size(); i++) {
            final StructType.Field f = type.getFields().get(i);
            if (f.getType() instanceof PrimitiveType) {
                switch ((PrimitiveType) f.getType()) {
                    case i32:
                        initArgs.add(ConstExpressions.i32.c(0));
                        break;
                    case f32:
                        initArgs.add(ConstExpressions.f32.c(0.0f));
                        break;
                    case i64:
                        initArgs.add(ConstExpressions.i64.c(0L));
                        break;
                    case f64:
                        initArgs.add(ConstExpressions.f64.c(0.0d));
                        break;
                    default:
                        throw new IllegalArgumentException("Field type " + f.getType() + " not supported!");
                }
            } else if (f.getType() instanceof HostType) {
                initArgs.add(ConstExpressions.ref.nullRef((HostType) f.getType()));
            } else if (f.getType() instanceof RefType) {
                initArgs.add(ConstExpressions.ref.nullRef(((RefType) f.getType()).getType()));
            } else {
                throw new IllegalArgumentException("Field type " + f.getType() + " not supported!");
            }
        }

        return ConstExpressions.struct.newInstance(type, initArgs);
    }

    private WasmValue toWasmValue(final ReadInstanceField readInstanceField) {
        final ResolvedClass cl = compileUnit.findClass(readInstanceField.type);
        final StructType type = objectTypeMappings.get(cl);
        return ConstExpressions.struct.get(type, toWasmValue((Value) readInstanceField.incomingDataFlows[0]),
                readInstanceField.resolvedField.name);
    }

    private WasmValue toWasmValue(final StaticMethodInvocationExpression staticMethodInvocationExpression) {
        final ResolvedMethod rm = staticMethodInvocationExpression.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> callArgs = new ArrayList<>();

        final String className = WasmHelpers.generateClassName(cl.type);
        if (cl.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            callArgs.add(ConstExpressions.call(initFunction, Collections.emptyList()));
        } else {
            final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
            callArgs.add(ConstExpressions.getGlobal(global));
        }

        final String functionName = WasmHelpers.generateClassName(cl.type) + "$" + WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
        for (final Node arg : staticMethodInvocationExpression.incomingDataFlows) {
            callArgs.add(toWasmValue((Value) arg));
        }
        return ConstExpressions.call(ConstExpressions.weakFunctionReference(functionName), callArgs);
    }

    private WasmValue toType(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
            case Type.LONG:
            case Type.FLOAT:
            case Type.DOUBLE: {
                // Primitive type

                // TODO: resolve correct class here
                final ResolvedClass cl = compileUnit.findClass(Type.getType(Object.class));
                final String className = WasmHelpers.generateClassName(cl.type);

                final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
                return ConstExpressions.getGlobal(global);
            }
            case Type.OBJECT: {
                // Object type
                final ResolvedClass cl = compileUnit.findClass(type);
                final String className = WasmHelpers.generateClassName(cl.type);
                if (cl.requiresClassInitializer()) {
                    final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
                    return ConstExpressions.call(initFunction, Collections.emptyList());
                }
                final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
                return ConstExpressions.getGlobal(global);
            }
            case Type.ARRAY: {
                // Array type

                // TODO: resolve correct class here
                final ResolvedClass cl = compileUnit.findClass(Type.getType(Object.class));
                final String className = WasmHelpers.generateClassName(cl.type);

                final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
                return ConstExpressions.getGlobal(global);
            }
            default: {
                throw new IllegalStateException("Unsupported type " + type);
            }
        }
    }

    private WasmValue toWasmValue(final TypeReference typeReference) {
        return toType(typeReference.type);
    }

    private WasmValue toWasmValue(final Value value) {
        if (value instanceof This) {
            return toWasmValue((This) value);
        } else if (value instanceof ObjectString) {
            return toWasmValue((ObjectString) value);
        } else if (value instanceof PrimitiveInt) {
            return toWasmValue((PrimitiveInt) value);
        } else if (value instanceof MethodArgument) {
            return toWasmValue((MethodArgument) value);
        } else if (value instanceof AbstractVar) {
            return toWasmValue((AbstractVar) value);
        } else if (value instanceof NullReference) {
            return toWasmValue((NullReference) value);
        } else if (value instanceof New) {
            return toWasmValue((New) value);
        } else if (value instanceof ReadInstanceField) {
            return toWasmValue((ReadInstanceField) value);
        } else if (value instanceof StaticMethodInvocationExpression) {
            return toWasmValue((StaticMethodInvocationExpression) value);
        } else if (value instanceof TypeReference) {
            return toWasmValue((TypeReference) value);
        }
        throw new IllegalArgumentException("Not implemented " + value.getClass());
    }

    @Override
    public void write(final Copy node) {
        final Value value = (Value) node.incomingDataFlows[0];
        final Node target = node.outgoingFlows[0];
        if (target instanceof AbstractVar) {
            final Local local = varLocalMap.get((AbstractVar) target);
            if (local == null) {
                throw new IllegalArgumentException("Cannot find Wasm local for variable " + target);
            }
            targetContainer.flow.setLocal(local, toWasmValue(value));

        } else {
            targetContainer.flow.comment("Copy from " + value.getClass() + " to " + target.getClass());
        }
    }

    @Override
    public void writeIfAndStartTrueBlock(final If node) {
        targetContainer.flow.comment("if");
    }

    @Override
    public void startIfElseBlock(final If node) {
        targetContainer.flow.comment("Else");
    }

    @Override
    public void finishBlock() {
        targetContainer.flow.comment("finishBlock");
    }

    @Override
    public void startBlock(final Sequencer.Block node) {
        targetContainer.flow.comment("Start " + node.label + ", " + node.type);
    }

    @Override
    public void write(final LineNumberDebugInfo node) {
        targetContainer.flow.comment("Line number #" + node.lineNumber);
    }

    @Override
    public void write(final FrameDebugInfo node) {
        targetContainer.flow.comment("FrameDebugInfo");
    }

    @Override
    public void write(final Goto node) {
        targetContainer.flow.comment("Goto");
    }

    @Override
    public void write(final MonitorEnter node) {
        targetContainer.flow.comment("MonitorEnter");
    }

    @Override
    public void write(final MonitorExit node) {
        targetContainer.flow.comment("MonitorExit");
    }

    @Override
    public void write(final Unwind node) {
        targetContainer.flow.comment("Unwind");
    }

    @Override
    public void write(final Return node) {
        targetContainer.flow.comment("Return");
    }

    @Override
    public void write(final ReturnValue node) {
        targetContainer.flow.comment("ReturnValue");
    }

    @Override
    public void write(final SetInstanceField node) {
        targetContainer.flow.comment("SetInstanceField");
    }

    @Override
    public void write(final SetClassField node) {
        targetContainer.flow.comment("SetClassField");
    }

    @Override
    public void write(final ArrayStore node) {
        targetContainer.flow.comment("ArrayStore");
    }

    @Override
    public void write(final CheckCast node) {
        targetContainer.flow.comment("CheckCast");
    }

    @Override
    public void writeBreakTo(final String label) {
        targetContainer.flow.comment("writeBreakTo " + label);
    }

    @Override
    public void writeContinueTo(final String label) {
        targetContainer.flow.comment("writeContinueTo " + label);
    }

    @Override
    public void startTryCatch(final String label) {
        targetContainer.flow.comment("startTryCatch " + label);
    }

    @Override
    public void startCatchBlock() {
        targetContainer.flow.comment("startCatchBlock");
    }

    @Override
    public void startCatchHandler(final Type type) {
        targetContainer.flow.comment("startCatchHandler");
    }

    @Override
    public void endCatchHandler() {
        targetContainer.flow.comment("endCatchHandler");
    }

    @Override
    public void writeRethrowException() {
        targetContainer.flow.comment("writeRethrowException");
    }

    @Override
    public void startFinallyBlock() {
        targetContainer.flow.comment("startFinallyBlock");
    }

    @Override
    public void writeSwitch(final TableSwitch node) {
        targetContainer.flow.comment("TableSwitch");
    }

    @Override
    public void startTableSwitchDefaultBlock() {
        targetContainer.flow.comment("startTableSwitchDefaultBlock");
    }

    @Override
    public void writeSwitch(final LookupSwitch node) {
        targetContainer.flow.comment("writeSwitch");
    }

    @Override
    public void writeSwitchCase(final int index) {
        targetContainer.flow.comment("writeSwitchCase");
    }

    @Override
    public void writeSwitchDefaultCase() {
        targetContainer.flow.comment("writeSwitchDefaultCase");
    }

    @Override
    public void finishSwitchCase() {
        targetContainer.flow.comment("finishSwitchCase");
    }

    @Override
    public void writeDebugNote(final String message) {
        targetContainer.flow.comment(message);
    }
}
