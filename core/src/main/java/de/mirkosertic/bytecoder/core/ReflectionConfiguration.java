/*
 * Copyright 2020 Mirko Sertic
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mirkosertic.bytecoder.api.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ReflectionConfiguration {

    public static class ExternalClassConfig {
        private boolean enableClassForName = true;

        public boolean isEnableClassForName() {
            return enableClassForName;
        }

        public void setEnableClassForName(final boolean enableClassForName) {
            this.enableClassForName = enableClassForName;
        }
    }

    public static class ReflectiveClass {
        private final String name;
        private boolean supportsClassForName;

        public ReflectiveClass(final String name) {
            this.name = name;
        }

        public void setSupportsClassForName(final boolean status) {
            supportsClassForName = status;
        }

        public boolean supportsClassForName() {
            return supportsClassForName;
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, ReflectiveClass> reflectiveClasses;

    public ReflectionConfiguration() {
        reflectiveClasses = new HashMap<>();
    }

    public ReflectiveClass resolve(final String className) {
        return reflectiveClasses.computeIfAbsent(className, ReflectiveClass::new);
    }

    public void mergeWithConfigFrom(final URL url, final Logger logger) throws IOException {
        final TypeReference<HashMap<String, ExternalClassConfig>> typeToRead
                = new TypeReference<HashMap<String, ExternalClassConfig>>() {};
        final Map<String, ExternalClassConfig> configurationData = MAPPER.readValue(url, typeToRead);
        for (final Map.Entry<String, ExternalClassConfig> entry : configurationData.entrySet()) {
            logger.info("Configuring reflective access for class {}", entry.getKey());
            resolve(entry.getKey()).setSupportsClassForName(entry.getValue().isEnableClassForName());
        }
    }
}
