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
class StangeNameTest {

    class EntryKotlin {
        private data class DemoTile(var fps: Int)

        private val t1 = DemoTile(3)

        init {
            println("EntryKotlin:init")
            t1.fps++
            println("${t1.fps}")
        }

        companion object {
            @JvmStatic
            fun main(args: Array<String>?) {
                EntryKotlin()
                println("hello world")
            }

        }
    }

    @Test
    fun strangeNameTest() {
        EntryKotlin.main(null)
    }
}