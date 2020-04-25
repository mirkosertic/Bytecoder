/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa;

import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;

import java.util.HashSet;
import java.util.Set;

public class StaticDependencies {

    private final Set<BytecodeLinkedClass> dependencies;

    public StaticDependencies(final Program aProgram) {
        dependencies = new HashSet<>();
        searchDependencies(aProgram);
    }

    private void searchDependencies(final Program aProgram) {
        for (final RegionNode theNode : aProgram.getControlFlowGraph().dominators().getPreOrder()) {
            searchDependencies(aProgram, theNode.getExpressions());
        }
    }

    private void searchDependencies(final Program aProgram, final ExpressionList aList) {
        for (final Expression e : aList.toList()) {
            if (e instanceof ExpressionListContainer) {
                final ExpressionListContainer c = (ExpressionListContainer) e;
                for (final ExpressionList l : c.getExpressionLists()) {
                    searchDependencies(aProgram, l);
                }
            }
            searchDependencies(aProgram, e);
        }
    }

    private void searchDependencies(final Program aProgram, final Expression e) {
        if (e instanceof InvokeStaticMethodExpression) {
            final InvokeStaticMethodExpression inv = (InvokeStaticMethodExpression) e;
            dependencies.add(aProgram.getLinkerContext().resolveClass(inv.getClassName()));
        } else if (e instanceof PutStaticExpression) {
            final PutStaticExpression put = (PutStaticExpression) e;
            dependencies.add(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(put.getField().getClassIndex().getClassConstant().getConstant())));
        } else if (e instanceof GetStaticExpression) {
            final GetStaticExpression get = (GetStaticExpression) e;
            dependencies.add(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(get.getField().getClassIndex().getClassConstant().getConstant())));
        } else if (e instanceof NewArrayExpression) {
            dependencies.add(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
        } else if (e instanceof NewMultiArrayExpression) {
            dependencies.add(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
/*        } else if (e instanceof NewObjectAndConstructExpression) {
            final NewObjectAndConstructExpression n = (NewObjectAndConstructExpression) e;
            dependencies.add(aProgram.getLinkerContext().resolveClass(n.getClazz()));
        */} else {
            searchDeeper(aProgram, e);
        }
    }

    private void searchDeeper(final Program aProgram, final Value source) {
        for (final Value v : source.incomingDataFlows()) {
            if (v instanceof Expression) {
                searchDependencies(aProgram, (Expression) v);
            } else if (v instanceof Value) {
                searchDeeper(aProgram, v);
            }
        }
    }
}
