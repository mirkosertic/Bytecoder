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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class VTableTest {

    public static abstract class Base {

        public abstract void process();
    }

    public static class Impl extends Base {
        @Override
        public void process() {
        }
    }

    public static class Impl2 extends Impl {

        public void doNothing() {
        }
    }

    @Test
    public void testVTable() {
        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        final BytecodeLinkedClass theBaseClass = theLinkerContext.resolveClass(new BytecodeObjectTypeRef(Base.class.getName()));
        final BytecodeLinkedClass theImplClass = theLinkerContext.resolveClass(new BytecodeObjectTypeRef(Impl.class.getName()));
        final BytecodeLinkedClass theImpl2Class = theLinkerContext.resolveClass(new BytecodeObjectTypeRef(Impl2.class.getName()));
        theImpl2Class.resolveVirtualMethod("process", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));
        theImpl2Class.resolveVirtualMethod("doNothing", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0]));

        theLinkerContext.resolveAbstractMethodsInSubclasses();

        final BytecodeVTable theImpl2Table = theImpl2Class.resolveVTable();
        final List<BytecodeVTable.Slot> theImpl2Slots = theImpl2Table.sortedSlots();

        Assert.assertEquals(2, theImpl2Slots.size());
        Assert.assertEquals(0, theImpl2Slots.get(0).getPos());
        Assert.assertEquals(1, theImpl2Slots.get(1).getPos());

        Assert.assertEquals("process", theImpl2Table.slot(theImpl2Slots.get(0)).getMethodName());
        Assert.assertEquals(theImplClass.getClassName().name(), theImpl2Table.slot(theImpl2Slots.get(0)).getImplementingClass().name());

        Assert.assertEquals("doNothing", theImpl2Table.slot(theImpl2Slots.get(1)).getMethodName());
        Assert.assertEquals(theImpl2Class.getClassName().name(), theImpl2Table.slot(theImpl2Slots.get(1)).getImplementingClass().name());

        final BytecodeVTable theBaseTable = theBaseClass.resolveVTable();
        final List<BytecodeVTable.Slot> theBaseSlots = theBaseTable.sortedSlots();

        Assert.assertEquals(1, theBaseSlots.size());
        Assert.assertEquals(0, theBaseSlots.get(0).getPos());

        Assert.assertEquals("process", theBaseTable.slot(theBaseSlots.get(0)).getMethodName());
        Assert.assertEquals(theBaseClass.getClassName().name(), theBaseTable.slot(theBaseSlots.get(0)).getImplementingClass().name());

        Assert.assertEquals(theBaseSlots.get(0), theBaseTable.slotOf("process", new BytecodeMethodSignature(BytecodePrimitiveTypeRef.VOID, new BytecodeTypeRef[0])));
    }
}
