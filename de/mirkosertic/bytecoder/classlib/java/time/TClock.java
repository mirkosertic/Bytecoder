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
package de.mirkosertic.bytecoder.classlib.java.time;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

@SubstitutesInClass(completeReplace = true)
public class TClock {

    private final TimeZone timeZone;

    public static Clock systemDefaultZone() {
        return (Clock) (Object) new TClock(TimeZone.getDefault());
    }

    public TClock(final TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public Instant instant() {
        return Instant.ofEpochMilli(System.currentTimeMillis());
    }

    public ZoneId getZone() {
        return this.timeZone.toZoneId();
    }
}
