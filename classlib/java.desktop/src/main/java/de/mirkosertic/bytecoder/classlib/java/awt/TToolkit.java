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
package de.mirkosertic.bytecoder.classlib.java.awt;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.awt.*;

@SubstitutesInClass(completeReplace = true)
public class TToolkit {

    private static final TToolkit TK = new TToolkit();

    private final EventQueue queue;

    private TToolkit() {
        queue = new EventQueue();
    }

    public static Toolkit getDefaultToolkit() {
        return (Toolkit) (Object) TK;
    }

    public static void loadLibraries() {
    }

    public static String getProperty(final String key, final String defaultValue) {
        return defaultValue;
    }

    public static EventQueue getEventQueue() {
        return TK.queue;
    }

    static boolean enabledOnToolkit(final long eventMask) {
        return true;
    }
}
