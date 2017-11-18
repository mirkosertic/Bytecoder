/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.wasm;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mirkosertic.bytecoder.annotations.Export;
import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.backend.js.JSWriterUtils;
import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.core.BytecodeAnnotation;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.Logger;
import de.mirkosertic.bytecoder.ssa.ControlFlowGraph;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.PrimitiveValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.ProgramGenerator;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.Variable;

public class WASMSSACompilerBackend implements CompileBackend {

    @Override
    public String generateCodeFor(Logger aLogger, BytecodeLinkerContext aLinkerContext, Class aEntryPointClass, String aEntryPointMethodName, BytecodeMethodSignature aEntryPointSignatue) {

        // Link required mamory management code
        BytecodeLinkedClass theManagerClass = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));
        theManagerClass.linkStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));
        theManagerClass.linkStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));

        theManagerClass.linkStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class)}));
        theManagerClass.linkStaticMethod("malloc", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));
        theManagerClass.linkStaticMethod("newObject", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT, BytecodePrimitiveTypeRef.INT}));

        String theMallocName = WASMWriterUtils.toMethodName(
                BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                "malloc",
                new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                        Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));

        StringWriter theStringWriter = new StringWriter();
        PrintWriter theWriter = new PrintWriter(theStringWriter);

        theWriter.println("(module");
        theWriter.println("   (type $RESOLVEMETHOD (func (param i32) (result i32)))");

        List<String> theGeneratedFunctions = new ArrayList<>();
        List<BytecodeLinkedClass> theLinkedClasses = new ArrayList<>();
        Set<String> theGeneratedTypes = new HashSet<>();
        aLinkerContext.forEachClass(aEntry -> {

            if (aEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }
            if (aEntry.getKey().equals(BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            theLinkedClasses.add(aEntry.getValue());

            String theClassName = WASMWriterUtils.toClassName(aEntry.getKey());

            theGeneratedFunctions.add(theClassName + "__classinitcheck");
            theGeneratedFunctions.add(theClassName + "__resolvevtableindex");

            aEntry.getValue().forEachMethod(t -> {

                // Do not generate code for abstract methods
                if (t.getAccessFlags().isAbstract()) {
                    return;
                }

                BytecodeMethodSignature theSignature = t.getSignature();

                String theMethodSignature = WASMWriterUtils.toMethodSignature(theSignature);
                if (theGeneratedTypes.add(theMethodSignature)) {
                    theWriter.print("   (type $t_");
                    theWriter.print(theMethodSignature);

                    theWriter.print(" (func ");

                    theWriter.print("(param ");
                    theWriter.print(WASMWriterUtils.toType((BytecodeObjectTypeRef) null));
                    theWriter.print(") ");

                    for (int i=0;i<theSignature.getArguments().length;i++) {
                        BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                        theWriter.print("(param ");
                        theWriter.print(WASMWriterUtils.toType(theParamType));
                        theWriter.print(") ");
                    }

                    if (!theSignature.getReturnType().isVoid()) {
                        theWriter.print("(result "); // result
                        theWriter.print(WASMWriterUtils.toType(theSignature.getReturnType()));
                        theWriter.print(")");
                    }
                    theWriter.println("))");
                }

                String theMethodName = WASMWriterUtils.toMethodName(aEntry.getKey(), t.getName(), theSignature);
                theGeneratedFunctions.add(theMethodName);
            });
        });

        theWriter.println("   (memory 256 256)");

        // Write virtual method table
        if (!theGeneratedFunctions.isEmpty()) {
            theWriter.println();
            theWriter.print("   (table ");
            theWriter.print(theGeneratedFunctions.size());
            theWriter.println(" anyfunc)");

            for (int i=0;i<theGeneratedFunctions.size();i++) {
                theWriter.print("   (elem (i32.const ");
                theWriter.print(i);
                theWriter.print(") $");
                theWriter.print(theGeneratedFunctions.get(i));
                theWriter.println(")");
            }

            theWriter.println();
        }

        theWriter.println("   (func $virtualTableByClassID (param $p1 i32) (result i32) ");

        aLinkerContext.forEachClass(t -> {

            if (t.getValue().getAccessFlags().isInterface()) {
                return;
            }
            if (t.getKey().equals(BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            theWriter.println("         ;; " + t.getKey().name());
            theWriter.println("         (block $b");
            theWriter.print("             (br_if $b (i32.ne (get_local $p1) (i32.const ");
            theWriter.print(t.getValue().getUniqueId());
            theWriter.println(")))");

            String theFullMethodName = WASMWriterUtils.toClassName(t.getKey()) + "__resolvevtableindex";
            int theIndex = theGeneratedFunctions.indexOf(theFullMethodName);
            if (theIndex < 0) {
                throw new IllegalStateException("Unknown index : " + theFullMethodName);
            }

            theWriter.print("             (return (i32.const ");
            theWriter.print(theIndex);
            theWriter.println("))");
            theWriter.println("         )");

        });

        theWriter.println("         (unreachable)");
        theWriter.println("   )");

        aLinkerContext.forEachClass(aEntry -> {

            System.out.println(aEntry.getKey().name());

            if (aEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }
            if (aEntry.getKey().equals(BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            aEntry.getValue().forEachMethod(t -> {

                System.out.println("   " + t.getName().stringValue());

                // Do not generate code for abstract methods
                if (t.getAccessFlags().isAbstract()) {
                    return;
                }

                BytecodeMethodSignature theSignature = t.getSignature();

                theWriter.print("   (func ");
                theWriter.print("$");
                theWriter.print(WASMWriterUtils.toMethodName(aEntry.getKey(), t.getName(), theSignature));
                theWriter.print(" ");

                if (!t.getAccessFlags().isStatic()) {
                    theWriter.print("(param $thisRef");
                    theWriter.print(" ");
                    theWriter.print(WASMWriterUtils.toType((BytecodeObjectTypeRef) null));
                    theWriter.print(") ");
                }

                for (int i=0;i<theSignature.getArguments().length;i++) {
                    BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                    theWriter.print("(param $p");
                    theWriter.print((i + 1));
                    theWriter.print(" ");
                    theWriter.print(WASMWriterUtils.toType(theParamType));
                    theWriter.print(") ");
                }

                if (!theSignature.getReturnType().isVoid()) {
                    theWriter.print("(result "); // result
                    theWriter.print(WASMWriterUtils.toType(theSignature.getReturnType()));
                    theWriter.print(")");
                }
                theWriter.println();

                ProgramGenerator theGenerator = new ProgramGenerator(aLinkerContext);
                Program theSSAProgram = theGenerator.generateFrom(aEntry.getValue().getBytecodeClass(), t);

                WASMSSAWriter.IDResolver theResolver = new WASMSSAWriter.IDResolver() {
                    @Override
                    public int resolveVTableMethodByType(BytecodeObjectTypeRef aObjectType) {
                        String theClassName = WASMWriterUtils.toClassName(aObjectType);

                        String theMethodName = theClassName + "__resolvevtableindex";
                        int theIndex = theGeneratedFunctions.indexOf(theMethodName);
                        if (theIndex < 0) {
                            throw new IllegalStateException("Cannot resolve vtable method for " + theClassName);
                        }
                        return theIndex;
                    }
                };

                WASMSSAWriter theSSAWriter = new WASMSSAWriter(theSSAProgram, "         ", theWriter, aLinkerContext, theResolver);

                for (Variable theVariable : theSSAProgram.getVariables()) {

                    if (!(theVariable.getValue() instanceof PrimitiveValue) &&
                            !(theVariable.getValue() instanceof MethodParameterValue) &&
                            !(theVariable.getValue() instanceof SelfReferenceParameterValue) &&
                            !theSSAWriter.isStackVariable(theVariable)) {

                        theSSAWriter.print("(local $");
                        theSSAWriter.print(theVariable.getName());
                        theSSAWriter.print(" ");
                        theSSAWriter.print(WASMWriterUtils.toType(theVariable.getType()));
                        theSSAWriter.print(") ;; ");
                        theSSAWriter.println(theVariable.getType().name());
                    }
                }

                ControlFlowGraph.Node theNode = theSSAProgram.getControlFlowGraph().toRootNode();
                theSSAWriter.writeStartNode(theNode);

                theWriter.println("   )");
                theWriter.println();
            });

            BytecodeLinkedClass theLinkedClass = aEntry.getValue();
            String theClassName = WASMWriterUtils.toClassName(aEntry.getKey());

            theWriter.print("   (func ");
            theWriter.print("$");
            theWriter.print(theClassName);
            theWriter.println("__resolvevtableindex (param $p1 i32) (result i32)");

            theLinkedClass.forEachVirtualMethod(t -> {

                if (!t.getValue().getTargetMethod().getAccessFlags().isAbstract()) {
                    theWriter.println("         (block $b");
                    theWriter.print("             (br_if $b (i32.ne (get_local $p1) (i32.const ");
                    theWriter.print(t.getKey().getIdentifier());
                    theWriter.println(")))");

                    BytecodeLinkedClass.LinkedMethod theMethod = t.getValue();
                    String theFullMethodName = WASMWriterUtils.toMethodName(theMethod.getDeclaringType(),
                            theMethod.getTargetMethod().getName(),
                            theMethod.getTargetMethod().getSignature());

                    int theIndex = theGeneratedFunctions.indexOf(theFullMethodName);
                    if (theIndex < 0) {
                        throw new IllegalStateException("Unknown index : " + theFullMethodName);
                    }

                    theWriter.print("             (return (i32.const ");
                    theWriter.print(theIndex);
                    theWriter.println("))");
                    theWriter.println("         )");
                }

            });

            theWriter.println("         (unreachable)");
            theWriter.println("   )");
            theWriter.println();

            theWriter.print("   (func ");
            theWriter.print("$");
            theWriter.print(theClassName);
            theWriter.println("__classinitcheck");

            theWriter.println("      (block $check");
            theWriter.print("         (br_if $check (i32.eq (get_global $");
            theWriter.print(theClassName);
            theWriter.println("__initialized) (i32.const 1)))");

            if (theLinkedClass.hasStaticFields()) {

                theWriter.print("         (set_global $");
                theWriter.print(theClassName);
                theWriter.print("__staticdata (call $");
                theWriter.print(theMallocName);
                theWriter.print(" (i32.const ");
                theWriter.print(WASMWriterUtils.computeClassSizeFor(theLinkedClass));
                theWriter.println(")))");
            }

            if (theLinkedClass.hasClassInitializer()) {
                theWriter.print("         (call $");
                theWriter.print(theClassName);
                theWriter.println("_VOIDclinit)");
            }

            theWriter.print("         (set_global $");
            theWriter.print(theClassName);
            theWriter.println("__initialized (i32.const 1))");
            theWriter.println("      )");
            theWriter.println("   )");
            theWriter.println();
        });

        theWriter.println("   (func $bootstrap");

        theWriter.print("      (set_global $STACK (call $");
        theWriter.print(theMallocName);
        theWriter.print(" (i32.const ");
        theWriter.print(8192L);
        theWriter.println(")))");

        aLinkerContext.forEachClass(theEntry -> {
            if (!theEntry.getValue().getAccessFlags().isInterface()) {

                if (!theEntry.getKey().equals(BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                    theWriter.print("      (call $");
                    theWriter.print(JSWriterUtils.toClassName(theEntry.getKey()));
                    theWriter.println("__classinitcheck)");
                }
            }
        });


        theWriter.println("   )");
        theWriter.println();


        theWriter.println("   (global $STACK (mut i32) (i32.const 0))");
        theWriter.println("   (global $STACKTOP (mut i32) (i32.const 0))");

        // Globals for static class data
        aLinkerContext.forEachClass(aEntry -> {

            if (aEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }
            if (aEntry.getKey().equals(BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                return;
            }

            if (aEntry.getValue().hasStaticFields()) {
                String theClassName = WASMWriterUtils.toClassName(aEntry.getKey());

                theWriter.print("   (global $");
                theWriter.print(theClassName);
                theWriter.println("__staticdata (mut i32) (i32.const 0))");
            };

            theWriter.print("   (global $");
            theWriter.print(WASMWriterUtils.toClassName(aEntry.getKey()));
            theWriter.println("__initialized (mut i32) (i32.const 0))");
        });

        theWriter.println();

        theWriter.println("   (export \"bootstrap\" (func $bootstrap))");

        // Write exports
        aLinkerContext.forEachClass(aEntry -> {

            System.out.println(aEntry.getKey().name());

            if (aEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {
                return;
            }

            aEntry.getValue().forEachMethod(t -> {

                BytecodeAnnotation theExport = t.getAttributes().getAnnotationByType(Export.class.getName());
                if (theExport != null) {
                    theWriter.print("   (export \"");
                    theWriter.print(theExport.getElementValueByName("value").stringValue());
                    theWriter.print("\" (func $");
                    theWriter.print(WASMWriterUtils.toMethodName(aEntry.getKey(), t.getName(), t.getSignature()));
                    theWriter.println("))");
                }
            });
        });

        theWriter.print("   (export \"main\" (func $");
        theWriter.print(WASMWriterUtils.toMethodName(BytecodeObjectTypeRef.fromRuntimeClass(aEntryPointClass), aEntryPointMethodName, aEntryPointSignatue));
        theWriter.println("))");


        theWriter.println(")");
        theWriter.flush();;

        return theStringWriter.toString();
    }

    @Override
    public String generatedFileName() {
        return "bytecoder.wat";
    }
}