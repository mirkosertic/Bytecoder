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
package de.mirkosertic.bytecoder.classlib.java.text;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;
import sun.util.locale.provider.LocaleProviderAdapter;

import java.text.DecimalFormatSymbols;
import java.text.spi.DecimalFormatSymbolsProvider;
import java.util.Locale;

@SubstitutesInClass(completeReplace = false)
public class TDecimalFormatSymbols {

    public static DecimalFormatSymbols getInstance() {
        return getInstance(VM.defaultLocale());
    }

    public static DecimalFormatSymbols getInstance(final Locale aLocale) {
        final LocaleProviderAdapter adapter  = LocaleProviderAdapter.getAdapter(DecimalFormatSymbolsProvider.class, VM.defaultLocale());
        final DecimalFormatSymbolsProvider prov = adapter.getDecimalFormatSymbolsProvider();
        return prov.getInstance(aLocale);
    }

}