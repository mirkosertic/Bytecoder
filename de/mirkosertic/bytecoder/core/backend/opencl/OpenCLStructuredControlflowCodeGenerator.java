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
package de.mirkosertic.bytecoder.core.backend.opencl;

import de.mirkosertic.bytecoder.classlib.Array;
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
import de.mirkosertic.bytecoder.core.ir.CMP;
import de.mirkosertic.bytecoder.core.ir.Cast;
import de.mirkosertic.bytecoder.core.ir.CaughtException;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Div;
import de.mirkosertic.bytecoder.core.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.core.ir.Goto;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.core.ir.InstanceMethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.InterfaceMethodInvocation;
import de.mirkosertic.bytecoder.core.ir.InterfaceMethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.core.ir.LookupSwitch;
import de.mirkosertic.bytecoder.core.ir.MethodArgument;
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
import de.mirkosertic.bytecoder.core.ir.Or;
import de.mirkosertic.bytecoder.core.ir.PHI;
import de.mirkosertic.bytecoder.core.ir.PrimitiveDouble;
import de.mirkosertic.bytecoder.core.ir.PrimitiveFloat;
import de.mirkosertic.bytecoder.core.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.core.ir.PrimitiveLong;
import de.mirkosertic.bytecoder.core.ir.PrimitiveShort;
import de.mirkosertic.bytecoder.core.ir.ReadClassField;
import de.mirkosertic.bytecoder.core.ir.ReadInstanceField;
import de.mirkosertic.bytecoder.core.ir.ReferenceTest;
import de.mirkosertic.bytecoder.core.ir.Rem;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.ReturnValue;
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
import de.mirkosertic.bytecoder.core.ir.VirtualMethodInvocation;
import de.mirkosertic.bytecoder.core.ir.VirtualMethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.XOr;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.mirkosertic.bytecoder.core.backend.js.JSHelpers.*;

public class OpenCLStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    int level = 4;

    private final Map<AbstractVar, String> variableToName;

    private final PrintWriter pw;

    private final ResolvedClass cl;

    private final CompileUnit compileUnit;

    public OpenCLStructuredControlflowCodeGenerator(final CompileUnit compileUnit, final ResolvedClass cl, final PrintWriter pw) {
        this.compileUnit = compileUnit;
        this.cl = cl;
        this.pw = pw;
        this.variableToName = new HashMap<>();
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
            switch (v.type.getSort()) {
                case Type.ARRAY: {
                    pw.print("__global ");
                    pw.print(OpenCLHelpers.toType(v.type));
                    pw.print("* ");
                    pw.print(varName);
                    pw.println(";");
                    break;
                }
                default: {
                    pw.print(OpenCLHelpers.toType(v.type));
                    pw.print(" ");
                    pw.print(varName);
                    pw.println(";");
                    break;
                }
            }
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
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void write(final MonitorExit node) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void write(final Unwind node) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
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

    private void writeExpression(final CMP node) {
        pw.print("bytecoder.cmp(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(",");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Cast node) {
        writeExpression(node.incomingDataFlows[0]);
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

        final ResolvedClass resolvedClass = node.method.owner;

        pw.print(generateClassName(resolvedClass.type));
        if (resolvedClass.requiresClassInitializer()) {
            pw.print(".$i");
        }

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
        final Node target = node.outgoingFlows[0];
        final Node value = node.incomingDataFlows[0];
        if (target instanceof AbstractVar) {
            pw.print(variableToName.get(target));
        } else if (target instanceof MethodArgument) {
            writeExpression(target);
        } else {
            throw new IllegalStateException("Invalid copy target : " + target);
        }
        pw.print(" = ");
        writeExpression(value);
        pw.println(";");
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
        } else if (node instanceof Cast) {
            writeExpression((Cast) node);
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
    public void finishBlock() {
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
    public void startTryCatch() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void startCatchBlock() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void startCatchHandler(final Type type) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void finishCatchHandler() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void writeRethrowException() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
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
