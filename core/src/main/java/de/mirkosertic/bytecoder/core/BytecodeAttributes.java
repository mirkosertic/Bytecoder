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

import java.util.Arrays;
import java.util.stream.Collectors;

public class BytecodeAttributes {

    private final BytecodeAttributeInfo[] attributes;
    private final BytecodeAnnotationAttributeInfo[] annotations;

    public BytecodeAttributes(
            BytecodeAttributeInfo[] aAttributes) {
        this.attributes = aAttributes;
        this.annotations = Arrays.stream(aAttributes).filter(t -> t instanceof BytecodeAnnotationAttributeInfo)
                .map(t -> (BytecodeAnnotationAttributeInfo) t).collect(Collectors.toList()).toArray(new BytecodeAnnotationAttributeInfo[0]);
    }

    public BytecodeAnnotation getAnnotationByType(String aTypeName) {
        for (BytecodeAnnotationAttributeInfo theAnnotationInfo : annotations) {
            BytecodeAnnotation theFound =  theAnnotationInfo.getAnnotationByType(aTypeName);
            if (theFound != null) {
                return theFound;
            }
        }
        return null;
    }

    public <T extends BytecodeAttributeInfo> T getByType(Class<T> aType) {
        for (BytecodeAttributeInfo theInfo : attributes) {
            if (theInfo.getClass() == aType) {
                return (T) theInfo;
            }
        }
        return null;
    }
}
