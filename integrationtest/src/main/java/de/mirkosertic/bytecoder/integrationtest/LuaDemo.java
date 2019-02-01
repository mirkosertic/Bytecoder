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
package de.mirkosertic.bytecoder.integrationtest;

import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.web.Document;
import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.api.web.EventListener;
import de.mirkosertic.bytecoder.api.web.HTMLButton;
import de.mirkosertic.bytecoder.api.web.HTMLElement;
import de.mirkosertic.bytecoder.api.web.HTMLTextAreaElement;
import de.mirkosertic.bytecoder.api.web.Window;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;

public class LuaDemo {

    private static Globals globals;
    private static HTMLTextAreaElement inputCode;
    private static HTMLElement output;
    private static HTMLButton button;

    public static void main(final String[] args) {
        globals = new Globals();
        LuaC.install(globals);
        final Window window = Window.window();
        final Document document = window.document();
        inputCode = document.getElementById("luainput");
        output = document.getElementById("luaoutput");
        button = document.getElementById("runlua");
        button.addEventListener("click", new EventListener<Event>() {
            @Override
            public void run(final Event aEvent) {
                runLua();
            }
        });
    }

    private static void runLua() {
        final String theLuaCode = inputCode.value();
        final LuaValue chunk = globals.load(theLuaCode);
        final LuaString theResult = chunk.call().strvalue();
        output.innerHTML(theResult.tojstring());
    }
}
