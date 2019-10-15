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
package de.mirkosertic.bytecoder.classlib.sun.util.locale.provider;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import sun.util.locale.provider.LocaleProviderAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SubstitutesInClass(completeReplace = true)
public class TLocaleProviderAdapter {

    public static LocaleProviderAdapter getAdapter(final Class clazz, final Locale locale) {
        return null;
    }

    public static LocaleProviderAdapter forJRE() {
        return null;
    }

    public static LocaleProviderAdapter forType(final LocaleProviderAdapter.Type type) {
        return null;
    }

    public static List getAdapterPreference() {
        return Collections.emptyList();
    }
}
