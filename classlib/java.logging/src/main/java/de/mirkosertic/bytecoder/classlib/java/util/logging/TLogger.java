/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.util.logging;

import de.mirkosertic.bytecoder.api.AnyTypeMatches;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.logging.Level;

@SubstitutesInClass(completeReplace = true)
public class TLogger {

    public static AnyTypeMatches getLogger(final String name) {
        return (AnyTypeMatches) new SystemOutLogger(name, Level.INFO);
    }

    private final String name;
    private Level level;
    private final int offValue;

    TLogger(final String name, final String resourceBundleName) {
        this.name = name;
        this.offValue = Integer.MAX_VALUE;
        this.level = Level.INFO;
    }

    public void setLevel(final Level level) {
        this.level = level;
    }

    public boolean isLoggable(final Level aLogLevel) {
        final int levelValue = level.intValue();
        if (aLogLevel.intValue() < levelValue || levelValue == offValue) {
            return false;
        }
        return true;
    }

    public void config(final String message) {
        log(Level.CONFIG, message);
    }

    public void fine(final String message) {
        log(Level.FINE, message);
    }

    public void finer(final String message) {
        log(Level.FINER, message);
    }

    public void finest(final String message) {
        log(Level.FINEST, message);
    }

    public void info(final String message) {
        log(Level.INFO, message);
    }

    public void severe(final String message) {
        log(Level.SEVERE, message);
    }

    public void warning(final String message) {
        log(Level.WARNING, message);
    }

    public void log(final Level level, final String msg) {
        if (!isLoggable(level)) {
            return;
        }
        System.out.print("[");
        System.out.print(name);
        System.out.print("] : ");
        System.out.print("[");
        System.out.print(level.getName());
        System.out.print("] : ");
        System.out.println(msg);
    }
}
