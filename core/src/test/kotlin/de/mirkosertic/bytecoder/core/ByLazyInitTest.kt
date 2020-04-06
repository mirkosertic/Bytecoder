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

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BytecoderUnitTestRunner::class)
class ByLazyInitTest {

    class EntryKotlin {
        val me: String by lazy {
            "Hola"
        }

        init {
            println("new entry kotlin")
            //val nada = Class.forName("")
        }

        companion object {
            @JvmStatic
            fun main(args: Array<String>?) {
                var x = EntryKotlin()
                println(x.me);
            }
        }
    }

    @Test
    fun byLazyInit() {
        EntryKotlin.main(null)
    }
}