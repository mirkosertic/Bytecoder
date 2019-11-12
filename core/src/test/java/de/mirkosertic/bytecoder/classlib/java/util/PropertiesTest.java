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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

@RunWith(BytecoderUnitTestRunner.class)
public class PropertiesTest {

    @Test
    @Ignore
    public void testSpecialCharacters() throws IOException {
        Properties p = new Properties();
        p.load(new StringReader("DecimalFormatSymbols.perMill=\\u2030"));
        System.out.println(p.getProperty("DecimalFormatSymbols.perMill"));
        Assert.assertEquals("‰", p.getProperty("DecimalFormatSymbols.perMill"));
    }
}
