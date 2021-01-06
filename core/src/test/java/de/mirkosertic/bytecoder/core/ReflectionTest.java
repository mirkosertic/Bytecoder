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
    value = {@BytecoderTestOption(backend = CompileTarget.BackendType.js)})
public class ReflectionTest {

    public static class BaseClass {
        public static String staticField = "staticFieldValue";
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
}
