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
package de.mirkosertic.bytecoder.core.parser;

import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.Value;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public interface Intrinsic {

    default Value intrinsifyStaticFieldAccess(final CompileUnit compileUnit, final AnalysisStack analysisStack, final FieldInsnNode node, final ResolvedClass sourceType, final Graph graph, final GraphParser graphParser) {
        return null;
    }

    default Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        return null;
    }

    default ControlTokenConsumer intrinsifyMethodInvocation(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        return null;
    }

    default ControlTokenConsumer intrinsifyWriteStaticField(final CompileUnit compileUnit, final AnalysisStack analysisStack, final FieldInsnNode node, final ResolvedClass sourceType, final Graph graph, final GraphParser graphParser) {
        return null;
    }
}
