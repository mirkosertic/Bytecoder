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

import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeConstantPool;
import de.mirkosertic.bytecoder.core.BytecodeMethod;

public class JSBackend {

    public void generateCodeFor(BytecodeConstantPool aConstantPool, BytecodeMethod aMethod) {
        BytecodeCodeAttributeInfo theCode = aMethod.attributeByType(BytecodeCodeAttributeInfo.class);
        System.out.println("var " + aMethod.getName().stringValue() + " = function() {");
        for (int i=0;i<theCode.getMaxLocals();i++) {
            System.out.println("    var l" + i+";");
        }
        System.out.println("    var s = [" + theCode.getMaxStack() + "];");
        System.out.println("    var so = 0;");
        System.out.println("}");
    }
}
