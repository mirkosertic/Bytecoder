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
package de.mirkosertic.bytecoder.asm.backend.js;

import de.mirkosertic.bytecoder.asm.AbstractVar;
import de.mirkosertic.bytecoder.asm.Add;
import de.mirkosertic.bytecoder.asm.And;
import de.mirkosertic.bytecoder.asm.ArrayLength;
import de.mirkosertic.bytecoder.asm.ArrayLoad;
import de.mirkosertic.bytecoder.asm.ArrayStore;
import de.mirkosertic.bytecoder.asm.CMP;
import de.mirkosertic.bytecoder.asm.CaughtException;
import de.mirkosertic.bytecoder.asm.CheckCast;
import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Copy;
import de.mirkosertic.bytecoder.asm.Div;
import de.mirkosertic.bytecoder.asm.FrameDebugInfo;
import de.mirkosertic.bytecoder.asm.Goto;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.If;
import de.mirkosertic.bytecoder.asm.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.asm.InstanceMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.InstanceOf;
import de.mirkosertic.bytecoder.asm.InterfaceMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.InvokeDynamicExpression;
import de.mirkosertic.bytecoder.asm.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.asm.MethodArgument;
import de.mirkosertic.bytecoder.asm.MethodReference;
import de.mirkosertic.bytecoder.asm.MethodType;
import de.mirkosertic.bytecoder.asm.MonitorEnter;
import de.mirkosertic.bytecoder.asm.MonitorExit;
import de.mirkosertic.bytecoder.asm.Mul;
import de.mirkosertic.bytecoder.asm.Neg;
import de.mirkosertic.bytecoder.asm.New;
import de.mirkosertic.bytecoder.asm.NewArray;
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.NullReference;
import de.mirkosertic.bytecoder.asm.NullTest;
import de.mirkosertic.bytecoder.asm.NumericalTest;
import de.mirkosertic.bytecoder.asm.ObjectDouble;
import de.mirkosertic.bytecoder.asm.ObjectFloat;
import de.mirkosertic.bytecoder.asm.ObjectInteger;
import de.mirkosertic.bytecoder.asm.ObjectLong;
import de.mirkosertic.bytecoder.asm.ObjectString;
import de.mirkosertic.bytecoder.asm.Or;
import de.mirkosertic.bytecoder.asm.PHI;
import de.mirkosertic.bytecoder.asm.PrimitiveDouble;
import de.mirkosertic.bytecoder.asm.PrimitiveFloat;
import de.mirkosertic.bytecoder.asm.PrimitiveInt;
import de.mirkosertic.bytecoder.asm.PrimitiveLong;
import de.mirkosertic.bytecoder.asm.PrimitiveShort;
import de.mirkosertic.bytecoder.asm.ReadClassField;
import de.mirkosertic.bytecoder.asm.ReadInstanceField;
import de.mirkosertic.bytecoder.asm.ReferenceTest;
import de.mirkosertic.bytecoder.asm.Rem;
import de.mirkosertic.bytecoder.asm.ResolveCallsite;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedField;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.Return;
import de.mirkosertic.bytecoder.asm.ReturnValue;
import de.mirkosertic.bytecoder.asm.SHL;
import de.mirkosertic.bytecoder.asm.SHR;
import de.mirkosertic.bytecoder.asm.SetClassField;
import de.mirkosertic.bytecoder.asm.SetInstanceField;
import de.mirkosertic.bytecoder.asm.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.StaticMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.Sub;
import de.mirkosertic.bytecoder.asm.This;
import de.mirkosertic.bytecoder.asm.TypeConversion;
import de.mirkosertic.bytecoder.asm.TypeReference;
import de.mirkosertic.bytecoder.asm.USHR;
import de.mirkosertic.bytecoder.asm.Unwind;
import de.mirkosertic.bytecoder.asm.VirtualMethodInvocation;
import de.mirkosertic.bytecoder.asm.VirtualMethodInvocationExpression;
import de.mirkosertic.bytecoder.asm.XOr;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import de.mirkosertic.bytecoder.asm.sequencer.StructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.classlib.Array;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSBackend {

    public String generateClassName(final Type type) {
        return type.getClassName().replace('.', '$');
    }

    public String generateFieldName(final String name) {
        return name;
    }

    public String generateMethodName(final String name, final Type[] argumentTypes) {
        final StringBuilder builder = new StringBuilder(name);
        for (final Type arg : argumentTypes) {
            builder.append("$").append(arg);
        }
        return builder.toString()
                .replace('<', '$')
                .replace('>', '$')
                .replace('/', '$')
                .replace(';', '$')
                .replace('[', '$');
    }

    public void generateCodeFor(final CompileUnit compileUnit, final OutputStream out) {
        final PrintWriter pw = new PrintWriter(out);

        for (final ResolvedClass cl : compileUnit.computeClassDependencies()) {
            final String className = generateClassName(cl.type);
            if (Modifier.isInterface(cl.classNode.access)) {
                pw.print("const ");
                pw.print(className);

                int bracesCounter = 0;
                final StringBuilder extendsClause = new StringBuilder();
                if (!cl.superClass.type.getClassName().equals(Object.class.getName())) {
                    extendsClause.append(generateClassName(cl.superClass.type));
                    extendsClause.append("(");
                    bracesCounter++;
                }
                for (int i = cl.interfaces.length - 1; i >= 0; i--) {
                    extendsClause.insert(0, generateClassName(cl.interfaces[i].type) + "(");
                    bracesCounter++;
                }

                pw.print(" = (superclass) => class extends ");
                if (extendsClause.length() > 0) {
                    pw.print(extendsClause);
                }
                pw.print("superclass");
                for (int i = 0; i < bracesCounter; i++) {
                    pw.print(")");
                }
                pw.println(" {");

                generateFieldsFor(pw, compileUnit, cl);

                generateClassInitFor(pw, compileUnit, cl);

                generateMethodsFor(pw, compileUnit, cl);

                pw.println("};");
                pw.println();
            } else {
                pw.print("class ");
                pw.print(className);

                int bracesCounter = 0;
                final StringBuilder extendsClause = new StringBuilder();
                for (int i = 0; i < cl.interfaces.length; i++) {
                    extendsClause.append(generateClassName(cl.interfaces[i].type)).append("(");
                    bracesCounter++;
                }
                if (cl.superClass != null) {
                    extendsClause.append(generateClassName(cl.superClass.type));
                }
                for (int i = 0; i < bracesCounter; i++) {
                    extendsClause.append(")");
                }

                if (extendsClause.length() > 0) {
                    pw.print(" extends ");
                    pw.print(extendsClause);
                }

                pw.print(" ");
                pw.println("{");

                generateFieldsFor(pw, compileUnit, cl);

                pw.println("  constructor() {");
                if (cl.superClass != null) {
                    pw.println("    super();");
                }
                pw.println("  }");

                generateClassInitFor(pw, compileUnit, cl);

                generateMethodsFor(pw, compileUnit, cl);

                pw.println("}");
                pw.println();
            }
        }

        pw.flush();
    }

    private void generateClassInitFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        if (cl.requiresClassInitializer()) {
            pw.println();
            pw.println("  static #iguard = false;");
            pw.println("  static get i() {");
            pw.print("    if (!");
            pw.print(generateClassName(cl.type));
            pw.println(".#iguard) {");
            pw.print("      ");
            pw.print(generateClassName(cl.type));
            pw.println(".#iguard = true;");
            if (cl.superClass != null && cl.superClass.requiresClassInitializer()) {
                pw.print("      ");
                pw.print(generateClassName(cl.superClass.type));
                pw.println(".i;");
            }

            if (cl.classInitializer != null) {
                pw.print("      ");
                pw.print(generateClassName(cl.type));
                pw.print(".");
                pw.print(generateMethodName("<clinit>", Type.getMethodType(cl.classInitializer.methodNode.desc).getArgumentTypes()));
                pw.println("();");
                pw.println("    }");
            }
            pw.print("    return ");
            pw.print(generateClassName(cl.type));
            pw.println(";");

            pw.println("  }");
        }
    }

    private void generateFieldsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        if (!cl.resolvedFields.isEmpty()) {
            pw.println();
            for (final ResolvedField f : cl.resolvedFields) {
                pw.print("  ");
                if (Modifier.isStatic(f.access)) {
                    pw.print("static ");
                } else if (Modifier.isPrivate(f.access)) {
                    pw.print("#");
                }
                pw.print(generateFieldName(f.name));
                pw.print(" ");
                switch (f.type.getSort()) {
                    case Type.FLOAT:
                    case Type.DOUBLE:
                        pw.print(" = 0.0");
                        break;
                    case Type.BOOLEAN:
                        pw.print(" = false");
                        break;
                    case Type.ARRAY:
                    case Type.OBJECT:
                    case Type.METHOD:
                        pw.print(" = null");
                        break;
                    default:
                        pw.print(" = 0");
                        break;
                }
                pw.println(";");
            }

            pw.println();
        }
    }

    private void generateMethodsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        for (final ResolvedMethod m : cl.resolvedMethods) {
            if (m.methodBody != null) {

                System.out.println("Writing method for " + cl.type + " . " + m.methodNode.name + m.methodNode.desc);

                pw.println();
                pw.print("  ");
                if (Modifier.isStatic(m.methodNode.access)) {
                    pw.print("static ");
                }
                if (Modifier.isPrivate(m.methodNode.access)) {
                    pw.print("#");
                }
                final String methodName = generateMethodName(m.methodNode.name, Type.getArgumentTypes(m.methodNode.desc));
                pw.print(methodName);

                final Type[] arguments = Type.getArgumentTypes(m.methodNode.desc);

                pw.print("(");
                for (int i = 0; i < arguments.length; i++) {
                    if (i > 0) {
                        pw.print(",");
                    }
                    pw.print("arg");
                    pw.print(i);
                }
                pw.println(") {");

                final Graph g = m.methodBody;
                final Optimizer o = Optimizations.DEFAULT;


                while (o.optimize(m, g)) {
                    //
                }

                final DominatorTree dt = new DominatorTree(g);

                try {
                    new Sequencer(g, dt, new StructuredControlflowCodeGenerator() {

                        int level = 4;

                        private final Map<AbstractVar, String> variableToName = new HashMap<>();

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
                        public void write(final CheckCast node) {
                            writeIndent();
                            pw.print("// Check cast on ");
                            writeExpression(node.incomingDataFlows[1]);
                            pw.print(" for ");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.println();
                        }

                        @Override
                        public void write(final Unwind node) {
                            writeIndent();
                            pw.print("throw ");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.println();
                        }

                        @Override
                        public void write(final InstanceMethodInvocation node) {

                            final Type invocationTarget = Type.getObjectType(node.insnNode.owner);

                            writeIndent();
                            if (invocationTarget.equals(cl.type)) {
                                writeExpression(node.incomingDataFlows[0]);

                                pw.print(".");

                                if (Modifier.isPrivate(node.resolvedMethod.methodNode.access)) {
                                    pw.print("#");
                                }

                                pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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

                                if (Modifier.isPrivate(node.resolvedMethod.methodNode.access)) {
                                    pw.print("#");
                                }

                                pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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

                                if (Modifier.isPrivate(node.resolvedMethod.methodNode.access)) {
                                    pw.print("#");
                                }

                                pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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

                                if (Modifier.isPrivate(node.resolvedMethod.methodNode.access)) {
                                    pw.print("#");
                                }

                                pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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

                            // Resolve callsite
                            writeExpression(node.incomingDataFlows[0]);
                            // Invoke callsite
                            pw.print(".invokeExact(");
                            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                                if (i > 1) {
                                    pw.print(", ");
                                }
                                writeExpression(node.incomingDataFlows[i]);
                            }
                            pw.print(")");
                        }

                        private void writeExpression(final ReadInstanceField node) {

                            pw.print("(");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print(".");
                            if (Modifier.isPrivate(node.resolvedField.access)) {
                                pw.print("#");
                            }
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

                            pw.print("(new Array(");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print("))");
                        }

                        private void writeExpression(final ArrayLoad node) {

                            pw.print("(");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print("[");
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
                            pw.print("'");
                            pw.print(node.value);
                            pw.print("'");
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
                            pw.print(".length");
                        }

                        private void writeExpression(final ObjectLong node) {
                            // TODO
                            pw.print(node.value);
                        }

                        private void writeExpression(final ObjectInteger node) {
                            // TODO
                            pw.print(node.value);
                        }

                        private void writeExpression(final ObjectDouble node) {
                            // TODO
                            pw.print(node.value);
                        }

                        private void writeExpression(final ObjectFloat node) {
                            // TODO
                            pw.print(node.value);
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
                            pw.print("(-");
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

                        private void writeExpression(final ResolveCallsite node) {
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print("(null"); // First arg: MethodHandles.Lookup
                            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                                pw.print(", ");
                                writeExpression(node.incomingDataFlows[i]);
                            }
                            pw.print(")");
                        }

                        private void writeExpression(final MethodReference node) {
                            final ResolvedMethod m = node.resolvedMethod;
                            pw.print(generateClassName(m.owner.type));
                            pw.print(".");
                            pw.print(generateMethodName(m.methodNode.name, node.type.getArgumentTypes()));
                        }

                        private void writeType(final Type type) {
                            switch (type.getSort()) {
                                case Type.OBJECT:
                                    pw.print(generateClassName(type));
                                    break;
                                default:
                                    pw.print("null");
                                    break;
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
                            pw.print("cmp(");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print(",");
                            writeExpression(node.incomingDataFlows[1]);
                            pw.print(")");
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
                            pw.print("((");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print("  instanceof ");
                            writeExpression(node.incomingDataFlows[1]);
                            pw.print(") ? 1 : 0)");
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
                            if (Modifier.isPrivate(node.resolvedField.access)) {
                                pw.print("#");
                            }
                            pw.print(generateFieldName(node.resolvedField.name));
                            pw.print(" = ");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.println(";");
                        }

                        @Override
                        public void write(final SetClassField node) {

                            writeIndent();
                            writeExpression(node.outgoingFlows[0]);
                            pw.print(".");
                            pw.print(generateFieldName(node.resolvedField.name));
                            pw.print(" = ");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.println(";");
                        }

                        @Override
                        public void write(final ArrayStore node) {
                            writeIndent();
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print("[");
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
                            pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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
                            pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
                            pw.print("(");
                            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                                if (i > 1) {
                                    pw.print(",");
                                }
                                writeExpression(node.incomingDataFlows[i]);
                            }
                            pw.print("))");
                        }

                        private void writeExpression(final InterfaceMethodInvocationExpression node) {

                            pw.print("(");
                            writeExpression(node.incomingDataFlows[0]);

                            pw.print(".");
                            pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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
                        public void write(final StaticMethodInvocation node) {

                            writeIndent();

                            final Type target = Type.getObjectType(node.insnNode.owner);

                            final ResolvedClass resolvedClass = compileUnit.resolveClass(target, null);

                            pw.print(generateClassName(target));
                            if (resolvedClass.requiresClassInitializer()) {
                                pw.print(".i");
                            }

                            pw.print(".");
                            if (Modifier.isPrivate(node.resolvedMethod.methodNode.access)) {
                                pw.print("#");
                            }
                            pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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

                            final Type target = Type.getObjectType(node.insnNode.owner);
                            final ResolvedClass resolvedClass = compileUnit.resolveClass(target, null);

                            pw.print(generateClassName(target));
                            if (resolvedClass.requiresClassInitializer()) {
                                pw.print(".i");
                            }

                            pw.print(".");
                            if (Modifier.isPrivate(node.resolvedMethod.methodNode.access)) {
                                pw.print("#");
                            }
                            pw.print(generateMethodName(node.insnNode.name, Type.getArgumentTypes(node.insnNode.desc)));
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
                            } else if (node instanceof ObjectLong) {
                                writeExpression((ObjectLong) node);
                            } else if (node instanceof ObjectInteger) {
                                writeExpression((ObjectInteger) node);
                            } else if (node instanceof ObjectDouble) {
                                writeExpression((ObjectDouble) node);
                            } else if (node instanceof ObjectFloat) {
                                writeExpression((ObjectFloat) node);
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
                            } else if (node instanceof ResolveCallsite) {
                                writeExpression((ResolveCallsite) node);
                            } else if (node instanceof MethodReference) {
                                writeExpression((MethodReference) node);
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
                            } else {
                                throw new IllegalArgumentException("Not implemented : " + node);
                            }
                        }

                        private void writeExpression(final TypeReference node) {
                            final Type type = node.type;
                            if (type.getSort() == Type.ARRAY) {
                                final ResolvedClass cl = compileUnit.resolveClass(Type.getType(Array.class), null);
                                pw.print(generateClassName(node.type));
                                if (cl.requiresClassInitializer()) {
                                    pw.print(".i");
                                }
                            } else {
                                final ResolvedClass cl = compileUnit.resolveClass(type, null);
                                pw.print(generateClassName(node.type));
                                if (cl.requiresClassInitializer()) {
                                    pw.print(".i");
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
                            pw.print("(");
                            writeExpression(node.incomingDataFlows[0]);
                            pw.print(" / ");
                            writeExpression(node.incomingDataFlows[1]);
                            pw.print(")");
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
                        public void writeIfAndStartTrueBlock(final If node) {
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
                        public void startTryCatch(final String label) {
                            writeIndent();
                            if (label != null) {
                                pw.print(label);
                                pw.print(": ");
                            }
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
                        public void endCatchHandler() {
                            level--;
                            writeIndent();
                            pw.println("}");
                        }

                        @Override
                        public void startFinallyBlock() {
                            level--;
                            writeIndent();
                            pw.println("} finally {");
                            level++;
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
                        public void writeSwitch(final ControlTokenConsumer node) {
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
                        public void finishSwitchCase() {
                            level--;
                            writeIndent();
                            pw.println("}");
                        }
                    });
                } catch (final Exception ex) {

                    try {
                        g.writeDebugTo(Files.newOutputStream(Paths.get(generateClassName(cl.type) + "." + methodName + "_debug.dot")));
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        g.writeDebugTo(Files.newOutputStream(Paths.get(generateClassName(cl.type) + "." + methodName + "_debug_optimized.dot")));
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        dt.writeDebugTo(Files.newOutputStream(Paths.get(generateClassName(cl.type) + "." + methodName + "_dominatortree.dot")));
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }

                    throw ex;
                }

                pw.println("  }");
            }
        }
    }
}
