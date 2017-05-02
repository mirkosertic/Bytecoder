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
package de.mirkosertic.bytecoder.backend.js;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeConstantPool;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIRETURN;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;

public class JSBackend {

    public String generateCodeFor(BytecodeConstantPool aConstantPool, BytecodeMethod aMethod) {

        StringWriter theStrWriter = new StringWriter();
        PrintWriter theWriter = new PrintWriter(theStrWriter);

        BytecodeCodeAttributeInfo theCode = aMethod.attributeByType(BytecodeCodeAttributeInfo.class);
        BytecodeMethodSignature theSignature = aMethod.getSignature();
        StringBuffer theArguments = new StringBuffer();
        for (int i=1;i<=theSignature.getArguments().length;i++) {
            if (theArguments.length() > 0) {
                theArguments.append(",");
            }
            theArguments.append("p" + i);
        }
        theWriter.println("var " + aMethod.getName().stringValue() + " = function(" + theArguments.toString() + ") {");
        for (int i=1;i<=theCode.getMaxLocals();i++) {
            theWriter.println("    var local" + i+";");
        }
        theWriter.println("    var stack = [" + theCode.getMaxStack() + "];");
        theWriter.println("    var stackOffset = -1;");

        // Push parameters to local variables
        for (int i=1;i<=theSignature.getArguments().length;i++) {
            theWriter.println("    local" + i + " = p" + i +";");
        }

        theWriter.println("    // Begin method code");
        for (BytecodeInstruction theInstruction : theCode.getProgramm().getInstructions()) {
            if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theLoad = (BytecodeInstructionGenericLOAD) theInstruction;
                theWriter.println("    stack[++stackOffset] = local" + theLoad.getIndex() + ";");
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theAdd = (BytecodeInstructionGenericADD) theInstruction;
                theWriter.println("    {");
                theWriter.println("        var temp = stack[stackOffset--] + stack[stackOffset--];");
                theWriter.println("        stack[++stackOffset] = temp;");
                theWriter.println("    }");
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theDiv = (BytecodeInstructionGenericDIV) theInstruction;
                theWriter.println("    {");
                theWriter.println("        var temp = Math.floor(stack[stackOffset-1] / stack[stackOffset]);");
                theWriter.println("        stackOffset -= 1;");
                theWriter.println("        stack[stackOffset] = temp;");
                theWriter.println("    }");
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theDiv = (BytecodeInstructionGenericMUL) theInstruction;
                theWriter.println("    {");
                theWriter.println("        var temp = stack[stackOffset-1] * stack[stackOffset];");
                theWriter.println("        stackOffset -= 1;");
                theWriter.println("        stack[stackOffset] = temp;");
                theWriter.println("    }");
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theDiv = (BytecodeInstructionGenericSUB) theInstruction;
                theWriter.println("    {");
                theWriter.println("        var temp = stack[stackOffset-1] - stack[stackOffset];");
                theWriter.println("        stackOffset -= 1;");
                theWriter.println("        stack[stackOffset] = temp;");
                theWriter.println("    }");
            } else if (theInstruction instanceof BytecodeInstructionIRETURN) {
                BytecodeInstructionIRETURN theReturn = (BytecodeInstructionIRETURN) theInstruction;
                theWriter.println("    return stack[stackOffset];");
            } else {
                throw new IllegalStateException("Cannot compile " + theInstruction);
            }
        }

        theWriter.println("}");
        theWriter.flush();

        return theStrWriter.toString();
    }
}
