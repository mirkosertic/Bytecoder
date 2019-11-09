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

    public static Locale getDefault(final Locale.Category category) {
        return VM.defaultLocale();
    }

    public static Locale forLanguageTag(final String aTag) {
        return null;
    }

    @SubstitutesInClass(completeReplace = true)
    public static class Builder {

        private String language;
        private String country;
        private String variant;

        public Locale.Builder setLocale(final Locale aLocale) {
            language = aLocale.getLanguage();
            country = aLocale.getCountry();
            variant = aLocale.getVariant();
            return (Locale.Builder)(Object) this;
        }

        public Locale.Builder setRegion(final String value) {
            variant = value;
            return (Locale.Builder)(Object) this;
        }

        public Locale build() {
            return new Locale(language, country, variant);
        }

        public Locale.Builder clear() {
            language = "";
            country = "";
            variant = "";
            return (Locale.Builder)(Object) this;
        }
    }

    private final String language;
    private String country;
    private String variant;

    public TLocale(final String language, final String country, final String variant) {
        this.language = language;
        this.country = country;
        this.variant = variant;
    }

    public TLocale(final String language, final String country) {
        this.language = language;
        this.country = country;
    }

    public TLocale(final String language) {
        this.language = language;
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

    public String getUnicodeLocaleType(final String key) {
        return null;
    }

    public Locale stripExtensions() {
        return (Locale) (Object) this;
    }

    public boolean hasExtensions() {
        return false;
    }

    public String toLanguageTag() {
        return null;
    }
}
