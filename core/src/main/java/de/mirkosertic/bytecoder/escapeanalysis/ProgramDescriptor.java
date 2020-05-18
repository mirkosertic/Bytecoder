/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.escapeanalysis;

import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.ssa.Program;

public class ProgramDescriptor {

    final BytecodeLinkedClass linkedClass;
    final BytecodeMethod method;
    final Program program;

    public ProgramDescriptor(final BytecodeLinkedClass linkedClass, final BytecodeMethod method, final Program program) {
        this.linkedClass = linkedClass;
        this.method = method;
        this.program = program;
    }
}