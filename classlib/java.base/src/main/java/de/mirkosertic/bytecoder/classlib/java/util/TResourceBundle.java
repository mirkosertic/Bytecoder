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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@SubstitutesInClass(completeReplace = true)
public class TResourceBundle {

    protected ResourceBundle parent = null;

    public static ResourceBundle getBundle(final String aName) {
        return null;
    }

    public static ResourceBundle getBundle(final String aName, final Locale aLocale) {
        return null;
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
