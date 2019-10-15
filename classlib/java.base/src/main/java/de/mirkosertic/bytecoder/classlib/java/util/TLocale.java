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

import java.util.Locale;

@SubstitutesInClass(completeReplace = true)
public class TLocale {

    public enum Category {

        /**
         * Category used to represent the default locale for
         * displaying user interfaces.
         */
        DISPLAY("user.language.display",
                "user.script.display",
                "user.country.display",
                "user.variant.display",
                "user.extensions.display"),

        /**
         * Category used to represent the default locale for
         * formatting dates, numbers, and/or currencies.
         */
        FORMAT("user.language.format",
                "user.script.format",
                "user.country.format",
                "user.variant.format",
                "user.extensions.format");

        Category(final String languageKey, final String scriptKey, final String countryKey,
                 final String variantKey, final String extensionsKey) {
            this.languageKey = languageKey;
            this.scriptKey = scriptKey;
            this.countryKey = countryKey;
            this.variantKey = variantKey;
            this.extensionsKey = extensionsKey;
        }

        final String languageKey;
        final String scriptKey;
        final String countryKey;
        final String variantKey;
        final String extensionsKey;
    }

    public final static Locale ROOT = new Locale("", "");

    public final static Locale ENGLISH = new Locale("en");

    public final static Locale US = new Locale("en", "usa");

    public final static Locale JAPAN = new Locale("en", "usa");

    public final static Locale JAPANESE = new Locale("en", "usa");

    public final static Locale KOREA = new Locale("en", "usa");

    public final static Locale KOREAN = new Locale("en", "usa");

    public static Locale getDefault() {
        return ENGLISH;
    }

    public static Locale getDefault(final Locale.Category category) {
        return ENGLISH;
    }

    public static Locale forLanguageTag(final String aTag) {
        return getDefault();
    }

    private final String language;
    private final String country;
    private final String region;
    private final Category category;

    public TLocale(final String language) {
        this(language, null);
    }

    public TLocale(final String language, final String country) {
        this(language, country, null, null);
    }

    public TLocale(final String language, final String country, final String region) {
        this(language, country, region,null);
    }

    public TLocale(final String language, final String country, final String region, final Category category) {
        this.language = language;
        this.country = country;
        this.region = region;
        this.category = category;
    }

}
