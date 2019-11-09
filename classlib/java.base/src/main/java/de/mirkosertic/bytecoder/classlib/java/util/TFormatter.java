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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.PrintStream;
import java.util.Formatter;
import java.util.Locale;

@SubstitutesInClass(completeReplace = true)
public class TFormatter {

    public TFormatter() {
    }

    public TFormatter(final Locale aLocale) {
    }


    public TFormatter(final PrintStream aOut) {
    }

    public TFormatter(final Appendable aOut) {
    }

    public Formatter format(final String aPattern, final Object[] aValues) {
        return (Formatter) (Object) this;
    }

    public Formatter format(final Locale aLocale, final String aPattern, final Object[] aValues) {
        return (Formatter) (Object) this;
    }

    public Locale locale() {
        return Locale.getDefault();
    }
}
