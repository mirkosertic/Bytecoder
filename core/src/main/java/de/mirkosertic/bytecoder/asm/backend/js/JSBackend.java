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

import de.mirkosertic.bytecoder.asm.Add;
import de.mirkosertic.bytecoder.asm.CompileUnit;
import de.mirkosertic.bytecoder.asm.Copy;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.If;
import de.mirkosertic.bytecoder.asm.InstanceMethodInvocation;
import de.mirkosertic.bytecoder.asm.Int;
import de.mirkosertic.bytecoder.asm.New;
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.PHI;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.ReturnNothing;
import de.mirkosertic.bytecoder.asm.StaticMethodInvocation;
import de.mirkosertic.bytecoder.asm.Sub;
import de.mirkosertic.bytecoder.asm.This;
import de.mirkosertic.bytecoder.asm.TypeReference;
import de.mirkosertic.bytecoder.asm.Variable;
import de.mirkosertic.bytecoder.asm.VirtualMethodInvocation;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import de.mirkosertic.bytecoder.asm.sequencer.StructuredControlflowCodeGenerator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSBackend {

    public String generateClassName(final Type type) {
        return type.getClassName().replace('.', '$');
    }

    public String generateMethodName(final String name) {
        return name.replace('<', '$').replace('>', '$');
    }

    public void generateCodeFor(final CompileUnit compileUnit, final OutputStream out) {
        final PrintWriter pw = new PrintWriter(out);

        for (final ResolvedClass cl : compileUnit.computeClassDependencies()) {
            final String className = generateClassName(cl.type);
            if (cl.isInterface()) {
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
                pw.println("  constructor() {");
                if (cl.superClass != null) {
                    pw.println("    super();");
                }
                pw.println("  }");

                generateMethodsFor(pw, compileUnit, cl);

                pw.println("};");
                pw.println();
            }
        }

        pw.flush();
    }

    private void generateMethodsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        for (final ResolvedMethod m : cl.resolvedMethods) {
            if (m.methodBody != null) {
                pw.println();
                pw.print("  ");
                if ((m.methodNode.access & Opcodes.ACC_STATIC) > 0) {
                    pw.print("static ");
                }
                if ((m.methodNode.access & Opcodes.ACC_PRIVATE) > 0) {
                    pw.print("#");
                }
                final String methodName = generateMethodName(m.methodNode.name);
                pw.print(methodName);
                pw.println("() {");

                final Graph g = m.methodBody;
                final Optimizer o = Optimizations.DEFAULT;

                try {
                    g.writeDebugTo(Files.newOutputStream(Paths.get(generateClassName(cl.type) + "." + methodName + "_debug.dot")));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }

                while (o.optimize(g)) {
                    //
                }

                final DominatorTree dt = new DominatorTree(g);

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

                new Sequencer(g, dt, new StructuredControlflowCodeGenerator() {

                    int level = 4;

                    private final Map<Variable, String> variableToName = new HashMap<>();

                    @Override
                    public void registerVariables(final List<Variable> variables) {
                        for (int i = 0; i < variables.size(); i++) {
                            final Variable v = variables.get(i);
                            if (v instanceof PHI) {
                                final String varName = "phi" + i;
                                variableToName.put(variables.get(i), "phi" + i);
                                writeIndent();
                                pw.print("var ");
                                pw.print(varName);
                                pw.println(";");
                            } else {
                                variableToName.put(variables.get(i), "var" + i);
                            }
                        }
                    }

                    private void writeIndent() {
                        for (int i = 0; i < level; i++) {
                            pw.print(" ");
                        }
                    }

                    @Override
                    public void write(final InstanceMethodInvocation node) {

                        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);

                        writeIndent();
                        if (invocationTarget.equals(cl.type)) {
                            writeExpression(node.incomingDataFlows[0]);

                            pw.print(".");

                            if ((node.resolvedMethod.methodNode.access & Opcodes.ACC_PRIVATE) > 0) {
                                pw.print("#");
                            }

                            pw.print(generateMethodName(node.insnNode.name));
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

                            if ((node.resolvedMethod.methodNode.access & Opcodes.ACC_PRIVATE) > 0) {
                                pw.print("#");
                            }

                            pw.print(generateMethodName(node.insnNode.name));
                            pw.print(".call(");
                            writeExpression(node.incomingDataFlows[0]);
                            for (int i = 1; i < node.incomingDataFlows.length; i++) {
                                if (i > 1) {
                                    pw.print(",");
                                }
                                writeExpression(node.incomingDataFlows[i]);
                            }
                            pw.println(");");
                        }
                    }

                    @Override
                    public void write(final VirtualMethodInvocation node) {

                        writeIndent();
                        writeExpression(node.incomingDataFlows[0]);

                        pw.print(".");
                        pw.print(generateMethodName(node.insnNode.name));
                        pw.print("(");
                        for (int i = 1; i < node.incomingDataFlows.length; i++) {
                            if (i > 1) {
                                pw.print(",");
                            }
                            writeExpression(node.incomingDataFlows[i]);
                        }
                        pw.println(");");
                    }

                    @Override
                    public void write(final StaticMethodInvocation node) {

                        writeIndent();

                        final Type target = Type.getObjectType(node.insnNode.owner);

                        pw.print(generateClassName(target));
                        pw.print(".");
                        if ((node.resolvedMethod.methodNode.access & Opcodes.ACC_PRIVATE) > 0) {
                            pw.print("#");
                        }
                        pw.print(generateMethodName(node.insnNode.name));
                        pw.print("(");
                        for (int i = 1; i < node.incomingDataFlows.length; i++) {
                            if (i > 1) {
                                pw.print(",");
                            }
                            writeExpression(node.incomingDataFlows[i]);
                        }
                        pw.println(");");
                    }

                    @Override
                    public void write(final Copy node) {
                        writeIndent();
                        final Node target = node.outgoingFlows[0];
                        final Node value = node.incomingDataFlows[0];
                        if (target instanceof PHI) {
                            pw.print(variableToName.get(target));
                        } else if (target instanceof Variable) {
                            pw.print("const ");
                            pw.print(variableToName.get(target));
                        } else {
                            throw new IllegalStateException("Invalid copy target : " + target);
                        }
                        pw.print(" = ");
                        writeExpression(value);
                        pw.println(";");
                    }

                    private void writeExpression(final Node node) {
                        if (node instanceof Variable) {
                            writeExpression((Variable) node);
                        } else if (node instanceof Sub) {
                            writeExpression((Sub) node);
                        } else if (node instanceof Add) {
                            writeExpression((Add) node);
                        } else if (node instanceof Int) {
                            writeExpression((Int) node);
                        } else if (node instanceof New) {
                            writeExpression((New) node);
                        } else if (node instanceof TypeReference) {
                            writeExpression((TypeReference) node);
                        } else if (node instanceof This) {
                            writeExpression((This) node);
                        } else {
                            throw new IllegalArgumentException("Not implemented : " + node);
                        }
                    }

                    private void writeExpression(final TypeReference node) {
                        pw.print(generateClassName(node.type));
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

                    private void writeExpression(final Variable node) {
                        pw.print(variableToName.get(node));
                    }

                    private void writeExpression(final Int node) {
                        pw.print(node.value);
                    }

                    @Override
                    public void writeIfAndStartTrueBlock(final If node, final String optionalLabel) {
                        writeIndent();
                        if (optionalLabel != null) {
                            pw.print(optionalLabel);
                            pw.print(": ");
                        }
                        pw.print("if (");

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
                                pw.print(" <= ");
                                break;
                            case NE:
                                pw.print(" != ");
                                break;
                            default:
                                throw new IllegalStateException("Not implemented : " + node.operation);
                        }

                        writeExpression(node.incomingDataFlows[1]);

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
                    public void write(final ReturnNothing node) {
                        writeIndent();
                        pw.println("return;");
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
                });

                pw.println("  }");
            }
        }
    }
}
