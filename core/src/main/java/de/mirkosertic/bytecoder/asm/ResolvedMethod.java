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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class ResolvedMethod {

    public final ResolvedClass owner;

    public final MethodNode methodNode;

    public final Graph methodBody;

    public ResolvedMethod(final ResolvedClass owner, final MethodNode methodNode, final AnalysisStack analysisStack) {
        this.owner = owner;
        this.methodNode = methodNode;

        if (((methodNode.access & Opcodes.ACC_ABSTRACT) == 0) && ((methodNode.access & Opcodes.ACC_NATIVE) == 0)) {
            // Method is not abstract and not native
            final GraphParser graphParser = new GraphParser(owner.compileUnit, owner.type, methodNode, analysisStack);
            methodBody = graphParser.graph();
        } else {
            methodBody = null;
        }
    }
}
