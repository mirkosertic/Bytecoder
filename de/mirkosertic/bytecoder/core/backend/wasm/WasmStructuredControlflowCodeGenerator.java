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
package de.mirkosertic.bytecoder.core.backend.wasm;

import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethod;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethodsRegistry;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.backend.sequencer.StructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Block;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Callable;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Container;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Expressions;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.FunctionType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Global;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.HostType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ImportReference;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.LabeledContainer;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Local;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Loop;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.RefType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.StructSubtype;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.StructType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Try;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.WasmType;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.WasmValue;
import de.mirkosertic.bytecoder.core.ir.AbstractVar;
import de.mirkosertic.bytecoder.core.ir.Add;
import de.mirkosertic.bytecoder.core.ir.And;
import de.mirkosertic.bytecoder.core.ir.ArrayLength;
import de.mirkosertic.bytecoder.core.ir.ArrayLoad;
import de.mirkosertic.bytecoder.core.ir.ArrayStore;
import de.mirkosertic.bytecoder.core.ir.BootstrapMethod;
import de.mirkosertic.bytecoder.core.ir.CMP;
import de.mirkosertic.bytecoder.core.ir.Cast;
import de.mirkosertic.bytecoder.core.ir.CaughtException;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Div;
import de.mirkosertic.bytecoder.core.ir.EnumValuesOf;
import de.mirkosertic.bytecoder.core.ir.FieldReference;
import de.mirkosertic.bytecoder.core.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.core.ir.Goto;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.core.ir.InstanceMethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.InstanceOf;
import de.mirkosertic.bytecoder.core.ir.InterfaceMethodInvocation;
import de.mirkosertic.bytecoder.core.ir.InterfaceMethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.InvokeDynamicExpression;
import de.mirkosertic.bytecoder.core.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.core.ir.LookupSwitch;
import de.mirkosertic.bytecoder.core.ir.MethodArgument;
import de.mirkosertic.bytecoder.core.ir.MethodReference;
import de.mirkosertic.bytecoder.core.ir.MethodType;
import de.mirkosertic.bytecoder.core.ir.MonitorEnter;
import de.mirkosertic.bytecoder.core.ir.MonitorExit;
import de.mirkosertic.bytecoder.core.ir.Mul;
import de.mirkosertic.bytecoder.core.ir.Neg;
import de.mirkosertic.bytecoder.core.ir.New;
import de.mirkosertic.bytecoder.core.ir.NewArray;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NullReference;
import de.mirkosertic.bytecoder.core.ir.NullTest;
import de.mirkosertic.bytecoder.core.ir.NumericalTest;
import de.mirkosertic.bytecoder.core.ir.ObjectString;
import de.mirkosertic.bytecoder.core.ir.Or;
import de.mirkosertic.bytecoder.core.ir.PHI;
import de.mirkosertic.bytecoder.core.ir.PrimitiveClassReference;
import de.mirkosertic.bytecoder.core.ir.PrimitiveDouble;
import de.mirkosertic.bytecoder.core.ir.PrimitiveFloat;
import de.mirkosertic.bytecoder.core.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.core.ir.PrimitiveLong;
import de.mirkosertic.bytecoder.core.ir.PrimitiveShort;
import de.mirkosertic.bytecoder.core.ir.ReadClassField;
import de.mirkosertic.bytecoder.core.ir.ReadInstanceField;
import de.mirkosertic.bytecoder.core.ir.Reference;
import de.mirkosertic.bytecoder.core.ir.ReferenceTest;
import de.mirkosertic.bytecoder.core.ir.Reinterpret;
import de.mirkosertic.bytecoder.core.ir.Rem;
import de.mirkosertic.bytecoder.core.ir.ResolveCallsite;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedField;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.ReturnValue;
import de.mirkosertic.bytecoder.core.ir.RuntimeClass;
import de.mirkosertic.bytecoder.core.ir.RuntimeClassOf;
import de.mirkosertic.bytecoder.core.ir.SHL;
import de.mirkosertic.bytecoder.core.ir.SHR;
import de.mirkosertic.bytecoder.core.ir.SetClassField;
import de.mirkosertic.bytecoder.core.ir.SetInstanceField;
import de.mirkosertic.bytecoder.core.ir.StaticMethodInvocation;
import de.mirkosertic.bytecoder.core.ir.StaticMethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.Sub;
import de.mirkosertic.bytecoder.core.ir.TableSwitch;
import de.mirkosertic.bytecoder.core.ir.This;
import de.mirkosertic.bytecoder.core.ir.TypeConversion;
import de.mirkosertic.bytecoder.core.ir.TypeReference;
import de.mirkosertic.bytecoder.core.ir.USHR;
import de.mirkosertic.bytecoder.core.ir.Unwind;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.ir.VirtualMethodInvocation;
import de.mirkosertic.bytecoder.core.ir.VirtualMethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.XOr;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class WasmStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    static class NestingLevel<T extends Container> {

        final NestingLevel<?> parent;

        Expressions activeFlow;

        final T activeContainer;

        public NestingLevel(final NestingLevel<?> parent, final Expressions activeFlow, final T activeContainer) {
            this.parent = parent;
            this.activeFlow = activeFlow;
            this.activeContainer = activeContainer;
        }

        public NestingLevel(final Expressions activeFlow, final T activeContainer) {
            this(null, activeFlow, activeContainer);
        }

        public LabeledContainer findByLabelInHierarchy(final String label) {
            return activeContainer.findByLabelInHierarchy(label);
        }

        int depth() {
            int count = 0;
            NestingLevel<?> t = this;
            while (t != null) {
                count = count + 1;
                t = t.parent;
            }
            return count;
        }

        void writeDebug(final String message) {
            activeFlow.comment(message);
        }
    }

    static class NestingLevelIff extends NestingLevel<Iff> {

        public NestingLevelIff(final NestingLevel<?> parent, final Expressions activeFlow, final Iff activeContainer) {
            super(parent, activeFlow, activeContainer);
        }
    }

    static class NestingLevelBlock extends NestingLevel<Block> {

        public NestingLevelBlock(final NestingLevel<?> parent, final Expressions activeFlow, final Block activeContainer) {
            super(parent, activeFlow, activeContainer);
        }
    }

    static class NestingLevelLoop extends NestingLevel<Loop> {

        public NestingLevelLoop(final NestingLevel<?> parent, final Expressions activeFlow, final Loop activeContainer) {
            super(parent, activeFlow, activeContainer);
        }
    }

    static class NestingLevelSwitch extends NestingLevel<LabeledContainer> {

        final WasmValue valueToCheck;

        public NestingLevelSwitch(final NestingLevel<?> parent, final Expressions activeFlow, final LabeledContainer activeContainer, final WasmValue valueToCheck) {
            super(parent, activeFlow, activeContainer);
            this.valueToCheck = valueToCheck;
        }
    }

    static class NestingLevelTry extends NestingLevel<Try> {

        public NestingLevelTry(final NestingLevel<?> parent, final Expressions activeFlow, final Try activeContainer) {
            super(parent, activeFlow, activeContainer);
        }
    }

    private final CompileUnit compileUnit;

    private final Module module;

    private final Map<ResolvedClass, StructType> objectTypeMappings;

    private final Map<ResolvedClass, StructType> rtMappings;

    private final ExportableFunction exportableFunction;

    private final Function<Type, WasmType> typeConverter;

    private final Function<ResolvedMethod, FunctionType> functionTypeConverter;

    private final MethodToIDMapper methodToIDMapper;

    private final Map<AbstractVar, Local> varLocalMap;

    private NestingLevel<?> activeLevel;

    private final Graph graph;

    private final List<ResolvedClass> resolvedClasses;

    private final VTableResolver vTableResolver;

    private final GeneratedMethodsRegistry generatedMethodsRegistry;

    public WasmStructuredControlflowCodeGenerator(final CompileUnit compileUnit, final Module module,
                                                  final Map<ResolvedClass, StructType> rtMappings,
                                                  final Map<ResolvedClass, StructType> objectTypeMappings,
                                                  final ExportableFunction exportableFunction,
                                                  final Function<Type, WasmType> typeConverter,
                                                  final Function<ResolvedMethod, FunctionType> functionTypeConverter,
                                                  final MethodToIDMapper methodToIDMapper,
                                                  final Graph graph,
                                                  final List<ResolvedClass> resolvedClasses,
                                                  final VTableResolver vTableResolver,
                                                  final GeneratedMethodsRegistry generatedMethodsRegistry) {
        this.compileUnit = compileUnit;
        this.module = module;
        this.exportableFunction = exportableFunction;
        this.rtMappings = rtMappings;
        this.objectTypeMappings = objectTypeMappings;
        this.typeConverter = typeConverter;
        this.functionTypeConverter = functionTypeConverter;
        this.methodToIDMapper = methodToIDMapper;
        this.varLocalMap = new HashMap<>();
        this.activeLevel = new NestingLevel<>(exportableFunction.flow, exportableFunction);
        this.graph = graph;
        this.resolvedClasses = resolvedClasses;
        this.vTableResolver = vTableResolver;
        this.generatedMethodsRegistry = generatedMethodsRegistry;
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
        final ResolvedMethod rm = node.method;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> callArgs = new ArrayList<>();

        final String functionName = WasmHelpers.generateClassName(cl.type) + "$" + WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
        for (int i = 0; i < node.incomingDataFlows.length; i++) {
            final Value arg = (Value) node.incomingDataFlows[i];
            callArgs.add(toWasmValue(arg));
        }
        activeLevel.activeFlow.voidCall(ConstExpressions.weakFunctionReference(functionName), callArgs);
    }

    @Override
    public void write(final VirtualMethodInvocation node) {
        final ResolvedMethod rm = node.resolvedMethod;
        //final ResolvedClass cl = rm.owner;

        final List<WasmValue> indirectCallArgs = new ArrayList<>();

        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);

        for (final Node arg : node.incomingDataFlows) {
            indirectCallArgs.add(toWasmValue((Value) arg));
        }

        final List<WasmValue> resolverArgs = new ArrayList<>();
        resolverArgs.add(ConstExpressions.i32.c(methodToIDMapper.resolveIdFor(rm)));

        //final StructType classType = rtMappings.get(cl);
        final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));

        resolverArgs.add(ConstExpressions.struct.get(
                objectType,
                //ConstExpressions.ref.cast(objectType, toWasmValue((Value) node.incomingDataFlows[0])),
                toWasmValue((Value) node.incomingDataFlows[0]),
                "vt_resolver"
        ));

        final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);
        final FunctionType ft = functionTypeConverter.apply(node.resolvedMethod);

        activeLevel.activeFlow.voidCallIndirect(ft, indirectCallArgs, resolver);
    }

    @Override
    public void write(final StaticMethodInvocation node) {
        final ResolvedMethod rm = node.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> callArgs = new ArrayList<>();

        final String functionName = WasmHelpers.generateClassName(cl.type) + "$" + WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
        for (int i = 0; i < node.incomingDataFlows.length; i++) {
            callArgs.add(toWasmValue((Value) node.incomingDataFlows[i]));
        }
        activeLevel.activeFlow.voidCall(ConstExpressions.weakFunctionReference(functionName), callArgs);
    }

    @Override
    public void write(final InterfaceMethodInvocation node) {
        final ResolvedMethod rm = node.method;
        //final ResolvedClass cl = rm.owner;

        final List<WasmValue> indirectCallArgs = new ArrayList<>();

        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);

        for (final Node arg : node.incomingDataFlows) {
            indirectCallArgs.add(toWasmValue((Value) arg));
        }

        final List<WasmValue> resolverArgs = new ArrayList<>();
        resolverArgs.add(ConstExpressions.i32.c(methodToIDMapper.resolveIdFor(rm)));

        final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));

        resolverArgs.add(ConstExpressions.struct.get(
                objectType,
                //ConstExpressions.ref.cast(objectType, toWasmValue((Value) node.incomingDataFlows[0])),
                toWasmValue((Value) node.incomingDataFlows[0]),
                "vt_resolver"
        ));

        final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);
        final FunctionType ft = functionTypeConverter.apply(node.method);

        activeLevel.activeFlow.voidCallIndirect(ft, indirectCallArgs, resolver);
    }

    private WasmValue toWasmValue(final This value) {
        final Local local = exportableFunction.localByLabel("thisref");
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final ObjectString value) {
        final int index = value.value.index;
        final Global global = module.getGlobals().globalsIndex().globalByLabel("stringpool_" + index);
        return ConstExpressions.getGlobal(global);
    }

    private WasmValue toWasmValue(final PrimitiveShort value) {
        return ConstExpressions.i32.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveInt value) {
        return ConstExpressions.i32.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveLong value) {
        return ConstExpressions.i64.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveFloat value) {
        return ConstExpressions.f32.c(value.value);
    }

    private WasmValue toWasmValue(final PrimitiveDouble value) {
        return ConstExpressions.f64.c(value.value);
    }

    private WasmValue toWasmValue(final MethodArgument value) {
        final String localName = "arg" + value.index;
        final Local local = exportableFunction.localByLabel(localName);
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final AbstractVar value) {
        final Local local = varLocalMap.get(value);
        if (local == null) {
            throw new IllegalArgumentException("Cannot find Wasm local for variable " + value);
        }
        return ConstExpressions.getLocal(local);
    }

    private WasmValue toWasmValue(final NullReference value) {
        return ConstExpressions.ref.nullRef();
    }

    public static WasmValue createNewInstanceOf(final Type instanceType,
                                                final Module module,
                                                final CompileUnit compileUnit,
                                                final Map<ResolvedClass, StructType> objectTypeMappings,
                                                final Map<ResolvedClass, StructType> rtMappings,
                                                final WasmValue externRef) {
        final ResolvedClass cl = compileUnit.findClass(instanceType);
        if (cl == null) {
            throw new IllegalArgumentException("Cannot find resolved class for " + instanceType);
        }
        final StructType type = objectTypeMappings.get(cl);
        final List<WasmValue> initArgs = new ArrayList<>();

        final String className = WasmHelpers.generateClassName(cl.type);
        final WasmValue rttype;

        final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");

        if (cl.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            rttype = ConstExpressions.call(initFunction, Collections.emptyList());
        } else {
            rttype = ConstExpressions.getGlobal(global);
        }

        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(cl),
                        rttype,
                        "factoryFor"
                )
        );

        initArgs.add(ConstExpressions.ref.ref(module.functionIndex().firstByLabel(WasmHelpers.generateClassName(cl.type) + "_vt")));

        initArgs.add(externRef);

        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(cl),
                        ConstExpressions.getGlobal(global),
                        "classImplTypes"
                )
        );

        for (int i = 4; i < type.getFields().size(); i++) {
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
                initArgs.add(ConstExpressions.ref.externNullRef());
            } else if (f.getType() instanceof RefType) {
                initArgs.add(ConstExpressions.ref.nullRef());
            } else {
                throw new IllegalArgumentException("Field type " + f.getType() + " not supported!");
            }
        }

        return ConstExpressions.struct.newInstance(type, initArgs);
    }

    private WasmValue toWasmValue(final New value) {
        return createNewInstanceOf(value.type,
                module, compileUnit, objectTypeMappings, rtMappings, ConstExpressions.ref.externNullRef());
    }

    private WasmValue toWasmValue(final ReadInstanceField value) {
        final ResolvedClass cl = compileUnit.findClass(value.resolvedField.owner.type);
        final StructType type = objectTypeMappings.get(cl);
        return ConstExpressions.struct.get(type,
                ConstExpressions.ref.cast(type, toWasmValue((Value) value.incomingDataFlows[0])),
                value.resolvedField.name);
    }

    private WasmValue toWasmValue(final StaticMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> callArgs = new ArrayList<>();

        final String functionName = WasmHelpers.generateClassName(cl.type) + "$" + WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
        for (int i = 0; i < value.incomingDataFlows.length; i++) {
            callArgs.add(toWasmValue((Value) value.incomingDataFlows[i]));
        }
        return ConstExpressions.call(ConstExpressions.weakFunctionReference(functionName), callArgs);
    }

    private WasmValue toWasmValue(final InstanceMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;
        final ResolvedClass cl = rm.owner;

        final List<WasmValue> callArgs = new ArrayList<>();

        final String functionName = WasmHelpers.generateClassName(cl.type) + "$" + WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
        for (int i = 0; i < value.incomingDataFlows.length; i++) {
            final Value arg = (Value) value.incomingDataFlows[i];
            callArgs.add(toWasmValue(arg));
        }
        return ConstExpressions.call(ConstExpressions.weakFunctionReference(functionName), callArgs);
    }

    private WasmValue toWasmValue(final VirtualMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;

        final List<WasmValue> indirectCallArgs = new ArrayList<>();

        for (final Node arg : value.incomingDataFlows) {
            indirectCallArgs.add(toWasmValue((Value) arg));
        }

        final List<WasmValue> resolverArgs = new ArrayList<>();
        resolverArgs.add(ConstExpressions.i32.c(methodToIDMapper.resolveIdFor(rm)));
        final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));
        resolverArgs.add(ConstExpressions.struct.get(
                objectType,
                toWasmValue((Value) value.incomingDataFlows[0]),
                "vt_resolver"
        ));

        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);
        final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);
        final FunctionType ft = functionTypeConverter.apply(value.resolvedMethod);

        return ConstExpressions.call(ft, indirectCallArgs, resolver);
    }

    private WasmValue toWasmValue(final InterfaceMethodInvocationExpression value) {
        final ResolvedMethod rm = value.resolvedMethod;
        //final ResolvedClass cl = rm.owner;

        final List<WasmValue> indirectCallArgs = new ArrayList<>();

        final List<WasmType> vtArgs = new ArrayList<>();
        vtArgs.add(PrimitiveType.i32);
        final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);

        for (final Node arg : value.incomingDataFlows) {
            indirectCallArgs.add(toWasmValue((Value) arg));
        }

        final List<WasmValue> resolverArgs = new ArrayList<>();
        resolverArgs.add(ConstExpressions.i32.c(methodToIDMapper.resolveIdFor(rm)));

        final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));

        resolverArgs.add(ConstExpressions.struct.get(
                objectType,
                ConstExpressions.ref.cast(objectType, toWasmValue((Value) value.incomingDataFlows[0])),
                "vt_resolver"
        ));

        final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);
        final FunctionType ft = functionTypeConverter.apply(value.resolvedMethod);

        return ConstExpressions.call(ft, indirectCallArgs, resolver);
    }

    static int lambdaCounter = 0;

    static class LambdaInstance {

        final WasmValue instance;
        final StructSubtype type;

        public LambdaInstance(final WasmValue instance, final StructSubtype type) {
            this.instance = instance;
            this.type = type;
        }
    }

    private LambdaInstance createLambdaInstance(final Type type, final ExportableFunction implFunction,
                                                final List<StructType.Field> closureFields,
                                                final List<WasmValue> closureArguments) {
        final ResolvedClass rc = compileUnit.findClass(type);
        final Set<ResolvedMethod> abstractMethods = rc.abstractResolvedMethods();
        if (abstractMethods.size() != 1) {
            throw new IllegalArgumentException("Expected one abstract method in type " + type + ", got " + abstractMethods.size());
        }
        final ResolvedMethod lambdaMethod = abstractMethods.iterator().next();
        final VTable vtable = vTableResolver.resolveFor(rc);

        final Map<Integer, String> implMethods = new HashMap<>();
        for (final Map.Entry<Integer, ResolvedMethod> entry : vtable.getMethods().entrySet()) {
            final ResolvedMethod rm = entry.getValue();
            final int methodId = entry.getKey();

            final String ownerClassName = WasmHelpers.generateClassName(rm.owner.type);
            final String methodName = WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
            implMethods.put(methodId, ownerClassName + "$" + methodName);
        }
        implMethods.put(methodToIDMapper.resolveIdFor(lambdaMethod), implFunction.getLabel());

        final String lambdaName = type.getClassName() + "$lambda$" + lambdaCounter++;
        final String lambdavtName = lambdaName + "_vt";
        final ExportableFunction lambdaVt = WasmHelpers.createVTableResolver(module, lambdavtName, implMethods);

        final StructType baseType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(type));
        final StructSubtype lambdaSubType = module.getTypes().structSubtype(lambdaName, baseType, closureFields);

        final String baseTypeClassName = WasmHelpers.generateClassName(rc.type);
        final WasmValue rttype;

        final Global global = module.getGlobals().globalsIndex().globalByLabel(baseTypeClassName  + "_cls");

        if (rc.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(baseTypeClassName + "_i");
            rttype = ConstExpressions.call(initFunction, Collections.emptyList());
        } else {
            rttype = ConstExpressions.getGlobal(global);
        }

        final List<WasmValue> initArgs = new ArrayList<>();
        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(rc),
                        rttype,
                        "factoryFor"
                )
        );

        initArgs.add(ConstExpressions.ref.ref(lambdaVt));

        initArgs.add(ConstExpressions.ref.externNullRef());

        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(rc),
                        ConstExpressions.getGlobal(global),
                        "classImplTypes"
                )
        );

        for (int i = 4; i < baseType.getFields().size(); i++) {
            final StructType.Field f = baseType.getFields().get(i);
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
                initArgs.add(ConstExpressions.ref.externNullRef());
            } else if (f.getType() instanceof RefType) {
                initArgs.add(ConstExpressions.ref.nullRef());
            } else {
                throw new IllegalArgumentException("Field type " + f.getType() + " not supported!");
            }
        }

        initArgs.addAll(closureArguments);

        return new LambdaInstance(ConstExpressions.struct.newInstance(lambdaSubType, initArgs), lambdaSubType);
    }

    private WasmValue generateInvokeDynamicLambdaMetaFactoryInvocation(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        // Ok, we can create a lambda invocation here
        final ObjectString argMethodName = (ObjectString) resolveCallsite.incomingDataFlows[1];
        final MethodType argInvokedType = (MethodType) resolveCallsite.incomingDataFlows[2];
        final MethodType argSamMethodType = (MethodType) resolveCallsite.incomingDataFlows[3];
        final MethodReference argImplMethod = (MethodReference) resolveCallsite.incomingDataFlows[4];
        final MethodType argInstanceMethodType = (MethodType) resolveCallsite.incomingDataFlows[5];

        final ResolvedMethod implementationMethod = argImplMethod.resolvedMethod;

        // Collect the closure fields
        final List<StructType.Field> closureFields = new ArrayList<>();
        final List<WasmValue> closureArguments = new ArrayList<>();
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            final Value closureArgument = (Value) node.incomingDataFlows[i];
            final WasmValue wasmValue = toWasmValue(closureArgument);
            closureFields.add(new StructType.Field("link" + i, typeConverter.apply(closureArgument.type)));
            closureArguments.add(wasmValue);
        }

        // Construct the method to embed into the lambda type
        final Type returnType = argInvokedType.type.getReturnType();
        final ExportableFunction lambdaMethod;
        final List<Param> lambdaMethodArgs = new ArrayList<>();
        final WasmType thisRef = ConstExpressions.ref.type(module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class))), true);
        lambdaMethodArgs.add(ConstExpressions.param("thisref", thisRef));
        for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
            lambdaMethodArgs.add(ConstExpressions.param("arg" + i, typeConverter.apply(argSamMethodType.type.getArgumentTypes()[i])));
        }
        if (argImplMethod.kind == Reference.Kind.INVOKECONSTRUCTOR) {
            lambdaMethod = module.getFunctions().newFunction("lambda" + lambdaCounter++, lambdaMethodArgs, typeConverter.apply(implementationMethod.owner.type));
        } else {
            if (implementationMethod.methodType.getReturnType() == Type.VOID_TYPE) {
                lambdaMethod = module.getFunctions().newFunction("lambda" + lambdaCounter++, lambdaMethodArgs);
            } else {
                lambdaMethod = module.getFunctions().newFunction("lambda" + lambdaCounter++, lambdaMethodArgs, typeConverter.apply(implementationMethod.methodType.getReturnType()));
            }
        }
        lambdaMethod.toTable();

        // Finally construct a new type with virtual method table and
        // the linked closure fields
        final LambdaInstance lambdaInstance = createLambdaInstance(returnType, lambdaMethod, closureFields, closureArguments);

        lambdaMethod.flow.comment("Invocation kind is " + argImplMethod.kind);

        // Depending on the method kind we generate different implementations of the
        // lambda method body
        switch (argImplMethod.kind) {
            case INVOKESTATIC: {
                final String implMethodName = WasmHelpers.generateClassName(implementationMethod.owner.type) + "$" + WasmHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType);
                final List<WasmValue> arguments = new ArrayList<>();

                final ResolvedClass cl = implementationMethod.owner;
                final String className = WasmHelpers.generateClassName(implementationMethod.owner.type);
                if (cl.requiresClassInitializer()) {
                    final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
                    arguments.add(ConstExpressions.call(initFunction, Collections.emptyList()));
                } else {
                    final Global global = module.getGlobals().globalsIndex().globalByLabel(className + "_cls");
                    arguments.add(ConstExpressions.getGlobal(global));
                }

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    final String fieldName = "link" + i;
                    arguments.add(
                      ConstExpressions.struct.get(
                              lambdaInstance.type,
                              ConstExpressions.ref.cast(
                                      lambdaInstance.type,
                                      ConstExpressions.getLocal(lambdaMethod.localByLabel("thisref"))
                              ),
                              fieldName
                      )
                    );
                }

                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    arguments.add(ConstExpressions.getLocal(lambdaMethod.localByLabel("arg" + i)));
                }

                if (implementationMethod.methodType.getReturnType() == Type.VOID_TYPE) {
                    lambdaMethod.flow.voidCall(ConstExpressions.weakFunctionReference(implMethodName), arguments);
                } else {
                    lambdaMethod.flow.ret(ConstExpressions.call(ConstExpressions.weakFunctionReference(implMethodName), arguments));
                }

                return lambdaInstance.instance;
            }
            case INVOKEVIRTUAL: {
                final List<WasmValue> arguments = new ArrayList<>();

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    final String fieldName = "link" + i;
                    arguments.add(
                            ConstExpressions.struct.get(
                                    lambdaInstance.type,
                                    ConstExpressions.ref.cast(
                                            lambdaInstance.type,
                                            ConstExpressions.getLocal(lambdaMethod.localByLabel("thisref"))
                                    ),
                                    fieldName
                            )
                    );
                }

                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    arguments.add(ConstExpressions.getLocal(lambdaMethod.localByLabel("arg" + i)));
                }

                final List<WasmValue> resolverArgs = new ArrayList<>();
                resolverArgs.add(ConstExpressions.i32.c(methodToIDMapper.resolveIdFor(implementationMethod)));
                final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));
                resolverArgs.add(ConstExpressions.struct.get(
                        objectType,
                        arguments.get(0),
                        "vt_resolver"
                ));

                final List<WasmType> vtArgs = new ArrayList<>();
                vtArgs.add(PrimitiveType.i32);
                final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);
                final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);
                final FunctionType ft = functionTypeConverter.apply(implementationMethod);

                if (implementationMethod.methodType.getReturnType() == Type.VOID_TYPE) {
                    lambdaMethod.flow.voidCallIndirect(ft, arguments, resolver);
                } else {
                    lambdaMethod.flow.ret(ConstExpressions.call(ft, arguments, resolver));
                }

                return lambdaInstance.instance;
            }
            case INVOKEINTERFACE: {
                final List<WasmValue> arguments = new ArrayList<>();
                final List<WasmType> argumentTypes = new ArrayList<>();
                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    final String fieldName = "link" + i;
                    arguments.add(
                            ConstExpressions.struct.get(
                                    lambdaInstance.type,
                                    ConstExpressions.ref.cast(
                                            lambdaInstance.type,
                                            ConstExpressions.getLocal(lambdaMethod.localByLabel("thisref"))
                                    ),
                                    fieldName
                            )
                    );
                    argumentTypes.add(lambdaInstance.type.fieldByName(fieldName).getType());
                }

                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    final Local l = lambdaMethod.localByLabel("arg" + i);
                    arguments.add(ConstExpressions.getLocal(l));
                    argumentTypes.add(l.getType());
                }

                final List<WasmValue> resolverArgs = new ArrayList<>();
                resolverArgs.add(ConstExpressions.i32.c(methodToIDMapper.resolveIdFor(implementationMethod)));
                final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));
                resolverArgs.add(ConstExpressions.struct.get(
                        objectType,
                        arguments.get(0),
                        "vt_resolver"
                ));

                final List<WasmType> vtArgs = new ArrayList<>();
                vtArgs.add(PrimitiveType.i32);
                final FunctionType vtType = module.getTypes().functionType(vtArgs, PrimitiveType.i32);
                final WasmValue resolver = ConstExpressions.ref.callRef(vtType, resolverArgs);

                if (implementationMethod.methodType.getReturnType() == Type.VOID_TYPE) {
                    final FunctionType ft = module.getTypes().functionType(argumentTypes);
                    lambdaMethod.flow.voidCallIndirect(ft, arguments, resolver);
                } else {
                    final FunctionType ft = module.getTypes().functionType(argumentTypes, typeConverter.apply(implementationMethod.methodType.getReturnType()));
                    lambdaMethod.flow.ret(ConstExpressions.call(ft, arguments, resolver));
                }

                return lambdaInstance.instance;
            }
            case INVOKESPECIAL: {
                final String implMethodName = WasmHelpers.generateClassName(implementationMethod.owner.type) + "$" + WasmHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType);
                final List<WasmValue> arguments = new ArrayList<>();

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    final String fieldName = "link" + i;
                    arguments.add(
                            ConstExpressions.struct.get(
                                    lambdaInstance.type,
                                    ConstExpressions.ref.cast(
                                            lambdaInstance.type,
                                            ConstExpressions.getLocal(lambdaMethod.localByLabel("thisref"))
                                    ),
                                    fieldName
                            )
                    );
                }

                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    arguments.add(ConstExpressions.getLocal(lambdaMethod.localByLabel("arg" + i)));
                }

                if (implementationMethod.methodType.getReturnType() == Type.VOID_TYPE) {
                    lambdaMethod.flow.voidCall(ConstExpressions.weakFunctionReference(implMethodName), arguments);
                } else {
                    lambdaMethod.flow.ret(ConstExpressions.call(ConstExpressions.weakFunctionReference(implMethodName), arguments));
                }

                return lambdaInstance.instance;
            }
            case INVOKECONSTRUCTOR: {

                final List<WasmValue> arguments = new ArrayList<>();

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    final String fieldName = "link" + i;
                    arguments.add(
                            ConstExpressions.struct.get(
                                    lambdaInstance.type,
                                    ConstExpressions.ref.cast(
                                            lambdaInstance.type,
                                            ConstExpressions.getLocal(lambdaMethod.localByLabel("thisref"))
                                    ),
                                    fieldName
                            )
                    );
                }

                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    arguments.add(ConstExpressions.getLocal(lambdaMethod.localByLabel("arg" + i)));
                }

                final Local objInstance = lambdaMethod.newLocal("newInstance", typeConverter.apply(implementationMethod.owner.type));

                lambdaMethod.flow.setLocal(objInstance,
                        createNewInstanceOf(implementationMethod.owner.type,
                                module, compileUnit, objectTypeMappings, rtMappings, ConstExpressions.ref.externNullRef()));

                arguments.add(0, ConstExpressions.getLocal(objInstance));

                final String implMethodName = WasmHelpers.generateClassName(implementationMethod.owner.type) + "$" + WasmHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType);
                lambdaMethod.flow.voidCall(ConstExpressions.weakFunctionReference(implMethodName), arguments);

                lambdaMethod.flow.ret(ConstExpressions.getLocal(objInstance));

                return lambdaInstance.instance;
            }
        }
        throw new IllegalArgumentException("Not implemented!");
    }

    private WasmValue generateInvokeDynamicStringMakeConcatWithConstants(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        final MethodType functionType = (MethodType) resolveCallsite.incomingDataFlows[2];
        final ObjectString receipe = (ObjectString) resolveCallsite.incomingDataFlows[3];
        final String receipeStr = compileUnit.getConstantPool().getPooledStrings().get(receipe.value.index);

        final List<Param> params = new ArrayList<>();
        final List<WasmValue> arguments = new ArrayList<>();

        final boolean hasLinkArg;
        if (resolveCallsite.incomingDataFlows.length > 4) {
            final Value v = (Value) resolveCallsite.incomingDataFlows[4];
            final WasmValue arg = toWasmValue(v);
            arguments.add(arg);
            params.add(ConstExpressions.param("linkarg", typeConverter.apply(v.type)));
            hasLinkArg = true;
        } else {
            hasLinkArg = false;
        }

        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            final Value v = (Value) node.incomingDataFlows[i];
            switch (v.type.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY: {
                    final Type toStringMethodType = Type.getMethodType(
                            Type.getType(String.class),
                            Type.getType(Object.class)
                    );
                    final String methodName = WasmHelpers.generateMethodName("objectToString", toStringMethodType);
                    final String vmClassName = WasmHelpers.generateClassName(Type.getType(VM.class));

                    final String functionName = vmClassName + "$" + methodName;

                    final ResolvedClass objectClass = compileUnit.findClass(Type.getType(Object.class));

                    final List<WasmValue> toStringArgs = new ArrayList<>();
                    toStringArgs.add(ConstExpressions.ref.nullRef());
                    toStringArgs.add(toWasmValue(v));

                    arguments.add(ConstExpressions.struct.get(
                            objectTypeMappings.get(objectClass),
                            ConstExpressions.call(
                                    ConstExpressions.weakFunctionReference(functionName), toStringArgs
                            ),
                            "nativeObject"
                    ));
                    params.add(ConstExpressions.param("dynarg" + (i - 1), ConstExpressions.ref.host()));
                    break;
                }
                default: {
                    final WasmValue arg = toWasmValue(v);
                    arguments.add(arg);
                    params.add(ConstExpressions.param("dynarg" + (i - 1), typeConverter.apply(v.type)));
                    break;
                }
            }
        }

        final int index = generatedMethodsRegistry.register(new GeneratedMethod() {
            @Override
            public void generateCode(final PrintWriter pw, final int index) {
                pw.print("bytecoder.imports.bytecoder.stringoperations");
                pw.print(index);
                pw.print(" = function(");
                if (hasLinkArg) {
                    pw.print("linkarg");

                    for (int i = 1; i < node.incomingDataFlows.length; i++) {
                        pw.print(",");
                        pw.print("dynArg" + (i - 1));
                    }
                } else {
                    for (int i = 1; i < node.incomingDataFlows.length; i++) {
                        if (i > 1) {
                            pw.print(",");
                        }
                        pw.print("dynArg" + (i - 1));
                    }
                }

                pw.println(") {");

                pw.println("    let str = '';");
                final int linkingArgOffset = 0;
                int dynamicArgoffset = 0;
                int totalIndex = 0;

                for (int i = 0; i < receipeStr.length(); i++) {
                    final char c = receipeStr.charAt(i);
                    // TODO: generate code
                    switch (c) {
                        case 1: {
                            final Type typeToAdd = functionType.type.getArgumentTypes()[totalIndex];

                            pw.print("    str = str + dynArg");
                            pw.print(dynamicArgoffset);
                            pw.println(";");

                            dynamicArgoffset++;
                            totalIndex++;
                            break;
                        }
                        case 2: {
                            final Type typeToAdd = functionType.type.getArgumentTypes()[totalIndex];
                            totalIndex++;
                            break;
                        }
                        default: {
                            pw.println("    str = str + '" + c + "';");
                            break;
                        }
                    }
                }

                pw.println("    return str;");

                pw.println("};");
            }
        });
        final Callable concatFunction = module.getImports().importFunction(new ImportReference("bytecoder", "stringoperations" + index), "stringoperations" + index, params, ConstExpressions.ref.host());

        final ResolvedClass stringClass = compileUnit.findClass(Type.getType(String.class));
        final Global stringGlobal = module.getGlobals().globalsIndex().globalByLabel(WasmHelpers.generateClassName(stringClass.type)  + "_cls");
        final StructType stringType = objectTypeMappings.get(stringClass);

        final List<WasmValue> initArgs = new ArrayList<>();
        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(stringClass),
                        ConstExpressions.getGlobal(stringGlobal),
                        "factoryFor"
                ));
        initArgs.add(ConstExpressions.ref.ref(module.functionIndex().firstByLabel(WasmHelpers.generateClassName(stringClass.type) + "_vt")));

        initArgs.add(ConstExpressions.call(concatFunction, arguments));

        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(stringClass),
                        ConstExpressions.getGlobal(stringGlobal),
                        "classImplTypes"
                )
        );

        return ConstExpressions.struct.newInstance(
            stringType,
            initArgs
        );
    }

    private WasmValue generateInvokeDynamicObjectMethodsToString(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        final ObjectString fields = (ObjectString) resolveCallsite.incomingDataFlows[4];
        final TypeReference sourceType = (TypeReference) resolveCallsite.incomingDataFlows[3];

        final List<Param> params = new ArrayList<>();
        final List<WasmValue> arguments = new ArrayList<>();

        final ResolvedClass resolvedClass = compileUnit.findClass(sourceType.type);
        final StructType structType = objectTypeMappings.get(resolvedClass);

        final WasmValue source = toWasmValue((Value) node.incomingDataFlows[1]);

        for (int i = 5; i < resolveCallsite.incomingDataFlows.length; i++) {
            final FieldReference fieldRef = (FieldReference) resolveCallsite.incomingDataFlows[i];

            final WasmValue fieldValue = ConstExpressions.struct.get(
                structType,
                ConstExpressions.ref.cast(structType, source),
                fieldRef.resolvedField.name
            );

            switch (fieldRef.type.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY: {
                    final Type toStringMethodType = Type.getMethodType(
                            Type.getType(String.class),
                            Type.getType(Object.class)
                    );
                    final String methodName = WasmHelpers.generateMethodName("objectToString", toStringMethodType);
                    final String vmClassName = WasmHelpers.generateClassName(Type.getType(VM.class));

                    final String functionName = vmClassName + "$" + methodName;

                    final ResolvedClass objectClass = compileUnit.findClass(Type.getType(Object.class));

                    final List<WasmValue> toStringArgs = new ArrayList<>();
                    toStringArgs.add(ConstExpressions.ref.nullRef());
                    toStringArgs.add(fieldValue);

                    arguments.add(ConstExpressions.struct.get(
                            objectTypeMappings.get(objectClass),
                            ConstExpressions.call(
                                    ConstExpressions.weakFunctionReference(functionName), toStringArgs
                            ),
                            "nativeObject"
                    ));
                    params.add(ConstExpressions.param("dynarg" + (i - 1), ConstExpressions.ref.host()));
                    break;
                }
                default: {
                    arguments.add(fieldValue);
                    params.add(ConstExpressions.param("dynarg" + (i - 1), typeConverter.apply(fieldRef.type)));
                    break;
                }
            }
        }

        final int index = generatedMethodsRegistry.register(new GeneratedMethod() {
            @Override
            public void generateCode(final PrintWriter pw, final int index) {
                pw.print("bytecoder.imports.bytecoder.stringoperations");
                pw.print(index);
                pw.print(" = function(");
                for (int i = 5; i < resolveCallsite.incomingDataFlows.length; i++) {
                    if (i > 5) {
                        pw.print(",");
                    }
                    pw.print("dynArg" + (i - 5));
                }

                pw.println(") {");

                String sourceTypeName = sourceType.type.getClassName();
                int x = sourceTypeName.lastIndexOf(".");
                if (x > -1) {
                    sourceTypeName = sourceTypeName.substring(x + 1);
                }
                x = sourceTypeName.lastIndexOf("$");
                if (x > -1) {
                    sourceTypeName = sourceTypeName.substring(x + 1);
                }

                pw.print("    let str = '");
                pw.print(sourceTypeName);
                pw.println("[';");
                for (int i = 5; i < resolveCallsite.incomingDataFlows.length; i++) {
                    final FieldReference fieldRef = (FieldReference) resolveCallsite.incomingDataFlows[i];
                    if (i > 5) {
                        pw.println("    str = str + ', ';");
                    }
                    pw.print("    str = str + '");
                    pw.print(fieldRef.resolvedField.name);
                    pw.println("=';");

                    pw.print("    str = str + dynArg");
                    pw.print((i - 5));
                    pw.println(";");

                }
                pw.println("    str = str + ']';");

                pw.println("    return str;");

                pw.println("};");
            }
        });
        final Callable concatFunction = module.getImports().importFunction(new ImportReference("bytecoder", "stringoperations" + index), "stringoperations" + index, params, ConstExpressions.ref.host());

        final ResolvedClass stringClass = compileUnit.findClass(Type.getType(String.class));
        final Global stringGlobal = module.getGlobals().globalsIndex().globalByLabel(WasmHelpers.generateClassName(stringClass.type)  + "_cls");
        final StructType stringType = objectTypeMappings.get(stringClass);

        final List<WasmValue> initArgs = new ArrayList<>();
        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(stringClass),
                        ConstExpressions.getGlobal(stringGlobal),
                        "factoryFor"
                ));
        initArgs.add(ConstExpressions.ref.ref(module.functionIndex().firstByLabel(WasmHelpers.generateClassName(stringClass.type) + "_vt")));

        initArgs.add(ConstExpressions.call(concatFunction, arguments));

        initArgs.add(
                ConstExpressions.struct.get(
                        rtMappings.get(stringClass),
                        ConstExpressions.getGlobal(stringGlobal),
                        "classImplTypes"
                )
        );

        return ConstExpressions.struct.newInstance(
                stringType,
                initArgs
        );
    }

    int generatedEquals = 0;

    private WasmValue generateInvokeDynamicObjectMethodsEquals(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        final ObjectString fields = (ObjectString) resolveCallsite.incomingDataFlows[4];
        final TypeReference sourceType = (TypeReference) resolveCallsite.incomingDataFlows[3];

        final List<Param> params = new ArrayList<>();
        final List<WasmValue> arguments = new ArrayList<>();

        final ResolvedClass resolvedClass = compileUnit.findClass(sourceType.type);
        final StructType structType = objectTypeMappings.get(resolvedClass);

        final WasmValue source = ConstExpressions.ref.cast(structType, toWasmValue((Value) node.incomingDataFlows[1]));

        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            final Value v = (Value) node.incomingDataFlows[i];
            final WasmType type = typeConverter.apply(v.type);
            params.add(ConstExpressions.param("dynArg" + (i - 1), type));
            arguments.add(toWasmValue(v));
        }

        final ExportableFunction hashCodeFunction = module.getFunctions().newFunction("generatedEquals" + (generatedEquals++), params, PrimitiveType.i32);
        final Local dynArg0 = hashCodeFunction.localByLabel("dynArg0");
        final Local dynArg1 = hashCodeFunction.localByLabel("dynArg1");

        final WasmValue instanceOfCheck = toInstanceOfCheck(ConstExpressions.getLocal(dynArg1), sourceType.type);
        final Iff iCheck = hashCodeFunction.flow.iff("icheck", ConstExpressions.i32.ne(instanceOfCheck, ConstExpressions.i32.c(1)));
        iCheck.flow.ret(ConstExpressions.i32.c(0));

        final WasmValue leftCast = ConstExpressions.ref.cast(structType, ConstExpressions.getLocal(dynArg0));
        final WasmValue rightCast = ConstExpressions.ref.cast(structType, ConstExpressions.getLocal(dynArg1));
        for (int i = 5; i < resolveCallsite.incomingDataFlows.length; i++) {
            final FieldReference fieldRef = (FieldReference) resolveCallsite.incomingDataFlows[i];
            final WasmValue left = ConstExpressions.struct.get(structType, leftCast, fieldRef.resolvedField.name);
            final WasmValue right = ConstExpressions.struct.get(structType, rightCast, fieldRef.resolvedField.name);
            final WasmValue condition;
            switch (fieldRef.type.getSort()) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.SHORT:
                case Type.INT: {
                    condition = ConstExpressions.i32.eq(left, right);
                    break;
                }
                case Type.FLOAT: {
                    condition = ConstExpressions.f32.eq(left, right);
                    break;
                }
                case Type.LONG: {
                    condition = ConstExpressions.i64.eq(left, right);
                    break;
                }
                case Type.DOUBLE: {
                    condition = ConstExpressions.f64.eq(left, right);
                    break;
                }
                case Type.OBJECT:
                case Type.ARRAY: {
                    final Type compareMethodTyoe = Type.getMethodType(
                            Type.BOOLEAN_TYPE,
                            Type.getType(Object.class),
                            Type.getType(Object.class)
                    );
                    final String methodName = WasmHelpers.generateMethodName("nullsafeEquals", compareMethodTyoe);
                    final String vmClassName = WasmHelpers.generateClassName(Type.getType(VM.class));

                    final String functionName = vmClassName + "$" + methodName;

                    final List<WasmValue> callArgs = new ArrayList<>();
                    callArgs.add(ConstExpressions.ref.nullRef());
                    callArgs.add(left);
                    callArgs.add(right);

                    condition = ConstExpressions.call(ConstExpressions.weakFunctionReference(functionName), callArgs);
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Not supported field type for equals generation " + fieldRef.type + " on " + fieldRef.resolvedField.name);
                }
            }

            final Iff check = hashCodeFunction.flow.iff("fcheck" + (i - 5), ConstExpressions.i32.ne(condition, ConstExpressions.i32.c(1)));
            check.flow.ret(ConstExpressions.i32.c(0));
        }

        hashCodeFunction.flow.ret(ConstExpressions.i32.c(1));

        return ConstExpressions.call(hashCodeFunction, arguments);
    }

    private WasmValue toWasmValue(final InvokeDynamicExpression value) {
        final ResolveCallsite resolveCallsite = (ResolveCallsite) value.incomingDataFlows[0];
        final BootstrapMethod bootstrapMethod = (BootstrapMethod) resolveCallsite.incomingDataFlows[0];
        if (bootstrapMethod.className.getClassName().equals(LambdaMetafactory.class.getName())) {
            if ("metafactory".equals(bootstrapMethod.methodName)) {
                return generateInvokeDynamicLambdaMetaFactoryInvocation(value, resolveCallsite);
            } else {
                throw new IllegalArgumentException("Not supported method " + bootstrapMethod.methodName + " on " + bootstrapMethod.className);
            }
        } else if (bootstrapMethod.className.getClassName().equals("java.lang.invoke.StringConcatFactory")) {
            if ("makeConcatWithConstants".equals(bootstrapMethod.methodName)) {
                return generateInvokeDynamicStringMakeConcatWithConstants(value, resolveCallsite);
            } else {
                throw new IllegalArgumentException("Not supported method " + bootstrapMethod.methodName + " on " + bootstrapMethod.className);
            }
        } else if (bootstrapMethod.className.getClassName().equals("java.lang.runtime.ObjectMethods")) {
            if ("bootstrap".equals(bootstrapMethod.methodName)) {
                final ObjectString operation = (ObjectString) resolveCallsite.incomingDataFlows[1];
                final String operationStr = compileUnit.getConstantPool().getPooledStrings().get(operation.value.index);
                if ("toString".equals(operationStr)) {
                    return generateInvokeDynamicObjectMethodsToString(value, resolveCallsite);
                } else if ("hashCode".equals(operationStr)) {
                    return ConstExpressions.i32.c(0);
                    //return generateInvokeDynamicObjectMethodsHashCode(value, resolveCallsite);
                } else if ("equals".equals(operationStr)) {
                    return generateInvokeDynamicObjectMethodsEquals(value, resolveCallsite);
                } else {
                    throw new IllegalArgumentException("Not supported operation " +operationStr + " on " + bootstrapMethod.methodName + " on " + bootstrapMethod.className);
                }
            } else {
                throw new IllegalArgumentException("Not supported method " + bootstrapMethod.methodName + " on " + bootstrapMethod.className);
            }
        } else {
            throw new IllegalArgumentException("Not supported bootstrap class : " + bootstrapMethod.className);
        }
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

    private WasmValue toWasmValue(final TypeReference value) {
        return toType(value.type);
    }

    private WasmValue toWasmValue(final ReadClassField value) {
        final ResolvedField field = value.resolvedField;
        final ResolvedClass cl = field.owner;
        final StructType type = rtMappings.get(cl);
        final String className = WasmHelpers.generateClassName(cl.type);
        if (cl.requiresClassInitializer()) {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            return ConstExpressions.struct.get(type,
                    ConstExpressions.ref.cast(type, ConstExpressions.call(initFunction, Collections.emptyList())),
                    field.name);
        }
        final Global global = module.getGlobals().globalsIndex().globalByLabel(className  + "_cls");
        return ConstExpressions.struct.get(type, ConstExpressions.getGlobal(global),
                    field.name);
    }

    private WasmValue toWasmValue(final RuntimeClass value) {
        final TypeReference typeReference = (TypeReference) value.incomingDataFlows[0];
        return toType(typeReference.type);
    }

    private WasmValue toWasmValue(final CaughtException value) {
        final Global g = module.getGlobals().globalsIndex().globalByLabel("lastcaughtexception");
        return ConstExpressions.getGlobal(g);
    }

    private WasmValue toWasmValue(final CMP value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        final List<WasmValue> arguments = new ArrayList<>();
        arguments.add(toWasmValue(left));
        arguments.add(toWasmValue(right));
        switch (left.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_i32"), arguments);
            case Type.FLOAT:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_f32"), arguments);
            case Type.LONG:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_i64"), arguments);
            case Type.DOUBLE:
                return ConstExpressions.call(ConstExpressions.weakFunctionReference("compare_f64"), arguments);
            default:
                throw new IllegalStateException("Not implemented compare for " + left.type);
        }
    }

    private WasmValue toWasmValue(final Mul value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.mul(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.mul(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.mul(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.mul(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented mul for " + left.type);
        }
    }

    private WasmValue toWasmValue(final SHR value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.shr_s(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.shr_s(toWasmValue(left), ConstExpressions.i64.extend_i32s(toWasmValue(right)));
            default:
                throw new IllegalStateException("Not implemented SHR for " + left.type);
        }
    }

    private WasmValue convertToType(final Value incoming, final Type targetType) {
        switch (incoming.type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                switch (targetType.getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        // Nothing to do
                        return toWasmValue(incoming);
                    case Type.FLOAT:
                        return ConstExpressions.f32.convert_si32(toWasmValue(incoming));
                    case Type.LONG:
                        return ConstExpressions.i64.extend_i32s(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return ConstExpressions.f64.convert_si32(toWasmValue(incoming));
                    default:
                        throw new IllegalStateException("Not implemented type conversion for " + incoming.type + " to " + targetType + " for node #" + graph.nodes().indexOf(incoming));
                }
            case Type.LONG:
                switch (targetType.getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        return ConstExpressions.i32.wrap_i64(toWasmValue(incoming));
                    case Type.LONG:
                        return toWasmValue(incoming);
                    case Type.FLOAT:
                        return ConstExpressions.f32.convert_si64(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return ConstExpressions.f64.convert_si64(toWasmValue(incoming));
                    default:
                        throw new IllegalStateException("Not implemented type conversion for " + incoming.type + " to " + targetType + " for node #" + graph.nodes().indexOf(incoming));
                }
            case Type.FLOAT:
                switch (targetType.getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        return ConstExpressions.i32.trunc_sf32(toWasmValue(incoming));
                    case Type.FLOAT:
                        return toWasmValue(incoming);
                    case Type.LONG:
                        return ConstExpressions.i64.trunc_sf32(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return ConstExpressions.f64.promote_f32(toWasmValue(incoming));
                    default:
                        throw new IllegalStateException("Not implemented type conversion for " + incoming.type + " to " + targetType + " for node #" + graph.nodes().indexOf(incoming));
                }
            case Type.DOUBLE:
                switch (targetType.getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.INT:
                        return ConstExpressions.i32.trunc_f64s(toWasmValue(incoming));
                    case Type.FLOAT:
                        return ConstExpressions.f32.demote_f64(toWasmValue(incoming));
                    case Type.LONG:
                        return ConstExpressions.i64.trunc_sf64(toWasmValue(incoming));
                    case Type.DOUBLE:
                        return toWasmValue(incoming);
                    default:
                        throw new IllegalStateException("Not implemented type conversion for " + incoming.type + " to " + targetType + " for node #" + graph.nodes().indexOf(incoming));
                }
            default:
                throw new IllegalStateException("Not implemented type conversion for " + incoming.type + " to " + targetType + " for node #" + graph.nodes().indexOf(incoming));
        }
    }

    private WasmValue toWasmValue(final TypeConversion value) {
        final Value incoming = (Value) value.incomingDataFlows[0];
        return convertToType(incoming, value.type);
    }

    private WasmValue toWasmValue(final NewArray value) {
        final Type arrayType = value.type;
        final Type elementType = arrayType.getElementType();
        final Value length = (Value) value.incomingDataFlows[0];

        final String typeToInstantiate;
        final WasmValue emptyArray;
        switch (elementType.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.BOOLEAN:
            case Type.INT:
                typeToInstantiate = "i32_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.i32), toWasmValue(length));
                break;
            case Type.LONG:
                typeToInstantiate = "i64_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.i64), toWasmValue(length));
                break;
            case Type.FLOAT:
                typeToInstantiate = "f32_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.f32), toWasmValue(length));
                break;
            case Type.DOUBLE:
                typeToInstantiate = "f64_array";
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(PrimitiveType.f64), toWasmValue(length));
                break;
            case Type.OBJECT:
                typeToInstantiate = "obj_array";
                final StructType runtimeObject = module.getTypes().structTypeByName("java$lang$Object");
                emptyArray = ConstExpressions.array.newInstanceDefault(module.getTypes().arrayType(ConstExpressions.ref.type(runtimeObject, true)), toWasmValue(length));
                break;
            default:
                throw new IllegalArgumentException("Not supported array type " + elementType);
        }

        final StructType structType = module.getTypes().structTypeByName(typeToInstantiate);
        final List<WasmValue> initArguments = new ArrayList<>();
        final Type arrayClass = Type.getType(Array.class);

        final ResolvedClass arrayCls = compileUnit.findClass(arrayClass);
        final String arrayClsName = WasmHelpers.generateClassName(arrayCls.type);
        final Global global = module.getGlobals().globalsIndex().globalByLabel(arrayClsName  + "_cls");

        initArguments.add(ConstExpressions.i32.c(resolvedClasses.indexOf(arrayCls)));
        initArguments.add(ConstExpressions.ref.ref(module.functionIndex().firstByLabel(WasmHelpers.generateClassName(arrayClass) + "_vt")));
        initArguments.add(ConstExpressions.ref.externNullRef());
        initArguments.add(ConstExpressions.struct.get(
                rtMappings.get(arrayCls),
                ConstExpressions.getGlobal(global),
                "implTypes"
        ));

        initArguments.add(emptyArray);
        return ConstExpressions.struct.newInstance(structType, initArguments);
    }

    private WasmValue toWasmValue(final And value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.and(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.and(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented and for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Or value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.or(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.or(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented and for " + value.type);
        }
    }

    private WasmValue toWasmValue(final InstanceOf value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final TypeReference right = (TypeReference) value.incomingDataFlows[1];
        return toInstanceOfCheck(toWasmValue(left), right.type);
    }

    private WasmValue toInstanceOfCheck(final WasmValue value, final Type typeToCheck) {
        final List<WasmValue> params = new ArrayList<>();
        params.add(value);
        final ResolvedClass questionType = compileUnit.findClass(typeToCheck);
        params.add(ConstExpressions.i32.c(resolvedClasses.indexOf(questionType)));
        return ConstExpressions.call(ConstExpressions.weakFunctionReference("instanceOf"), params);
    }

    private WasmValue toWasmValue(final Sub value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.sub(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.sub(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.sub(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.sub(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented sub for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Div value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.div_s(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.div(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.div_s(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.div(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented div for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Add value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.add(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT:
                return ConstExpressions.f32.add(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.add(toWasmValue(left), toWasmValue(right));
            case Type.DOUBLE:
                return ConstExpressions.f64.add(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented add for " + value.type);
        }
    }

    private WasmValue toWasmValue(final ArrayLength value) {
        final Value array = (Value) value.incomingDataFlows[0];
        if (array.type.getDimensions() > 1) {
            final StructType type = module.getTypes().structTypeByName("obj_array");
            return ConstExpressions.array.len(
                    module.getTypes().arrayType(typeConverter.apply(Type.getType(Object.class))),
                    ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
            );
        }
        switch (array.type.getElementType().getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: {
                final StructType type = module.getTypes().structTypeByName("i32_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.i32),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.FLOAT: {
                final StructType type = module.getTypes().structTypeByName("f32_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.f32),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.LONG: {
                final StructType type = module.getTypes().structTypeByName("i64_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.i64),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.DOUBLE: {
                final StructType type = module.getTypes().structTypeByName("f64_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(PrimitiveType.f64),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            case Type.OBJECT:
            case Type.ARRAY: {
                final StructType type = module.getTypes().structTypeByName("obj_array");
                return ConstExpressions.array.len(
                        module.getTypes().arrayType(typeConverter.apply(Type.getType(Object.class))),
                        ConstExpressions.struct.get(type, ConstExpressions.ref.cast(type, toWasmValue(array)), "data")
                );
            }
            default:
                throw new IllegalStateException("Not implemented arraylength for " + array.type + " sort " + array.type.getElementType().getSort());
        }
    }

    private WasmValue toWasmValue(final SHL value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.shl(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.shl(toWasmValue(left), ConstExpressions.i64.extend_i32s(toWasmValue(right)));
            default:
                throw new IllegalStateException("Not implemented shl for " + value.type);
        }
    }

    private WasmValue toWasmValue(final USHR value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.shr_u(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.shr_u(toWasmValue(left), ConstExpressions.i64.extend_i32s(toWasmValue(right)));
            default:
                throw new IllegalStateException("Not implemented ushr for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Neg value) {
        final Value left = (Value) value.incomingDataFlows[0];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.sub(ConstExpressions.i32.c(0), toWasmValue(left));
            case Type.LONG:
                return ConstExpressions.i64.sub(ConstExpressions.i64.c(0L), toWasmValue(left));
            case Type.FLOAT:
                return ConstExpressions.f32.neg(toWasmValue(left));
            case Type.DOUBLE:
                return ConstExpressions.f64.neg(toWasmValue(left));
            default:
                throw new IllegalStateException("Not implemented neg for " + value.type);
        }
    }

    private WasmValue toWasmValue(final Rem value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (left.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.rem_s(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.rem_s(toWasmValue(left), toWasmValue(right));
            case Type.FLOAT: {
                final WasmValue l = toWasmValue(left);
                final WasmValue r = toWasmValue(right);
                return ConstExpressions.f32.sub(l, ConstExpressions.f32.mul(r, ConstExpressions.f32.trunc(ConstExpressions.f32.div(l, r))));
            }
            case Type.DOUBLE: {
                final WasmValue l = toWasmValue(left);
                final WasmValue r = toWasmValue(right);
                return ConstExpressions.f64.sub(l, ConstExpressions.f64.mul(r, ConstExpressions.f64.trunc(ConstExpressions.f32.div(l, r))));
            }
            default:
                throw new IllegalStateException("Not implemented rem for " + left.type);
        }
    }

    private WasmValue toWasmValue(final Cast value) {
        final Type targetType = value.type;
        if (targetType.getSort() == Type.OBJECT) {
            final ResolvedClass targetClass = compileUnit.findClass(targetType);
            if (targetClass.isOpaqueReferenceType()) {
                // We need to create a new instance
                return createNewInstanceOf(
                        targetType,
                        module,
                        compileUnit,
                        objectTypeMappings,
                        rtMappings,
                        ConstExpressions.struct.get(
                                objectTypeMappings.get(compileUnit.findClass(Type.getType(Object.class))),
                                toWasmValue((Value) value.incomingDataFlows[0]),
                                "nativeObject"
                        )
                );
            }
        }
        final RefType refType = (RefType) typeConverter.apply(value.type);
        return ConstExpressions.ref.cast((StructType) refType.getType(), toWasmValue((Value) value.incomingDataFlows[0]));
    }

    private WasmValue toWasmValue(final RuntimeClassOf value) {
        final List<WasmValue> arguments = new ArrayList<>();
        arguments.add(toWasmValue((Value) value.incomingDataFlows[0]));
        return ConstExpressions.call(ConstExpressions.weakFunctionReference("runtimetypeof"), arguments);
    }

    private WasmValue toWasmValue(final EnumValuesOf value) {
        final StructType t = module.getTypes().structTypeByName(WasmHelpers
                .generateClassName(Type.getType(Class.class)) + "_rtt");
        final StructType objectArray = module.getTypes().structTypeByName("obj_array");
        return ConstExpressions.ref.cast(objectArray,ConstExpressions.struct.get(
                t,
                ConstExpressions.ref.cast(t, toWasmValue((Value) value.incomingDataFlows[0])),
                "$VALUES"
        ));
    }

    private WasmValue toWasmValue(final Reinterpret value) {
        final Value v = (Value) value.incomingDataFlows[0];
        switch (value.type.getSort()) {
            case Type.INT: {
                switch (v.type.getSort()) {
                    case Type.FLOAT: {
                        return ConstExpressions.i32.reinterpretf32(toWasmValue(v));
                    }
                    default: {
                        throw new IllegalArgumentException("Cannot reinterpret to int : " + v.type);
                    }
                }
            }
            case Type.LONG: {
                switch (v.type.getSort()) {
                    case Type.DOUBLE: {
                        return ConstExpressions.i64.reinterpretf64(toWasmValue(v));
                    }
                    default: {
                        throw new IllegalArgumentException("Cannot reinterpret to int : " + v.type);
                    }
                }
            }
            case Type.FLOAT: {
                switch (v.type.getSort()) {
                    case Type.INT: {
                        return ConstExpressions.f32.reinterpreti32(toWasmValue(v));
                    }
                    default: {
                        throw new IllegalArgumentException("Cannot reinterpret to int : " + v.type);
                    }
                }
            }
            case Type.DOUBLE: {
                switch (v.type.getSort()) {
                    case Type.LONG: {
                        return ConstExpressions.f64.reinterpreti64(toWasmValue(v));
                    }
                    default: {
                        throw new IllegalArgumentException("Cannot reinterpret to int : " + v.type);
                    }
                }
            }
            default: {
                throw new IllegalArgumentException("Cannot reinterpret to " + value.type);
            }
        }
    }

    private WasmValue toWasmValue(final PrimitiveClassReference reference) {
        switch (reference.referenceType.getSort()) {
            case Type.BOOLEAN:
                return ConstExpressions.getGlobal(
                    module.getGlobals().globalsIndex().globalByLabel("primitive_boolean")
                );
            case Type.BYTE:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_byte")
                );
            case Type.CHAR:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_char")
                );
            case Type.SHORT:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_short")
                );
            case Type.INT:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_int")
                );
            case Type.LONG:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_long")
                );
            case Type.FLOAT:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_float")
                );
            case Type.DOUBLE:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_double")
                );
            case Type.VOID:
                return ConstExpressions.getGlobal(
                        module.getGlobals().globalsIndex().globalByLabel("primitive_void")
                );
            default:
                throw new IllegalArgumentException("Not supported primitive class for " + reference.type);
        }
    }

    private WasmValue toWasmValue(final ReferenceTest value) {
        switch (value.operation) {
            case NE:
                // We need to reverse the isnull condition here! Take care
                return ConstExpressions.select(ConstExpressions.i32.c(0), ConstExpressions.i32.c(1), ConstExpressions.ref.eq(toWasmValue((Value) value.incomingDataFlows[0]), toWasmValue((Value) value.incomingDataFlows[1])));
            case EQ:
                return ConstExpressions.ref.eq(toWasmValue((Value) value.incomingDataFlows[0]), toWasmValue((Value) value.incomingDataFlows[1]));
            default:
                throw new IllegalArgumentException("Unsupported operation : " + value.operation);
        }
    }

    private WasmValue toWasmValue(final NullTest value) {
        switch (value.operation) {
            case NOTNULL:
                // We need to reverse the isnull condition here! Take care
                return ConstExpressions.select(ConstExpressions.i32.c(0), ConstExpressions.i32.c(1), ConstExpressions.ref.isnull(toWasmValue((Value) value.incomingDataFlows[0])));
            case NULL:
                return ConstExpressions.ref.isnull(toWasmValue((Value) value.incomingDataFlows[0]));
            default:
                throw new IllegalArgumentException("Unsupported operation : " + value.operation);
        }
    }

    private WasmValue toWasmValue(final NumericalTest value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];

        final WasmValue leftWasm = toWasmValue(left);
        final WasmValue rightWasm = toWasmValue(right);

        switch (value.operation) {
            case EQ: {
                switch (left.type.getSort()) {
                    case Type.FLOAT:
                        return ConstExpressions.f32.eq(leftWasm, rightWasm);
                    case Type.LONG:
                        return ConstExpressions.i64.eq(leftWasm, rightWasm);
                    case Type.DOUBLE:
                        return ConstExpressions.f64.eq(leftWasm, rightWasm);
                    default:
                        return ConstExpressions.i32.eq(leftWasm, rightWasm);
                }
            }
            case GE: {
                switch (left.type.getSort()) {
                    case Type.FLOAT:
                        return ConstExpressions.f32.ge(leftWasm, rightWasm);
                    case Type.LONG:
                        return ConstExpressions.i64.ge_s(leftWasm, rightWasm);
                    case Type.DOUBLE:
                        return ConstExpressions.f64.ge(leftWasm, rightWasm);
                    default:
                        return ConstExpressions.i32.ge_s(leftWasm, rightWasm);
                }
            }
            case NE: {
                switch (left.type.getSort()) {
                    case Type.FLOAT:
                        return ConstExpressions.f32.ne(leftWasm, rightWasm);
                    case Type.LONG:
                        return ConstExpressions.i64.ne(leftWasm, rightWasm);
                    case Type.DOUBLE:
                        return ConstExpressions.f64.ne(leftWasm, rightWasm);
                    default:
                        return ConstExpressions.i32.ne(leftWasm, rightWasm);
                }
            }
            case GT: {
                switch (left.type.getSort()) {
                    case Type.FLOAT:
                        return ConstExpressions.f32.gt(leftWasm, rightWasm);
                    case Type.LONG:
                        return ConstExpressions.i64.gt_s(leftWasm, rightWasm);
                    case Type.DOUBLE:
                        return ConstExpressions.f64.gt(leftWasm, rightWasm);
                    default:
                        return ConstExpressions.i32.gt_s(leftWasm, rightWasm);
                }
            }
            case LE: {
                switch (left.type.getSort()) {
                    case Type.FLOAT:
                        return ConstExpressions.f32.le(leftWasm, rightWasm);
                    case Type.LONG:
                        return ConstExpressions.i64.le_s(leftWasm, rightWasm);
                    case Type.DOUBLE:
                        return ConstExpressions.f64.le(leftWasm, rightWasm);
                    default:
                        return ConstExpressions.i32.le_s(leftWasm, rightWasm);
                }
            }
            case LT: {
                switch (left.type.getSort()) {
                    case Type.FLOAT:
                        return ConstExpressions.f32.lt(leftWasm, rightWasm);
                    case Type.LONG:
                        return ConstExpressions.i64.lt_s(leftWasm, rightWasm);
                    case Type.DOUBLE:
                        return ConstExpressions.f64.lt(leftWasm, rightWasm);
                    default:
                        return ConstExpressions.i32.lt_s(leftWasm, rightWasm);
                }
            }
            default:
                throw new IllegalArgumentException("Not supported operation : " + value.operation);
        }
    }

    private WasmValue toWasmValue(final XOr value) {
        final Value left = (Value) value.incomingDataFlows[0];
        final Value right = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return ConstExpressions.i32.xor(toWasmValue(left), toWasmValue(right));
            case Type.LONG:
                return ConstExpressions.i64.xor(toWasmValue(left), toWasmValue(right));
            default:
                throw new IllegalStateException("Not implemented xor for " + value.type);
        }
    }

    private WasmValue toWasmValue(final ArrayLoad value) {
        final Value array = (Value) value.incomingDataFlows[0];
        final Value index = (Value) value.incomingDataFlows[1];
        switch (value.type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: {
                final StructType arrayType = module.getTypes().structTypeByName("i32_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.i32),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.FLOAT: {
                final StructType arrayType = module.getTypes().structTypeByName("f32_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.f32),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.LONG: {
                final StructType arrayType = module.getTypes().structTypeByName("i64_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.i64),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.DOUBLE: {
                final StructType arrayType = module.getTypes().structTypeByName("f64_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(PrimitiveType.f64),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            case Type.ARRAY:
            case Type.OBJECT: {
                final StructType arrayType = module.getTypes().structTypeByName("obj_array");
                return ConstExpressions.array.get(
                        module.getTypes().arrayType(typeConverter.apply(Type.getType(Object.class))),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index)
                );
            }
            default:
                throw new IllegalStateException("Not implemented arrayload for " + value.type + " sort " + value.type.getSort());
        }
    }

    private WasmValue toWasmValue(final Value value) {
        if (value instanceof This) {
            return toWasmValue((This) value);
        } else if (value instanceof ObjectString) {
            return toWasmValue((ObjectString) value);
        } else if (value instanceof PrimitiveShort) {
            return toWasmValue((PrimitiveShort) value);
        } else if (value instanceof PrimitiveInt) {
            return toWasmValue((PrimitiveInt) value);
        } else if (value instanceof PrimitiveLong) {
            return toWasmValue((PrimitiveLong) value);
        } else if (value instanceof PrimitiveFloat) {
            return toWasmValue((PrimitiveFloat) value);
        } else if (value instanceof PrimitiveDouble) {
            return toWasmValue((PrimitiveDouble) value);
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
        } else if (value instanceof VirtualMethodInvocationExpression) {
            return toWasmValue((VirtualMethodInvocationExpression) value);
        } else if (value instanceof InterfaceMethodInvocationExpression) {
            return toWasmValue((InterfaceMethodInvocationExpression) value);
        } else if (value instanceof InstanceMethodInvocationExpression) {
            return toWasmValue((InstanceMethodInvocationExpression) value);
        } else if (value instanceof InvokeDynamicExpression) {
            return toWasmValue((InvokeDynamicExpression) value);
        } else if (value instanceof TypeReference) {
            return toWasmValue((TypeReference) value);
        } else if (value instanceof ReadClassField) {
            return toWasmValue((ReadClassField) value);
        } else if (value instanceof RuntimeClass) {
            return toWasmValue((RuntimeClass) value);
        } else if (value instanceof CaughtException) {
            return toWasmValue((CaughtException) value);
        } else if (value instanceof NewArray) {
            return toWasmValue((NewArray) value);
        } else if (value instanceof CMP) {
            return toWasmValue((CMP) value);
        } else if (value instanceof Mul) {
            return toWasmValue((Mul) value);
        } else if (value instanceof SHR) {
            return toWasmValue((SHR) value);
        } else if (value instanceof TypeConversion) {
            return toWasmValue((TypeConversion) value);
        } else if (value instanceof And) {
            return toWasmValue((And) value);
        } else if (value instanceof Sub) {
            return toWasmValue((Sub) value);
        } else if (value instanceof ArrayLoad) {
            return toWasmValue((ArrayLoad) value);
        } else if (value instanceof Div) {
            return toWasmValue((Div) value);
        } else if (value instanceof SHL) {
            return toWasmValue((SHL) value);
        } else if (value instanceof XOr) {
            return toWasmValue((XOr) value);
        } else if (value instanceof Add) {
            return toWasmValue((Add) value);
        } else if (value instanceof ArrayLength) {
            return toWasmValue((ArrayLength) value);
        } else if (value instanceof Or) {
            return toWasmValue((Or) value);
        } else if (value instanceof InstanceOf) {
            return toWasmValue((InstanceOf) value);
        } else if (value instanceof USHR) {
            return toWasmValue((USHR) value);
        } else if (value instanceof Neg) {
            return toWasmValue((Neg) value);
        } else if (value instanceof Cast) {
            return toWasmValue((Cast) value);
        } else if (value instanceof ReferenceTest) {
            return toWasmValue((ReferenceTest) value);
        } else if (value instanceof NullTest) {
            return toWasmValue((NullTest) value);
        } else if (value instanceof NumericalTest) {
            return toWasmValue((NumericalTest) value);
        } else if (value instanceof Rem) {
            return toWasmValue((Rem) value);
        } else if (value instanceof PrimitiveClassReference) {
            return toWasmValue((PrimitiveClassReference) value);
        } else if (value instanceof RuntimeClassOf) {
            return toWasmValue((RuntimeClassOf) value);
        } else if (value instanceof EnumValuesOf) {
            return toWasmValue((EnumValuesOf) value);
        } else if (value instanceof Reinterpret) {
            return toWasmValue((Reinterpret) value);
        }
        throw new IllegalArgumentException("Not implemented " + value.getClass());
    }

    @Override
    public void write(final Copy node) {
        final Value value = (Value) node.incomingDataFlows[0];
        final Node target = node.outgoingFlows[0];
        if (target instanceof AbstractVar) {
            final AbstractVar targetVar = (AbstractVar) target;
            final Local local = varLocalMap.get(targetVar);
            if (local == null) {
                throw new IllegalArgumentException("Cannot find Wasm local for variable " + target);
            }
            if (targetVar.type.getSort() != value.type.getSort() && targetVar.type.getSort() != Type.OBJECT && targetVar.type.getSort() != Type.ARRAY) {
                activeLevel.activeFlow.setLocal(local, convertToType(value, targetVar.type));
            } else {
                if (value.type.getSort() == targetVar.type.getSort()) {
                    activeLevel.activeFlow.setLocal(local, toWasmValue(value));
                } else {
                    activeLevel.activeFlow.comment("Unable to assign " + value.type + " to " + targetVar.type + " for " + targetVar +" from " + value);
                }
            }
        } else {
            activeLevel.activeFlow.comment("Copy from " + value.getClass() + " to " + target.getClass());
        }
    }

    int ifcounter;

    @Override
    public void startIfWithTrueBlock(final If node) {

        activeLevel.writeDebug("writeIfAndStartTrueBlock");

        final Iff iff = activeLevel.activeFlow.iff("if" + ifcounter ++, toWasmValue((Value) node.incomingDataFlows[0]));

        activeLevel = new NestingLevelIff(activeLevel, iff.flow, iff);
    }

    @Override
    public void startIfElseBlock(final If node) {

        activeLevel.writeDebug("Start else");

        if (!(activeLevel instanceof NestingLevelIff)) {
            throw new IllegalArgumentException("Active container is not an If, got " + activeLevel);
        }

        final NestingLevelIff nestingLevelIff = (NestingLevelIff) activeLevel;
        nestingLevelIff.activeFlow = nestingLevelIff.activeContainer.falseFlow;
    }

    @Override
    public void finishIfBlock() {

        activeLevel = activeLevel.parent;

        activeLevel.writeDebug("finishIfBlock");
    }

    @Override
    public void startBlock(final Sequencer.Block node) {

        activeLevel.writeDebug("startBlock type=" + node.type + ", label = " + node.label);

        switch (node.type) {
            case LOOP: {
                final Loop l = activeLevel.activeFlow.loop(node.label);
                activeLevel = new NestingLevelLoop(activeLevel, l.flow, l);
                break;
            }
            case NORMAL: {
                final Block b = activeLevel.activeFlow.block(node.label);
                activeLevel = new NestingLevelBlock(activeLevel, b.flow, b);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not supported block type " + node.type);
            }
        }
    }

    @Override
    public void finishBlock(final Sequencer.Block node, final boolean stackEmpty) {

        activeLevel = activeLevel.parent;

        activeLevel.writeDebug("finishBlock");
    }

    @Override
    public void write(final LineNumberDebugInfo node) {
        activeLevel.activeFlow.comment("Line number " + node.lineNumber);
    }

    @Override
    public void write(final FrameDebugInfo node) {
    }

    @Override
    public void write(final Goto node) {
        activeLevel.activeFlow.comment("Here was a goto statement");
    }

    @Override
    public void write(final MonitorEnter node) {
        activeLevel.activeFlow.comment("Monitor enter on " + node.incomingDataFlows[0]);
    }

    @Override
    public void write(final MonitorExit node) {
        activeLevel.activeFlow.comment("Monitor exit on " + node.incomingDataFlows[0]);
    }

    @Override
    public void write(final Unwind node) {

        final List<WasmValue> throwArguments = new ArrayList<>();
        throwArguments.add(toWasmValue((Value) node.incomingDataFlows[0]));

        activeLevel.activeFlow.throwException(
                module.getTags().tagIndex().byLabel("javaexception"),
                throwArguments
        );
    }

    @Override
    public void write(final Return node) {
        activeLevel.activeFlow.ret();
    }

    @Override
    public void write(final ReturnValue node) {
        activeLevel.activeFlow.ret(toWasmValue((Value) node.incomingDataFlows[0]));
    }

    @Override
    public void write(final SetInstanceField node) {
        final ResolvedField field = node.field;
        final StructType structType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(field.owner.type));
        switch (field.type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY: {
                final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));
                activeLevel.activeFlow.setStruct(
                        structType,
                        ConstExpressions.ref.cast(structType, toWasmValue((Value) node.outgoingFlows[0])),
                        WasmHelpers.generateFieldName(field.name),
                        ConstExpressions.ref.cast(objectType, toWasmValue((Value) node.incomingDataFlows[0]))
                );
                break;
            }
            default: {
                activeLevel.activeFlow.setStruct(
                        structType,
                        ConstExpressions.ref.cast(structType, toWasmValue((Value) node.outgoingFlows[0])),
                        WasmHelpers.generateFieldName(field.name),
                        toWasmValue((Value) node.incomingDataFlows[0])
                );
                break;
            }
        }
    }

    @Override
    public void write(final SetClassField node) {

        final ResolvedField field = node.field;
        final ResolvedClass cl = field.owner;
        final StructType structType = rtMappings.get(cl);

        switch (field.type.getSort()) {
            case Type.ARRAY:
            case Type.OBJECT: {
                final StructType objectType = module.getTypes().structTypeByName(WasmHelpers.generateClassName(Type.getType(Object.class)));
                activeLevel.activeFlow.setStruct(
                        structType,
                        ConstExpressions.ref.cast(structType, toWasmValue((Value) node.outgoingFlows[0])),
                        WasmHelpers.generateFieldName(field.name),
                        ConstExpressions.ref.cast(objectType, toWasmValue((Value) node.incomingDataFlows[0]))
                );
                break;
            }
            default: {
                activeLevel.activeFlow.setStruct(
                        structType,
                        ConstExpressions.ref.cast(structType, toWasmValue((Value) node.outgoingFlows[0])),
                        WasmHelpers.generateFieldName(field.name),
                        toWasmValue((Value) node.incomingDataFlows[0])
                );
                break;
            }
        }
    }

    @Override
    public void write(final ArrayStore node) {

        final Value array = (Value) node.incomingDataFlows[0];
        final Value index = (Value) node.incomingDataFlows[1];
        final Value value = (Value) node.incomingDataFlows[2];

        switch (value.type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT: {
                final StructType arrayType = module.getTypes().structTypeByName("i32_array");
                activeLevel.activeFlow.array.set(
                        module.getTypes().arrayType(PrimitiveType.i32),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index),
                        toWasmValue(value)
                );
                break;
            }
            case Type.FLOAT: {
                final StructType arrayType = module.getTypes().structTypeByName("f32_array");
                activeLevel.activeFlow.array.set(
                        module.getTypes().arrayType(PrimitiveType.f32),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index),
                        toWasmValue(value)
                );
                break;
            }
            case Type.LONG: {
                final StructType arrayType = module.getTypes().structTypeByName("i64_array");
                activeLevel.activeFlow.array.set(
                        module.getTypes().arrayType(PrimitiveType.i64),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index),
                        toWasmValue(value)
                );
                break;
            }
            case Type.DOUBLE: {
                final StructType arrayType = module.getTypes().structTypeByName("f64_array");
                activeLevel.activeFlow.array.set(
                        module.getTypes().arrayType(PrimitiveType.f64),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index),
                        toWasmValue(value)
                );
                break;
            }
            case Type.ARRAY:
            case Type.OBJECT: {
                final StructType arrayType = module.getTypes().structTypeByName("obj_array");
                activeLevel.activeFlow.array.set(
                        module.getTypes().arrayType(typeConverter.apply(Type.getType(Object.class))),
                        ConstExpressions.struct.get(arrayType, ConstExpressions.ref.cast(arrayType, toWasmValue(array)), "data"),
                        toWasmValue(index),
                        toWasmValue(value)
                );
                break;
            }
            default:
                throw new IllegalStateException("Not implemented arraystore for " + value.type + " sort " + value.type.getSort());
        }
    }

    @Override
    public void writeBreakTo(final String label) {
        activeLevel.activeFlow.branch(activeLevel.findByLabelInHierarchy(label));
    }

    @Override
    public void writeContinueTo(final String label) {
        activeLevel.activeFlow.branch(activeLevel.findByLabelInHierarchy(label));
    }

    @Override
    public void startTryCatch(final String label) {

        activeLevel.writeDebug("startTryCatch");

        final Try t = activeLevel.activeFlow.Try(label, module.tagIndex().byLabel("javaexception"));
        activeLevel = new NestingLevelTry(activeLevel, t.flow, t);
    }

    @Override
    public void startCatchBlock() {

        if (!(activeLevel instanceof NestingLevelTry)) {
            throw new IllegalArgumentException("Active container is not a try, got " + activeLevel);
        }

        final NestingLevelTry t = (NestingLevelTry) activeLevel;
        t.activeFlow = t.activeContainer.catchBlock.flow;

        activeLevel.writeDebug("startCatchBlock");

        final Global g = module.getGlobals().globalsIndex().globalByLabel("lastcaughtexception");
        t.activeFlow.setGlobal(g, ConstExpressions.pop(g.getType()));
    }

    int catchcheckcount = 0;

    @Override
    public void startCatchHandler(final Type type) {

        if (!(activeLevel instanceof NestingLevelTry)) {
            throw new IllegalArgumentException("Active container is not a try, got " + activeLevel);
        }

        final NestingLevelTry t = (NestingLevelTry) activeLevel;

        final String className = WasmHelpers.generateClassName(type);

        final Global g = module.getGlobals().globalsIndex().globalByLabel("lastcaughtexception");

        final List<WasmValue> callArguments = new ArrayList<>();
        callArguments.add(ConstExpressions.getGlobal(g));

        final ResolvedClass cl = compileUnit.findClass(type);
        final WasmValue typeToCheck;
        if (!cl.requiresClassInitializer()) {
            final Global cls = module.getGlobals().globalsIndex().globalByLabel(className + "_cls");
            typeToCheck = ConstExpressions.getGlobal(cls);
        } else {
            final Callable initFunction = ConstExpressions.weakFunctionReference(className + "_i");
            typeToCheck = ConstExpressions.call(initFunction, Collections.emptyList());
        }

        callArguments.add(
                ConstExpressions.struct.get(
                        rtMappings.get(cl),
                        typeToCheck,
                        "factoryFor"
                )
        );

        final Iff check = t.activeContainer.catchBlock.flow.iff("catchcheck_" + catchcheckcount++,
                ConstExpressions.call(ConstExpressions.weakFunctionReference("instanceOf"), callArguments));
        t.activeFlow = check.flow;
    }

    @Override
    public void finishCatchHandler() {

        if (!(activeLevel instanceof NestingLevelTry)) {
            throw new IllegalArgumentException("Active container is not a try, got " + activeLevel);
        }

        final NestingLevelTry t = (NestingLevelTry) activeLevel;
        t.activeFlow = t.activeContainer.catchBlock.flow;
    }

    @Override
    public void writeRethrowException() {
        NestingLevel current = activeLevel;
        while (!(current instanceof NestingLevelTry) && current != null) {
            current = current.parent;
        }
        if (current == null) {
            throw new IllegalStateException("Could not find enclosing try block!");
        }
        activeLevel.activeFlow.rethrowException(((NestingLevelTry) current).activeContainer);
    }

    @Override
    public void finishTryCatch() {
        activeLevel = activeLevel.parent;

        activeLevel.writeDebug("finishTryCatch");
    }

    int tableSwitchCount = 0;

    @Override
    public void startTableSwitch(final TableSwitch node) {

        activeLevel.writeDebug("writeTableSwitch");

        final WasmValue valueToCheck = toWasmValue((Value) node.incomingDataFlows[0]);
        final WasmValue minCheck = ConstExpressions.i32.ge_s(valueToCheck, ConstExpressions.i32.c(node.min));
        final WasmValue maxCheck = ConstExpressions.i32.le_s(valueToCheck, ConstExpressions.i32.c(node.max));

        final int switchNum = tableSwitchCount++;

        final Block outer = activeLevel.activeFlow.block("tableswitch_outer" +  switchNum);
        final Local calc = exportableFunction.newLocal("tableswitch_idx" + switchNum, PrimitiveType.i32);
        outer.flow.setLocal(calc, ConstExpressions.i32.sub(valueToCheck, ConstExpressions.i32.c(node.min)));
        final Block inner = outer.flow.block("tableswitch_inner" + switchNum);
        final Iff min = inner.flow.iff("tableswitch_min" + switchNum, minCheck);
        min.falseFlow.branch(inner);
        final Iff max = min.flow.iff("tableswitch_max" + switchNum, maxCheck);
        max.falseFlow.branch(inner);

        activeLevel = new NestingLevelSwitch(activeLevel, max.flow, min, ConstExpressions.getLocal(calc));
    }

    @Override
    public void finishTableSwitch() {

        if (!(activeLevel instanceof NestingLevelSwitch)) {
            throw new IllegalArgumentException("Active container is not a Switch, got " + activeLevel);
        }

        activeLevel = activeLevel.parent;

        activeLevel.writeDebug("finishTableSwitch");
    }

    @Override
    public void startTableSwitchDefaultBlock() {

        if (!(activeLevel instanceof NestingLevelSwitch)) {
            throw new IllegalArgumentException("Active container is not a Switch, got " + activeLevel);
        }

        final NestingLevelSwitch level = (NestingLevelSwitch) activeLevel;
        level.activeFlow = level.activeFlow.parent().parent().parent().parent().flow;

        activeLevel.writeDebug("startTableSwitchDefaultBlock");
    }

    @Override
    public void finishTableSwitchDefaultBlock() {

        if (!(activeLevel instanceof NestingLevelSwitch)) {
            throw new IllegalArgumentException("Active container is not a Switch, got " + activeLevel);
        }

        activeLevel.writeDebug("finishTableSwitchDefaultBlock");
    }

    int lookupSwitchCount = 0;

    @Override
    public void startLookupSwitch(final LookupSwitch node) {

        activeLevel.writeDebug("writeLookupSwitch");

        final WasmValue valueToCheck = toWasmValue((Value) node.incomingDataFlows[0]);

        final Block bouter = activeLevel.activeFlow.block("lookupswitchb_" + lookupSwitchCount++);
        final Block binner = bouter.flow.block("lookupswitchbi_" + lookupSwitchCount++);

        activeLevel = new NestingLevelSwitch(activeLevel, binner.flow, bouter, valueToCheck);
    }

    @Override
    public void finishLookupSwitch() {

        if (!(activeLevel instanceof NestingLevelSwitch)) {
            throw new IllegalArgumentException("Active container is not a Switch, got " + activeLevel);
        }

        activeLevel = activeLevel.parent;

        activeLevel.writeDebug("finishLookupSwitch");
    }


    int checkcounter = 0;

    @Override
    public void writeSwitchCase(final int index) {

        activeLevel.writeDebug("writeSwitchCase " + index);

        if (!(activeLevel instanceof NestingLevelSwitch)) {
            throw new IllegalArgumentException("Active container is not a Switch, got " + activeLevel);
        }

        final NestingLevelSwitch level = (NestingLevelSwitch) activeLevel;

        final Iff check = activeLevel.activeFlow.iff("casecheck" + checkcounter++, ConstExpressions.i32.eq(level.valueToCheck, ConstExpressions.i32.c(index)));

        activeLevel = new NestingLevelIff(activeLevel, check.flow, check);
    }

    @Override
    public void writeSwitchDefaultCase() {

        if (!(activeLevel instanceof NestingLevelSwitch)) {
            throw new IllegalArgumentException("Active container is not a Switch, got " + activeLevel);
        }

        activeLevel.writeDebug("writeSwitchDefaultCase");

        // TODO: Branch from inner to outer

        activeLevel.activeFlow = activeLevel.activeContainer.flow;
    }

    @Override
    public void finishSwitchDefault() {

        if (!(activeLevel instanceof NestingLevelSwitch)) {
            throw new IllegalArgumentException("Active container is not a Switch, got " + activeLevel);
        }

        activeLevel.writeDebug("finishSwitchDefault");
    }

    @Override
    public void finishSwitchCase() {

        activeLevel = activeLevel.parent;

        activeLevel.writeDebug("finishSwitchCase");
    }
}
