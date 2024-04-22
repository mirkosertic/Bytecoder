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
package de.mirkosertic.bytecoder.core.parser;

import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.ir.ControlTokenConsumer;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.Value;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

public class CoreIntrinsics implements Intrinsic {

    private final List<Intrinsic> intrinsics;

    public CoreIntrinsics(final Intrinsic... i) {
        this.intrinsics = new ArrayList<>();
        for (final Intrinsic in : i) {
            this.intrinsics.add(in);
        }
        this.intrinsics.add(new VMIntrinsics());
        this.intrinsics.add(new JavaLangClassIntrinsics());
        this.intrinsics.add(new JavaLangObjectIntrinsics());
        this.intrinsics.add(new JavaLangSystemIntrinsics());
        this.intrinsics.add(new JavaAssertionsIntrinsics());
    }

    @Override
    public Value intrinsifyStaticFieldAccess(final CompileUnit compileUnit,final AnalysisStack analysisStack,final FieldInsnNode node,final ResolvedClass sourceType,final Graph graph,final GraphParser graphParser) {
        for (final Intrinsic i : intrinsics) {
            final Value result = i.intrinsifyStaticFieldAccess(compileUnit, analysisStack, node, sourceType, graph, graphParser);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        for (final Intrinsic i : intrinsics) {
            final Value result = i.intrinsifyMethodInvocationWithReturnValue(compileUnit, analysisStack, node, incomingData, graph, graphParser);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public ControlTokenConsumer intrinsifyMethodInvocation(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        for (final Intrinsic i : intrinsics) {
            final ControlTokenConsumer result = i.intrinsifyMethodInvocation(compileUnit, analysisStack, node, incomingData, graph, graphParser);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public ControlTokenConsumer intrinsifyWriteStaticField(final CompileUnit compileUnit, final AnalysisStack analysisStack, final FieldInsnNode node, final ResolvedClass sourceType, final Graph graph, final GraphParser graphParser) {
        for (final Intrinsic i : intrinsics) {
            final ControlTokenConsumer result = i.intrinsifyWriteStaticField(compileUnit, analysisStack, node, sourceType, graph, graphParser);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
