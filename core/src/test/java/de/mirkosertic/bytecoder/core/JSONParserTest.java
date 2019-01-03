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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(BytecoderUnitTestRunner.class)
public class JSONParserTest {

    @Test
    public void run() {
        String theJSON = "{\n"
                + "  \"enableDebug\": \"true\",\n"
                + "  \"customProperties\": {\n"
                + "    \"data\": []\n"
                + "  },\n"
                + "  \"enablewebgl\": \"true\",\n"
                + "  \"scenes\": [\n"
                + "    \"scene1\",\n"
                + "    \"scene2\"\n"
                + "  ],\n"
                + "  \"enableNetworking\": \"false\",\n"
                + "  \"firebaseURL\": \"https://glowing-heat-2189.firebaseio.com\",\n"
                + "  \"defaultscene\": \"scene1\",\n"
                + "  \"name\": \"Dukes Adventure\"\n"
                + "}";

        JSONParser parser = new JSONParser();
        Map<String, Object> theData = parser.fromJSON(theJSON);
        Assert.assertEquals(8, theData.size(), 0);
    }
}
