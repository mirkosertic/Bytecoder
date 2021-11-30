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

import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.junit.Test;

import java.io.IOException;

public class BytecodeLoaderTest {

    @Test
    public void testLoadRuntime1() {
        final AnalysisStack analysisStack = new AnalysisStack();
        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.resolveClass(new BytecodeObjectTypeRef(Object.class.getName()), analysisStack);
    }

    @Test
    public void testLoadRuntime2() {
        final AnalysisStack analysisStack = new AnalysisStack();
        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.resolveClass(new BytecodeObjectTypeRef(String.class.getName()), analysisStack);
    }

    @Test
    public void testLoadRuntime3() {
        final AnalysisStack analysisStack = new AnalysisStack();
        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.resolveClass(new BytecodeObjectTypeRef(Math.class.getName()), analysisStack);
    }

    @Test
    public void testLoadRuntime4() {
        final AnalysisStack analysisStack = new AnalysisStack();
        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.resolveClass(new BytecodeObjectTypeRef(Class.class.getName()), analysisStack);
    }

    @Test
    public void testLoadRuntime5() {
        final AnalysisStack analysisStack = new AnalysisStack();
        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, Slf4JLogger.INSTANCE);
        theLinkerContext.resolveClass(new BytecodeObjectTypeRef(Object.class.getName()), analysisStack);
    }

    @Test
    public void testLoadInterface() throws IOException, ClassNotFoundException {
        final BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader());
        theLoader.loadByteCode(new BytecodeObjectTypeRef(SimpleInterface.class.getName()));
    }
}