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
package de.mirkosertic.bytecoder.complex;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Prototype;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;

import java.io.IOException;
import java.io.StringReader;

@RunWith(BytecoderUnitTestRunner.class)
public class LuaTest {

    @Test
    public void testLuaReturnString() {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final LuaValue chunk = theGlobals.load("return 'hello, world'");
        final LuaString theResult = chunk.call().strvalue();
        Assert.assertEquals("hello, world", theResult.tojstring());
    }

    @Test
    public void testLuaReturnInteger() {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final LuaValue chunk = theGlobals.load("return 123");
        final LuaString theResult = chunk.call().strvalue();
        Assert.assertEquals("123", theResult.tojstring());
    }

    @Test
    public void testLuaReturnIntegerAdd() {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final LuaValue chunk = theGlobals.load("return 123 + 231");
        final LuaString theResult = chunk.call().strvalue();
        Assert.assertEquals("354", theResult.tojstring());
    }

    @Test
    public void testLuaFunction() {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final LuaValue chunk = theGlobals.load("function add(a,b)\nreturn a+b\nend\nreturn add(1,2)");
        Assert.assertFalse(chunk.isnil());
        Assert.assertTrue(chunk instanceof LuaClosure);
        final LuaClosure luaClosure = (LuaClosure) chunk;
        final LuaString theResult = luaClosure.call().strvalue();
        Assert.assertEquals("3", theResult.tojstring());
    }

    @Test
    public void testVarArgs() {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final Varargs theArguments = LuaValue.varargsOf(new LuaValue[]{
                LuaInteger.valueOf(100),
                LuaInteger.valueOf(200)
        });
    }

    @Test
    public void testCall() throws IOException {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final Prototype thePrototype = theGlobals.compilePrototype(new StringReader("function add(a,b) return a + b end"), "script");
        new LuaClosure(thePrototype, theGlobals).call();
        final LuaValue theFunction = theGlobals.get("add");
        Assert.assertFalse(theFunction.isnil());
        final Varargs theArguments = LuaValue.varargsOf(new LuaValue[] {
                LuaInteger.valueOf(100),
                LuaInteger.valueOf(200)
        });
        final LuaInteger theResult = (LuaInteger) theFunction.invoke(theArguments);
        Assert.assertEquals("300", theResult.tojstring());
    }

    @Test
    public void testCallStringResult() throws IOException {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final Prototype thePrototype = theGlobals.compilePrototype(new StringReader("function add(a,b) return 'hello' end"), "script");
        new LuaClosure(thePrototype, theGlobals).call();
        final Varargs theArguments = LuaValue.varargsOf(new LuaValue[] {
                LuaInteger.valueOf(100),
                LuaInteger.valueOf(200)
        });
        final LuaValue theFunction = theGlobals.get("add");
        final LuaValue theValue = (LuaValue) theFunction.invoke(theArguments);
        Assert.assertTrue(theValue.isstring());
        Assert.assertEquals("hello", theValue.tojstring());
    }

    @Test
    public void testLuaError() {
        final LuaError error = new LuaError("Test");
        Assert.assertEquals("Test", error.getMessage());
    }

    @Test
    public void testLuaTable() {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final LuaTable theTable = new LuaTable();
        theTable.set("20", 200);
        Assert.assertEquals(200, theTable.get("20").toint(), 0);
    }

    @Test
    public void testLuaConversion() {
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        theGlobals.set("key", 10);
        Assert.assertEquals(10, theGlobals.get("key").toint());
        Assert.assertEquals(10, theGlobals.get(LuaString.valueOf("key")).toint());

        final LuaValue chunk = theGlobals.load("return key").call();
        Assert.assertEquals(10, chunk.toint());
    }

    @Test
    public void testGlobalSize() {
        final Globals theGlobals = new Globals();
        for (int i =0;i<=100;i++) {
            theGlobals.set("ABC", Integer.toString(i));
        }
        theGlobals.presize(1000);
        Assert.assertEquals(100, theGlobals.get("ABC").toint(),0);
    }
}