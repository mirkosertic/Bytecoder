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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(UnitTestRunner.class)
public class EnumTest {

    public enum Value {
        ONE, TWO, THREE
    }
    public enum SubclassedEnum {
        ONE{
            private final int innerField1 = 0;
        }, TWO{
            private final String innerField2 = "0";
        }, THREE
    }

    private static Value getEnum() {
        return Value.TWO;
    }

    @Test
    public void testGetEnum() {
        Value theEnum = getEnum();
        System.out.println(theEnum.name());
        Assert.assertNotNull(theEnum);
        Assert.assertEquals(1, theEnum.ordinal(), 0);
    }

    @Test
    public void testValueOfLight() {
        Value theEnum = Value.valueOf("ONE");
    }

    @Test
    public void testValuesRuntimeClass() {
        Value[] theValues= Value.class.getEnumConstants();
        Assert.assertEquals(3, theValues.length, 0);
    }
    @Test
    public void testValuesRuntimeClassOnSubclassingEnum() {
        SubclassedEnum[] theValues= SubclassedEnum.class.getEnumConstants();
        Assert.assertEquals(3, theValues.length, 0);
    }

    @Test
    public void testValuesRuntimeClassIndirect() {
        Class theClass = Value.ONE.getClass();
        Object[] theValues= theClass.getEnumConstants();
        Assert.assertEquals(3, theValues.length, 0);
    }

    @Test
    public void testValueOf() {
        Value theEnum = Value.valueOf("THREE");
        Assert.assertEquals("THREE", theEnum.name());
        Assert.assertSame(theEnum, Value.THREE);
        Assert.assertSame(Value.THREE, Value.THREE);
        Value theValue = Value.TWO;
        Assert.assertNotSame(Value.THREE, theValue);
        Assert.assertSame(Value.TWO, theValue);
    }

    @Test
    public void testValues() {
        Value[] theValues = Value.values();
        Assert.assertEquals(3, theValues.length, 0);
        Assert.assertTrue(theValues[0] == Value.ONE);
        Assert.assertTrue(theValues[0].getClass() == Value.class);
        Assert.assertTrue(theValues[1].name().equals("TWO"));
        System.out.println(theValues[1].name());
    }

    @Test
    public void testValuesForSubclassedEnum() {
        SubclassedEnum[] theValues = SubclassedEnum.values();
        Assert.assertEquals(3, theValues.length, 0);
        Assert.assertTrue(theValues[0] == SubclassedEnum.ONE);
        Assert.assertTrue(theValues[1].name().equals("TWO"));
        System.out.println(theValues[1].name());
    }
    @Test
    public void testGetDeclaringClass(){
        Value v = Value.TWO;
        Class<Value> desclaring = v.getDeclaringClass();
        for (Value val : Value.values()){
            Assert.assertEquals(desclaring, val.getDeclaringClass());
        }
    }
    @Test
    public void testGetDeclaringClassSubclassed(){
        SubclassedEnum v = SubclassedEnum.TWO;
        Class<SubclassedEnum> declaring = v.getDeclaringClass();
        for (SubclassedEnum val : SubclassedEnum.values()){
            Assert.assertEquals(declaring, val.getDeclaringClass());
        }
    }
}
