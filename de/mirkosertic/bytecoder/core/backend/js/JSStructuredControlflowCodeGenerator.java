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
package de.mirkosertic.bytecoder.core.backend.js;

import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethod;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethodsRegistry;
import de.mirkosertic.bytecoder.core.backend.OpaqueReferenceTypeHelpers;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.backend.sequencer.StructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.core.ir.AbstractVar;
import de.mirkosertic.bytecoder.core.ir.Add;
import de.mirkosertic.bytecoder.core.ir.And;
import de.mirkosertic.bytecoder.core.ir.AnnotationUtils;
import de.mirkosertic.bytecoder.core.ir.ArrayLength;
import de.mirkosertic.bytecoder.core.ir.ArrayLoad;
import de.mirkosertic.bytecoder.core.ir.ArrayStore;
import de.mirkosertic.bytecoder.core.ir.BootstrapMethod;
import de.mirkosertic.bytecoder.core.ir.CMP;
import de.mirkosertic.bytecoder.core.ir.Cast;
import de.mirkosertic.bytecoder.core.ir.CaughtException;
import de.mirkosertic.bytecoder.core.ir.ClassInitialization;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Div;
import de.mirkosertic.bytecoder.core.ir.EnumValuesOf;
import de.mirkosertic.bytecoder.core.ir.FieldReference;
import de.mirkosertic.bytecoder.core.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.core.ir.Goto;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.InstanceOf;
import de.mirkosertic.bytecoder.core.ir.InvokeDynamicExpression;
import de.mirkosertic.bytecoder.core.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.core.ir.LookupSwitch;
import de.mirkosertic.bytecoder.core.ir.MethodArgument;
import de.mirkosertic.bytecoder.core.ir.MethodInvocation;
import de.mirkosertic.bytecoder.core.ir.MethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.MethodReference;
import de.mirkosertic.bytecoder.core.ir.MethodType;
import de.mirkosertic.bytecoder.core.ir.MonitorEnter;
import de.mirkosertic.bytecoder.core.ir.MonitorExit;
import de.mirkosertic.bytecoder.core.ir.Mul;
import de.mirkosertic.bytecoder.core.ir.Neg;
import de.mirkosertic.bytecoder.core.ir.New;
import de.mirkosertic.bytecoder.core.ir.NewArray;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NodeType;
import de.mirkosertic.bytecoder.core.ir.NullReference;
import de.mirkosertic.bytecoder.core.ir.NullTest;
import de.mirkosertic.bytecoder.core.ir.NumericalTest;
import de.mirkosertic.bytecoder.core.ir.ObjectString;
import de.mirkosertic.bytecoder.core.ir.Or;
import de.mirkosertic.bytecoder.core.ir.PrimitiveClassReference;
import de.mirkosertic.bytecoder.core.ir.PrimitiveDouble;
import de.mirkosertic.bytecoder.core.ir.PrimitiveFloat;
import de.mirkosertic.bytecoder.core.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.core.ir.PrimitiveLong;
import de.mirkosertic.bytecoder.core.ir.PrimitiveShort;
import de.mirkosertic.bytecoder.core.ir.ReadClassField;
import de.mirkosertic.bytecoder.core.ir.ReadInstanceField;
import de.mirkosertic.bytecoder.core.ir.ReferenceTest;
import de.mirkosertic.bytecoder.core.ir.Rem;
import de.mirkosertic.bytecoder.core.ir.ResolveCallsite;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.ReturnValue;
import de.mirkosertic.bytecoder.core.ir.RuntimeClass;
import de.mirkosertic.bytecoder.core.ir.RuntimeClassOf;
import de.mirkosertic.bytecoder.core.ir.SHL;
import de.mirkosertic.bytecoder.core.ir.SHR;
import de.mirkosertic.bytecoder.core.ir.SetClassField;
import de.mirkosertic.bytecoder.core.ir.SetInstanceField;
import de.mirkosertic.bytecoder.core.ir.Sub;
import de.mirkosertic.bytecoder.core.ir.TableSwitch;
import de.mirkosertic.bytecoder.core.ir.This;
import de.mirkosertic.bytecoder.core.ir.TypeConversion;
import de.mirkosertic.bytecoder.core.ir.TypeReference;
import de.mirkosertic.bytecoder.core.ir.USHR;
import de.mirkosertic.bytecoder.core.ir.Unwind;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.ir.XOr;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.lang.invoke.LambdaMetafactory;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static de.mirkosertic.bytecoder.core.backend.js.JSHelpers.generateClassName;
import static de.mirkosertic.bytecoder.core.backend.js.JSHelpers.generateFieldName;
import static de.mirkosertic.bytecoder.core.backend.js.JSHelpers.generateMethodName;

