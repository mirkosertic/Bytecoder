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

import de.mirkosertic.bytecoder.annotations.Export;
import de.mirkosertic.bytecoder.backend.CompileBackend;
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

// /home/sertic/Development/Projects/wabt/build/wat2wasm -v -o ./testcode.wasm /tmp/bytecoder.wat

public class WASMSSACompilerBackend implements CompileBackend {

    @Override
    public String generateCodeFor(Logger aLogger, BytecodeLinkerContext aLinkerContext) {

        // Link required mamory management code
        BytecodeLinkedClass theManagerClass = aLinkerContext.linkClass(BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class));
        theManagerClass.linkStaticMethod("freeMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));
        theManagerClass.linkStaticMethod("usedMem", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.LONG, new BytecodeTypeRef[0]));

        theManagerClass.linkStaticMethod("free", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[] {BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class)}));
        theManagerClass.linkStaticMethod("malloc", new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));

        StringWriter theStringWriter = new StringWriter();
        PrintWriter theWriter = new PrintWriter(theStringWriter);

        theWriter.println("(module");

        theWriter.println("   (memory 256 256)");

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

                WASMSSAWriter theSSAWriter = new WASMSSAWriter(theSSAProgram, "         ", theWriter, aLinkerContext);

                for (Variable theVariable : theSSAProgram.getVariables()) {
                    if (!(theVariable.getValue() instanceof PrimitiveValue) &&
                            !(theVariable.getValue() instanceof MethodParameterValue) &&
                            !(theVariable.getValue() instanceof SelfReferenceParameterValue)) {

                        theSSAWriter.print("(local $");
                        theSSAWriter.print(theVariable.getName());
                        theSSAWriter.print(" ");
                        theSSAWriter.print(WASMWriterUtils.toType(theVariable.getType()));
                        theSSAWriter.println(")");
                    }
                }

                ControlFlowGraph.Node theNode = theSSAProgram.getControlFlowGraph().toRootNode();
                theSSAWriter.writeNode(theNode);

                theWriter.println("   )");
                theWriter.println();
            });

            BytecodeLinkedClass theLinkedClass = aEntry.getValue();
            String theClassName = WASMWriterUtils.toClassName(aEntry.getKey());

            theWriter.print("   (func ");
            theWriter.print("$");
            theWriter.print(theClassName);
            theWriter.println("__classinitcheck");

            theWriter.println("      (block $check");
            theWriter.print("         (br_if $check (i32.eq (get_global $");
            theWriter.print(theClassName);
            theWriter.println("__initialized) (i32.const 1)))");

            if (theLinkedClass.hasStaticFields()) {

                String theMallocName = WASMWriterUtils.toMethodName(
                        BytecodeObjectTypeRef.fromRuntimeClass(MemoryManager.class),
                        "malloc",
                        new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(
                                Address.class), new BytecodeTypeRef[] {BytecodePrimitiveTypeRef.INT}));

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
                theWriter.println("__staticdata i32 (i32.const 0))");
            };

            theWriter.print("   (global $");
            theWriter.print(WASMWriterUtils.toClassName(aEntry.getKey()));
            theWriter.println("__initialized i32 (i32.const 0))");
        });

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

        theWriter.println(")");
        theWriter.flush();;

        return theStringWriter.toString();
    }

    @Override
    public String generatedFileName() {
        return "bytecoder.wat";
    }
}