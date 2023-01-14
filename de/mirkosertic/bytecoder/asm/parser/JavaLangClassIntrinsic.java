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
package de.mirkosertic.bytecoder.asm.parser;

import de.mirkosertic.bytecoder.asm.AnalysisStack;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.ReadClassField;
import de.mirkosertic.bytecoder.asm.ResolvedClass;
import de.mirkosertic.bytecoder.asm.ResolvedField;
import de.mirkosertic.bytecoder.asm.TypeReference;
import de.mirkosertic.bytecoder.asm.Value;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class JavaLangClassIntrinsic implements Intrinsic {

    private final Type relevantType;

    public JavaLangClassIntrinsic() {
        this.relevantType = Type.getType(Class.class);
    }

    @Override
    public Value intrinsifyMethodInvocationWithReturnValue(final CompileUnit compileUnit, final AnalysisStack analysisStack, final MethodInsnNode node, final Value[] incomingData, final Graph graph, final GraphParser graphParser) {
        if (relevantType.equals(Type.getObjectType(node.owner))) {
            if ("getEnumConstants".equals(node.name)) {
                final Type targetType = incomingData[0].type;
                // TODO: How to convert this into an object array type?
                final Type fieldType = targetType;
                final TypeReference classRef = graph.newTypeReference(targetType);
                final ResolvedClass resolvedType = compileUnit.resolveClass(targetType, analysisStack);
                final ResolvedField resolvedField = resolvedType.resolveField("$VALUES", fieldType);
                final ReadClassField getStatic = graph.newClassFieldExpression(fieldType, resolvedField);
                getStatic.addIncomingData(classRef);
                return getStatic;
            }
        }
        return null;
    }
}
