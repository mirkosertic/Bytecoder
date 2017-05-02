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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;

import de.mirkosertic.bytecoder.backend.js.JSBackend;

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
    public void testSimpleClassSum() throws IOException, ScriptException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClassMethod(new BytecodeObjectTypeRef(SimpleClass.class.getName()), "sum");

        JSBackend theBackend = new JSBackend();
        String theCode = theBackend.generateCodeFor(theLinkerContext);
        theCode = theCode+ "\nsum(10, 20);";

        System.out.println(theCode);

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        Assert.assertEquals(30, engine.eval(theCode));
    }

    @Test
    public void testSimpleClassDiv() throws IOException, ScriptException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClassMethod(new BytecodeObjectTypeRef(SimpleClass.class.getName()), "div");

        JSBackend theBackend = new JSBackend();
        String theCode = theBackend.generateCodeFor(theLinkerContext);
        theCode = theCode+ "\ndiv(30, 7);";

        System.out.println(theCode);

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        Assert.assertEquals(4, engine.eval(theCode));
    }

    @Test
    public void testSimpleClassMul() throws IOException, ScriptException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClassMethod(new BytecodeObjectTypeRef(SimpleClass.class.getName()), "mul");

        JSBackend theBackend = new JSBackend();
        String theCode = theBackend.generateCodeFor(theLinkerContext);
        theCode = theCode+ "\nmul(30, 7);";

        System.out.println(theCode);

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        Assert.assertEquals(210, engine.eval(theCode));
    }

    @Test
    public void testSimpleClassSub() throws IOException, ScriptException {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClassMethod(new BytecodeObjectTypeRef(SimpleClass.class.getName()), "sub");

        JSBackend theBackend = new JSBackend();
        String theCode = theBackend.generateCodeFor(theLinkerContext);
        theCode = theCode+ "\nsub(30, 7);";

        System.out.println(theCode);

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        Assert.assertEquals(23, engine.eval(theCode));
    }

    @Test
    public void testLoadInterface() throws IOException {
        BytecodeLoader theLoader = new BytecodeLoader();
        theLoader.loadByteCode(getClass().getResourceAsStream("SimpleInterface.class"));
    }

    @Test
    public void testLinkMain() {
        BytecodeLoader theLoader = new BytecodeLoader();
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader);
        theLinkerContext.linkClassMethod(new BytecodeObjectTypeRef(SimpleClass.class.getName()), "main");
        System.out.println("lala");
    }
}