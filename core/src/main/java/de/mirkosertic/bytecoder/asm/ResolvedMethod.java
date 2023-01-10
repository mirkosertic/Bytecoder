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

import de.mirkosertic.bytecoder.asm.parser.GraphParser;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;

public class ResolvedMethod {

    public final ResolvedClass owner;

    public final MethodNode methodNode;

    public Graph methodBody;

    public ResolvedMethod(final ResolvedClass owner, final MethodNode methodNode) {
        this.owner = owner;
        this.methodNode = methodNode;
    }

    public void parseBody(final AnalysisStack analysisStack) {
        if (!Modifier.isAbstract(methodNode.access) && !Modifier.isNative(methodNode.access)) {
            // Method is not abstract and not native
            final AnalysisStack newStack = analysisStack.addAction(new AnalysisStack.Action("Parsing method body of " + owner.type + "," + methodNode.name));
            try {
                final GraphParser graphParser = new GraphParser(owner.compileUnit, owner.type, methodNode, newStack);
                methodBody = graphParser.graph();
            } catch (final AnalysisException e) {
                throw e;
            } catch (final RuntimeException e) {
                throw new AnalysisException(e, newStack);
            }
        } else {
            methodBody = null;
        }
    }
}
