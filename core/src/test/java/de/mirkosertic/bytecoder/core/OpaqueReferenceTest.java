/*
 * Copyright 2018 Mirko Sertic
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

import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.api.web.EventListener;
import de.mirkosertic.bytecoder.api.web.HTMLDocument;
import de.mirkosertic.bytecoder.api.web.Window;
import de.mirkosertic.bytecoder.classlib.java.lang.TSystem;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import de.mirkosertic.bytecoder.unittest.JSAndWASMOnly;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
@JSAndWASMOnly
public class OpaqueReferenceTest {

    @Test
    public void testGetSetTitle() {
        final HTMLDocument currentDocument = Window.window().document();
        //final String currentTitle = currentDocument.getTitle();

        final String theABC = "Bytecoder";
        TSystem.logDebug(theABC);
        TSystem.logDebug(theABC.getBytes());
        currentDocument.title(theABC);
        Assert.assertEquals("Bytecoder", currentDocument.title());
    }

    @Test
    public void testEventListenerLambda() {
        final Window w = Window.window();
        w.document().addEventListener("click", aValue -> w.document().title("clicked!"));
    }

    @Test
    public void testEventListenerAnonymousInnerClass() {
        final Window w = Window.window();
        //noinspection Convert2Lambda
        w.document().addEventListener("click", new EventListener<Event>() {
            @Override
            public void run(final Event aValue) {
                w.document().title("clicked!");
            }
        });
    }

}
