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

import de.mirkosertic.bytecoder.asm.AnnotationUtils;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedField;
import de.mirkosertic.bytecoder.asm.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizations;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.ConstantPool;
import de.mirkosertic.bytecoder.asm.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateClassName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateFieldName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateMethodName;

public class JSBackend {

    private void generateHeader(final PrintWriter pw) {

        pw.println("const bytecoder = {");
        pw.println("  imports: {");
        pw.println("    \"java.lang.Math.I$min$I$I\": function(a,b) {");
        pw.println("        return Math.min(a,b);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.F$min$F$F\": function(a,b) {");
        pw.println("        return Math.min(a,b);");
        pw.println("    },");
        pw.println("    \"java.lang.Math.I$max$I$I\": function(a,b) {");
        pw.println("        return Math.max(a,b);");
        pw.println("    },");
        pw.println("    \"java.lang.StringUTF16.Z$isBigEndian$$\": function(a,b) {");
        pw.println("        return 1;");
        pw.println("    },");
        pw.println("    \"java.lang.Class.Ljava$lang$ClassLoader$$getClassLoader$$\": function(classRef) {");
        pw.println("        return null;");
        pw.println("    },");
        pw.println("    \"java.lang.Class.Ljava$lang$Class$$forName$Ljava$lang$String$$Z$Ljava$lang$ClassLoader$\": function(className, initialize, classLoader) {");
        pw.println("        return sun$nio$cs$ISO_8859_1.$rt;");
        pw.println("    }");
        pw.println("  },");
        pw.println("  exports: {},");
        pw.println("  stringconstants: [],");
        pw.println("  cmp: function(a,b) {");
        pw.println("    if (a > b) return 1;");
        pw.println("    if (a < b) return -1;");
        pw.println("    return 0;");
        pw.println("  },");
        pw.println("  newRuntimeClassFor: function(type) {");
        pw.println("    return {");
        pw.println("      Ljava$lang$ClassLoader$$getClassLoader$$: function() {");
        pw.println("         return null;");
        pw.println("      },");
        pw.println("      Z$desiredAssertionStatus$$: function() {");
        pw.println("         return false;");
        pw.println("      },");
        pw.println("      Ljava$lang$Object$$newInstance$$: function() {");
        pw.println("         const x = new type.$i();");
        pw.println("         x.V$$init$$$();");
        pw.println("         return x;");
        pw.println("      }");
        pw.println("    };");
        pw.println("  }");
        pw.println("};");
        pw.println();
    }

    public void generateCodeFor(final CompileUnit compileUnit, final OutputStream out) {
        final PrintWriter pw = new PrintWriter(out);

        generateHeader(pw);

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

        // Generate string pool
        pw.println("const cs = sun$nio$cs$UTF_8.$rt.Ljava$lang$Object$$newInstance$$();");
        final String stringInitConstructor = generateMethodName("<init>", Type.getMethodType("([BLjava/nio/charset/Charset;)V"));
        final ConstantPool constantPool = compileUnit.getConstantPool();
        final List<String> pooledStrings = constantPool.getPooledStrings();
        for (int i = 0; i < pooledStrings.size(); i++) {
            pw.print("bytecoder.stringconstants[");
            pw.print(i);
            pw.print("] = new ");
            pw.print(generateClassName(Type.getType(String.class)));
            pw.println(".$i();");
            pw.print("bytecoder.stringconstants[");
            pw.print(i);
            pw.print("].");
            pw.print(stringInitConstructor);
            pw.print("([");

            final byte[] b = pooledStrings.get(i).getBytes(StandardCharsets.UTF_8);
            for (int j = 0;j < b.length; j++) {
                if (j > 0) {
                    pw.print(",");
                }
                pw.print(b[j]);
            }

            pw.println("], cs);");

        }

        // Generate exports
        compileUnit.processExportedMethods((s, method) -> {
            pw.print("bytecoder.exports['");
            pw.print(s);
            pw.print("'] = ");
            pw.print(generateClassName(method.owner.type));
            pw.print(".");
            pw.print(generateMethodName(method.methodNode.name, Type.getMethodType(method.methodNode.desc)));
            pw.println(";");
        });

        pw.flush();
    }

    private void generateClassInitFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {

        pw.println();
        pw.println("  static #rt = undefined;");
        pw.println("  static get $rt() {");
        pw.println("    if (!this.#rt) {");
        pw.print("      this.#rt = bytecoder.newRuntimeClassFor(");
        pw.print(generateClassName(cl.type));
        pw.println(");");

        pw.println("    }");
        pw.println("    return this.#rt;");
        pw.println("  }");

        if (cl.requiresClassInitializer()) {
            pw.println();
            pw.println("  static #iguard = false;");
            pw.println("  static get $i() {");
            pw.println("    if (!this.#iguard) {");
            pw.println("      this.#iguard = true;");
            if (cl.superClass != null && cl.superClass.requiresClassInitializer()) {
                pw.print("      ");
                pw.print(generateClassName(cl.superClass.type));
                pw.println(".$i;");
            }

            if (cl.classInitializer != null) {
                pw.print("      this.");
                pw.print(generateMethodName("<clinit>", Type.getMethodType(cl.classInitializer.methodNode.desc)));
                pw.println("();");
            }
            pw.println("    }");
            pw.println("    return this;");

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
            if (m.owner == cl) {
                if (Modifier.isNative(m.methodNode.access) || AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/EmulatedByRuntime;", m.methodNode.visibleAnnotations)) {
                    generateNativeMethod(pw, compileUnit, cl, m);
                } else {
                    if (m.methodBody != null) {
                        generateMethod(pw, compileUnit, cl, m);
                    }
                }
            }
        }
    }

    public void generateNativeMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m) {
        System.out.println("Writing native method for " + cl.type + " . " + m.methodNode.name + m.methodNode.desc);

        pw.println();
        pw.print("  ");
        if (Modifier.isStatic(m.methodNode.access)) {
            pw.print("static ");
        }
        final String methodName = generateMethodName(m.methodNode.name, Type.getMethodType(m.methodNode.desc));
        pw.print(methodName);

        final Type[] arguments = Type.getArgumentTypes(m.methodNode.desc);
        final Type returnType = Type.getReturnType(m.methodNode.desc);

        pw.print("(");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                pw.print(",");
            }
            pw.print("arg");
            pw.print(i);
        }
        pw.println(") {");

        if (!returnType.equals(Type.VOID_TYPE)) {
            pw.print("    return ");
        } else {
            pw.print("    ");
        }
        pw.print("bytecoder.imports['");
        pw.print(m.owner.type.getClassName());
        pw.print(".");
        pw.print(methodName);
        pw.print("'](");

        if (!Modifier.isStatic(m.methodNode.access)) {
            pw.print("this");
            for (int i = 0; i < arguments.length; i++) {
                pw.print(", arg");
                pw.print(i);
            }
        } else {
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }
                pw.print("arg");
                pw.print(i);
            }
        }

        pw.println(");");

        pw.println("  }");
    }

    public void generateMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m) {
        System.out.println("Writing method for " + cl.type + " . " + m.methodNode.name + m.methodNode.desc);

        pw.println();
        pw.print("  ");
        if (Modifier.isStatic(m.methodNode.access)) {
            pw.print("static ");
        }
        final String methodName = generateMethodName(m.methodNode.name, Type.getMethodType(m.methodNode.desc));
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
            if (cl.classNode.sourceFile != null) {
                pw.print("    // source file is ");
                pw.println(cl.classNode.sourceFile);
            }

            new Sequencer(g, dt, new JSStructuredControlflowCodeGenerator(compileUnit, cl, pw));

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
