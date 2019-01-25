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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(BytecoderUnitTestRunner.class)
public class MethodOverwriteCallTest {

    public class Parent {

        Map<String, Object> data = new HashMap<>();

        public void add(final String key, final String aValue) {
            data.remove(key);
        }

        public void add(final String aKey, final Object aValue) {
            add(aKey, aValue.toString());
        }
    }

    public class Child extends Parent {

        public void add(final String aKey, final String aValue) {
            data.put(aKey, aValue);
        }

        public Object get(final String aValue) {
            return data.get(aValue);
        }
    }

    @Test
    public void testCall() {
        final Child c = new Child();
        c.add("key", Integer.valueOf(10));
        Assert.assertEquals("10", c.get("key"));
    }
}
