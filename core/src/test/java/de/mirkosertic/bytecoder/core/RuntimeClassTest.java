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

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(includeJVM = false)
public class RuntimeClassTest {

    @Test
    public void testRuntimeClass() {
        final String str1 = new String("1");
        final String str2 = new String("1");
        Assert.assertSame(str1.getClass(), str2.getClass());
        Assert.assertNotSame(str1.getClass(), RuntimeClassTest.class);
    }

    @Test
    public void testGetName() {
        System.out.println(RuntimeClassTest.class.getName());
        Assert.assertEquals("de.mirkosertic.bytecoder.core.RuntimeClassTest", RuntimeClassTest.class.getName());
    }

    @Test
    public void testForName() throws ClassNotFoundException {
        final Class cl = Class.forName(Object.class.getName());
        Assert.assertSame(Object.class, cl);
    }

    @Test
    public void testNewInstance() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final Class cl = Class.forName(Object.class.getName());
        final Object o = cl.newInstance();
        Assert.assertTrue(o instanceof Object);
    }

    @Test
    public void testNewInstanceConstructor() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        final Class cl = Class.forName(Object.class.getName());
        final Object o = cl.getConstructor(new Class[0]).newInstance();
        Assert.assertTrue(o instanceof Object);
    }

    @Test
    public void testAssignableFrom() {
        Assert.assertTrue(Object.class.isAssignableFrom(String.class));
        Assert.assertFalse(String.class.isAssignableFrom(Object.class));
    }

    private static final Class superClassOf(final Class aClass) {
        return aClass.getSuperclass();
    }

    private static final Class superClassOfObject(final Object aObject) {
        return aObject.getClass().getSuperclass();
    }

    @Test
    public void testGetSuperclassOfRuntimeClass() {
        Assert.assertSame(Object.class, superClassOf(String.class));
    }

    @Test
    public void testGetSuperclassofObject() {
        Assert.assertSame(Object.class, superClassOfObject(new Runnable() {
            @Override
            public void run() {
            }
        }));
    }

    @Test
    public void testGetSuperclassNull() {
        Assert.assertNull(superClassOf(Object.class));
    }

}
