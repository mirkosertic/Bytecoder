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
import de.mirkosertic.bytecoder.classlib.VM;

import java.util.logging.Level;

@SubstitutesInClass(completeReplace = true)
public class TLevel {

    public static Level SEVERE;

    public static Level WARNING;

    public static Level INFO;

    public static Level CONFIG;

    public static Level FINE;

    public static Level FINER;

    public static Level FINEST;

    static {
        VM.setClassMemnber(Level.class, "SEVERE", new TLevel("SEVERE",1000, null));
        VM.setClassMemnber(Level.class, "WARNING", new TLevel("WARNING", 900, null));
        VM.setClassMemnber(Level.class, "INFO", new TLevel("INFO", 800, null));
        VM.setClassMemnber(Level.class, "CONFIG", new TLevel("CONFIG", 700, null));
        VM.setClassMemnber(Level.class, "FINE", new TLevel("FINE", 500, null));
        VM.setClassMemnber(Level.class, "FINER", new TLevel("FINER", 400, null));
        VM.setClassMemnber(Level.class, "FINEST", new TLevel("FINEST", 300, null));
    }

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
