/*
 * Copyright 2024 Mirko Sertic
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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class JavaAssertionsIntrinsics implements Intrinsic {

    private boolean assertionsEnabled() {
        return Boolean.parseBoolean(System.getProperty("bytecoder.assertionsEnabled", "false"));
    }

    @Override
    public Value intrinsifyStaticFieldAccess(final CompileUnit compileUnit, final AnalysisStack analysisStack, final FieldInsnNode node, final ResolvedClass sourceType, final Graph graph, final GraphParser graphParser) {
        if ("$assertionsDisabled".equals(node.name)) {
            if (assertionsEnabled()) {
                return graph.newInt(0);
            }
            return graph.newInt(1);
        }
        return null;
    }

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if (node.getOpcode() != Opcodes.INVOKESTATIC) {
            final Type targetClass = Type.getObjectType(node.owner);
            if (Class.class.getName().equals(targetClass.getClassName())) {
                if ("desiredAssertionStatus".equals(node.name)) {
                    if (assertionsEnabled()) {
                        return graph.newInt(1);
                    } else {
                        return graph.newInt(0);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ControlTokenConsumer intrinsifyWriteStaticField(final CompileUnit compileUnit, final AnalysisStack analysisStack, final FieldInsnNode node, final ResolvedClass sourceType, final Graph graph, final GraphParser graphParser) {
        if ("$assertionsDisabled".equals(node.name) && !assertionsEnabled()) {
            return graph.newNop();
        }
        return null;
    }
}
