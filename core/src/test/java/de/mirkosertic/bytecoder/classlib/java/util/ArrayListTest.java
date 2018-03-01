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
package de.mirkosertic.bytecoder.classlib.java.util;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class ArrayListTest {

    @Test
    public void add() throws Exception {
        ArrayList theList = new ArrayList();
        Assert.assertTrue(theList.add(new Integer(1)));
        Assert.assertTrue(theList.add(new Integer(2)));
        Assert.assertTrue(theList.add(new Integer(3)));
        Assert.assertTrue(theList.add(new Integer(4)));
        Assert.assertTrue(theList.add(new Integer(5)));
        Assert.assertTrue(theList.add(new Integer(6)));
        Assert.assertTrue(theList.add(new Integer(7)));
        Assert.assertTrue(theList.add(new Integer(8)));
        Assert.assertTrue(theList.add(new Integer(9)));
        Assert.assertTrue(theList.add(new Integer(10)));
        Assert.assertTrue(theList.add(new Integer(11)));
        Assert.assertEquals(11, theList.size(), 0);
        Assert.assertEquals(new Integer(1), theList.get(0));
        Assert.assertEquals(new Integer(2), theList.get(1));
        Assert.assertEquals(new Integer(3), theList.get(2));
        Assert.assertEquals(new Integer(4), theList.get(3));
        Assert.assertEquals(new Integer(5), theList.get(4));
        Assert.assertEquals(new Integer(6), theList.get(5));
        Assert.assertEquals(new Integer(7), theList.get(6));
        Assert.assertEquals(new Integer(8), theList.get(7));
        Assert.assertEquals(new Integer(9), theList.get(8));
        Assert.assertEquals(new Integer(10), theList.get(9));
        Assert.assertEquals(new Integer(11), theList.get(10));
    }

    @Test
    public void clear() throws Exception {
        ArrayList theList = new ArrayList();
        theList.add(new Integer(1));
        theList.add(new Integer(2));
        Assert.assertFalse(theList.isEmpty());
        theList.clear();;
        Assert.assertTrue(theList.isEmpty());
        Assert.assertEquals(0, theList.size(), 0);
    }

    @Test
    public void contains() throws Exception {
        ArrayList theList = new ArrayList();
        theList.add(new Integer(1));
        theList.add(new Integer(2));
        Assert.assertFalse(theList.contains("Hello"));
        Assert.assertFalse(theList.contains(new Integer(3)));
        Assert.assertTrue(theList.contains(new Integer(2)));
    }

    @Test
    public void isEmpty() throws Exception {
        ArrayList theList = new ArrayList();
        Assert.assertTrue(theList.isEmpty());
        theList.add("Hello");
        Assert.assertFalse(theList.isEmpty());
    }

    @Test
    public void remove() throws Exception {
        ArrayList theList = new ArrayList();
        theList.add(new Integer(1));
        theList.add(new Integer(2));
        Assert.assertTrue(theList.remove(new Integer(1)));
        Assert.assertFalse(theList.remove("lala"));
        Assert.assertEquals(1, theList.size(), 0);
        Assert.assertFalse(theList.contains(new Integer(1)));
        Assert.assertTrue(theList.contains(new Integer(2)));
    }

    @Test
    public void testToArray() {
        ArrayList theList = new ArrayList();
        theList.add("A");
        theList.add("B");
        Object[] theArray = theList.toArray();
        Assert.assertEquals(2, theArray.length, 0);
        Assert.assertEquals("A", theArray[0]);
        Assert.assertEquals("B", theArray[1]);

        theArray = theList.toArray(new Object[theList.size()]);
        Assert.assertEquals(2, theArray.length, 0);
        Assert.assertEquals("A", theArray[0]);
        Assert.assertEquals("B", theArray[1]);
    }

    @Test
    public void testIterator() {
        ArrayList theList = new ArrayList();
        theList.add("A");
        theList.add("B");
        AtomicLong theLong = new AtomicLong(0);
        for (Object a : theList) {
            theLong.incrementAndGet();
        }
        Assert.assertEquals(2, theLong.get(), 0);
    }

    @Test
    public void testAddAll() {
        ArrayList theList = new ArrayList();
        theList.add("A");

        ArrayList theList2 = new ArrayList();
        theList2.add("B");

        Assert.assertTrue(theList.addAll(theList2));
        Assert.assertEquals(2, theList.size(), 0);
        Assert.assertTrue(theList.contains("A"));
        Assert.assertTrue(theList.contains("B"));
    }

    @Test
    public void testRemoveAll() {
        ArrayList theList = new ArrayList();
        theList.add("A");
        theList.add("B");
        theList.add("C");

        ArrayList theList2 = new ArrayList();
        theList2.add("B");

        Assert.assertTrue(theList.removeAll(theList2));
        Assert.assertEquals(2, theList.size(), 0);
        Assert.assertTrue(theList.contains("A"));
        Assert.assertTrue(theList.contains("C"));
    }
}