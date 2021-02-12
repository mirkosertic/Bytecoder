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
package de.mirkosertic.bytecoder.classlib.java.lang.invoke;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.lang.invoke.MethodHandles;

@SubstitutesInClass(completeReplace = true)
public class TMethodHandles {

    @SubstitutesInClass(completeReplace = true)
    public static class Lookup {
    }

    public static MethodHandles.Lookup lookup() {
        return null;
    }
}