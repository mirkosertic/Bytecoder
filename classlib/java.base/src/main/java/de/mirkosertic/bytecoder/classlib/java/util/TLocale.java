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
import java.util.Locale.Category;

import sun.util.locale.BaseLocale;
import sun.util.locale.LocaleExtensions;

@SubstitutesInClass(completeReplace = true)
public class TLocale {

    public static final Locale ROOT = new Locale("", "");

    public static final Locale ENGLISH = new Locale("en");
    public static final Locale JAPANESE = new Locale("ja");
    public static final Locale KOREAN = new Locale("ko");

    public static final Locale US = new Locale("en", "US");
    public static final Locale JAPAN = new Locale("ja", "JP");
    public static final Locale KOREA = new Locale("ko", "KR");
    private transient BaseLocale baseLocale;
    private transient LocaleExtensions localeExtensions;

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

    public String getDisplayName(){
        return getDisplayName(getDefault(Category.DISPLAY));
    }

    public String getDisplayName(Locale inLocale) {
        return "Locale display name. (Hack)";
    }

    public boolean hasExtensions(){
        return false;
    }

    public String getUnicodeLocaleType(String key) {
        return null;
    }

    private static LocaleExtensions getCompatibilityExtensions(String language,
                                                               String script,
                                                               String country,
                                                               String variant) {
        return null;
    }

    static Locale getInstance(String language, String country, String variant) {
        return getInstance(language, "", country, variant, null);
    }

    static Locale getInstance(String language, String script, String country,
                                      String variant, LocaleExtensions extensions) {
        if (language== null || script == null || country == null || variant == null) {
            throw new NullPointerException();
        }

        if (extensions == null) {
            extensions = getCompatibilityExtensions(language, script, country, variant);
        }

        BaseLocale baseloc = BaseLocale.getInstance(language, script, country, variant);
        return getInstance(baseloc, extensions);
    }

    static Locale getInstance(BaseLocale baseloc, LocaleExtensions extensions) {
        return null;
    }

    public String toLanguageTag() {
        return null;
    }

    public static Locale forLanguageTag(String languageTag) {
        return VM.defaultLocale();
    }

    public Locale stripExtensions() {
        return VM.defaultLocale();
    }
}
