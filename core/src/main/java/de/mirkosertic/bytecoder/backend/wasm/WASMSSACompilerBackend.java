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

import de.mirkosertic.bytecoder.backend.CompileBackend;
import de.mirkosertic.bytecoder.core.*;
import de.mirkosertic.bytecoder.ssa.*;

import java.io.PrintWriter;
import java.io.StringWriter;

// /home/sertic/Development/Projects/wabt/build/wat2wasm -v -o ./testcode.wasm /tmp/bytecoder.wat

public class WASMSSACompilerBackend implements CompileBackend {

    @Override
    public String generateCodeFor(Logger aLogger, BytecodeLinkerContext aLinkerContext) {
        StringWriter theStringWriter = new StringWriter();
        PrintWriter theWriter = new PrintWriter(theStringWriter);
        theWriter.println("(module");

        aLinkerContext.forEachClass(aEntry -> {

            System.out.println(aEntry.getKey().name());

            if (aEntry.getValue().getBytecodeClass().getAccessFlags().isInterface()) {
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
                    theWriter.print("(param $thisref");
                    theWriter.print(" ");
                    theWriter.print(WASMWriterUtils.toType((BytecodeObjectTypeRef) null));
                    theWriter.print(") ");
                }

                for (int i=0;i<theSignature.getArguments().length;i++) {
                    BytecodeTypeRef theParamType = theSignature.getArguments()[i];
                    theWriter.print("(param $p");
                    theWriter.print(i);
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

                for (Variable theVariable : theSSAProgram.getVariables()) {
                    if (!(theVariable.getValue() instanceof PrimitiveValue) &&
                            !(theVariable.getValue() instanceof MethodParameterValue) &&
                            !(theVariable.getValue() instanceof SelfReferenceParameterValue)) {

                        theWriter.print("         (local $");
                        theWriter.print(theVariable.getName());
                        theWriter.print(" ");
                        theWriter.print(WASMWriterUtils.toType(theVariable.getType()));
                        theWriter.println(")");
                    }
                }

                ControlFlowGraph.Node theNode = theSSAProgram.getControlFlowGraph().toRootNode();

                WASMSSAWriter theSSAWriter = new WASMSSAWriter(theSSAProgram, "         ", theWriter, aLinkerContext);
                theSSAWriter.writeNode(theNode);

                theWriter.println("   )");
                theWriter.println();
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