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
package de.mirkosertic.bytecoder.classlib.java.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class ListTest {

    @Test
    public void testListOf() {
        final List<String> list = new ArrayList<>();
        list.add("Hello");
        list.add("World!");
        list.stream().forEach(new Consumer<String>() {
            @Override
            public void accept(final String s) {
                System.out.println(s);
            }
        });
    }

    @Test
    public void testListOfLambda() {
        final List<String> list = new ArrayList<>();
        list.add("Hello");
        list.add("World!");
        list.stream().forEach(s -> System.out.println(s));
    }

    @Test
    public void testListOfMethodRef() {
        final List<String> list = new ArrayList<>();
        list.add("Hello");
        list.add("World!");
        list.stream().forEach(System.out::println);
    }
}
