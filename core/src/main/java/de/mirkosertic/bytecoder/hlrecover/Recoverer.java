/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.hlrecover;

import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeProgram;

public class Recoverer {

    public Element generateFrom(BytecodeClass aOwningClass, BytecodeMethod aMethod) {
        final BytecodeCodeAttributeInfo theCode = aMethod.getCode(aOwningClass);
        final BytecodeProgram theProgram = theCode.getProgram();

        // First, we create the program flow information
        final BytecodeProgram.FlowInformation flowinfo = theProgram.toFlow();

        return null;
    }
}