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
import de.mirkosertic.bytecoder.core.backend.OpaqueReferenceTypeHelpers;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethod;
import de.mirkosertic.bytecoder.core.backend.GeneratedMethodsRegistry;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.backend.sequencer.StructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.core.ir.*;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.lang.invoke.LambdaMetafactory;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.mirkosertic.bytecoder.core.backend.js.JSHelpers.*;

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
    public void registerVariables(final List<AbstractVar> variables) {
        for (int i = 0; i < variables.size(); i++) {
            final AbstractVar v = variables.get(i);
            final String varName;
            if (v instanceof PHI) {
                varName = "phi" + i;
            } else {
                varName = "var" + i;
            }
            variableToName.put(variables.get(i), varName);

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
        pw.print("// line number ");
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
    public void write(final InstanceMethodInvocation node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);

        writeIndent();
        if (invocationTarget.equals(cl.type)) {
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
        } else {
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
    }

    private void writeExpression(final InstanceMethodInvocationExpression node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);

        pw.print("(");
        if (invocationTarget.equals(cl.type)) {
            writeExpression(node.incomingDataFlows[0]);

            pw.print(".");

            pw.print(generateMethodName(node.insnNode.name, node.resolvedMethod.methodType));
            pw.print("(");
            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                if (i > 1) {
                    pw.print(",");
                }
                writeExpression(node.incomingDataFlows[i]);
            }
            pw.print("))");
        } else {
            pw.print(generateClassName(invocationTarget));
            pw.print(".prototype.");

            pw.print(generateMethodName(node.insnNode.name, node.resolvedMethod.methodType));
            pw.print(".call(");
            writeExpression(node.incomingDataFlows[0]);
            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                pw.print(",");
                writeExpression(node.incomingDataFlows[i]);
            }
            pw.print("))");
        }
    }

    private void writeExpression(final InvokeDynamicExpression node) {

        final ResolveCallsite resolveCallsite = (ResolveCallsite) node.incomingDataFlows[0];
        final BootstrapMethod bootstrapMethod = (BootstrapMethod) resolveCallsite.incomingDataFlows[0];
        if (bootstrapMethod.className.getClassName().equals(LambdaMetafactory.class.getName())) {
            if ("metafactory".equals(bootstrapMethod.methodName)) {
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

    private void generateInvokeDynamicLambdaMetaFactoryInvocation(final InvokeDynamicExpression node, final ResolveCallsite resolveCallsite) {
        // Ok, we can create a lambda invocation here
        final ObjectString argMethodName = (ObjectString) resolveCallsite.incomingDataFlows[1];
        final MethodType argInvokedType = (MethodType) resolveCallsite.incomingDataFlows[2];
        final MethodType argSamMethodType = (MethodType) resolveCallsite.incomingDataFlows[3];
        final MethodReference argImplMethod = (MethodReference) resolveCallsite.incomingDataFlows[4];
        final MethodType argInstanceMethodType = (MethodType) resolveCallsite.incomingDataFlows[5];

        final ResolvedMethod implementationMethod = argImplMethod.resolvedMethod;

        final List<Object> allArgs = new ArrayList<>();
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            allArgs.add(node.incomingDataFlows[i]);
        }

        for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
            allArgs.add("arg" + i);
        }

        switch (argImplMethod.kind) {
            case INVOKESTATIC: {
                final Type returnType = argInvokedType.type.getReturnType();
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("arg");
                    pw.print(i);
                }
                pw.print(") { return ");
                pw.print(JSHelpers.generateClassName(implementationMethod.owner.type));
                pw.print(".");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print(".call(this");

                for (final Object o : allArgs) {
                    pw.print(", ");
                    if (o instanceof String) {
                        pw.print((String) o);
                    } else {
                        writeExpression((Node) o);
                    }
                }

                pw.print(");");

                pw.print("})");
                return;
            }
            case INVOKEVIRTUAL:
            case INVOKEINTERFACE: {
                final Type returnType = argInvokedType.type.getReturnType();
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("arg");
                    pw.print(i);
                }
                pw.print(") { return ");

                final Object firstArg = allArgs.get(0);
                if (firstArg instanceof String) {
                    pw.print((String) firstArg);
                } else {
                    writeExpression((Node) firstArg);
                }
                pw.print("['");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print("'].call(");

                if (firstArg instanceof String) {
                    pw.print((String) firstArg);
                } else {
                    writeExpression((Node) firstArg);
                }
                for (int i = 1; i < allArgs.size(); i++) {
                    pw.print(", ");
                    final Object arg = allArgs.get(i);
                    if (arg instanceof String) {
                        pw.print((String) arg);
                    } else {
                        writeExpression((Node) arg);
                    }
                }
                pw.print(");");

                pw.print("})");
                return;
            }
            case INVOKESPECIAL: {
                final Type returnType = argInvokedType.type.getReturnType();
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("arg");
                    pw.print(i);
                }
                pw.print(") { return ");

                // TODO: we need to call the right prototype here to support super.x invocations for overwritten methods
                final Object firstArg = allArgs.get(0);
                if (firstArg instanceof String) {
                    pw.print((String) firstArg);
                } else {
                    writeExpression((Node) firstArg);
                }
                pw.print("['");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print("'].call(");

                if (firstArg instanceof String) {
                    pw.print((String) firstArg);
                } else {
                    writeExpression((Node) firstArg);
                }
                for (int i = 1; i < allArgs.size(); i++) {
                    pw.print(", ");
                    final Object arg = allArgs.get(i);
                    if (arg instanceof String) {
                        pw.print((String) arg);
                    } else {
                        writeExpression((Node) arg);
                    }
                }
                pw.print(");");

                pw.print("})");
                return;
            }
            case INVOKECONSTRUCTOR: {
                final Type returnType = argInvokedType.type.getReturnType();
                pw.print("bytecoder.instanceWithLambdaImpl(");
                pw.print(JSHelpers.generateClassName(returnType));
                pw.print(", function(");
                for (int i = 0; i < argSamMethodType.type.getArgumentTypes().length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("arg");
                    pw.print(i);
                }
                pw.print(") { return function() {");

                pw.print("const obj = new ");
                pw.print(JSHelpers.generateClassName(implementationMethod.owner.type));
                pw.print("();");

                pw.print("obj['");
                pw.print(JSHelpers.generateMethodName(implementationMethod.methodNode.name, implementationMethod.methodType));
                pw.print("'].call(obj");

                for (final Object arg : allArgs) {
                    pw.print(", ");
                    if (arg instanceof String) {
                        pw.print((String) arg);
                    } else {
                        writeExpression((Node) arg);
                    }
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
        writeExpression(node.outgoingFlows[0]);
        pw.print(".");
        pw.print(generateFieldName(node.field.name));
        pw.print(" = ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(";");
    }

    @Override
    public void write(final SetClassField node) {

        writeIndent();
        writeExpression(node.outgoingFlows[0]);
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

    @Override
    public void write(final VirtualMethodInvocation node) {

        writeIndent();
        writeExpression(node.incomingDataFlows[0]);

        pw.print(".");
        pw.print(generateMethodName(node.insnNode.name, node.resolvedMethod.methodType));
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpression(final VirtualMethodInvocationExpression node) {

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

    @Override
    public void write(final InterfaceMethodInvocation node) {

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

    private void writeExpression(final InterfaceMethodInvocationExpression node) {

        final ResolvedMethod method = node.resolvedMethod;
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

                final Type returnType = node.resolvedMethod.methodType.getReturnType();
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
            pw.print(generateMethodName(node.insnNode.name, node.resolvedMethod.methodType));
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

    @Override
    public void write(final StaticMethodInvocation node) {

        writeIndent();

        final ResolvedClass resolvedClass = node.resolvedMethod.owner;

        pw.print(generateClassName(resolvedClass.type));
        if (resolvedClass.requiresClassInitializer()) {
            pw.print(".$i");
        }

        pw.print(".");
        pw.print(generateMethodName(node.resolvedMethod.methodNode.name, node.resolvedMethod.methodType));
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpression(final StaticMethodInvocationExpression node) {

        pw.print("(");

        final ResolvedClass resolvedClass = node.resolvedMethod.owner;

        pw.print(generateClassName(node.resolvedMethod.owner.type));
        if (resolvedClass.requiresClassInitializer()) {
            pw.print(".$i");
        }

        pw.print(".");
        pw.print(generateMethodName(node.resolvedMethod.methodNode.name, node.resolvedMethod.methodType));
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
        final Value target = (Value) node.outgoingFlows[0];
        final Node value = node.incomingDataFlows[0];
        if (target instanceof AbstractVar) {
            pw.print(variableToName.get(target));
        } else if (target instanceof MethodArgument) {
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
        if (node instanceof AbstractVar) {
            writeExpression((AbstractVar) node);
        } else if (node instanceof PrimitiveShort) {
            writeExpression((PrimitiveShort) node);
        } else if (node instanceof Sub) {
            writeExpression((Sub) node);
        } else if (node instanceof Add) {
            writeExpression((Add) node);
        } else if (node instanceof Div) {
            writeExpression((Div) node);
        } else if (node instanceof PrimitiveInt) {
            writeExpression((PrimitiveInt) node);
        } else if (node instanceof New) {
            writeExpression((New) node);
        } else if (node instanceof TypeReference) {
            writeExpression((TypeReference) node);
        } else if (node instanceof This) {
            writeExpression((This) node);
        } else if (node instanceof VirtualMethodInvocationExpression) {
            writeExpression((VirtualMethodInvocationExpression) node);
        } else if (node instanceof InterfaceMethodInvocationExpression) {
            writeExpression((InterfaceMethodInvocationExpression) node);
        } else if (node instanceof StaticMethodInvocationExpression) {
            writeExpression((StaticMethodInvocationExpression) node);
        } else if (node instanceof InstanceMethodInvocationExpression) {
            writeExpression((InstanceMethodInvocationExpression) node);
        } else if (node instanceof InvokeDynamicExpression) {
            writeExpression((InvokeDynamicExpression) node);
        } else if (node instanceof ReadInstanceField) {
            writeExpression((ReadInstanceField) node);
        } else if (node instanceof ReadClassField) {
            writeExpression((ReadClassField) node);
        } else if (node instanceof NewArray) {
            writeExpression((NewArray) node);
        } else if (node instanceof ArrayLoad) {
            writeExpression((ArrayLoad) node);
        } else if (node instanceof MethodArgument) {
            writeExpression((MethodArgument) node);
        } else if (node instanceof NumericalTest) {
            writeExpression((NumericalTest) node);
        } else if (node instanceof NullReference) {
            writeExpression((NullReference) node);
        } else if (node instanceof ObjectString) {
            writeExpression((ObjectString) node);
        } else if (node instanceof ReferenceTest) {
            writeExpression((ReferenceTest) node);
        } else if (node instanceof NullTest) {
            writeExpression((NullTest) node);
        } else if (node instanceof CaughtException) {
            writeExpression((CaughtException) node);
        } else if (node instanceof And) {
            writeExpression((And) node);
        } else if (node instanceof TypeConversion) {
            writeExpression((TypeConversion) node);
        } else if (node instanceof ArrayLength) {
            writeExpression((ArrayLength) node);
        } else if (node instanceof SHR) {
            writeExpression((SHR) node);
        } else if (node instanceof SHL) {
            writeExpression((SHL) node);
        } else if (node instanceof Or) {
            writeExpression((Or) node);
        } else if (node instanceof Neg) {
            writeExpression((Neg) node);
        } else if (node instanceof Mul) {
            writeExpression((Mul) node);
        } else if (node instanceof MethodType) {
            writeExpression((MethodType) node);
        } else if (node instanceof CMP) {
            writeExpression((CMP) node);
        } else if (node instanceof PrimitiveLong) {
            writeExpression((PrimitiveLong) node);
        } else if (node instanceof PrimitiveDouble) {
            writeExpression((PrimitiveDouble) node);
        } else if (node instanceof PrimitiveFloat) {
            writeExpression((PrimitiveFloat) node);
        } else if (node instanceof XOr) {
            writeExpression((XOr) node);
        } else if (node instanceof USHR) {
            writeExpression((USHR) node);
        } else if (node instanceof Rem) {
            writeExpression((Rem) node);
        } else if (node instanceof InstanceOf) {
            writeExpression((InstanceOf) node);
        } else if (node instanceof RuntimeClass) {
            writeExpression((RuntimeClass) node);
        } else if (node instanceof Cast) {
            writeExpression((Cast) node);
        } else if (node instanceof PrimitiveClassReference) {
            writeExpression((PrimitiveClassReference) node);
        } else if (node instanceof RuntimeClassOf) {
            writeExpression((RuntimeClassOf) node);
        } else if (node instanceof EnumValuesOf) {
            writeExpression((EnumValuesOf) node);
        } else {
            throw new IllegalArgumentException("Not implemented : " + node);
        }
    }

    private void writeExpression(final TypeReference node) {
        final Type type = node.type;
        if (type.getSort() == Type.ARRAY) {
            final ResolvedClass cl = compileUnit.resolveClass(Type.getType(Array.class), null);
            pw.print(generateClassName(cl.type));
            if (cl.requiresClassInitializer()) {
                pw.print(".$i");
            }
        } else {
            final ResolvedClass cl = compileUnit.resolveClass(type, null);
            pw.print(generateClassName(cl.type));
            if (cl.requiresClassInitializer()) {
                pw.print(".$i");
            }
        }
    }

    private void writeExpression(final This node) {
        pw.print("this");
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
        pw.print(block.label);
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
        pw.print(label);
        pw.println(";");
    }

    @Override
    public void writeContinueTo(final String label) {
        writeIndent();
        pw.print("continue ");
        pw.print(label);
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
