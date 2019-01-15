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

import de.mirkosertic.bytecoder.api.web.Window;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import de.mirkosertic.bytecoder.unittest.JSAndWASMOnly;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;

@RunWith(BytecoderUnitTestRunner.class)
@JSAndWASMOnly
public class LuaTest {

    @Test
    public void testLua() {
        System.out.println("Testing lua!");
        final Globals theGlobals = new Globals();
        LuaC.install(theGlobals);
        final LuaValue chunk = theGlobals.load("return 'hello, world'");
        final LuaString theResult = chunk.call().strvalue();
        Window.window().document().title(theResult.tojstring());
    }
}