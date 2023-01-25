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

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.asm.ir.AnnotationUtils;
import de.mirkosertic.bytecoder.asm.ir.Graph;
import de.mirkosertic.bytecoder.asm.ir.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ir.ResolvedField;
import de.mirkosertic.bytecoder.asm.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.asm.optimizer.Optimizer;
import de.mirkosertic.bytecoder.asm.parser.CompileUnit;
import de.mirkosertic.bytecoder.asm.parser.ConstantPool;
import de.mirkosertic.bytecoder.asm.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.asm.sequencer.Sequencer;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.core.ReflectionConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateClassName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateFieldName;
import static de.mirkosertic.bytecoder.asm.backend.js.JSHelpers.generateMethodName;

public class JSBackend {

    private void generateHeader(final CompileUnit compileUnit, final PrintWriter pw) {

        try {
            final String runtimeCode = IOUtils.resourceToString("/runtime.js", StandardCharsets.UTF_8);
            pw.println(runtimeCode);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        pw.println("bytecoder.imports[\"java.lang.Class.Ljava$lang$Class$$forName$Ljava$lang$String$$Z$Ljava$lang$ClassLoader$\"] = function(className, initialize, classLoader) {");

        for (final ReflectionConfiguration.ReflectiveClass rc : compileUnit.getReflectionConfiguration().configuredClasses()) {
            if (rc.supportsClassForName()) {
                final Type cl = Type.getObjectType(rc.getName().replace('.', '/'));
                final int idx = compileUnit.getConstantPool().getPooledStrings().indexOf(rc.getName());
                pw.print("  if (bytecoder.stringconstants[");
                pw.print(idx);
                pw.println("].Z$equals$Ljava$lang$Object$(className)) {");
                pw.print("    return ");
                pw.print(generateClassName(cl));
                pw.println(".$rt;");
                pw.println("  }");
            }
        }

        pw.println("  throw 'Not supported class for reflective access';");
        pw.println("};");
        pw.println();
    }

    public JSCompileResult generateCodeFor(final CompileUnit compileUnit, final CompileOptions compileOptions) {

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        generateHeader(compileUnit, pw);

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

                generateLambdaLogicFor(pw, compileUnit, cl);

                generateMethodsFor(pw, compileUnit, cl, compileOptions);

                pw.println("};");

                pw.print(className);
                pw.print(".$modifiers = ");
                pw.print(cl.classNode.access);
                pw.println(";");

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

                pw.print("  static $modifiers = ");
                pw.print(cl.classNode.access);
                pw.println(";");

                generateFieldsFor(pw, compileUnit, cl);

                pw.println("  constructor() {");
                if (cl.superClass != null) {
                    pw.println("    super();");
                }
                pw.println("  }");

                generateClassInitFor(pw, compileUnit, cl);

                generateLambdaLogicFor(pw, compileUnit, cl);

                generateMethodsFor(pw, compileUnit, cl, compileOptions);

                pw.println("}");
                pw.println();
            }
        }

        // Generate string pool
        final ConstantPool constantPool = compileUnit.getConstantPool();
        final List<String> pooledStrings = constantPool.getPooledStrings();
        for (int i = 0; i < pooledStrings.size(); i++) {
            pw.print("bytecoder.stringconstants[");
            pw.print(i);
            pw.print("] = bytecoder.toBytecoderString('");
            pw.print(StringEscapeUtils.escapeEcmaScript(pooledStrings.get(i)));
            pw.println("');");
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

        final JSCompileResult result = new JSCompileResult();
        result.add(new CompileResult.StringContent("classes.js", sw.toString()));

        final List<String> resourcesToInclude = new ArrayList<>();
        for (final ClassLibProvider provider : ClassLibProvider.availableProviders()) {
            Collections.addAll(resourcesToInclude, provider.additionalResources());
        }
        Collections.addAll(resourcesToInclude, compileOptions.getAdditionalResources());

        // Finally, we add the list of additional resources to the result
        for (final String theResource : resourcesToInclude) {
            final URL theUrl = compileUnit.getLoader().getResource(theResource);
            if (theUrl != null) {
                result.add(new CompileResult.URLContent(theResource, theUrl));
            } else {
                //aOptions.getLogger().warn("Cannot find resource {}", theResource);
            }
        }

        return result;
    }

    private void generateLambdaLogicFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {
        pw.println();
        pw.println("  set $lambdaimpl(impl) {");

        final Set<ResolvedMethod> abstractMethods = cl.abstractResolvedMethods();
        if (abstractMethods.size() == 1) {
            final ResolvedMethod m = abstractMethods.iterator().next();
            final String methodName = generateMethodName(m.methodNode.name, Type.getMethodType(m.methodNode.desc));
            pw.print("    this.");
            pw.print(methodName);
            pw.println(" = impl;");
        }
        pw.println("  }");
    }

    private void generateClassInitFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {

        pw.println();
        pw.println("  static #rt = undefined;");
        pw.println("  static get $rt() {");
        pw.println("    if (!this.#rt) {");
        pw.print("      this.#rt = bytecoder.newRuntimeClassFor(");
        pw.print(generateClassName(cl.type));
        pw.print(",[");
        boolean f = true;
        for (final ResolvedClass type : cl.allTypesOf()) {
            if (f) {
                f = false;
            } else {
                pw.print(",");
            }
            pw.print(generateClassName(type.type));
        }
        pw.println("]);");

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

    public void generateMethodsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final CompileOptions compileOptions) {
        for (final ResolvedMethod m : cl.resolvedMethods) {
            if (m.owner == cl) {
                if (Modifier.isNative(m.methodNode.access) || AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/EmulatedByRuntime;", m.methodNode.visibleAnnotations)) {
                    generateNativeMethod(pw, compileUnit, cl, m);
                } else {
                    if (m.methodBody != null) {
                        generateMethod(pw, compileUnit, cl, m, compileOptions);
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

    public void generateMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m, final CompileOptions options) {
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
        final Optimizer o = options.getOptimizer();


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
