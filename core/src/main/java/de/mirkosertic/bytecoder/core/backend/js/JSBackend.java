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

import de.mirkosertic.bytecoder.api.ClassLibProvider;
import de.mirkosertic.bytecoder.core.ReflectionConfiguration;
import de.mirkosertic.bytecoder.core.backend.CodeGenerationFailure;
import de.mirkosertic.bytecoder.core.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.backend.CompileResult;
import de.mirkosertic.bytecoder.core.backend.StringConcatMethod;
import de.mirkosertic.bytecoder.core.backend.StringConcatRegistry;
import de.mirkosertic.bytecoder.core.backend.sequencer.DominatorTree;
import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.ir.AnnotationUtils;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedField;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.optimizer.Optimizer;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.parser.ConstantPool;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.mirkosertic.bytecoder.core.backend.js.JSHelpers.*;

public class JSBackend {

    private void generateHeader(final CompileUnit compileUnit, final PrintWriter pw) {

        try {
            final String runtimeCode = IOUtils.resourceToString("/runtime.js", StandardCharsets.UTF_8);
            pw.println(runtimeCode);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        pw.println("bytecoder.imports[\"java.lang.Class\"][\"Ljava$lang$Class$$forName$Ljava$lang$String$$Z$Ljava$lang$ClassLoader$\"] = function(className, initialize, classLoader) {");

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

        final StringConcatRegistry stringConcatRegistry = new StringConcatRegistry();

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

                generateMethodsFor(pw, compileUnit, cl, compileOptions, stringConcatRegistry);

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

                generateMethodsFor(pw, compileUnit, cl, compileOptions, stringConcatRegistry);

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

        // Generate string concatenation
        final List<StringConcatMethod> stringConcatMethods = stringConcatRegistry.getMethods();
        for (int i = 0; i < stringConcatMethods.size(); i++) {
            stringConcatMethods.get(i).generateCode(pw, i);
        }

        // Generate exports
        compileUnit.processExportedMethods((s, method) -> {
            pw.print("bytecoder.exports['");
            pw.print(s);
            pw.print("'] = ");
            pw.print(generateClassName(method.owner.type));
            pw.print(".");
            if (!Modifier.isStatic(method.methodNode.access)) {
                pw.print("prototype.");
            }
            pw.print(generateMethodName(method.methodNode.name, method.methodType));
            pw.println(";");
        });

        pw.flush();

        final JSCompileResult result = new JSCompileResult();
        if (!StringUtils.isEmpty(compileOptions.getFilenamePrefix())) {
            result.add(new CompileResult.StringContent(compileOptions.getFilenamePrefix() + "classes.js", sw.toString()));
        } else {
            result.add(new CompileResult.StringContent("classes.js", sw.toString()));
        }

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
                compileOptions.getLogger().warn("Cannot find resource {}", theResource);
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
            final String methodName = generateMethodName(m.methodNode.name, m.methodType);
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
                pw.print(generateMethodName("<clinit>", cl.classInitializer.methodType));
                pw.println("();");
            }
            pw.println("    }");
            pw.println("    return this;");

            pw.println("  }");
        }
    }

    private void generateFieldsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl) {

        pw.println("  nativeObject = null;");

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

    public void generateMethodsFor(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final CompileOptions compileOptions, final StringConcatRegistry stringConcatRegistry) {

        for (final ResolvedMethod m : cl.resolvedMethods) {
            if (m.owner == cl) {
                if (Modifier.isNative(m.methodNode.access) || AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/EmulatedByRuntime;", m.methodNode.visibleAnnotations)) {
                    generateNativeMethod(pw, compileUnit, cl, m);
                } else {
                    if (m.methodBody != null) {
                        generateMethod(pw, compileUnit, cl, m, compileOptions, stringConcatRegistry);
                    } else if (cl.isOpaqueReferenceType()) {
                        generateOpaqueAdapterMethod(pw, compileUnit, cl, m);
                    }
                }
            }
        }
    }

    public void generateNativeMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m) {
        pw.println();
        pw.print("  ");
        if (Modifier.isStatic(m.methodNode.access)) {
            pw.print("static ");
        }
        final String methodName = generateMethodName(m.methodNode.name, m.methodType);
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

        if (cl.isOpaqueReferenceType()) {

            pw.print("   return bytecoder.wrapNativeIntoTypeInstance(");

            pw.print(generateClassName(returnType));
            pw.print(",");

            String moduleName = m.owner.type.getClassName();
            String functionName = methodName;

            if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/Import;", m.methodNode.visibleAnnotations)) {
                final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/Import;", m.methodNode.visibleAnnotations);
                moduleName = (String) values.get("module");
                functionName = (String) values.get("name");
            }

            pw.print("bytecoder.imports['");
            pw.print(moduleName);
            pw.print("'].");
            pw.print(functionName);
            pw.print("(");

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

            pw.println("));");

            pw.println("  }");
        } else {

            if (!returnType.equals(Type.VOID_TYPE)) {
                pw.print("    return ");
            } else {
                pw.print("    ");
            }

            String moduleName = m.owner.type.getClassName();
            String functionName = methodName;

            if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/Import;", m.methodNode.visibleAnnotations)) {
                final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/Import;", m.methodNode.visibleAnnotations);
                moduleName = (String) values.get("module");
                functionName = (String) values.get("name");
            }

            pw.print("bytecoder.imports['");
            pw.print(moduleName);
            pw.print("'].");
            pw.print(functionName);
            pw.print("(");

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
    }

    public void generateOpaqueAdapterMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m) {
        pw.println();
        pw.print("  ");
        final String methodName = generateMethodName(m.methodNode.name, m.methodType);
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

        boolean wrapped = false;

        if (!returnType.equals(Type.VOID_TYPE)) {
            pw.print("    return ");
            if (returnType.getSort()  == Type.OBJECT) {
                wrapped = true;
                pw.print("bytecoder.wrapNativeIntoTypeInstance(");
                pw.print(generateClassName(returnType));
                pw.print(",");
            }
        } else {
            pw.print("    ");
        }

        pw.print("this.nativeObject");

        if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", m.methodNode.visibleAnnotations)) {

            final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueProperty;", m.methodNode.visibleAnnotations);
            final String propertyName = (String) values.get("value");
            pw.print(".");
            if (propertyName != null) {
                pw.print(propertyName);
            } else {
                pw.print(m.methodNode.name);
            }
            if (arguments.length > 0) {
                if (arguments[0].getSort() == Type.OBJECT) {
                    pw.print(" = arg0.nativeObject");
                } else {
                    if (arguments[0].getSort() == Type.BOOLEAN) {
                        pw.print(" = (arg0 == 1 ? true : false)");
                    } else {
                        pw.print(" = arg0");
                    }
                }
            }
        } else if (AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/OpaqueIndexed;", m.methodNode.visibleAnnotations)) {

            pw.print("[arg0]");

            if (arguments.length > 1) {
                if (arguments[1].getSort() == Type.OBJECT) {
                    pw.print(" = arg1.nativeObject");
                } else {
                    pw.print(" = arg1");
                }
            }

        } else {

            pw.print(".");
            pw.print(m.methodNode.name);
            pw.print("(");

            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    pw.print(", ");
                }

                final Type argType = arguments[i];
                if (argType == Type.BYTE_TYPE) {
                    pw.print("arg");
                    pw.print(i);
                } else if (argType == Type.CHAR_TYPE) {
                    pw.print("arg");
                    pw.print(i);
                } else if (argType == Type.SHORT_TYPE) {
                    pw.print("arg");
                    pw.print(i);
                } else if (argType == Type.INT_TYPE) {
                    pw.print("arg");
                    pw.print(i);
                } else if (argType == Type.LONG_TYPE) {
                    pw.print("arg");
                    pw.print(i);
                } else if (argType == Type.FLOAT_TYPE) {
                    pw.print("arg");
                    pw.print(i);
                } else if (argType == Type.DOUBLE_TYPE) {
                    pw.print("arg");
                    pw.print(i);
                } else if (argType == Type.BOOLEAN_TYPE) {
                    pw.print("(arg");
                    pw.print(i);
                    pw.print(" == 1 ? true : false)");
                } else {
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
                                case Type.BYTE: {
                                    pw.print("arg");
                                    pw.print(j);
                                    break;
                                }
                                case Type.CHAR: {
                                    pw.print("arg");
                                    pw.print(j);
                                    break;
                                }
                                case Type.SHORT: {
                                    pw.print("arg");
                                    pw.print(j);
                                    break;
                                }
                                case Type.INT: {
                                    pw.print("arg");
                                    pw.print(j);
                                    break;
                                }
                                case Type.LONG: {
                                    pw.print("arg");
                                    pw.print(j);
                                    break;
                                }
                                case Type.FLOAT: {
                                    pw.print("arg");
                                    pw.print(j);
                                    break;
                                }
                                case Type.DOUBLE: {
                                    pw.print("arg");
                                    pw.print(j);
                                    break;
                                }
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
                                    throw new IllegalStateException("Type " + methodType.getArgumentTypes()[j] + " not supported in opaque reference method of " + callbackMethod.owner.type + "." + callbackMethod.methodNode.name);
                                }
                            }
                        }
                        pw.print(")");
                        pw.print("}.bind(arg");
                        pw.print(i);
                        pw.print(")");
                    } else {
                        pw.print("arg");
                        pw.print(i);
                        pw.print(".nativeObject");
                    }
                }
            }

            pw.print(")");
        }

        if (wrapped) {
            pw.print(")");
        }

        pw.println(";");

        pw.println("  }");
    }

    public void generateMethod(final PrintWriter pw, final CompileUnit compileUnit, final ResolvedClass cl, final ResolvedMethod m, final CompileOptions options, final StringConcatRegistry stringConcatRegistry) {
        pw.println();
        pw.print("  ");
        if (Modifier.isStatic(m.methodNode.access)) {
            pw.print("static ");
        }
        final String methodName = generateMethodName(m.methodNode.name, m.methodType);
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


        while (o.optimize(m)) {
            //
        }

        final DominatorTree dt = new DominatorTree(g);

        if (cl.classNode.sourceFile != null) {
            pw.print("    // source file is ");
            pw.println(cl.classNode.sourceFile);
        }

        try {
            new Sequencer(g, dt, new JSStructuredControlflowCodeGenerator(compileUnit, cl, pw, stringConcatRegistry));
        } catch (final RuntimeException e) {
            throw new CodeGenerationFailure(m, dt, e);
        }

        pw.println("  }");
    }
}
