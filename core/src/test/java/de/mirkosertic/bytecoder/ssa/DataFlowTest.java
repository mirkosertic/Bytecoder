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

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class DataFlowTest {

    @Test
    public void testAdd() {
        final IntegerValue int1 = new IntegerValue(10);
        final Variable theVar1 = new Variable(TypeRef.Native.INT, "var1");
        theVar1.initializeWith(int1);

        final Variable theVar2 = new Variable(TypeRef.Native.INT, "var2");
        final IntegerValue int2 = new IntegerValue(10);
        theVar2.initializeWith(int2);

        final Variable theVar3 = new Variable(TypeRef.Native.INT, "var3");
        final BinaryExpression b = new BinaryExpression(TypeRef.Native.INT, theVar1, BinaryExpression.Operator.ADD, theVar2);
        theVar3.initializeWith(b);

        final List<Value> theIncomingDataFlows = theVar3.incomingDataFlowsRecursive();
        assertEquals(6, theIncomingDataFlows.size(), 0);
        assertTrue(theIncomingDataFlows.contains(int1));
        assertTrue(theIncomingDataFlows.contains(theVar1));
        assertTrue(theIncomingDataFlows.contains(int2));
        assertTrue(theIncomingDataFlows.contains(theVar2));
        assertTrue(theIncomingDataFlows.contains(b));
        assertTrue(theIncomingDataFlows.contains(theVar3));

        assertTrue(theVar3.isTrulyFunctional());

        final IntegerValue theInt3 = new IntegerValue(100);
        theVar3.replaceIncomingDataEdgeRecursive(int1, theInt3);

        final List<Value> theNewIncomingDataFlows = theVar3.incomingDataFlowsRecursive();
        assertEquals(6, theNewIncomingDataFlows.size(), 0);
        assertTrue(theNewIncomingDataFlows.contains(theInt3));
        assertTrue(theNewIncomingDataFlows.contains(theVar1));
        assertTrue(theNewIncomingDataFlows.contains(int2));
        assertTrue(theNewIncomingDataFlows.contains(theVar2));
        assertTrue(theNewIncomingDataFlows.contains(b));
        assertTrue(theIncomingDataFlows.contains(theVar3));

        assertTrue(theVar3.isTrulyFunctional());
    }
}
