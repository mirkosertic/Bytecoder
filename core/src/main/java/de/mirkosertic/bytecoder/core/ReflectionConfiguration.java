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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ReflectionConfiguration {

    public static class ReflectiveClass {
        private final String name;
        private boolean supportsClassForName;

        public ReflectiveClass(final String name) {
            this.name = name;
        }

        public void supportsClassForName(final boolean status) {
            supportsClassForName = status;
        }
    }

    private final Map<String, ReflectiveClass> reflectiveClasses;

    public ReflectionConfiguration() {
        reflectiveClasses = new HashMap<>();
    }

    public ReflectiveClass register(final String className) {
        return reflectiveClasses.computeIfAbsent(className, ReflectiveClass::new);
    }

    public void mergeWithConfigFrom(final URL url) throws IOException {
    }
}
