/*
 * Copyright 2021 Mirko Sertic
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

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(additionalClassesToLink = {"de.mirkosertic.bytecoder.core.ReflectionTest$ReflectionTarget",
        "de.mirkosertic.bytecoder.core.ReflectionTest$BaseClass"},
    value = {@BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = true),
            @BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = false)})
public class ReflectionTest {

    public static class BaseClass {
        public static String staticField = "staticFieldValue";
        public static boolean booleanField = true;
        public static byte byteField = (byte) 10;
        public static char charField = 'c';
        public static double doubleField = 1d;
        public static float floatField = 1f;
        public static long longField = 1L;
        public static short shortField = (short) 1;
        public static int intField = 1;
    }

    public static class ReflectionTarget extends BaseClass  {
        public String objectField = "objectFieldValue";
    }

    @Test
    public void testClassForName() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final Object o = Class.forName("de.mirkosertic.bytecoder.core.ReflectionTest$ReflectionTarget").newInstance();
        assertTrue(o instanceof ReflectionTarget);
    }

    @Test
    public void testGetDeclaredFields() {
        final Field[] f = ReflectionTarget.class.getDeclaredFields();
        assertEquals(1, f.length);
        assertEquals("objectField", f[0].getName());
        assertTrue(Modifier.isPublic(f[0].getModifiers()));
    }

    @Test
    public void testStaticFieldAccess() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("staticField");
        assertEquals("staticField", f.getName());
        assertTrue(Modifier.isStatic(f.getModifiers()));
        final Object value = f.get(BaseClass.class);
        assertEquals("staticFieldValue", value);
    }

    @Test
    public void testObjectFieldAccess() throws NoSuchFieldException, IllegalAccessException {
        final Object o = new ReflectionTarget();
        final Field f = o.getClass().getField("objectField");
        assertEquals("objectField", f.getName());
        assertFalse(Modifier.isStatic(f.getModifiers()));
        final Object value = f.get(o);
        assertEquals("objectFieldValue", value);
    }

    @Test
    public void testStaticFieldAccessBooleanBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("booleanField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Boolean);
        assertTrue(((Boolean) value).booleanValue());
    }

    @Test
    public void testStaticFieldAccessByteBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("byteField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Byte);
        assertEquals(10, ((Byte) value).byteValue());
    }

    @Test
    public void testStaticFieldAccessCharBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("charField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Character);
        assertEquals('c', ((Character) value).charValue());
    }

    @Test
    public void testStaticFieldAccessDoubleBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("doubleField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Double);
        assertEquals(1d, ((Double) value).doubleValue(), 0);
    }

    @Test
    public void testStaticFieldAccessFloatBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("floatField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Float);
        assertEquals(1f, ((Float) value).floatValue(), 0);
    }

    @Test
    public void testStaticFieldAccessLongBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("longField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Long);
        assertEquals(1L, ((Long) value).longValue());
    }

    @Test
    public void testStaticFieldAccessShortBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("shortField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Short);
        assertEquals((short) 1, ((Short) value).shortValue());
    }

    @Test
    public void testStaticFieldAccessIntBoxing() throws NoSuchFieldException, IllegalAccessException {
        final Field f = BaseClass.class.getField("intField");
        final Object value = f.get(BaseClass.class);
        assertTrue(value instanceof Integer);
        assertEquals(1, ((Integer) value).intValue());
    }
}
