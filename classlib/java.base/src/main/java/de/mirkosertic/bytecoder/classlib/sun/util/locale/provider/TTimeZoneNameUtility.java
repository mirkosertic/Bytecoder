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

import java.util.Locale;
import java.util.Optional;

@SubstitutesInClass(completeReplace = true)
public class TTimeZoneNameUtility {

    public static Optional<String> convertLDMLShortID(final String shortID) {
        return Optional.empty();
    }

    public static String retrieveDisplayName(final String id, final boolean daylight, final int style, final Locale locale) {
        return null;
    }

    public static String[][] getZoneStrings(final Locale locale) {
        return null;
    }
}
