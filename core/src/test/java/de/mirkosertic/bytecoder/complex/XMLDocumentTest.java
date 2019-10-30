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
package de.mirkosertic.bytecoder.complex;

import de.mirkosertic.bytecoder.unittest.BytecoderTestOptions;
import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

@RunWith(BytecoderUnitTestRunner.class)
@BytecoderTestOptions(additionalClassesToLink = {
        "de.mirkosertic.bytecoder.classlib.BytecoderGraphicsEnvironment",
})
public class XMLDocumentTest {

    @Test
    @Ignore
    public void testCreate() throws ParserConfigurationException, TransformerException, IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.newDocument();

        Assert.assertTrue(doc instanceof Document);
        /*final SVGGraphics2D graphics2D = new SVGGraphics2D(doc);
        graphics2D.setSVGCanvasSize(new Dimension(100, 100));
        graphics2D.drawLine(0,0, 100, 100);
        graphics2D.stream(new FileWriter("test.svg"));*/
    }
}
