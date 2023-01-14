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

import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedField;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;

import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateClassName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateFieldName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateMethodName;

public class JSBackend {

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

    public void generateMethodsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        for (final ResolvedMethod m : cl.resolvedMethods) {
            if (m.methodBody != null) {
                generateMethod(pw, compileUnit, cl, m);
            }
        }
    }

    public void generateMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m) {
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

        try {
            if (cl.classNode.sourceFile != null) {
                pw.print("    // source file is ");
                pw.println(cl.classNode.sourceFile);
            }

            new Sequencer(g, dt, new JSStructuredControlflowCodeGenerator(compileUnit, cl, pw));

        } catch (final Exception ex) {

            throw ex;
        }

        pw.println("  }");
    }
}
