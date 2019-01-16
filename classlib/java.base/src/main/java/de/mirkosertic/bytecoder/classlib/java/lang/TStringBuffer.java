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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TStringBuffer {

    private final StringBuilder builder;

    public TStringBuffer() {
        builder = new StringBuilder();
    }

    public StringBuffer append(final String aString) {
        builder.append(aString);
        final Object o = this;
        return (StringBuffer) o;
    }

    public StringBuffer append(final Object aObject) {
        builder.append(aObject);
        final Object o = this;
        return (StringBuffer) o;
    }

    public StringBuffer append(final int aValue) {
        builder.append(aValue);
        final Object o = this;
        return (StringBuffer) o;
    }

    public StringBuffer append(final char aValue) {
        builder.append(aValue);
        final Object o = this;
        return (StringBuffer) o;
    }

    public String toString() {
        return builder.toString();
    }
}
