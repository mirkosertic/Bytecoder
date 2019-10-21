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
package de.mirkosertic.bytecoder.classlib.sun.util.logging;

import de.mirkosertic.bytecoder.api.AnyTypeMatches;
import de.mirkosertic.bytecoder.api.Substitutes;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TPlatformLogger {

    private static final TPlatformLogger PLATFORM_LOGGER = new TPlatformLogger();

    public static TPlatformLogger getLogger(final String name) {
        return PLATFORM_LOGGER;
    }

    public void config(final String aConfig) {
    }

    @Substitutes("isLoggable")
    public boolean isLoggable(final AnyTypeMatches level) {
        return false;
    }

    public boolean isEnabled() {
        return false;
    }

    public void setLevel(final AnyTypeMatches level) {
    }

    public void fine(final String message) {
    }

    public void fine(final String message, final Throwable aCause) {
    }

    public void fine(final String message, final Object... params) {
    }

    public void finer(final String message) {
    }

    public void finer(final String message, final Throwable aCause) {
    }

    public void finer(final String message, final Object... params) {
    }

    public void finest(final String message) {
    }

    public void finest(final String message, final Throwable aCause) {
    }

    public void finest(final String message, final Object... params) {
    }

    public void warning(final String message) {
    }

    public void warning(final String message, final Object... params) {
    }

    public void warning(final String message, final Throwable cause) {
    }

    public void info(final String message) {
    }

    public void info(final String message, final Object... params) {
    }

    public void severe(final String message) {
    }

    public void severe(final String message, final Throwable cause) {
    }
}
