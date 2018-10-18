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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public class SBlockTest {

    @Test
    public void testSimpleCase() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final SBlock block = new SBlock(new SLabel("label"));
        try (final STextWriter writer = new STextWriter(pw)) {
            block.writeTo(writer);
        }

        Assert.assertEquals("(block $label)", strWriter.toString());
    }

    @Test
    public void testConditionalBranchOut() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final SBlock block = new SBlock(new SLabel("label"));
        block.addChild(new SBranchIf(block, SI32If.eq(new SI32Const(42), new SI32Const(12))));
        try (final STextWriter writer = new STextWriter(pw)) {
            block.writeTo(writer);
        }

        Assert.assertEquals("(block $label\n"
                + "    (br_if $label\n"
                + "        (i32.eq (i32.const 42) (i32.const 12))))", strWriter.toString());
    }

    @Test
    public void testBranchOut() throws IOException {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(strWriter);

        final SBlock block = new SBlock(new SLabel("label"));
        block.addChild(new SBranch(block));
        try (final STextWriter writer = new STextWriter(pw)) {
            block.writeTo(writer);
        }

        Assert.assertEquals("(block $label\n"
                + "    (br $label))", strWriter.toString());
    }
}