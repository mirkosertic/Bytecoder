/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.util.regex;

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

@RunWith(UnitTestRunner.class)
@Ignore
public class PatternTest {

    @Test
    public void testCompile1() {
        System.out.println("Before Pattern call");
        final Pattern javascriptPattern = Pattern.compile("^[a-zA-Z_$][a-zA-Z_$0-9]*$");
        System.out.println("After Pattern call");
    }

    @Test
    public void testCompile2() {
        System.out.println("Before Pattern call");
        final Pattern javascriptPattern = Pattern.compile("^[^\":,}/ ][^:]*$");
        System.out.println("After Pattern call");
    }
}