public class JSStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    int level = 4;

    private final Map<AbstractVar, String> variableToName;

    private final PrintWriter pw;

    private final ResolvedClass cl;

    private final CompileUnit compileUnit;

    private final GeneratedMethodsRegistry generatedMethodsRegistry;

    public JSStructuredControlflowCodeGenerator(final CompileUnit compileUnit, final ResolvedClass cl, final PrintWriter pw, final GeneratedMethodsRegistry generatedMethodsRegistry) {
        this.compileUnit = compileUnit;
        this.cl = cl;
        this.pw = pw;
        this.variableToName = new HashMap<>();
        this.generatedMethodsRegistry = generatedMethodsRegistry;
    }

    @Override
    public void registerVariables(final Graph g) {

        if (g.nodes().stream().anyMatch(t -> t.nodeType == NodeType.This)) {
            writeIndent();
            pw.println("var th = this;");
        }

        final List<AbstractVar> variables = g.nodes().stream().filter(t -> t instanceof AbstractVar).map(t -> (AbstractVar) t).collect(Collectors.toList());
        for (int i = 0; i < variables.size(); i++) {
            final AbstractVar v = variables.get(i);
            final String varName;
            if (v.nodeType == NodeType.PHI) {
                varName = "phi" + i;
            } else {
                varName = "var" + i;
            }
            variableToName.put(v, varName);

            writeIndent();
            pw.print("var ");
            pw.print(varName);

            if (v.type == null) {
                pw.print(" = null");
            } else {
                switch (v.type.getSort()) {
                    case Type.FLOAT:
                    case Type.DOUBLE:
                        pw.print(" = .0");
                        break;
                    case Type.OBJECT:
                        pw.print(" = null");
                        break;
                    case Type.ARRAY:
                        pw.print(" = null");
                        break;
                    default:
                        pw.print(" = 0");
                        break;
                }
            }

            pw.println(";");
        }
    }

    private void writeIndent() {
        for (int i = 0; i < level; i++) {
            pw.print(" ");
        }
    }

    @Override
    public void write(final LineNumberDebugInfo node) {
        writeIndent();
        pw.print("// ");
        pw.print(node.sourceFile);
        pw.print("#");
        pw.println(node.lineNumber);
    }

    @Override
    public void write(final Goto node) {
        writeIndent();
        pw.println("// Here was a goto statement");
    }

    @Override
    public void write(final FrameDebugInfo node) {
    }

    @Override
    public void write(final MonitorEnter node) {
        writeIndent();
        pw.print("// Monitor enter on ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println();
    }

    @Override
    public void write(final MonitorExit node) {
        writeIndent();
        pw.print("// Monitor exit on ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println();
    }

    @Override
    public void write(final Unwind node) {
        writeIndent();
        pw.print("throw bytecoder.registerStack(");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(", new Error().stack);");
    }

    @Override
    public void write(final MethodInvocation invocation) {
        switch (invocation.invocationType) {
            case DIRECT: {
                writeDirect(invocation);
                break;
            }
            case STATIC: {
                writeStatic(invocation);
                break;
            }
            case INTERFACE: {
                writeInterface(invocation);
                break;
            }
            case VIRTUAL: {
                writeVirtual(invocation);
                break;
            }
        }
    }

    private void writeDirect(final MethodInvocation node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);

        writeIndent();

        pw.print(generateClassName(invocationTarget));
        pw.print(".prototype.");
        pw.print(generateMethodName(node.insnNode.name, node.method.methodType));
        pw.print(".call(");
        writeExpression(node.incomingDataFlows[0]);
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            pw.print(",");
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpression(final MethodInvocationExpression node) {
        switch (node.invocationType) {
            case STATIC: {
                writeExpressionStaticInvocation(node);
                break;
            }
            case DIRECT: {
                writeExpressionDirectInvocation(node);
                break;
            }
            case VIRTUAL: {
                writeExpressionVirtualInvocation(node);
                break;
            }
            case INTERFACE: {
                writeExpressionInterfaceInvocation(node);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented : " + node.invocationType);
            }
        }
    }

    @Override
    public void write(final ClassInitialization node) {
        final ResolvedClass tc = compileUnit.findClass(node.type);
        if (tc.requiresClassInitializer()) {
            writeIndent();
            pw.print(generateClassName(node.type));
            pw.println(".$i;");
        }
    }

    private void writeExpressionDirectInvocation(final MethodInvocationExpression node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);

        pw.print("(");
        pw.print(generateClassName(invocationTarget));
        if ("<init>".equals(node.method.methodNode.name)) {
            pw.print("$");
        } else {
            pw.print(".prototype.");
        }
        pw.print(generateMethodName(node.insnNode.name, node.method.methodType));
        pw.print(".call(");
        writeExpression(node.incomingDataFlows[0]);
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            pw.print(",");
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print("))");
    }

    private void writeExpression(final InvokeDynamicExpression node) {

        final ResolveCallsite resolveCallsite = (ResolveCallsite) node.incomingDataFlows[0];
        final BootstrapMethod bootstrapMethod = (BootstrapMethod) resolveCallsite.incomingDataFlows[0];
        if (bootstrapMethod.className.getClassName().equals(LambdaMetafactory.class.getName())) {
            if ("metafactory".equals(bootstrapMethod.methodName)) {
                generateInvokeDynamicLambdaMetaFactoryInvocation(node, resolveCallsite);
            } else if ("altMetafactory".equals(bootstrapMethod.methodName)) {
                generateInvokeDynamicLambdaMetaFactoryInvocation(node, resolveCallsite);
            } else {
                throw new IllegalArgumentException("Not supported method " + bootstrapMethod.methodName + " on " + bootstrapMethod.className);
            }
        } else if (bootstrapMethod.className.getClassName().equals("java.lang.invoke.StringConcatFactory")) {
            if ("makeConcatWithConstants".equals(bootstrapMethod.methodName)) {
                generateInvokeDynamicStringMakeConcatWithConstants(node, resolveCallsite);
            } else {
                throw new IllegalArgumentException("Not supported method " + bootstrapMethod.methodName + " on " + bootstrapMethod.className);
            }
        } else if (bootstrapMethod.className.getClassName().equals("java.lang.runtime.ObjectMethods")) {
            if ("bootstrap".equals(bootstrapMethod.methodName)) {
                final ObjectString operation = (ObjectString) resolveCallsite.incomingDataFlows[1];
                final String operationStr = compileUnit.getConstantPool().getPooledStrings().get(operation.value.index);
                if ("toString".equals(operationStr)) {
                    generateInvokeDynamicObjectMethodsToString(node, resolveCallsite);
                } else if ("hashCode".equals(operationStr)) {
                    generateInvokeDynamicObjectMethodsHashCode(node, resolveCallsite);
                } else if ("equals".equals(operationStr)) {
                    generateInvokeDynamicObjectMethodsEquals(node, resolveCallsite);
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

    private void generateInvokeDynamicStringMakeConcatWithConstants(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        final MethodType functionType = (MethodType) resolveCallsite.incomingDataFlows[2];
        final ObjectString receipe = (ObjectString) resolveCallsite.incomingDataFlows[3];
        final String receipeStr = compileUnit.getConstantPool().getPooledStrings().get(receipe.value.index);
        final int index = generatedMethodsRegistry.register(new GeneratedMethod() {
            @Override
            public void generateCode(final PrintWriter pw, final int index) {
                pw.print("bytecoder.generated[");
                pw.print(index);
                pw.print("] = function(linkArg");

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    pw.print(",");
                    pw.print("dynArg" + (i - 1));
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
                            switch (typeToAdd.getSort()) {
                                case Type.OBJECT:
                                case Type.ARRAY: {
                                    pw.print("    str = str + de$mirkosertic$bytecoder$classlib$VM.Ljava$lang$String$$objectToString$Ljava$lang$Object$(dynArg");
                                    pw.print(dynamicArgoffset);
                                    pw.println(").nativeObject;");
                                    break;
                                }
                                default: {
                                    pw.print("    str = str + dynArg");
                                    pw.print(dynamicArgoffset);
                                    pw.println(";");
                                    break;
                                }
                            }
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

                pw.println("    return bytecoder.toBytecoderString(str);");

                pw.println("};");
            }
        });
        pw.print("bytecoder.generated[");
        pw.print(index);
        pw.print("](");
        if (resolveCallsite.incomingDataFlows.length > 4) {
            writeExpression(resolveCallsite.incomingDataFlows[4]);
            pw.print(",");
        } else {
            pw.print("null,");
        }
        boolean first = true;
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (first) {
                first = false;
            } else {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print(")");
    }

    private void generateInvokeDynamicObjectMethodsToString(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        final ObjectString fields = (ObjectString) resolveCallsite.incomingDataFlows[4];
        final TypeReference sourceType = (TypeReference) resolveCallsite.incomingDataFlows[3];
        final int index = generatedMethodsRegistry.register(new GeneratedMethod() {
            @Override
            public void generateCode(final PrintWriter pw, final int index) {
                pw.print("bytecoder.generated[");
                pw.print(index);
                pw.print("] = function(");

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    if (i > 1) {
                        pw.print(",");
                    }
                    pw.print("dynArg" + (i - 1));
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
                    switch (fieldRef.type.getSort()) {
                        case Type.ARRAY:
                        case Type.OBJECT: {
                            pw.print("    str = str + de$mirkosertic$bytecoder$classlib$VM.Ljava$lang$String$$objectToString$Ljava$lang$Object$(dynArg0.");
                            pw.print(JSHelpers.generateFieldName(fieldRef.resolvedField.name));
                            pw.println(").nativeObject;");
                            break;
                        }
                        default: {
                            pw.print("    str = str + dynArg0.");
                            pw.print(JSHelpers.generateFieldName(fieldRef.resolvedField.name));
                            pw.println(";");
                            break;
                        }
                    }
                }
                pw.println("    str = str + ']';");

                pw.println("    return bytecoder.toBytecoderString(str);");

                pw.println("};");
            }
        });
        pw.print("bytecoder.generated[");
        pw.print(index);
        pw.print("](");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print(")");
    }

    private void generateInvokeDynamicObjectMethodsHashCode(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        final ObjectString fields = (ObjectString) resolveCallsite.incomingDataFlows[4];
        final TypeReference sourceType = (TypeReference) resolveCallsite.incomingDataFlows[3];
        final int index = generatedMethodsRegistry.register(new GeneratedMethod() {
            @Override
            public void generateCode(final PrintWriter pw, final int index) {
                pw.print("bytecoder.generated[");
                pw.print(index);
                pw.print("] = function(");

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    if (i > 1) {
                        pw.print(",");
                    }
                    pw.print("dynArg" + (i - 1));
                }

                pw.println(") {");

                // Really inefficient hashcode, needs optimization!
                pw.println("  return 0;");

                pw.println("};");
            }
        });
        pw.print("bytecoder.generated[");
        pw.print(index);
        pw.print("](");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print(")");
    }

    private void generateInvokeDynamicObjectMethodsEquals(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        final ObjectString fields = (ObjectString) resolveCallsite.incomingDataFlows[4];
        final TypeReference sourceType = (TypeReference) resolveCallsite.incomingDataFlows[3];
        final int index = generatedMethodsRegistry.register(new GeneratedMethod() {
            @Override
            public void generateCode(final PrintWriter pw, final int index) {
                pw.print("bytecoder.generated[");
                pw.print(index);
                pw.print("] = function(");

                for (int i = 1; i < node.incomingDataFlows.length; i++) {
                    if (i > 1) {
                        pw.print(",");
                    }
                    pw.print("dynArg" + (i - 1));
                }

                pw.println(") {");

                pw.print("  if (bytecoder.instanceOf(dynArg1, ");

                final ResolvedClass tc = compileUnit.findClass(sourceType.type);
                pw.print(generateClassName(sourceType.type));
                if (tc.requiresClassInitializer()) {
                    pw.print(".$i");
                }
                pw.println(")) {");

                for (int i = 5; i < resolveCallsite.incomingDataFlows.length; i++) {
                    final FieldReference fieldRef = (FieldReference) resolveCallsite.incomingDataFlows[i];
                    final String fieldName = JSHelpers.generateFieldName(fieldRef.resolvedField.name);
                    switch (fieldRef.type.getSort()) {
                        case Type.OBJECT:
                        case Type.ARRAY: {
                            pw.print("    if (de$mirkosertic$bytecoder$classlib$VM.Z$nullsafeEquals$Ljava$lang$Object$$Ljava$lang$Object$(dynArg0.");
                            pw.print(fieldName);
                            pw.print(",dynArg1.");
                            pw.print(fieldName);
                            pw.println(") == 0) {");
                            pw.println("      return 0;");
                            pw.println("    }");
                            break;
                        }
                        default: {
                            pw.print("    if (dynArg0.");
                            pw.print(fieldName);
                            pw.print(" != dynArg1.");
                            pw.print(fieldName);
                            pw.println(") {");
                            pw.println("      return 0;");
                            pw.println("    }");
                            break;
                        }
                    }
                }

                pw.println("    return 1;");
                pw.println("  };");
                pw.println("  return 0;");
                pw.println("};");
            }
        });
        pw.print("bytecoder.generated[");
        pw.print(index);
        pw.print("](");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print(")");
    }

    static class LinkageArgument {
        final String name;
        final Type type;

        public LinkageArgument(final String name, final Type type) {
            this.name = name;
            this.type = type;
        }
    }

    private void generateInvokeDynamicLambdaMetaFactoryInvocation(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        // Ok, we can create a lambda invocation here
        final ObjectString argMethodName = (ObjectString) resolveCallsite.incomingDataFlows[1];
        final MethodType argInvokedType = (MethodType) resolveCallsite.incomingDataFlows[2];
        final MethodType argSamMethodType = (MethodType) resolveCallsite.incomingDataFlows[3];
        final MethodReference argImplMethod = (MethodReference) resolveCallsite.incomingDataFlows[4];
        final MethodType argInstanceMethodType = (MethodType) resolveCallsite.incomingDataFlows[5];

        final ResolvedMethod implementationMethod = argImplMethod.resolvedMethod;

        final List<Object> allArgs = new ArrayList<>();
        allArgs.addAll(Arrays.asList(node.incomingDataFlows).subList(1, node.incomingDataFlows.length));

        for (int i = 0; i < argInstanceMethodType.type.getArgumentTypes().length; i++) {
            allArgs.add(new LinkageArgument("linkarg" + i, argInstanceMethodType.type.getArgumentTypes()[i]));
        }

        final BiFunction<Type, Type, String> typeConverterFunction = (source, target) -> {
            if (source.getSort() != target.getSort()) {
                if (source.getSort() != Type.OBJECT && target.getSort() == Type.OBJECT) {
                    // Primitive to Object
                    switch (source.getSort()) {
                        case Type.BYTE: {
                            return "java$lang$Byte.Ljava$lang$Byte$$valueOf$B";
                        }
                        case Type.SHORT: {
                            return "java$lang$Short.Ljava$lang$Short$$valueOf$S";
                        }
                        case Type.INT: {
                            return "java$lang$Integer.Ljava$lang$Integer$$valueOf$I";
                        }
                        case Type.LONG: {
                            return "java$lang$Long.Ljava$lang$Long$$valueOf$L";
                        }
                        case Type.FLOAT: {
                            return "java$lang$Float.Ljava$lang$Float$$valueOf$F";
                        }
                        case Type.DOUBLE: {
                            return "java$lang$Double.Ljava$lang$Double$$valueOf$D";
                        }
                        default: {
                            throw new IllegalStateException("No converter from " + source + " to " + target + " implemented!");
                        }
                    }
                }
                if (source.getSort() == Type.OBJECT && target.getSort() != Type.OBJECT && target.getSort() != Type.VOID) {
                    // Object to primitive
                    switch (source.getSort()) {
                        default: {
                            throw new IllegalStateException("No converter from " + source + " to " + target + " implemented! ("  + argMethodName +")");
                        }
                    }
                }
            }
            return null;
        };

        final BiConsumer<Object, Type> writeArgumentWithOptionalConversion = (o, t)-> {
            if (o instanceof LinkageArgument) {
                final LinkageArgument argument = (LinkageArgument) o;
                final String conversionFunction = typeConverterFunction.apply(argument.type, t);
                if (conversionFunction != null) {
                    pw.print(conversionFunction);
                    pw.print("(");
                    pw.print(argument.name);
                    pw.print(")");
                } else {
                    pw.print(argument.name);
                }
            } else {
                final Value v = (Value) o;
                final String conversionFunction = typeConverterFunction.apply(v.type, t);
                if (conversionFunction != null) {
                    pw.print(conversionFunction);
                    pw.print("(");
                    writeExpression(v);
                    pw.print(")");
                } else {
                    writeExpression(v);
                }
            }
        };

        final Type returnType = argInvokedType.type.getReturnType();

        switch (argImplMethod.kind) {
            case INVOKESTATIC: {
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("linkarg");
                    pw.print(i);
                }
                pw.print(") { return ");

                final String converterFunction = typeConverterFunction.apply(implementationMethod.methodType.getReturnType(), argInstanceMethodType.type.getReturnType());
                if (converterFunction != null) {
                    pw.print(converterFunction);
                    pw.print("(");
                }

                pw.print(JSHelpers.generateClassName(implementationMethod.owner.type));
                pw.print(".");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print(".call(this");

                for (int x = 0; x < allArgs.size(); x++) {
                    final Object o = allArgs.get(x);

                    pw.print(", ");

                    writeArgumentWithOptionalConversion.accept(o, implementationMethod.methodType.getArgumentTypes()[x]);
                }

                pw.print(")");

                if (converterFunction != null) {
                    pw.print(")");
                }

                pw.print(";");

                pw.print("})");
                return;
            }
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE: {
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("linkarg");
                    pw.print(i);
                }
                pw.print(") { return ");

                final String converterFunction = typeConverterFunction.apply(implementationMethod.methodType.getReturnType(), argInstanceMethodType.type.getReturnType());
                if (converterFunction != null) {
                    pw.print(converterFunction);
                    pw.print("(");
                }

                final Object firstArg = allArgs.get(0);
                if (firstArg instanceof LinkageArgument) {
                    pw.print(((LinkageArgument) firstArg).name);
                } else {
                    writeExpression((Node) firstArg);
                }
                pw.print("['");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print("'].call(");

                if (firstArg instanceof LinkageArgument) {
                    pw.print(((LinkageArgument) firstArg).name);
                } else {
                    writeExpression((Node) firstArg);
                }

                for (int x = 1; x < allArgs.size(); x++) {
                    final Object arg = allArgs.get(x);
                    pw.print(", ");
                    writeArgumentWithOptionalConversion.accept(arg, implementationMethod.methodType.getArgumentTypes()[x - 1]);
                }
                pw.print(")");

                if (converterFunction != null) {
                    pw.print(")");
                }

                pw.print(";");

                pw.print("})");
                return;
            }
            case INVOKESPECIAL: {
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("linkarg");
                    pw.print(i);
                }
                pw.print(") { return ");

                final String converterFunction = typeConverterFunction.apply(implementationMethod.methodType.getReturnType(), argInstanceMethodType.type.getReturnType());
                if (converterFunction != null) {
                    pw.print(converterFunction);
                    pw.print("(");
                }

                // TODO: we need to call the right prototype here to support super.x invocations for overwritten methods
                final Object firstArg = allArgs.get(0);
                if (firstArg instanceof LinkageArgument) {
                    pw.print(((LinkageArgument) firstArg).name);
                } else {
                    writeExpression((Node) firstArg);
                }
                pw.print("['");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print("'].call(");

                if (firstArg instanceof LinkageArgument) {
                    pw.print(((LinkageArgument) firstArg).name);
                } else {
                    writeExpression((Node) firstArg);
                }

                for (int x = 1; x < allArgs.size(); x++) {
                    final Object arg = allArgs.get(x);
                    pw.print(", ");
                    writeArgumentWithOptionalConversion.accept(arg, implementationMethod.methodType.getArgumentTypes()[x - 1]);
                }

                pw.print(")");

                if (converterFunction != null) {
                    pw.print(")");
                }

                pw.print(";");

                pw.print("})");
                return;
            }
            case INVOKECONSTRUCTOR: {
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("linkarg");
                    pw.print(i);
                }
                pw.print(") { return function() {");

                pw.print("const obj = new ");
                pw.print(JSHelpers.generateClassName(implementationMethod.owner.type));
                pw.print("();");

                pw.print("obj['");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print("'].call(obj");

                for (int x = 0; x < allArgs.size(); x++) {
                    final Object arg = allArgs.get(x);
                    pw.print(", ");
                    writeArgumentWithOptionalConversion.accept(arg, implementationMethod.methodType.getArgumentTypes()[x]);
                }

                pw.print(");return obj;}();");

                pw.print("})");
                return;
            }
        }
    }

    private void writeExpression(final ReadInstanceField node) {

        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(".");
        pw.print(generateFieldName(node.resolvedField.name));
        pw.print(")");
    }

    private void writeExpression(final ReadClassField node) {

        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(".");
        pw.print(generateFieldName(node.resolvedField.name));
        pw.print(")");
    }

    private void writeExpression(final NewArray node) {

        pw.print("bytecoder.newarray((");
        writeExpression(node.incomingDataFlows[0]);
        pw.print("),");
        switch (node.type.getElementType().getSort()) {
            case Type.OBJECT:
                pw.print("null");
                break;
            default:
                pw.print("0");
                break;
        }
        pw.print(")");
    }

    private void writeExpression(final ArrayLoad node) {

        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(".data[");
        writeExpression(node.incomingDataFlows[1]);
        pw.print("])");
    }

    private void writeExpression(final MethodArgument node) {
        pw.print("arg");
        pw.print(node.index);
    }

    private void writeExpression(final NullReference node) {
        pw.print("null");
    }

    private void writeExpression(final ObjectString node) {
        pw.print("bytecoder.stringconstants[");
        pw.print(node.value.index);
        pw.print("]");
    }

    private void writeExpression(final ReferenceTest node) {
        writeExpression(node.incomingDataFlows[0]);
        switch (node.operation) {
            case EQ:
                pw.print(" == ");
                break;
            case NE:
                pw.print(" != ");
                break;
            default:
                throw new IllegalStateException("Not implemented operation : " + node.operation);
        }
        writeExpression(node.incomingDataFlows[1]);
    }

    private void writeExpression(final NullTest node) {
        writeExpression(node.incomingDataFlows[0]);
        switch (node.operation) {
            case NOTNULL:
                pw.print(" != null");
                break;
            case NULL:
                pw.print(" == null");
                break;
            default:
                throw new IllegalStateException("Not implemented operation : " + node.operation);
        }
    }

    private void writeExpression(final CaughtException node) {
        pw.print("__ex");
    }

    private void writeExpression(final And node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" & ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final TypeConversion node) {
        if (node.type.getSort() == Type.INT || node.type.getSort() == Type.LONG) {
            pw.print("(");
            writeExpression(node.incomingDataFlows[0]);
            pw.print(" | 0");
            pw.print(")");
        } else {
            writeExpression(node.incomingDataFlows[0]);
        }
    }

    private void writeExpression(final ArrayLength node) {
        writeExpression(node.incomingDataFlows[0]);
        pw.print(".data.length");
    }

    private void writeExpression(final SHR node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" >> ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final SHL node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" << ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Or node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" | ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Neg node) {
        pw.print("(0 - ");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(")");
    }

    private void writeExpression(final Mul node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" * ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final RuntimeClassOf runtimeClassOf) {
        pw.print("((");
        writeExpression(runtimeClassOf.incomingDataFlows[0]);
        pw.print(").constructor.$rt)");
    }

    private void writeExpression(final EnumValuesOf enumValuesOf) {
        pw.print("((");
        writeExpression(enumValuesOf.incomingDataFlows[0]);
        pw.print(").$Ljava$lang$Object$$getEnumConstants$$())");
    }

    private void writeType(final Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                final ResolvedClass rc = compileUnit.findClass(type);
                if (rc == null) {
                    throw new IllegalStateException("Cannot find resolved class for " + type);
                }
                pw.print(generateClassName(type));
                break;
            case Type.VOID:
                pw.print("bytecoder.primitives.void");
                break;
            case Type.FLOAT:
                pw.print("bytecoder.primitives.float");
                break;
            case Type.DOUBLE:
                pw.print("bytecoder.primitives.double");
                break;
            case Type.SHORT:
                pw.print("bytecoder.primitives.short");
                break;
            case Type.BYTE:
                pw.print("bytecoder.primitives.byte");
                break;
            case Type.CHAR:
                pw.print("bytecoder.primitives.char");
                break;
            case Type.INT:
                pw.print("bytecoder.primitives.int");
                break;
            case Type.LONG:
                pw.print("bytecoder.primitives.long");
                break;
            case Type.BOOLEAN:
                pw.print("bytecoder.primitives.boolean");
                break;
            case Type.ARRAY:
                pw.print(generateClassName(Type.getType(Object.class)));
                break;
            default:
                throw new IllegalStateException("Not implemented type for type reference : " + type + ", sort = " + type.getSort());
        }
    }

    private void writeExpression(final MethodType node) {
        final Type t = node.type;
        pw.print("[");
        writeType(t.getReturnType());
        pw.print(", [");
        final Type[] args = t.getArgumentTypes();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                pw.print(", ");
            }
            writeType(args[i]);
        }
        pw.print("]]");
    }

    private void writeExpression(final CMP node) {
        pw.print("bytecoder.cmp(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(",");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final RuntimeClass node) {
        final TypeReference typeReference = (TypeReference) node.incomingDataFlows[0];
        final Type t = typeReference.type;
        switch (t.getSort()) {
            case Type.ARRAY:
                pw.print(generateClassName(Type.getType(Array.class)));
                pw.print(".$rt");
                break;
            default:
                pw.print(generateClassName(typeReference.type));
                pw.print(".$rt");
                break;
        }
    }

    private void writeExpression(final Cast node) {
        writeExpression(node.incomingDataFlows[0]);
    }

    private void writeExpression(final PrimitiveClassReference reference) {
        switch (reference.referenceType.getSort()) {
            case Type.BOOLEAN:
                pw.print("bytecoder.primitives.boolean");
                break;
            case Type.BYTE:
                pw.print("bytecoder.primitives.byte");
                break;
            case Type.CHAR:
                pw.print("bytecoder.primitives.char");
                break;
            case Type.SHORT:
                pw.print("bytecoder.primitives.short");
                break;
            case Type.INT:
                pw.print("bytecoder.primitives.int");
                break;
            case Type.LONG:
                pw.print("bytecoder.primitives.long");
                break;
            case Type.FLOAT:
                pw.print("bytecoder.primitives.float");
                break;
            case Type.DOUBLE:
                pw.print("bytecoder.primitives.double");
                break;
            case Type.VOID:
                pw.print("bytecoder.primitives.void");
                break;
            default:
                throw new IllegalArgumentException("Not supported primitive class for " + reference.type);
        }
    }

    private void writeExpression(final PrimitiveLong node) {
        pw.print(node.value);
    }

    private void writeExpression(final PrimitiveDouble node) {
        pw.print(node.value);
    }

    private void writeExpression(final PrimitiveFloat node) {
        pw.print(node.value);
    }

    private void writeExpression(final XOr node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" ^ ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final USHR node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" >>> ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Rem node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" % ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final InstanceOf node) {
        pw.print("bytecoder.instanceOf(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(",");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final NumericalTest node) {
        writeExpression(node.incomingDataFlows[0]);

        switch (node.operation) {
            case EQ:
                pw.print(" == ");
                break;
            case GE:
                pw.print(" >= ");
                break;
            case GT:
                pw.print(" > ");
                break;
            case LE:
                pw.print(" <= ");
                break;
            case LT:
                pw.print(" < ");
                break;
            case NE:
                pw.print(" != ");
                break;
            default:
                throw new IllegalStateException("Not implemented : " + node.operation);
        }

        writeExpression(node.incomingDataFlows[1]);
    }

    @Override
    public void write(final SetInstanceField node) {

        writeIndent();
        writeExpression(node.outgoingDataFlows()[0]);
        pw.print(".");
        pw.print(generateFieldName(node.field.name));
        pw.print(" = ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(";");
    }

    @Override
    public void write(final SetClassField node) {

        writeIndent();
        writeExpression(node.outgoingDataFlows()[0]);
        pw.print(".");
        pw.print(generateFieldName(node.field.name));
        pw.print(" = ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(";");
    }

    @Override
    public void write(final ArrayStore node) {
        writeIndent();
        writeExpression(node.incomingDataFlows[0]);
        pw.print(".data[");
        writeExpression(node.incomingDataFlows[1]);
        pw.print("] = ");
        writeExpression(node.incomingDataFlows[2]);
        pw.println(";");
    }

    private void writeVirtual(final MethodInvocation node) {

        writeIndent();
        writeExpression(node.incomingDataFlows[0]);

        pw.print(".");
        pw.print(generateMethodName(node.insnNode.name, node.method.methodType));
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpressionVirtualInvocation(final MethodInvocationExpression node) {

        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);

        pw.print(".");
        pw.print(generateMethodName(node.insnNode.name, Type.getMethodType(node.insnNode.desc)));
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print("))");
    }

    private void writeInterface(final MethodInvocation node) {

        writeIndent();
        final ResolvedClass cl = compileUnit.findClass(Type.getObjectType(node.insnNode.owner));
        if (cl.isOpaqueReferenceType()) {

            final ResolvedMethod method = node.method;
            final Type[] arguments = method.methodType.getArgumentTypes();

            if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", method.methodNode.visibleAnnotations)) {
                final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", method.methodNode.visibleAnnotations);
                final String propertyName = (String) values.get("value");

                writeExpression(node.incomingDataFlows[0]);
                pw.print(".nativeObject.");
                if (propertyName != null) {
                    pw.print(propertyName);
                } else {
                    pw.print(OpaqueReferenceTypeHelpers.derivePropertyNameFromMethodName(method.methodNode.name));
                }
                if (arguments.length > 0) {
                    pw.print(" = ");
                    switch (arguments[0].getSort()) {
                        case Type.BOOLEAN: {
                            pw.print("(");
                            writeExpression(node.incomingDataFlows[1]);
                            pw.print(" === 1 ? true : false)");
                            break;
                        }
                        case Type.OBJECT: {
                            final ResolvedClass targetType = compileUnit.findClass(arguments[0]);
                            writeExpression(node.incomingDataFlows[1]);
                            pw.print(".nativeObject");
                            break;
                        }
                        default: {
                            writeExpression(node.incomingDataFlows[1]);
                            break;
                        }
                    }
                }
                pw.println(";");

            } else if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueIndexed;", method.methodNode.visibleAnnotations)) {

                writeExpression(node.incomingDataFlows[0]);
                pw.print(".nativeObject.");
                pw.print("[");
                writeExpression(node.incomingDataFlows[1]);
                pw.print("]");

                if (arguments.length > 1) {
                    pw.print(" = ");
                    writeExpression(node.incomingDataFlows[2]);
                    if (arguments[2].getSort() == Type.OBJECT) {
                        pw.print(".nativeObject");
                    }
                }
                pw.println(";");

            } else {

                writeExpression(node.incomingDataFlows[0]);
                pw.print(".nativeObject.");
                pw.print(method.methodNode.name);
                pw.print("(");

                for (int i = 0; i < arguments.length; i++) {
                    if (i > 0) {
                        pw.print(", ");
                    }
                    final Type argType = arguments[i];
                    switch (argType.getSort()) {
                        case Type.BOOLEAN: {
                            pw.print("(");
                            writeExpression(node.incomingDataFlows[i + 1]);
                            pw.print(" === 1 ? true : false)");
                            break;
                        }
                        case Type.OBJECT: {
                            final ResolvedClass typeClass = compileUnit.findClass(argType);
                            if (typeClass == null) {
                                throw new IllegalStateException("Cannot find linked class for type " + argType);
                            }
                            if (typeClass.isCallback()) {
                                if (!Modifier.isInterface(typeClass.classNode.access)) {
                                    throw new IllegalStateException("Only callback interfaces are allowed in method signatures!");
                                }

                                final List<ResolvedMethod> callbackMethods = typeClass.resolvedMethods.stream().filter(t -> !t.methodNode.name.equals("init")).collect(Collectors.toList());
                                if (callbackMethods.size() != 1) {
                                    throw new IllegalStateException("Unexpected number of callback methods, expected 1, got " + callbackMethods.size() + " for type " + typeClass.type);
                                }
                                final ResolvedMethod callbackMethod = callbackMethods.get(0);
                                final Type methodType = Type.getMethodType(callbackMethod.methodNode.desc);

                                pw.print("function(");
                                for (int j = 0; j < methodType.getArgumentTypes().length; j++) {
                                    if (j > 0) {
                                        pw.print(", ");
                                    }
                                    pw.print("arg");
                                    pw.print(j);
                                }
                                pw.print(") {this.");
                                pw.print(generateMethodName(callbackMethod.methodNode.name, methodType));
                                pw.print("(");
                                for (int j = 0; j < methodType.getArgumentTypes().length; j++) {
                                    if (j > 0) {
                                        pw.print(", ");
                                    }
                                    switch (methodType.getArgumentTypes()[j].getSort()) {
                                        case Type.BOOLEAN: {
                                            pw.print("(arg");
                                            pw.print(j);
                                            pw.print(" ? 1 : 0)");
                                            break;
                                        }
                                        case Type.OBJECT: {
                                            if (methodType.getArgumentTypes()[j].getClassName().equals(String.class.getName())) {
                                                pw.print("bytecoder.toBytecoderString(arg");
                                                pw.print(j);
                                                pw.print(")");
                                            } else {
                                                pw.print("bytecoder.wrapNativeIntoTypeInstance(");
                                                pw.print(generateClassName(methodType.getArgumentTypes()[j]));
                                                pw.print(", arg");
                                                pw.print(j);
                                                pw.print(")");
                                            }
                                            break;
                                        }
                                        default: {
                                            pw.print("arg");
                                            pw.print(j);
                                            break;
                                        }
                                    }
                                }
                                pw.print(")");
                                pw.print("}.bind(");
                                writeExpression(node.incomingDataFlows[i + 1]);
                                pw.print(")");
                            } else  {
                                writeExpression(node.incomingDataFlows[i + 1]);
                                pw.print(".nativeObject");
                            }
                            break;
                        }
                        default: {
                            writeExpression(node.incomingDataFlows[i + 1]);
                            break;
                        }
                    }
                }

                pw.println(");");
            }

        } else {
            writeExpression(node.incomingDataFlows[0]);
            pw.print(".");
            pw.print(generateMethodName(node.insnNode.name, node.method.methodType));
            pw.print("(");
            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                if (i > 1) {
                    pw.print(",");
                }
                writeExpression(node.incomingDataFlows[i]);
            }
            pw.println(");");
        }
    }

    private void writeExpressionInterfaceInvocation(final MethodInvocationExpression node) {

        final ResolvedMethod method = node.method;
        final ResolvedClass cl = method.owner;

        if (cl.isOpaqueReferenceType()) {

            final Type[] arguments = method.methodType.getArgumentTypes();

            if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", method.methodNode.visibleAnnotations)) {
                final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", method.methodNode.visibleAnnotations);
                final String propertyName = (String) values.get("value");

                final Type methodType = method.methodType;
                switch (methodType.getReturnType().getSort()) {
                    case Type.BOOLEAN: {
                        pw.print("bytecoder.toBytecoderBoolean(");
                        break;
                    }
                    case Type.OBJECT: {
                        if (String.class.getName().equals(methodType.getReturnType().getClassName())) {
                            pw.print("bytecoder.toBytecoderString(");
                            break;
                        } else {
                            final ResolvedClass targetType = compileUnit.findClass(methodType.getReturnType());
                            if (targetType.isOpaqueReferenceType()) {
                                pw.print("bytecoder.wrapNativeIntoTypeInstance(");
                                pw.print(generateClassName(methodType.getReturnType()));
                                pw.print(",");
                            } else {
                                throw new IllegalStateException("Type " + methodType.getReturnType() + " not supported as return type");
                            }
                        }
                        break;
                    }
                    default: {
                        pw.print("(");
                        break;
                    }
                }

                writeExpression(node.incomingDataFlows[0]);
                pw.print(".nativeObject.");
                if (propertyName != null) {
                    pw.print(propertyName);
                } else {
                    pw.print(OpaqueReferenceTypeHelpers.derivePropertyNameFromMethodName(method.methodNode.name));
                }
                if (arguments.length > 0) {
                    pw.print(" = ");
                    switch (arguments[0].getSort()) {
                        case Type.BOOLEAN: {
                            pw.print(" = (");
                            writeExpression(node.incomingDataFlows[1]);
                            pw.print(" === 1 ? true : false)");
                            break;
                        }
                        case Type.OBJECT: {
                            final ResolvedClass targetType = compileUnit.findClass(arguments[0]);
                            writeExpression(node.incomingDataFlows[1]);
                            if (targetType.isOpaqueReferenceType()) {
                                pw.print(".nativeObject");
                            } else {
                                throw new IllegalStateException("Type " + arguments[0] + " is not supported as an opaque property type.");
                            }
                            break;
                        }
                        default: {
                            pw.print(" = ");
                            writeExpression(node.incomingDataFlows[1]);
                            break;
                        }
                    }
                }

                pw.print(")");

            } else if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueIndexed;", method.methodNode.visibleAnnotations)) {

                writeExpression(node.incomingDataFlows[0]);
                pw.print(".nativeObject.");
                pw.print("[");
                writeExpression(node.incomingDataFlows[1]);
                pw.print("]");

                if (arguments.length > 1) {
                    pw.print(" = ");
                    writeExpression(node.incomingDataFlows[2]);
                    if (arguments[2].getSort() == Type.OBJECT) {
                        pw.print(".nativeObject");
                    }
                }
            } else {

                final Type returnType = node.method.methodType.getReturnType();
                switch (returnType.getSort()) {
                    case Type.OBJECT: {
                        if (String.class.getName().equals(returnType.getClassName())) {
                            pw.print("bytecoder.toBytecoderString(");
                        } else {
                            pw.print("bytecoder.wrapNativeIntoTypeInstance(");
                            pw.print(generateClassName(returnType));
                            pw.print(",");
                        }
                        break;
                    }
                    case Type.BOOLEAN: {
                        pw.print("bytecoder.toBytecoderBoolean(");
                        break;
                    }
                    default: {
                        pw.print("(");
                        break;
                    }
                }

                writeExpression(node.incomingDataFlows[0]);
                pw.print(".nativeObject.");
                pw.print(method.methodNode.name);
                pw.print("(");

                for (int i = 0; i < arguments.length; i++) {
                    if (i > 0) {
                        pw.print(", ");
                    }
                    final Type argType = arguments[i];
                    switch (argType.getSort()) {
                        case Type.BOOLEAN: {
                            pw.print("(");
                            writeExpression(node.incomingDataFlows[i + 1]);
                            pw.print(" === 1 ? true : false)");
                            break;
                        }
                        case Type.OBJECT: {
                            final ResolvedClass typeClass = compileUnit.findClass(argType);
                            if (typeClass == null) {
                                throw new IllegalStateException("Cannot find linked class for type " + argType);
                            }
                            if (typeClass.isCallback()) {
                                if (!Modifier.isInterface(typeClass.classNode.access)) {
                                    throw new IllegalStateException("Only callback interfaces are allowed in method signatures!");
                                }

                                final List<ResolvedMethod> callbackMethods = typeClass.resolvedMethods.stream().filter(t -> !t.methodNode.name.equals("init")).collect(Collectors.toList());
                                if (callbackMethods.size() != 1) {
                                    throw new IllegalStateException("Unexpected number of callback methods, expected 1, got " + callbackMethods.size() + " for type " + typeClass.type);
                                }
                                final ResolvedMethod callbackMethod = callbackMethods.get(0);
                                final Type methodType = callbackMethod.methodType;

                                pw.print("function(");
                                for (int j = 0; j < methodType.getArgumentTypes().length; j++) {
                                    if (j > 0) {
                                        pw.print(", ");
                                    }
                                    pw.print("arg");
                                    pw.print(j);
                                }
                                pw.print(") {this.");
                                pw.print(generateMethodName(callbackMethod.methodNode.name, methodType));
                                pw.print("(");
                                for (int j = 0; j < methodType.getArgumentTypes().length; j++) {
                                    if (j > 0) {
                                        pw.print(", ");
                                    }
                                    switch (methodType.getArgumentTypes()[j].getSort()) {
                                        case Type.BOOLEAN: {
                                            pw.print("bytecoder.toBytecoderBoolean(arg");
                                            pw.print(j);
                                            pw.print(")");
                                            break;
                                        }
                                        case Type.OBJECT: {
                                            if (methodType.getArgumentTypes()[j].getClassName().equals(String.class.getName())) {
                                                pw.print("bytecoder.toBytecoderString(arg");
                                                pw.print(j);
                                                pw.print(")");
                                            } else {
                                                pw.print("bytecoder.wrapNativeIntoTypeInstance(");
                                                pw.print(generateClassName(methodType.getArgumentTypes()[j]));
                                                pw.print(", arg");
                                                pw.print(j);
                                                pw.print(")");
                                            }
                                            break;
                                        }
                                        default: {
                                            pw.print("arg");
                                            pw.print(j);
                                            break;
                                        }
                                    }
                                }
                                pw.print(")");
                                pw.print("}.bind(");
                                writeExpression(node.incomingDataFlows[i + 1]);
                                pw.print(")");
                            } else {
                                writeExpression(node.incomingDataFlows[i + 1]);
                                pw.print(".nativeObject");
                            }
                            break;
                        }
                        default: {
                            writeExpression(node.incomingDataFlows[i + 1]);
                            break;
                        }
                    }
                }

                pw.print("))");
            }

        } else {
            pw.print("(");

            writeExpression(node.incomingDataFlows[0]);
            pw.print(".");
            pw.print(generateMethodName(node.insnNode.name, node.method.methodType));
            pw.print("(");
            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                if (i > 1) {
                    pw.print(",");
                }
                writeExpression(node.incomingDataFlows[i]);
            }
            pw.print(")");
            pw.print(")");
        }
    }

    private void writeStatic(final MethodInvocation node) {

        writeIndent();

        final ResolvedClass resolvedClass = node.method.owner;

        pw.print(generateClassName(resolvedClass.type));

        pw.print(".");
        pw.print(generateMethodName(node.method.methodNode.name, node.method.methodType));
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpressionStaticInvocation(final MethodInvocationExpression node) {

        pw.print("(");

        pw.print(generateClassName(node.method.owner.type));

        pw.print(".");
        pw.print(generateMethodName(node.method.methodNode.name, node.method.methodType));
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print("))");
    }

    @Override
    public void write(final Copy node) {
        writeIndent();
        final Value target = (Value) node.outgoingDataFlows()[0];
        final Node value = node.incomingDataFlows[0];
        if (target.nodeType == NodeType.Variable || target.nodeType == NodeType.PHI) {
            pw.print(variableToName.get(target));
        } else if (target.nodeType == NodeType.MethodArgument) {
            writeExpression(target);
        } else {
            throw new IllegalStateException("Invalid copy target : " + target);
        }
        pw.print(" = ");

        if (target.type == Type.INT_TYPE) {
            pw.print("(");
            writeExpression(value);
            pw.println(") | 0;");
        } else {
            writeExpression(value);
            pw.println(";");
        }
    }

    private void writeExpression(final Node node) {
        switch (node.nodeType) {
            case PHI:
            case Variable: {
                writeExpression((AbstractVar) node);
                break;
            }
            case PrimitiveShort: {
                writeExpression((PrimitiveShort) node);
                break;
            }
            case Sub: {
                writeExpression((Sub) node);
                break;
            }
            case Add: {
                writeExpression((Add) node);
                break;
            }
            case Div: {
                writeExpression((Div) node);
                break;
            }
            case PrimitiveInt: {
                writeExpression((PrimitiveInt) node);
                break;
            }
            case New: {
                writeExpression((New) node);
                break;
            }
            case TypeReference: {
                writeExpression((TypeReference) node);
                break;
            }
            case This: {
                writeExpression((This) node);
                break;
            }
            case MethodInvocationExpression: {
                writeExpression((MethodInvocationExpression) node);
                break;
            }
            case InvokeDynamicExpression: {
                writeExpression((InvokeDynamicExpression) node);
                break;
            }
            case ReadInstanceField: {
                writeExpression((ReadInstanceField) node);
                break;
            }
            case ReadClassField: {
                writeExpression((ReadClassField) node);
                break;
            }
            case NewArray: {
                writeExpression((NewArray) node);
                break;
            }
            case ArrayLoad: {
                writeExpression((ArrayLoad) node);
                break;
            }
            case MethodArgument: {
                writeExpression((MethodArgument) node);
                break;
            }
            case NumericalTest: {
                writeExpression((NumericalTest) node);
                break;
            }
            case NullReference: {
                writeExpression((NullReference) node);
                break;
            }
            case ObjectString: {
                writeExpression((ObjectString) node);
                break;
            }
            case ReferenceTest: {
                writeExpression((ReferenceTest) node);
                break;
            }
            case NullTest: {
                writeExpression((NullTest) node);
                break;
            }
            case CaughtException: {
                writeExpression((CaughtException) node);
                break;
            }
            case And: {
                writeExpression((And) node);
                break;
            }
            case TypeConversion: {
                writeExpression((TypeConversion) node);
                break;
            }
            case ArrayLength: {
                writeExpression((ArrayLength) node);
                break;
            }
            case SHR: {
                writeExpression((SHR) node);
                break;
            }
            case SHL: {
                writeExpression((SHL) node);
                break;
            }
            case Or: {
                writeExpression((Or) node);
                break;
            }
            case Neg: {
                writeExpression((Neg) node);
                break;
            }
            case Mul: {
                writeExpression((Mul) node);
                break;
            }
            case MethodType: {
                writeExpression((MethodType) node);
                break;
            }
            case CMP: {
                writeExpression((CMP) node);
                break;
            }
            case PrimitiveLong: {
                writeExpression((PrimitiveLong) node);
                break;
            }
            case PrimitiveDouble: {
                writeExpression((PrimitiveDouble) node);
                break;
            }
            case PrimitiveFloat: {
                writeExpression((PrimitiveFloat) node);
                break;
            }
            case XOr: {
                writeExpression((XOr) node);
                break;
            }
            case USHR: {
                writeExpression((USHR) node);
                break;
            }
            case Rem: {
                writeExpression((Rem) node);
                break;
            }
            case InstanceOf: {
                writeExpression((InstanceOf) node);
                break;
            }
            case RuntimeClass: {
                writeExpression((RuntimeClass) node);
                break;
            }
            case Cast: {
                writeExpression((Cast) node);
                break;
            }
            case PrimitiveClassReference: {
                writeExpression((PrimitiveClassReference) node);
                break;
            }
            case RuntimeClassOf: {
                writeExpression((RuntimeClassOf) node);
                break;
            }
            case EnumValuesOf: {
                writeExpression((EnumValuesOf) node);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented : " + node);
            }
        }
    }

    private void writeExpression(final TypeReference node) {
        final Type type = node.type;
        if (type.getSort() == Type.ARRAY) {
            final ResolvedClass cl = compileUnit.resolveClass(Type.getType(Array.class), null);
            pw.print(generateClassName(cl.type));
        } else {
            final ResolvedClass cl = compileUnit.resolveClass(type, null);
            pw.print(generateClassName(cl.type));
        }
    }

    private void writeExpression(final This node) {
        pw.print("th");
    }

    private void writeExpression(final New node) {
        pw.print("new ");
        writeExpression(node.incomingDataFlows[0]);
        pw.print("()");
    }

    private void writeExpression(final Sub node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" - ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Add node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" + ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Div node) {
        if (node.type == Type.DOUBLE_TYPE || node.type == Type.FLOAT_TYPE) {
            pw.print("(");
            writeExpression(node.incomingDataFlows[0]);
            pw.print(" / ");
            writeExpression(node.incomingDataFlows[1]);
            pw.print(")");
        } else {
            pw.print("Math.floor(");
            writeExpression(node.incomingDataFlows[0]);
            pw.print(" / ");
            writeExpression(node.incomingDataFlows[1]);
            pw.print(")");
        }
    }

    private void writeExpression(final AbstractVar node) {
        pw.print(variableToName.get(node));
    }

    private void writeExpression(final PrimitiveShort node) {
        pw.print(node.value);
    }

    private void writeExpression(final PrimitiveInt node) {
        pw.print(node.value);
    }

    @Override
    public void startIfWithTrueBlock(final If node) {
        writeIndent();
        pw.print("if (");

        writeExpression(node.incomingDataFlows[0]);

        pw.println(") {");
        level++;
    }

    @Override
    public void startIfElseBlock(final If node) {
        level--;
        writeIndent();
        pw.println("} else {");
        level++;
    }

    @Override
    public void finishIfBlock() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void startBlock(final Sequencer.Block block) {
        writeIndent();
        pw.print(block.label.replace("-",""));
        pw.print(": ");
        if (block.type == Sequencer.Block.Type.LOOP) {
            pw.print("while(true) ");
        }
        pw.println("{");
        level++;
    }

    @Override
    public void finishBlock(final Sequencer.Block block, final boolean stackEmpty) {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void startTryCatch(final String label) {
        writeIndent();
        pw.println("try {");
        level++;
    }

    @Override
    public void startCatchBlock() {
        level--;
        writeIndent();
        pw.println("} catch (__ex) {");
        level++;
    }

    @Override
    public void startCatchHandler(final Type type) {
        writeIndent();
        pw.print("if (__ex instanceof ");
        pw.print(generateClassName(type));
        pw.println(") {");
        level++;
    }

    @Override
    public void finishCatchHandler() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void writeRethrowException() {
        writeIndent();
        pw.println("throw __ex;");
    }

    @Override
    public void finishTryCatch() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void write(final Return node) {
        writeIndent();
        pw.println("return;");
    }

    @Override
    public void write(final ReturnValue node) {
        writeIndent();
        pw.print("return ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(";");
    }

    @Override
    public void writeBreakTo(final String label) {
        writeIndent();
        pw.print("break ");
        pw.print(label.replace("-",""));
        pw.println(";");
    }

    @Override
    public void writeContinueTo(final String label) {
        writeIndent();
        pw.print("continue ");
        pw.print(label.replace("-",""));
        pw.println(";");
    }

    @Override
    public void startTableSwitch(final TableSwitch node) {
        writeIndent();
        pw.print("if ((");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(") >= ");
        pw.print(node.min);
        pw.print(" && (");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(") <= ");
        pw.print(node.max);
        pw.print(") switch ((");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(") - ");
        pw.print(node.min);
        pw.println(") {");
        level++;
    }

    @Override
    public void startTableSwitchDefaultBlock() {
        level--;
        writeIndent();
        pw.println("} else {");
        level++;
    }

    @Override
    public void finishTableSwitchDefaultBlock() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void startLookupSwitch(final LookupSwitch node) {
        writeIndent();
        pw.print("switch (");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(") {");
        level++;
    }

    @Override
    public void writeSwitchCase(final int index) {
        writeIndent();
        pw.print("case ");
        pw.print(index);
        pw.println(": {");
        level++;
    }

    @Override
    public void writeSwitchDefaultCase() {
        writeIndent();
        pw.println("default: {");
        level++;
    }

    @Override
    public void finishSwitchDefault() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void finishSwitchCase() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void finishLookupSwitch() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void finishTableSwitch() {
    }
}
