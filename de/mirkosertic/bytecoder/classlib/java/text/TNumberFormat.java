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
package de.mirkosertic.bytecoder.classlib.java.text;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

@SubstitutesInClass(completeReplace = false)
public class TNumberFormat {

    public static NumberFormat getInstance() {
        return NumberFormat.getInstance(VM.defaultLocale());
    }

    public static NumberFormat getInstance(final Locale aLocale) {
        final ResourceBundle bundle = ResourceBundle.getBundle("localedata", aLocale);
        final DecimalFormatSymbols theSymbols = DecimalFormatSymbols.getInstance(aLocale);
        final DecimalFormat theFormat = new DecimalFormat(bundle.getString("numberformat.default"), theSymbols);
        return theFormat;
    }

    public static NumberFormat getIntegerInstance() {
        return NumberFormat.getIntegerInstance(VM.defaultLocale());
    }

    public static NumberFormat getIntegerInstance(final Locale aLocale) {
        final ResourceBundle bundle = ResourceBundle.getBundle("localedata", aLocale);
        final DecimalFormatSymbols theSymbols = DecimalFormatSymbols.getInstance(aLocale);
        final DecimalFormat theFormat = new DecimalFormat(bundle.getString("numberformat.integer"), theSymbols);
        theFormat.setParseIntegerOnly(true);
        return theFormat;
    }

    public static NumberFormat getCurrencyInstance() {
        return NumberFormat.getCurrencyInstance(VM.defaultLocale());
    }

    public static NumberFormat getCurrencyInstance(final Locale aLocale) {
        final ResourceBundle bundle = ResourceBundle.getBundle("localedata", aLocale);
        final DecimalFormatSymbols theSymbols = DecimalFormatSymbols.getInstance(aLocale);
        final DecimalFormat theFormat = new DecimalFormat(bundle.getString("numberformat.currency"), theSymbols);
        // TODO: Set currency
        return theFormat;
    }

    public static NumberFormat getPercentInstance() {
        return NumberFormat.getPercentInstance(VM.defaultLocale());
    }

    public static NumberFormat getPercentInstance(final Locale aLocale) {
        final ResourceBundle bundle = ResourceBundle.getBundle("localedata", aLocale);
        final DecimalFormatSymbols theSymbols = DecimalFormatSymbols.getInstance(aLocale);
        final DecimalFormat theFormat = new DecimalFormat(bundle.getString("numberformat.percent"), theSymbols);
        return theFormat;
    }
}