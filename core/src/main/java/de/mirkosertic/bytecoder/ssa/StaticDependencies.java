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
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StaticDependencies {

    private final Set<BytecodeLinkedClass> dependencies;

    public StaticDependencies(final Program aProgram) {
        dependencies = new HashSet<>();
        searchDependencies(aProgram);
    }

    public List<BytecodeLinkedClass> list() {
        return dependencies.stream().sorted(Comparator.comparing(o -> o.getClassName().name())).collect(Collectors.toList());
    }

    private void searchDependencies(final Program aProgram) {
        final Set<Value> theAlreadySeen = new HashSet<>();
        for (final RegionNode theNode : aProgram.getControlFlowGraph().dominators().getPreOrder()) {
            searchDependencies(aProgram, theNode.getExpressions(), theAlreadySeen);
        }
    }

    private void searchDependencies(final Program aProgram, final ExpressionList aList, final Set<Value> aAlreadySeen) {
        for (final Expression e : aList.toList()) {
            if (e instanceof ExpressionListContainer) {
                final ExpressionListContainer c = (ExpressionListContainer) e;
                for (final ExpressionList l : c.getExpressionLists()) {
                    searchDependencies(aProgram, l, aAlreadySeen);
                }
            }
            searchDependencies(aProgram, e, aAlreadySeen);
        }
    }

    private BytecodeResolvedFields.FieldEntry implementingClassForStaticField(final BytecodeLinkerContext aLinkerContext, final BytecodeObjectTypeRef aClass, final String aFieldName) {
        final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(aClass);
        final BytecodeResolvedFields theFields = theLinkedClass.resolvedFields();
        return theFields.fieldByName(aFieldName);
    }

    private void searchDependencies(final Program aProgram, final Value e, final Set<Value> alreadySeen) {
        if (alreadySeen.add(e)) {
            if (e instanceof InvokeStaticMethodExpression) {
                final InvokeStaticMethodExpression inv = (InvokeStaticMethodExpression) e;
                dependencies.add(aProgram.getLinkerContext().resolveClass(inv.getInvokedClass()));
            } else if (e instanceof PutStaticExpression) {
                final PutStaticExpression put = (PutStaticExpression) e;
                final BytecodeResolvedFields.FieldEntry theEntry = implementingClassForStaticField(aProgram.getLinkerContext(), BytecodeObjectTypeRef.fromUtf8Constant(put.getField().getClassIndex().getClassConstant().getConstant()),
                        put.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

                final BytecodeObjectTypeRef theClass = theEntry.getProvidingClass().getClassName();
                dependencies.add(aProgram.getLinkerContext().resolveClass(theClass));
            } else if (e instanceof GetStaticExpression) {
                final GetStaticExpression get = (GetStaticExpression) e;
                final BytecodeResolvedFields.FieldEntry theEntry = implementingClassForStaticField(aProgram.getLinkerContext(), BytecodeObjectTypeRef.fromUtf8Constant(get.getField().getClassIndex().getClassConstant().getConstant()),
                        get.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

                final BytecodeObjectTypeRef theClass = theEntry.getProvidingClass().getClassName();
                dependencies.add(aProgram.getLinkerContext().resolveClass(theClass));
            } else if (e instanceof NewArrayExpression) {
                dependencies.add(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
            } else if (e instanceof NewMultiArrayExpression) {
                dependencies.add(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
            } else if (e instanceof ClassReferenceValue) {
                final ClassReferenceValue r = (ClassReferenceValue) e;
                dependencies.add(aProgram.getLinkerContext().resolveClass(r.getType()));
            } else if (e instanceof NewInstanceExpression) {
                final NewInstanceExpression n = (NewInstanceExpression) e;
                dependencies.add(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(n.getType().getConstant())));
            } else if (e instanceof NewInstanceAndConstructExpression) {
                final NewInstanceAndConstructExpression n = (NewInstanceAndConstructExpression) e;
                dependencies.add(aProgram.getLinkerContext().resolveClass(n.getClazz()));
            }

            for (final Value v : e.incomingDataFlows()) {
                searchDependencies(aProgram, v, alreadySeen);
            }
        }
    }
}
