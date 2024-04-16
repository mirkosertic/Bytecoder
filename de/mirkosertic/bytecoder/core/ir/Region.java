/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.ir;

public class Region extends ControlTokenConsumer {

    public final String label;

    public Frame frame;

    Region(final Graph owner, final String label, final NodeType nodeType) {
        super(owner, nodeType);
        this.label = label;
    }

    Region(final Graph owner, final String label) {
        this(owner, label, NodeType.Region);
    }

    @Override
    public String additionalDebugInfo() {
        return label;
    }

    @Override
    public Region stampInto(final Graph target) {
        final Region r = target.newRegion(label);
        r.frame = frame;
        return r;
    }
}
