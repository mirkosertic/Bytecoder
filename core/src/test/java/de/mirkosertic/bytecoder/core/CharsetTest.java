/*
 * Copyright 2017 Mirko Sertic
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

import de.mirkosertic.bytecoder.backend.CompileTarget;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOption;
import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(value = {
        @BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = false, preferStackifier = false),
        @BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = true, preferStackifier = false),
        @BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = false, preferStackifier = true),
        @BytecoderTestOption(backend = CompileTarget.BackendType.js, minify = true, preferStackifier = true)
})
public class CharsetTest {

    @Test
    public void testPrint() {
        System.out.println("Münster!");
    }

    @Test
    @Ignore
    public void testUTF8() {
        final Charset cs = StandardCharsets.UTF_8;
        final ByteBuffer bf = cs.encode("Münster");
        final byte[] result = Arrays.copyOf(bf.array(), bf.limit());

        System.out.println(result.length);
        for (int i=0;i<result.length;i++) {
            System.out.println(result[i] & 0xff);
        }

        Assert.assertEquals(8, result.length);
        Assert.assertEquals(77, result[0] & 0xff);
        Assert.assertEquals(195, result[1] & 0xff);
        Assert.assertEquals(188, result[2] & 0xff);
        Assert.assertEquals(110, result[3] & 0xff);
        Assert.assertEquals(115, result[4] & 0xff);
        Assert.assertEquals(116, result[5] & 0xff);
        Assert.assertEquals(101, result[6] & 0xff);
        Assert.assertEquals(114, result[7] & 0xff);

        Assert.assertEquals(77, result[0]);
        Assert.assertEquals(-61, result[1]);
        Assert.assertEquals(-68, result[2]);
        Assert.assertEquals(110, result[3]);
        Assert.assertEquals(115, result[4]);
        Assert.assertEquals(116, result[5]);
        Assert.assertEquals(101, result[6]);
        Assert.assertEquals(114, result[7]);
    }
    
    @Test
    public void testByteArray() {
        final String muenster = "Münster";
        System.out.println(muenster);
        System.out.println(Charset.defaultCharset().name());
        final byte[] data = muenster.getBytes(Charset.defaultCharset());
        System.out.println(data.length);
        final String test = new String(data, Charset.defaultCharset());
        System.out.println(test);
        Assert.assertEquals(muenster, test);
    }

    @Test
    public void testUTF8Charset() throws CharacterCodingException {
        final String m = "Mün";
        final Charset cs = StandardCharsets.UTF_8;
        final CharsetEncoder encoder = cs.newEncoder();
        final ByteBuffer bb = encoder.encode(CharBuffer.wrap(new char[] {'M','ü','n'}));
        final byte[] arr = bb.array();
        Assert.assertEquals(77, arr[0]);
        Assert.assertEquals(-61, arr[1]);
        Assert.assertEquals(-68, arr[2]);

        //final byte[] theBytes = Arrays.copyOf(bb.array(), bb.limit());
        //Assert.assertEquals(4, theBytes.length);
        //Assert.assertEquals(77, theBytes[0]);
        //Assert.assertEquals(-61, theBytes[1]);
        //Assert.assertEquals(-68, theBytes[2]);
        //Assert.assertEquals(110, theBytes[3]);
    }

    public static char getChar() {
        return 'ü';
    }

    @Test
    public void testCharToByte() {
        final char c = getChar();
        final byte a = (byte)(0xc0 | (c >> 6));
        final byte b = (byte)(0x80 | (c & 0x3f));
        Assert.assertEquals(-61, a);
        Assert.assertEquals(-68, b);

    }
}
