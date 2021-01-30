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
import de.mirkosertic.bytecoder.classlib.VM;

import java.util.Locale;

@SubstitutesInClass(completeReplace = true)
public class TLocale {

    public static final Locale ROOT = new Locale("", "");

    public static final Locale ENGLISH = new Locale("en");

    public static final Locale US = new Locale("en", "US");

    public static final Locale JAPAN = new Locale("ja", "JP");

    public static Locale getDefault() {
        return VM.defaultLocale();
    }

    public static Locale getDefault(final Locale.Category aCategory) {
        return getDefault();
    }

    private final String language;
    private final String country;
    private final String variant;

    public TLocale(final String language, final String country, final String variant) {
        this.language = language;
        this.country = country;
        this.variant = variant;
    }

    public TLocale(final String language, final String country) {
        this.language = language;
        this.country = country;
        this.variant = null;
    }

    public TLocale(final String language) {
        this.language = language;
        this.country = null;
        this.variant = null;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public String getVariant() {
        return variant;
    }
}
