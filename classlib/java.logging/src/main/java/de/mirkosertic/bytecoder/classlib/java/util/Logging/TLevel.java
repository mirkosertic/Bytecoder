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
package de.mirkosertic.bytecoder.classlib.java.util.Logging;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.logging.Level;

@SubstitutesInClass(completeReplace = true)
public class TLevel {

    public static final Level OFF = (Level)(Object) new TLevel("OFF",Integer.MAX_VALUE, null);

    public static final Level SEVERE = (Level)(Object) new TLevel("SEVERE",1000, null);

    public static final Level WARNING = (Level)(Object) new TLevel("WARNING", 900, null);

    public static final Level INFO = (Level)(Object) new TLevel("INFO", 800, null);

    public static final Level CONFIG = (Level)(Object) new TLevel("CONFIG", 700, null);

    public static final Level FINE = (Level)(Object) new TLevel("FINE", 500, null);

    public static final Level FINER = (Level)(Object) new TLevel("FINER", 400, null);

    public static final Level FINEST = (Level)(Object) new TLevel("FINEST", 300, null);

    public static final Level ALL = (Level)(Object) new TLevel("ALL", Integer.MIN_VALUE, null);

    private final String name;
    private final int value;
    private final String resourceBundleName;

    private TLevel(final String name, final int value, final String resourceBundleName) {
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
    }

    public int intValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
