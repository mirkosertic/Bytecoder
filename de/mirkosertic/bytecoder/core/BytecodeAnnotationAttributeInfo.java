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

import java.util.HashMap;
import java.util.Map;

public class BytecodeAnnotationAttributeInfo implements BytecodeAttributeInfo {

    private final Map<String, BytecodeAnnotation> annotations;

    public BytecodeAnnotationAttributeInfo(final BytecodeAnnotation[] aAnnotations) {
        annotations = new HashMap<>();
        for (final BytecodeAnnotation annotation : aAnnotations) {
            annotations.put(annotation.getType().name(), annotation);
        }
    }

    public BytecodeAnnotation getAnnotationByType(final String aName) {
        return annotations.get(aName);
    }
}
