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
package de.mirkosertic.bytecoder.classlib.java.util.regex;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.regex.Pattern;

@SubstitutesInClass(completeReplace = true)
public class TPattern {

    private int flags;

    private String pattern;

    public static Pattern compile(final String regex) {
        return compile(regex, 0);
    }
    public static Pattern compile(final String regex, final int flags) {
        return (Pattern) (Object) new TPattern(flags, regex);
    }

    public TPattern(final int flags, final String pattern) {
        this.flags = flags;
        this.pattern = pattern;
    }
}
