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
import de.mirkosertic.bytecoder.classlib.FileResourceBundleImpl;
import de.mirkosertic.bytecoder.classlib.VM;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

@SubstitutesInClass(completeReplace = true)
public abstract class TResourceBundle {

    protected ResourceBundle parent = null;

    public static ResourceBundle getBundle(final String baseName) {
        return getBundle(baseName, VM.defaultLocale());
    }

    public static ResourceBundle getBundle(final String baseName, final Locale aLocale) {
        if (aLocale == null || (aLocale.getCountry() == null && aLocale.getLanguage() == null && aLocale.getVariant() == null)) {
            // root locale for this basename
            final FileResourceBundleImpl theCached = FileResourceBundleImpl.CACHE.get(baseName);
            if (theCached != null) {
                return theCached;
            }

            final StringBuilder completeFileName = new StringBuilder(baseName);
            completeFileName.append(".properties");
            final FileResourceBundleImpl theNew = new FileResourceBundleImpl(baseName, VM.defaultLocale(), new File(completeFileName.toString()));
            FileResourceBundleImpl.CACHE.put(baseName, theNew);
            return theNew;
        }
        final String theCountry = aLocale.getCountry();
        final String theLanguage = aLocale.getLanguage();
        final String theVariant = aLocale.getVariant();

        final StringBuilder theFinalName = new StringBuilder(baseName);
        if (theCountry != null) {
            theFinalName.append("_").append(theCountry);
        }
        if (theLanguage != null) {
            theFinalName.append("_").append(theLanguage);
        }
        if (theVariant != null) {
            theFinalName.append("_").append(theVariant);
        }

        final String theBundleName = theFinalName.toString();

        final FileResourceBundleImpl theCached = FileResourceBundleImpl.CACHE.get(theBundleName);
        if (theCached != null) {
            return theCached;
        }

        final ResourceBundle parent;
        if (theVariant != null) {
            final Locale parentLocale = new Locale(theCountry, theLanguage);
            parent = getBundle(baseName, parentLocale);
        } else if (theLanguage != null) {
            final Locale parentLocale = new Locale(theCountry);
            parent = getBundle(baseName, parentLocale);
        } else {
            parent = getBundle(baseName, null);
        }

        theFinalName.append(".properties");
        final FileResourceBundleImpl theNew = new FileResourceBundleImpl(theBundleName, aLocale, new File(theFinalName.toString()));
        theNew.setParent(parent);
        FileResourceBundleImpl.CACHE.put(theBundleName, theNew);
        return theNew;
    }

    protected void setParent(final ResourceBundle parent) {
        this.parent = parent;
    }

    public abstract String getBaseBundleName();

    public abstract Locale getLocale();

    public abstract boolean containsKey(final String key);

    public abstract Set<String> keySet();

    protected abstract Set<String> handleKeySet();

    protected abstract Object handleGetObject(String key);

    public abstract Enumeration<String> getKeys();

    public final Object getObject(final String key) {
        Object obj = handleGetObject(key);
        if (obj == null) {
            if (parent != null) {
                obj = parent.getObject(key);
            }
            if (obj == null) {
                throw new IllegalStateException("Can't find resource for bundle key ");
            }
        }
        return obj;
    }

    public final String getString(final String key) {
        return (String) getObject(key);
    }

    public final String[] getStringArray(final String key) {
        return (String[]) getObject(key);
    }

    public static void clearCache() {
    }

    public static void clearCache(final ClassLoader loader) {
    }

    @SubstitutesInClass(completeReplace = true)
    public static class Control {

        private static final Control INSTANCE = new Control();

        public static ResourceBundle.Control getControl(final List aList) {
            return (ResourceBundle.Control) (Object) INSTANCE;
        }

        public static final List<String> FORMAT_DEFAULT
                = Collections.unmodifiableList(Arrays.asList("java.class",
                "java.properties"));

        public static ResourceBundle.Control getNoFallbackControl(final List aList) {
            return (ResourceBundle.Control) (Object) INSTANCE;
        }

        public List getCandidateLocales(final String aName, final Locale aLocale) {
            return Collections.emptyList();
        }

        public String toBundleName(final String aBaseName, final Locale aLocale) {
            return aBaseName;
        }
    }

}
