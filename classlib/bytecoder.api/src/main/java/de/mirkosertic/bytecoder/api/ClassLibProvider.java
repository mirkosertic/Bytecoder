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
package de.mirkosertic.bytecoder.api;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public abstract class ClassLibProvider {

    private final static ServiceLoader<ClassLibProvider> LOADER = ServiceLoader.load(ClassLibProvider.class);

    public abstract String getResourceBase();

    public abstract String[] additionalResources();

    public static List<ClassLibProvider> availableProviders() {
        final List<ClassLibProvider> result = new ArrayList<>();
        for (final ClassLibProvider classLibProvider : LOADER) {
            result.add(classLibProvider);
        }
        return result;
    }
}
