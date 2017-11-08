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

import java.io.IOException;

import org.junit.Test;

import de.mirkosertic.bytecoder.classlib.java.lang.TObject;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

public class BytecodeLoaderTest {

    @Test
    public void testLoadRuntime1() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(Object.class.getName()));
    }

    @Test
    public void testLoadRuntime2() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(String.class.getName()));
    }

    @Test
    public void testLoadRuntime3() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(Math.class.getName()));
    }

    @Test
    public void testLoadRuntime4() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(Class.class.getName()));
    }

    @Test
    public void testLoadRuntime5() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.linkClass(new BytecodeObjectTypeRef(TObject.class.getName()));
    }

    @Test
    public void testLoadInterface() throws IOException, ClassNotFoundException {
        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        theLoader.loadByteCode(new BytecodeObjectTypeRef(SimpleInterface.class.getName()));
    }
}