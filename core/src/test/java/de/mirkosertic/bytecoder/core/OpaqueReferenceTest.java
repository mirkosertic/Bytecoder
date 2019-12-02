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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.api.web.Console;
import de.mirkosertic.bytecoder.api.web.Event;
import de.mirkosertic.bytecoder.api.web.EventListener;
import de.mirkosertic.bytecoder.api.web.FloatArray;
import de.mirkosertic.bytecoder.api.web.HTMLDocument;
import de.mirkosertic.bytecoder.api.web.Int8Array;
import de.mirkosertic.bytecoder.api.web.IntArray;
import de.mirkosertic.bytecoder.api.web.OpaqueArrays;
import de.mirkosertic.bytecoder.api.web.OpaqueReferenceArray;
import de.mirkosertic.bytecoder.api.web.Promise;
import de.mirkosertic.bytecoder.api.web.Response;
import de.mirkosertic.bytecoder.api.web.StringPromise;
import de.mirkosertic.bytecoder.api.web.Window;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(includeJVM = false)
public class OpaqueReferenceTest {

    @Test
    public void testGetSetTitle() {
        final HTMLDocument currentDocument = Window.window().document();
        //final String currentTitle = currentDocument.getTitle();

        final String theABC = "Bytecoder";
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

    @Test
    public void testIntArray() {
        final IntArray a = OpaqueArrays.createIntArray(10);
        a.setIntValue(1, 99);
        Assert.assertEquals(99, a.getIntValue(1), 0);
    }

    @Test
    public void testFloatArray() {
        final FloatArray a = OpaqueArrays.createFloatArray(10);
        a.setFloat(1, 99f);
        Assert.assertEquals(99f, a.getFloat(1), 0);
    }

    @Test
    public void testReferenceArray() {
        final Window w = Window.window();
        final OpaqueReferenceArray<Window> a = OpaqueArrays.createObjectArray();
        a.set(1, w);
        Assert.assertSame(w, a.get(1));

        final OpaqueReferenceArray<Window> b = OpaqueArrays.createObjectArray();
        b.push(w);
        Assert.assertEquals(1, b.objectArrayLength(), 0);
        final Window w2 = b.pop();
        Assert.assertEquals(0, b.objectArrayLength(), 0);
        Assert.assertSame(w, w2);
    }

    @Test
    @Ignore
    public void testFetchAPI() {
        final Window w = Window.window();
        final Console c = Console.console();
        final Object[] fetched = new Object[0];
        c.log("Fetching");
        w.fetch("https://httpbin.org/status/200").then(new Promise.Handler<Response>() {
            @Override
            public void handleObject(final Response aValue) {
                c.log("Data received");
                aValue.text().then(new StringPromise.Handler() {
                    @Override
                    public void handleString(final String aValue) {
                        c.log("String data is " + aValue);
                        fetched[0] = "ok";
                   }
                });
            }
        });
        c.log("Fetched");
        int counter = 0;
        while(fetched[0] == null && counter++ < 1000) {
            c.log("Waiting");
        }
    }

    @Test
    public void testInt8Array() {
        final Int8Array a = OpaqueArrays.createInt8Array(10);
        a.setByte(1, (byte) 99);
        Assert.assertEquals((byte) 99, a.getByte(1), 0);
    }

    private static class Logger {

        public void log(final String aValue) {
            System.out.println(aValue);
        }
    }

    @Test
    public void testNameConflict() {
        final Logger l = new Logger();
        l.log("Hello world!");
        final Console c = Console.console();
        c.log("Hello console!");
    }
}