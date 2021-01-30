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
package de.mirkosertic.bytecoder.classlib.java.io;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.lang.reflect.Field;

@SubstitutesInClass(completeReplace = true)
public class TObjectStreamField {

    public TObjectStreamField(final String name, final Class clazz) {
    }

    public TObjectStreamField(final String name, final String a, final boolean b) {
    }

    public TObjectStreamField(final String name, final Class clazz, final boolean b) {
    }

    public TObjectStreamField(final Field field, final boolean a, final boolean b) {
    }

    public static String getClassSignature(final Class clazz) {
        return null;
    }

    public static StringBuilder appendClassSignature(final StringBuilder builder, final Class aClass) {
        return builder.append(getClassSignature(aClass));
    }
}
