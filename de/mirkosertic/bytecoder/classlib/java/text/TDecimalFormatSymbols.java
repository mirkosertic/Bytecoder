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

import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import java.util.ResourceBundle;

@SubstitutesInClass(completeReplace = true)
public class TDecimalFormatSymbols {

    public static DecimalFormatSymbols getInstance() {
        return DecimalFormatSymbols.getInstance(VM.defaultLocale());
    }

    public static DecimalFormatSymbols getInstance(final Locale aLocale) {
        return new DecimalFormatSymbols(aLocale);
    }

    public TDecimalFormatSymbols() {
    }

    public TDecimalFormatSymbols(final Locale aLocale) {
        final ResourceBundle bundle = ResourceBundle.getBundle("localedata", aLocale);

        zeroDigit = bundle.getString("DecimalFormatSymbols.zeroDigit").charAt(0);
        groupingSeparator = bundle.getString("DecimalFormatSymbols.groupingSeparator").charAt(0);
        decimalSeparator = bundle.getString("DecimalFormatSymbols.decimalSeparator").charAt(0);
        perMill = bundle.getString("DecimalFormatSymbols.perMill").charAt(0);
        percent = bundle.getString("DecimalFormatSymbols.percent").charAt(0);
        digit = bundle.getString("DecimalFormatSymbols.digit").charAt(0);
        patternSeparator = bundle.getString("DecimalFormatSymbols.patternSeparator").charAt(0);
        infinity = bundle.getString("DecimalFormatSymbols.infinity");
        NaN = bundle.getString("DecimalFormatSymbols.NaN");
        minusSign = bundle.getString("DecimalFormatSymbols.minusSign").charAt(0);
        currencySymbol = bundle.getString("DecimalFormatSymbols.currencySymbol");
        intlCurrencySymbol = bundle.getString("DecimalFormatSymbols.intlCurrencySymbol");
        monetarySeparator = bundle.getString("DecimalFormatSymbols.monetarySeparator").charAt(0);
        exponential = bundle.getString("DecimalFormatSymbols.exponential").charAt(0);
        exponentialSeparator = bundle.getString("exponentialSeparator.exponentialSeparator");

        locale = aLocale;
    }

    public char getZeroDigit() {
        return zeroDigit;
    }

    public void setZeroDigit(char zeroDigit) {
        this.zeroDigit = zeroDigit;
    }

    public char getGroupingSeparator() {
        return groupingSeparator;
    }

    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
    }

    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public char getPerMill() {
        return perMill;
    }

    public void setPerMill(char perMill) {
        this.perMill = perMill;
    }

    public char getPercent() {
        return percent;
    }

    public void setPercent(char percent) {
        this.percent = percent;
    }

    public char getDigit() {
        return digit;
    }

    public void setDigit(char digit) {
        this.digit = digit;
    }

    public char getPatternSeparator() {
        return patternSeparator;
    }

    public void setPatternSeparator(char patternSeparator) {
        this.patternSeparator = patternSeparator;
    }

    public String getInfinity() {
        return infinity;
    }

    public void setInfinity(String infinity) {
        this.infinity = infinity;
    }

    public String getNaN() {
        return NaN;
    }

    public void setNaN(String NaN) {
        this.NaN = NaN;
    }

    public char getMinusSign() {
        return minusSign;
    }

    public void setMinusSign(char minusSign) {
        this.minusSign = minusSign;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currency) {
        currencySymbol = currency;
    }

    public String getInternationalCurrencySymbol() {
        return intlCurrencySymbol;
    }

    public void setInternationalCurrencySymbol(String currencyCode) {
        intlCurrencySymbol = currencyCode;
    }

    public Currency getCurrency() {
        return Currency.getInstance(locale);
    }

    public void setCurrency(Currency currency) {
        intlCurrencySymbol = currency.getCurrencyCode();
        currencySymbol = currency.getSymbol(locale);
    }

    public char getMonetaryDecimalSeparator() {
        return monetarySeparator;
    }

    public void setMonetaryDecimalSeparator(char sep) {
        monetarySeparator = sep;
    }

    char getExponentialSymbol() {
        return exponential;
    }

    public String getExponentSeparator() {
        return exponentialSeparator;
    }

    void setExponentialSymbol(char exp) {
        exponential = exp;
    }

    public void setExponentSeparator(String exp) {
        if (exp == null) {
            throw new NullPointerException();
        }
        exponentialSeparator = exp;
    }

    @Override
    public Object clone() {
        try {
            return (DecimalFormatSymbols) super.clone();
            // other fields are bit-copied
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private char zeroDigit;

    private char groupingSeparator;

    private char decimalSeparator;

    private char perMill;

    private char percent;

    private char digit;

    private char patternSeparator;

    private String infinity;

    private String NaN;

    private char minusSign;

    private String currencySymbol;

    private String intlCurrencySymbol;

    private char monetarySeparator; // Field new in JDK 1.1.6

    private char exponential;       // Field new in JDK 1.1.6

    private String exponentialSeparator;       // Field new in JDK 1.6

    private Locale locale;
}