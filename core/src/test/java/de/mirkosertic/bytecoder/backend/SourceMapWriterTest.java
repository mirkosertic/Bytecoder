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
package de.mirkosertic.bytecoder.backend;

import de.mirkosertic.bytecoder.ssa.DebugPosition;
import org.junit.Assert;
import org.junit.Test;

public class SourceMapWriterTest {

    @Test
    public void testSimpleCase() {
        final SourceMapWriter theWriter = new SourceMapWriter();
        theWriter.assignName(0, 0, "ABC", new DebugPosition("1.java", 10));
        theWriter.assignName(10, 5, "DEF", new DebugPosition("2.java", 20));
        theWriter.assignName(10, 15, "HIJ", new DebugPosition("3.java", 30));
        final String theSourceMap = theWriter.toSourceMap("filename.js");
        Assert.assertEquals("{\"version\":3,\"file\":\"filename.js\",\"sourceRoot\":\"\",\"names\":[\"ABC\",\"DEF\",\"HIJ\"],\"sources\":[\"1.java\",\"2.java\",\"3.java\"],\"mappings\":\"AAUAA;;;;;;;;;,KCUAC,UCUAC\"}", theSourceMap);
    }
}