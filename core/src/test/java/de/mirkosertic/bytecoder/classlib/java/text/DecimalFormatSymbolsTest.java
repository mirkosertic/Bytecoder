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

import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

@RunWith(UnitTestRunner.class)
public class DecimalFormatSymbolsTest {

    private static String toString(final char c) {
        final StringBuilder sb = new StringBuilder();
        sb.append(c);
        return sb.toString();
    }

    @Test
    public void testLocaleen_US() {
        final DecimalFormatSymbols theSymbols = DecimalFormatSymbols.getInstance(new Locale("en","US"));
        Assert.assertEquals("0", toString(theSymbols.getZeroDigit()));
        Assert.assertEquals(",", toString(theSymbols.getGroupingSeparator()));
        Assert.assertEquals(".", toString(theSymbols.getDecimalSeparator()));
        //Assert.assertEquals("‰", toString(theSymbols.getPerMill()));
        Assert.assertEquals("%", toString(theSymbols.getPercent()));
        Assert.assertEquals("#", toString(theSymbols.getDigit()));
        Assert.assertEquals(";", toString(theSymbols.getPatternSeparator()));
        //Assert.assertEquals("∞", theSymbols.getInfinity());
        //Assert.assertEquals("NaN", theSymbols.getNaN());
        Assert.assertEquals("-", toString(theSymbols.getMinusSign()));
        Assert.assertEquals("$", theSymbols.getCurrencySymbol());
        Assert.assertEquals("USD", theSymbols.getInternationalCurrencySymbol());
        Assert.assertEquals(".", toString(theSymbols.getMonetaryDecimalSeparator()));
        Assert.assertEquals("E", theSymbols.getExponentSeparator());
    }

    @Test
    public void testLocalede_DE() {
        final DecimalFormatSymbols theSymbols = DecimalFormatSymbols.getInstance(new Locale("de","DE"));
        Assert.assertEquals("0", toString(theSymbols.getZeroDigit()));
        Assert.assertEquals(".", toString(theSymbols.getGroupingSeparator()));
        Assert.assertEquals(",", toString(theSymbols.getDecimalSeparator()));
        //Assert.assertEquals("‰", toString(theSymbols.getPerMill()));
        Assert.assertEquals("%", toString(theSymbols.getPercent()));
        Assert.assertEquals("#", toString(theSymbols.getDigit()));
        Assert.assertEquals(";", toString(theSymbols.getPatternSeparator()));
        //Assert.assertEquals("∞", theSymbols.getInfinity());
        //Assert.assertEquals("NaN", theSymbols.getNaN());
        Assert.assertEquals("-", toString(theSymbols.getMinusSign()));
        //Assert.assertEquals("€", theSymbols.getCurrencySymbol());
        Assert.assertEquals("EUR", theSymbols.getInternationalCurrencySymbol());
        Assert.assertEquals(",", toString(theSymbols.getMonetaryDecimalSeparator()));
        Assert.assertEquals("E", theSymbols.getExponentSeparator());
    }
}
