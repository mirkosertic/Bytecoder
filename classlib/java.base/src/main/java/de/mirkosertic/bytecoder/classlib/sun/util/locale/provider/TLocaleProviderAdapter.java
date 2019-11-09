package de.mirkosertic.bytecoder.classlib.sun.util.locale.provider;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import sun.util.locale.provider.LocaleProviderAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.spi.LocaleServiceProvider;

@SubstitutesInClass(completeReplace = true)
public class TLocaleProviderAdapter {

    private static final LocaleProviderAdapter INSTANCE = (LocaleProviderAdapter) (Object) new TLocaleProviderAdapter();

    public static LocaleProviderAdapter getAdapter(final Class<? extends LocaleServiceProvider> providerClass,
                                                   final Locale locale) {
        return INSTANCE;
    }

    public static LocaleProviderAdapter forJRE() {
        return INSTANCE;
    }

    public static LocaleProviderAdapter getResourceBundleBased() {
        return INSTANCE;
    }

    public static List<LocaleProviderAdapter.Type> getAdapterPreference() {
        return Collections.singletonList(LocaleProviderAdapter.Type.SPI);
    }

    public static LocaleProviderAdapter forType(final LocaleProviderAdapter.Type type) {
        return INSTANCE;
    }

    public static Locale[] toLocaleArray(final Set<String> tags) {
        final Locale[] locs = new Locale[tags.size() + 1];
        int index = 0;
        locs[index++] = Locale.ROOT;
        for (final String tag : tags) {
            locs[index++] = Locale.forLanguageTag(tag);
        }
        return locs;
    }
}
