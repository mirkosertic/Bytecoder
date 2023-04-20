/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.time.format;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

@SubstitutesInClass(completeReplace = true)
public class TDateTimeFormatter {

    public static DateTimeFormatter ISO_INSTANT = (DateTimeFormatter) (Object) new TDateTimeFormatter();

    public TDateTimeFormatter() {
    }

    public String format(final TemporalAccessor temporalAccessor) {
        // TODO
        return "";
    }
}
