/*
 * Copyright 2021 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.HashSet;
import java.util.Set;

public class FrameWithControlFlow extends Frame<BasicValue> {

    public final Set<FrameWithControlFlow> successors;

    private final CompilationUnit compilationUnit;

    public FrameWithControlFlow(final CompilationUnit compilationUnit, final int numLocals, final int maxStack) {
        super(numLocals, maxStack);
        this.successors = new HashSet<>();
        this.compilationUnit = compilationUnit;
    }

    public FrameWithControlFlow(final CompilationUnit compilationUnit, final Frame<? extends BasicValue> frame) {
        super(frame);
        this.successors = new HashSet<>();
        this.compilationUnit = compilationUnit;
    }
}