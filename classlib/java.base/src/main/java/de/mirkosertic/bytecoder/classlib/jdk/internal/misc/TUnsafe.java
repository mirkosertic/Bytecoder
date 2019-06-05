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
package de.mirkosertic.bytecoder.classlib.jdk.internal.misc;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TUnsafe {

    private final static TUnsafe INSTANCE = new TUnsafe();

    public static TUnsafe getUnsafe() {
        return INSTANCE;
    }

    public static final int ARRAY_BOOLEAN_INDEX_SCALE = 0;

    public static final int ARRAY_BYTE_INDEX_SCALE = 0;

    public static final int ARRAY_CHAR_INDEX_SCALE = 0;

    public static final int ARRAY_SHORT_INDEX_SCALE = 0;

    public static final int ARRAY_INT_INDEX_SCALE = 0;

    public static final int ARRAY_LONG_INDEX_SCALE = 0;

    public static final int ARRAY_FLOAT_INDEX_SCALE = 0;

    public static final int ARRAY_DOUBLE_INDEX_SCALE = 0;

    public int arrayBaseOffset(final Class clazz) {
        return 0;
    }


    public int arrayIndexScale(final Class clazz) {
        return 0;
    }
}
