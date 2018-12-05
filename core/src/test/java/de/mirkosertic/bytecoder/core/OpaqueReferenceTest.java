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

import de.mirkosertic.bytecoder.api.Callback;
import de.mirkosertic.bytecoder.api.web.Document;
import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.api.web.Window;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import de.mirkosertic.bytecoder.unittest.JSOnly;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecoderUnitTestRunner.class)
@JSOnly
public class OpaqueReferenceTest {

    @Test
    public void testGetSetTitle() {
        final Document currentDocument = Window.window().document();
        final String currentTitle = currentDocument.getTitle();
        currentDocument.setTitle("Bytecoder");

        currentDocument.addEventListener("scroll", aValue -> System.out.println("scrolled!"));
    }

    @Test
    public void testEventListenerLambda() {
        final Document currentDocument = Window.window().document();
        currentDocument.addEventListener("scroll", aValue -> System.out.println("scrolled!"));
    }

    @Test
    public void testEventListenerAnonymousInnerClass() {
        final Document currentDocument = Window.window().document();
        //noinspection Convert2Lambda
        currentDocument.addEventListener("scroll", new Callback<Event>() {
            @Override
            public void run(final Event aValue) {
                System.out.println("scrolled!");
            }
        });
    }

}
