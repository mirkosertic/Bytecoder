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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;

@RunWith(UnitTestRunner.class)
public class SystemTest {

    @Test
    public void testGetEnv() {
        Assert.assertNull(System.getenv("UNKNOWN"));
    }

    @Test
    public void testPrintString() {
        //final Scanner scanner = new Scanner(System.in);
        System.out.println("Hello world!");
    }

    @Test
    public void testPrintSingleChars() {
        final String text = "Hello world!";
        for (int i=0;i<text.length();i++) {
            System.out.write((int) text.charAt(i));
        }
        System.out.println();
    }

    @Test
    public void arrayCopyNatives(){
        byte[] byteSource = new byte[]{1,2,3,4};
        byte[] byteTarget = new byte[4];
        System.arraycopy(byteSource,1,byteTarget,2,2);
        Assert.assertArrayEquals(byteTarget, new byte[]{0,0,2,3});


        short[] shortSource = new short[]{1,2,3,4};
        short[] shortTarget = new short[4];
        System.arraycopy(shortSource,1,shortTarget,2,2);
        Assert.assertArrayEquals(shortTarget, new short[]{0,0,2,3});


        int[] intArr = new int[]{1,2,3,4};
        int[] target = new int[4];
        System.arraycopy(intArr,1,target,2,2);
        Assert.assertArrayEquals(target, new int[]{0,0,2,3});

        long[] longSource = new long[]{1,2,3,4};
        long[] longTarget = new long[4];
        System.arraycopy(longSource,1,longTarget,2,2);
        Assert.assertArrayEquals(longTarget, new long[]{0,0,2,3});

        double[] doubleSource = new double[]{1,2,3,4};
        double[] doubleTarget = new double[4];
        System.arraycopy(doubleSource,1,doubleTarget,2,2);
        Assert.assertArrayEquals(doubleTarget, new double[]{0,0,2,3},0);

        float[] floatSource = new float[]{1,2,3,4};
        float[] floatTarget = new float[4];
        System.arraycopy(floatSource,1,floatTarget,2,2);
        Assert.assertArrayEquals(floatTarget, new float[]{0,0,2,3},0);

        char[] charSource = new char[]{1,2,3,4};
        char[] charTarget = new char[4];
        System.arraycopy(charSource,1,charTarget,2,2);
        Assert.assertArrayEquals(charTarget, new char[]{0,0,2,3});


        boolean[] booleanSource = new boolean[]{true,false,true,false};
        boolean[] booleanTarget = new boolean[4];
        System.arraycopy(booleanSource,1,booleanTarget,2,2);
        Assert.assertArrayEquals(booleanTarget, new boolean[]{false,false,false,true});
    }

    @Test
    public void arraycopyObjects(){
        Object[] objArr = new Object[]{"A","b","c"};
        Object[] targetArr = new Object[2];
        System.arraycopy(objArr,1,targetArr,0,2);
        Assert.assertArrayEquals(targetArr, new Object[]{"b","c"});

        Object[] stringarr = new String[]{"A","b","c"};
        Object[] targetStringArr = new String[2];
        System.arraycopy(stringarr,1,targetStringArr,0,2);
        Assert.assertArrayEquals(targetStringArr, new String[]{"b","c"});

        Object concealedArray = new Object[]{"A","b","c"};
        Object concealedTargetArray = new Object[2];
        System.arraycopy(concealedArray,1,concealedTargetArray,0,2);
        Assert.assertArrayEquals((Object[]) concealedTargetArray, new Object[]{"b","c"});


        Object concealedStringArray = new String[]{"A","b","c"};
        Object concealedTargetStringArray = new String[2];
        System.arraycopy(concealedStringArray,1,concealedTargetStringArray,0,2);
        Assert.assertArrayEquals((String[]) concealedTargetStringArray, new String[]{"b","c"});
    }
}
