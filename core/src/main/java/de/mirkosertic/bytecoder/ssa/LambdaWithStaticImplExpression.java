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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

public class LambdaWithStaticImplExpression extends Expression {

    public LambdaWithStaticImplExpression(final Program aProgram, final BytecodeOpcodeAddress aAddress, final Value type, final Value staticRef, final Value staticArguments, final Value name) {
        super(aProgram, aAddress);
        receivesDataFrom(type);
        receivesDataFrom(staticRef);
        receivesDataFrom(staticArguments);
        receivesDataFrom(name);
    }

    @Override
    public TypeRef resolveType() {
        return TypeRef.Native.REFERENCE;
    }

    public Value getType() {
        return incomingDataFlows().get(0);
    }

    public Value getStaticRef() {
        return incomingDataFlows().get(1);
    }

    public Value getStaticArguments() {
        return incomingDataFlows().get(2);
    }

    public Value getName() {
        return incomingDataFlows().get(3);
    }

}
