/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.languagefeatures.java20;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Objects;

@RunWith(UnitTestRunner.class)
public class RecordTest {

    record Person(String name, int age) {
        Person {
            Objects.requireNonNull(name);
            if (age <= 0) {
                throw new IllegalArgumentException("Invalid age " + age);
            }
        }
    }

    @Test
    public void testIterateAndPrint() {
        System.out.println("Hello JVM Wasm");
        final var persons = List.of(new Person("Foo", 40), new Person("Bar", 30));
        persons.forEach(System.out::println);
    }

    @Test
    public void testSimplePrintToString() {
        System.out.println("Hello JVM Wasm");
        final var p = new Person("Foo", 40);
        System.out.println(p);
        Assert.assertEquals("Person[name=Foo, age=40]", p.toString());
    }

    @Test
    public void testEquals() {
        final var p = new Person("Foo", 40);
        Assert.assertEquals(p, p);
        Assert.assertEquals(p, new Person("Foo", 40));
        Assert.assertNotEquals("error", p, null);
        Assert.assertNotEquals("error", p, Integer.valueOf(10));
        Assert.assertNotEquals("error", p, new Person("foo", 50));
        Assert.assertNotEquals("error", p, new Person("bar", 50));
    }
}
