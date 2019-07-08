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
package de.mirkosertic.bytecoder.stackifier;

public class DebugOutput {

    public static String toString(final Sequence sequence) {
        return toString(new StringBuilder(), 0, sequence).toString();
    }

    private static StringBuilder toString(final StringBuilder builder, final int level, final Sequence sequence) {
        for (final Element e : sequence.getElements()) {
            toString(builder, level, e);
        }
        return builder;
    }

    private static StringBuilder toString(final StringBuilder builder, final int level, final Element element) {
        if (element instanceof Sequence) {
            return toString(builder, level, (Sequence) element);
        }
        if (element instanceof RegionNodeElement) {
            return toString(builder, level, (RegionNodeElement) element);
        }
        throw new IllegalArgumentException("Not supported. : " + element);
    }

    private static StringBuilder toString(final StringBuilder builder, final int level, final RegionNodeElement element) {
        for (int i=0;i<level;i++) {
            builder.append("\t");
        }
        builder.append("$");
        builder.append(element.getRegionNode().getStartAddress().getAddress());
        builder.append(":");
        builder.append(System.lineSeparator());
        return builder;
    }

}
