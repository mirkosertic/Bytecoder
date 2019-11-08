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
package de.mirkosertic.bytecoder.classlib.java.io;

import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(includeJVM = false, additionalResources = {"testfile.txt"})
public class FileTest {

    @Test
    public void testNotExists() {
        final File notExists = new File("/lala");
        Assert.assertFalse(notExists.exists());
    }

    @Test
    public void testOpen() throws FileNotFoundException {
        final InputStream fos = new FileInputStream(new File("testfile.txt"));
    }

    @Test
    public void testRead() throws IOException {
        final StringBuilder theBuilder = new StringBuilder();
        final InputStream fos = new FileInputStream(new File("testfile.txt"));
        fos.skip(0);
        while(fos.available() > 0) {
            final int intValue = fos.read();
            theBuilder.append((char) intValue);
            final byte[] data = new byte[100];
            final int read = fos.read(data);
            for (int i=0;i<read;i++) {
                theBuilder.append((char) data[i]);
            }
        }
        fos.close();
        Assert.assertEquals("hello world!", theBuilder.toString());
        System.out.println(theBuilder.toString());
    }
}
