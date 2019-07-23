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
package de.mirkosertic.bytecoder.core

import de.mirkosertic.bytecoder.api.web.ClickEvent
import de.mirkosertic.bytecoder.api.web.EventListener
import de.mirkosertic.bytecoder.api.web.HTMLDocument
import de.mirkosertic.bytecoder.api.web.Window
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BytecoderUnitTestRunner::class)
@BytecoderTestOptions(includeJVM = false)
class OpaqueReferenceKotlinTest {

    var window : Window? = null
    var document : HTMLDocument? = null

    @Test
    fun setTitleTest() {
        val w = Window.window()
        val d = w.document()
        d.addEventListener("click", EventListener<ClickEvent> {
            d.title("Hello world!!")
        })
    }

    @Test
    fun setTitleTestMember() {
        window = Window.window()
        document = window!!.document()
        document!!.addEventListener("click", EventListener<ClickEvent> {
            document!!.title("Hello world!!")
        })
    }
}