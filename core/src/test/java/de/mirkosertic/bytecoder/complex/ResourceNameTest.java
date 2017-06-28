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
package de.mirkosertic.bytecoder.complex;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(BytecoderUnitTestRunner.class)
public class ResourceNameTest {

    public static class ResourceName {

        public final String name;

        private final String fileName;

        public ResourceName(String aName) {
            name = aName;

            if (name != null) {
                int p = name.lastIndexOf("/");
                if (p >= 0) {
                    fileName = name.substring(p + 1);
                } else {
                    fileName = name;
                }
            } else {
                fileName = null;
            }
        }

        public String get() {
            return name;
        }

        public String getOnlyFileName() {
            return fileName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceName that = (ResourceName) o;

            return !(name != null ? !name.equals(that.name) : that.name != null);

        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        public Map<String, Object> serialize() {
            Map<String, Object> theResult = new HashMap<>();
            theResult.put("name", name);
            return theResult;
        }

        public static ResourceName deserialize(Map<String, Object> aSerializedData) {
            return new ResourceName((String) aSerializedData.get("name"));
        }
    }

    @Test
    public void testEquals() {
        Assert.assertEquals(new ResourceName("A"), new ResourceName("A"));
    }
}
