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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.classlib.org.junit.TAssert;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class TArrayListTest {

    @Test
    public void add() throws Exception {
        TArrayList theList = new TArrayList();
        theList.add(new Integer(1));
        theList.add(new Integer(2));
        theList.add(new Integer(3));
        theList.add(new Integer(4));
        theList.add(new Integer(5));
        theList.add(new Integer(6));
        theList.add(new Integer(7));
        theList.add(new Integer(8));
        theList.add(new Integer(9));
        theList.add(new Integer(10));
        theList.add(new Integer(11));
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
        TArrayList theList = new TArrayList();
        theList.add(new Integer(1));
        theList.add(new Integer(2));
        Assert.assertFalse(theList.isEmpty());
        theList.clear();;
        Assert.assertTrue(theList.isEmpty());
        Assert.assertEquals(0, theList.size(), 0);
    }

    @Test
    public void contains() throws Exception {
        TArrayList theList = new TArrayList();
        theList.add(new Integer(1));
        theList.add(new Integer(2));
        Assert.assertFalse(theList.contains("Hello"));
        Assert.assertFalse(theList.contains(new Integer(3)));
        Assert.assertTrue(theList.contains(new Integer(2)));
    }

    @Test
    public void isEmpty() throws Exception {
        TArrayList theList = new TArrayList();
        TAssert.assertTrue(theList.isEmpty());
        theList.add("Hello");
        TAssert.assertFalse(theList.isEmpty());
    }

    @Test
    public void remove() throws Exception {
        TArrayList theList = new TArrayList();
        theList.add(new Integer(1));
        theList.add(new Integer(2));
        Assert.assertTrue(theList.remove(new Integer(1)));
        Assert.assertFalse(theList.remove("lala"));
        Assert.assertEquals(1, theList.size(), 0);
        Assert.assertFalse(theList.contains(new Integer(1)));
        Assert.assertTrue(theList.contains(new Integer(2)));
    }
}