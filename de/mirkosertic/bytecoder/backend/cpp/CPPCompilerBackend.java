/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.cpp;

import de.mirkosertic.bytecoder.allocator.AbstractAllocator;
import de.mirkosertic.bytecoder.api.EmulatedByRuntime;
import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.CompileResult;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.classlib.VM;
import de.mirkosertic.bytecoder.core.AnalysisStack;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassTopologicOrder;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeResolvedMethods;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.MethodHandleExpression;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGeneratorFactory;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.stackifier.HeadToHeadControlFlowException;
import de.mirkosertic.bytecoder.stackifier.Stackifier;
import org.apache.commons.io.IOUtils;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CPPCompilerBackend implements CompileBackend<CPPCompileResult> {

    private final ProgramGeneratorFactory programGeneratorFactory;
    private final CPPIntrinsics intrinsics;

    public CPPCompilerBackend(final ProgramGeneratorFactory aProgramGeneratorFactory) {
        programGeneratorFactory = aProgramGeneratorFactory;
        intrinsics = new CPPIntrinsics();
    }

    @Override
    public BytecodeLinkerContext newLinkerContext(final BytecodeLoader bytecodeLoader, final Logger logger) {
        return new BytecodeLinkerContext(bytecodeLoader, logger, programGeneratorFactory, intrinsics);
    }

    @Override
    public CPPCompileResult generateCodeFor(final CompileOptions aOptions,
                                           final BytecodeLinkerContext aLinkerContext,
                                           final Class aEntryPointClass,
                                           final String aEntryPointMethodName,
                                           final BytecodeMethodSignature aEntryPointSignature,
                                           final AnalysisStack analysisStack) {

        final BytecodeLinkedClass vmClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(VM.class), analysisStack);
        vmClass.resolveStaticMethod("newStringUTF8", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                String.class), new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1)}), analysisStack);
        vmClass.resolveStaticMethod("newByteArray", new BytecodeMethodSignature(new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}), analysisStack);
        vmClass.resolveStaticMethod("setByteArrayEntry", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1), BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.BYTE}), analysisStack);

        // We need this class and constructor to build reflective metadata
        final BytecodeMethodSignature fieldClassConstructorSignature = new BytecodeMethodSignature(
                BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {
                BytecodeObjectTypeRef.fromRuntimeClass(Class.class),
                BytecodeObjectTypeRef.fromRuntimeClass(String.class),
                BytecodePrimitiveTypeRef.INT,
                BytecodeObjectTypeRef.fromRuntimeClass(Class.class),
                BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                BytecodePrimitiveTypeRef.INT
        });
        final BytecodeLinkedClass fieldClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Field.class), analysisStack);
        fieldClass.tagWith(BytecodeLinkedClass.Tag.INSTANTIATED);
        fieldClass.resolveConstructorInvocation(fieldClassConstructorSignature, analysisStack);

        // We need this package-private constructor in String.class for bootstrap
        aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(String.class), analysisStack)
                .resolveConstructorInvocation(new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID,
                        new BytecodeTypeRef[] {new BytecodeArrayTypeRef(BytecodePrimitiveTypeRef.BYTE, 1),BytecodePrimitiveTypeRef.BYTE}), analysisStack);

        // We need runtime classes for Primitives
        final BytecodeLinkedClass byteClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Byte.class), analysisStack);
        final BytecodeLinkedClass integerClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Integer.class), analysisStack);
        final BytecodeLinkedClass charClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Character.class), analysisStack);
        final BytecodeLinkedClass booleanClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Boolean.class), analysisStack);
        final BytecodeLinkedClass floatClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Float.class), analysisStack);
        final BytecodeLinkedClass doubleClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Double.class), analysisStack);
        final BytecodeLinkedClass longClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Long.class), analysisStack);
        final BytecodeLinkedClass shortClass = aLinkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Short.class), analysisStack);

        aLinkerContext.finalizeLinkage();

        final ConstantPool constantPool = new ConstantPool();

        final BytecodeClassTopologicOrder orderedClasses = new BytecodeClassTopologicOrder(aLinkerContext);

        final CPPCompileResult compileResult = new CPPCompileResult();

        orderedClasses.getClassesInOrder().stream().forEach(classEntry -> {

            final BytecodeLinkedClass linkedClass = classEntry;
            final BytecodeResolvedMethods classMethods = aLinkerContext.resolveMethods(linkedClass);

            final CPPMinifier minifier = new CPPMinifier(aOptions);

            final List<String> privateParts = new ArrayList<>();
            final List<String> protectedParts = new ArrayList<>();
            final List<String> publicParts = new ArrayList<>();
            final List<BytecodeMethod> implMethods = new ArrayList<>();

            final String className = minifier.toClassName(classEntry.getClassName());
            if (linkedClass.getSuperClass() != null) {
                minifier.typeRefToString(linkedClass.getSuperClass().getClassName());
            }
            for (final BytecodeLinkedClass impl : linkedClass.getImplementingTypes(false, false)) {
                minifier.typeRefToString(impl.getClassName());
            }

            final BytecodeResolvedFields instanceFields = linkedClass.resolvedFields();
            instanceFields.streamForStaticFields().forEach(
                    aFieldEntry -> {
                        if (aFieldEntry.getProvidingClass() == linkedClass) {
                            final BytecodeTypeRef fieldType = aFieldEntry.getValue().getTypeRef();
                            final StringWriter decl = new StringWriter();
                            final CPPPrintWriter declWriter = new CPPPrintWriter(decl, minifier);

                            declWriter.tab(2).text("static ").text(minifier.typeRefToString(fieldType)).text(" ").text(minifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).text(";");
                            declWriter.flush();
                            if (aFieldEntry.getValue().getAccessFlags().isPrivate()) {
                                privateParts.add(decl.toString());
                            } else if (aFieldEntry.getValue().getAccessFlags().isProtected()) {
                                protectedParts.add(decl.toString());
                            } else {
                                publicParts.add(decl.toString());
                            }
                        }
                    });

            instanceFields.streamForInstanceFields().forEach(
                    aFieldEntry -> {
                        if (aFieldEntry.getProvidingClass() == linkedClass) {
                            final BytecodeTypeRef fieldType = aFieldEntry.getValue().getTypeRef();
                            final StringWriter decl = new StringWriter();
                            final CPPPrintWriter declWriter = new CPPPrintWriter(decl, minifier);

                            declWriter.tab(2).text(minifier.typeRefToString(fieldType)).text(" ").text(minifier.toSymbol(aFieldEntry.getValue().getName().stringValue())).text(";");
                            declWriter.flush();
                            if (aFieldEntry.getValue().getAccessFlags().isPrivate()) {
                                privateParts.add(decl.toString());
                            } else if (aFieldEntry.getValue().getAccessFlags().isProtected()) {
                                protectedParts.add(decl.toString());
                            } else {
                                publicParts.add(decl.toString());
                            }
                        }
                    });

            classMethods.stream().forEach(aEntry -> {
                final BytecodeMethod method = aEntry.getValue();
                final BytecodeMethodSignature currentMethodSignature = method.getSignature();

                // If the method is provided by the runtime, we do not need to generate the implementation
                if (null != method.getAttributes().getAnnotationByType(EmulatedByRuntime.class.getName())) {
                    return;
                }

                if (!(aEntry.getProvidingClass() == linkedClass)) {
                    return;
                }

                final StringWriter contentWriter = new StringWriter();
                final CPPPrintWriter methodBodyWriter = new CPPPrintWriter(contentWriter, minifier);

                aLinkerContext.getLogger().info("Compiling {}.{}", linkedClass.getClassName().name(), method.getName().stringValue());

                final Program ssaProgram = method.getProgram();

                //Run optimizer
                if (!method.getAccessFlags().isNative() && !method.getAccessFlags().isAbstract()) {
                    aOptions.getOptimizer().optimize(this, ssaProgram.getControlFlowGraph(), aLinkerContext, analysisStack);

                    implMethods.add(method);
                }

                final StringBuilder methodArguments = new StringBuilder();
                boolean myfirst = true;
                for (final Variable variable : ssaProgram.getArguments()) {
                    if (!Variable.THISREF_NAME.equals(variable.getName())) {
                        if (!myfirst) {
                            methodArguments.append(',');
                        }
                        final TypeRef t = variable.resolveType();
                        methodArguments.append(minifier.typeRefToString(t));
                        if (t.isArray()) {
                            methodArguments.append(" *");
                        } else {
                            methodArguments.append(" ");
                        }
                        methodArguments.append(minifier.toVariableName(variable.getName()));
                        myfirst = false;
                    }
                }

                final String returnType = minifier.typeRefToString(method.getSignature().getReturnType());

                methodBodyWriter.tab(2).text("// ").text(method.getSignature().toString()).newLine();
                methodBodyWriter.tab(2);
                if (method.getAccessFlags().isAbstract()) {
                    methodBodyWriter.text("virtual ");
                }
                if (method.getAccessFlags().isStatic()) {
                    methodBodyWriter.text("static ").text(returnType).text(" ").text(minifier.toMethodName(method.getName().stringValue(), currentMethodSignature))
                            .text("(").text(methodArguments.toString()).text(")");
                } else if (method.isConstructor()) {
                    methodBodyWriter.text(returnType).text(" ").text(minifier.toMethodName(method.getName().stringValue(), currentMethodSignature))
                            .text("(").text(methodArguments.toString()).text(")");
                } else {
                    methodBodyWriter.text(returnType).text(" ").text(minifier.toMethodName(method.getName().stringValue(), currentMethodSignature))
                            .text("(").text(methodArguments.toString()).text(")");
                }

                if (method.getAccessFlags().isAbstract()) {
                    methodBodyWriter.text(" = 0");
                }
                methodBodyWriter.text(";").newLine();
                methodBodyWriter.flush();

                methodBodyWriter.flush();

                if (method.getAccessFlags().isPrivate()) {
                    privateParts.add(contentWriter.toString());
                } else if (method.getAccessFlags().isProtected()) {
                    protectedParts.add(contentWriter.toString());
                } else {
                    publicParts.add(contentWriter.toString());
                }
            });

            // Generate Header file
            {
                final StringWriter classWriter = new StringWriter();
                final CPPPrintWriter classPrintWriter = new CPPPrintWriter(classWriter, minifier);
                classPrintWriter.print("#ifndef ").print(className).print("_H").newLine();
                classPrintWriter.print("#define ").print(className).print("_H").newLine();
                classPrintWriter.newLine();
                //classPrintWriter.print("#include <cstdint>").newLine();
                //classPrintWriter.newLine();
                classPrintWriter.print("#include \"Runtimeclass.hpp\"").newLine();
                classPrintWriter.print("#include \"Array.hpp\"").newLine();

                String superClass = null;
                if (linkedClass.getSuperClass() != null) {
                    superClass = minifier.typeRefToString(linkedClass.getSuperClass().getClassName()).replace("*", "");
                }

                final List<String> staticDependencies = minifier.staticDependencies();

                if (superClass != null) {
                    classPrintWriter.print("#include \"").text(superClass).text(".hpp\"").newLine();
                    staticDependencies.remove(superClass);
                }
                for (final BytecodeLinkedClass impl : linkedClass.getImplementingTypes(false, false)) {
                    final String inter = minifier.typeRefToString(impl.getClassName()).replace("*", "");
                    if (!inter.equals(superClass)) {
                        classPrintWriter.print("#include \"").text(inter).text(".hpp\"").newLine();
                        staticDependencies.remove(inter);
                    }
                }

                for (final String depClassName : staticDependencies) {
                        classPrintWriter.print("class ").print(depClassName).print(";").newLine();
                }

                classPrintWriter.newLine();

                if (linkedClass.getSuperClass() != null) {
                    classPrintWriter.print("class ").print(className).print(" : ").newLine();
                    final List<BytecodeLinkedClass> types = new ArrayList<>(linkedClass.getImplementingTypes(false, false));
                    boolean first = true;
                    for (final BytecodeLinkedClass impl : types) {
                        final String interf = minifier.typeRefToString(impl.getClassName()).replace("*", "");
                        if (!interf.equals(superClass)) {
                            if (!first) {
                                classPrintWriter.text(",").newLine();
                            }
                            first = false;
                            classPrintWriter.tab(2).text("public ").text(interf);
                        }
                    }
                    if (first) {
                        classPrintWriter.text("public jlObject");
                    }
                    classPrintWriter.text(" {").newLine();
                } else {
                    classPrintWriter.print("class ").print(className).print(" {").newLine();
                }
                classPrintWriter.tab().print("private:").newLine();
                classPrintWriter.tab(2).print("static Runtimeclass* __java__runtimeclass;").newLine();

                for (final String part : privateParts) {
                    classPrintWriter.print(part).newLine();
                }

                if (!protectedParts.isEmpty()) {
                    classPrintWriter.tab().print("protected:").newLine();
                    for (final String part : protectedParts) {
                        classPrintWriter.print(part).newLine();
                    }
                }

                if (!publicParts.isEmpty()) {
                    classPrintWriter.tab().print("public:").newLine();
                    for (final String part : publicParts) {
                        classPrintWriter.print(part).newLine();
                    }
                }

                classPrintWriter.newLine();

                classPrintWriter.print("};").newLine();
                classPrintWriter.print("#endif").newLine();
                classPrintWriter.flush();

                // Add the file to the compile result
                compileResult.add(new CompileResult.StringContent(className + ".hpp", classWriter.toString()));
            }
            // Generate Impl file
            {
                final CPPSSAWriter.IDResolver idResolver = new CPPSSAWriter.IDResolver() {
                    @Override
                    public String methodHandleDelegateFor(final MethodHandleExpression e) {
                        return "methodhandle";
                    }
                };

                final StringWriter classWriter = new StringWriter();
                final CPPPrintWriter classPrintWriter = new CPPPrintWriter(classWriter, minifier);
                //classPrintWriter.print("#include <cstdint>").newLine();
                //classPrintWriter.newLine();
                classPrintWriter.print("#include \"Runtimeclass.hpp\"").newLine();
                classPrintWriter.print("#include \"").print(className).print(".hpp\"").newLine();
                for (final String depClassName : minifier.staticDependencies()) {
                    classPrintWriter.print("#include \"").print(depClassName).print(".hpp\"").newLine();
                }

                classPrintWriter.newLine();

                for (final BytecodeMethod implMethod : implMethods) {
                    final BytecodeMethodSignature methodSignature = implMethod.getSignature();
                    final Program ssaProgram = implMethod.getProgram();

                    final StringBuilder methodArgumentsAsString = new StringBuilder();
                    boolean myfirst = true;
                    for (final Variable variable : ssaProgram.getArguments()) {
                        if (!Variable.THISREF_NAME.equals(variable.getName())) {
                            if (!myfirst) {
                                methodArgumentsAsString.append(',');
                            }
                            final TypeRef t = variable.resolveType();
                            methodArgumentsAsString.append(minifier.typeRefToString(t));
                            if (t.isArray()) {
                                methodArgumentsAsString.append(" *");
                            } else {
                                methodArgumentsAsString.append(" ");
                            }
                            methodArgumentsAsString.append(minifier.toVariableName(variable.getName()));
                            myfirst = false;
                        }
                    }

                    final String returnType = minifier.typeRefToString(implMethod.getSignature().getReturnType());

                    classPrintWriter.text("// ").text(implMethod.getSignature().toString()).newLine();
                    if (implMethod.isConstructor()) {
                        classPrintWriter.text(returnType).text(" ").text(className).text("::").text(minifier.toMethodName(implMethod.getName().stringValue(), methodSignature))
                                .text("(").text(methodArgumentsAsString.toString()).text(")");
                    } else {
                        classPrintWriter.text(returnType).text(" ").text(className).text("::").text(minifier.toMethodName(implMethod.getName().stringValue(), methodSignature))
                                .text("(").text(methodArgumentsAsString.toString()).text(")");
                    }

                    classPrintWriter.text(" {").newLine();

                    // Perform register allocation
                    final AbstractAllocator allocator = aOptions.getAllocator().allocate(ssaProgram, Variable::resolveType, aLinkerContext);

                    final CPPSSAWriter variablesWriter = new CPPSSAWriter(aOptions, ssaProgram, 2, classPrintWriter, aLinkerContext, constantPool, false, minifier, allocator, idResolver, analysisStack);
                    variablesWriter.printRegisterDeclarations();

                    // Try to reloop it or stackify it!
                    try {
                        if (aOptions.isPreferStackifier()) {
                            try {
                                final Stackifier stackifier = new Stackifier(ssaProgram.getControlFlowGraph());
                                variablesWriter.printStackified(stackifier);

                                aOptions.getLogger().debug("Method {}.{} successfully stackified ", linkedClass.getClassName().name(), implMethod.getName().stringValue());

                            } catch (final HeadToHeadControlFlowException e) {

                                // Stackifier has problems, we fallback to relooper instead
                                aOptions.getLogger().warn("Method {}.{} could not be stackified, using Relooper instead", linkedClass.getClassName().name(), implMethod.getName().stringValue());

                                final Relooper relooper = new Relooper(aOptions);
                                final Relooper.Block reloopedBlock = relooper.reloop(ssaProgram.getControlFlowGraph());

                                variablesWriter.printRelooped(reloopedBlock);
                            }
                        } else {

                            final Relooper relooper = new Relooper(aOptions);
                            final Relooper.Block reloopedBlock = relooper.reloop(ssaProgram.getControlFlowGraph());

                            variablesWriter.printRelooped(reloopedBlock);
                        }
                    } catch (final Exception e) {
                        throw new IllegalStateException("General error while processing " + linkedClass.getClassName().name() + '.'
                                + implMethod.getName().stringValue(), e);
                    }

                    classPrintWriter.text("}").newLine().newLine();

                    classPrintWriter.flush();
                }

                classPrintWriter.flush();

                // Add the file to the compile result
                compileResult.add(new CompileResult.StringContent(className + ".cpp", classWriter.toString()));
            }
        });

        try {
            compileResult.add(new CompileResult.StringContent("Array.hpp", IOUtils.toString(getClass().getResourceAsStream("/code/Array.hpp"), StandardCharsets.UTF_8)));
            compileResult.add(new CompileResult.StringContent("Runtimeclass.hpp", IOUtils.toString(getClass().getResourceAsStream("/code/Runtimeclass.hpp"), StandardCharsets.UTF_8)));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return compileResult;
    }

    public String toArray(final String aData) {
        final byte[] utf8Data = aData.getBytes(StandardCharsets.ISO_8859_1);
        final StringBuilder result = new StringBuilder("[");
        for (int i=0;i<utf8Data.length;i++) {
            if (i>0) {
                result.append(",");
            }
            result.append(utf8Data[i]);
        }
        result.append("]");
        return result.toString();
    }
}