package de.mirkosertic.bytecoder.classlib.java.text;

import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TDecimalFormat extends NumberFormat {

    public TDecimalFormat(final String aPattern, final DecimalFormatSymbols aSymbols) {
    }

    @Override
    public StringBuffer format(final double number, final StringBuffer toAppendTo, final FieldPosition pos) {
        return toAppendTo;
    }

    @Override
    public StringBuffer format(final long number, final StringBuffer toAppendTo, final FieldPosition pos) {
        return toAppendTo;
    }

    @Override
    public Number parse(final String source, final ParsePosition parsePosition) {
        return null;
    }
}
