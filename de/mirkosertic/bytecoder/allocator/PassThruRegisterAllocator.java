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
package de.mirkosertic.bytecoder.allocator;

import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class PassThruRegisterAllocator extends AbstractAllocator {

    public PassThruRegisterAllocator(final Program aProgram, final Function<Variable, TypeRef> aTypeConverter,
            final BytecodeLinkerContext aLinkerContext) {
        super(aTypeConverter, aLinkerContext);

        final List<Variable> theVariables = computeSSAReadyVariablesFor(aProgram);

        for (int i=0;i<theVariables.size(); i++) {
            final Variable v = theVariables.get(i);
            if (!v.isSynthetic()) {
                final TypeRef type = typeConverter.apply(v);

                final Register r = new Register(i, type);
                registerAssignments.put(v, r);

                final List<Register> theRegsForType = knownRegisters.computeIfAbsent(type, k -> new ArrayList<>());
                theRegsForType.add(r);
            }
        }
    }
}
