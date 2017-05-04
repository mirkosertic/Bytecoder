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

import org.junit.Test;

import java.io.IOException;

public class BytecodeLoaderTest {

    @Test
    public void testLoadRuntime1() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(Object.class.getName()));
    }

    @Test
    public void testLoadRuntime2() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(String.class.getName()));
    }

    @Test
    public void testLoadRuntime3() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(Math.class.getName()));
    }

    @Test
    public void testLoadRuntime4() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(Class.class.getName()));
    }

    @Test
    public void testLoadInterface() throws IOException, ClassNotFoundException {
        BytecodeLoader theLoader = new BytecodeLoader();
        theLoader.loadByteCode(new BytecodeObjectTypeRef(SimpleInterface.class.getName()));
    }
}