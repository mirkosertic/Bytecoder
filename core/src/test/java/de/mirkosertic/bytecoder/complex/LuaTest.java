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
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
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
        final LuaString theResult = chunk.call().strvalue();
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
        final Prototype thePrototype = theGlobals.compilePrototype(new StringReader("function add(a,b)\nreturn a + b\nend"), "script");
        new LuaClosure(thePrototype, theGlobals).call();
        final LuaValue theFunction = theGlobals.get("add");
        final Varargs theArguments = LuaValue.varargsOf(new LuaValue[] {
                LuaInteger.valueOf(100),
                LuaInteger.valueOf(200)
        });
        theFunction.invoke(theArguments);
    }
}