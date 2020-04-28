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
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeResolvedFields;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class DependencyAnalysis {

    public interface DependencyVisitor {
        void staticInvocation(BytecodeLinkedClass aClass, String aMethodName, BytecodeMethodSignature aSignature);
        void staticFieldAccess(BytecodeLinkedClass aClass, String aFieldName, BytecodeTypeRef aFieldType);
        void classReference(BytecodeLinkedClass aClass);
    }

    public DependencyAnalysis(final Program aProgram, final DependencyVisitor aVisitor) {
        searchDependencies(aProgram, aVisitor);
    }

    private void searchDependencies(final Program aProgram, final DependencyVisitor aVisitor) {
        for (final RegionNode theNode : aProgram.getControlFlowGraph().dominators().getPreOrder()) {
            searchDependencies(aProgram, theNode.getExpressions(), aVisitor);
        }
    }

    private void searchDependencies(final Program aProgram, final ExpressionList aList, final DependencyVisitor aVisitor) {
        for (final Expression e : aList.toList()) {
            if (e instanceof ExpressionListContainer) {
                final ExpressionListContainer c = (ExpressionListContainer) e;
                for (final ExpressionList l : c.getExpressionLists()) {
                    searchDependencies(aProgram, l, aVisitor);
                }
            }
            searchDependencies(aProgram, e, aVisitor);
        }
    }

    private BytecodeResolvedFields.FieldEntry implementingClassForStaticField(final BytecodeLinkerContext aLinkerContext, final BytecodeObjectTypeRef aClass, final String aFieldName) {
        final BytecodeLinkedClass theLinkedClass = aLinkerContext.resolveClass(aClass);
        final BytecodeResolvedFields theFields = theLinkedClass.resolvedFields();
        return theFields.fieldByName(aFieldName);
    }

    private void searchDependencies(final Program aProgram, final Value e, final DependencyVisitor aVisitor) {
        if (e instanceof InvokeStaticMethodExpression) {
            final InvokeStaticMethodExpression inv = (InvokeStaticMethodExpression) e;
            aVisitor.staticInvocation(aProgram.getLinkerContext().resolveClass(inv.getClassName()), inv.getMethodName(), inv.getSignature());
        } else if (e instanceof PutStaticExpression) {
            final PutStaticExpression put = (PutStaticExpression) e;
            final BytecodeResolvedFields.FieldEntry theEntry = implementingClassForStaticField(aProgram.getLinkerContext(), BytecodeObjectTypeRef.fromUtf8Constant(put.getField().getClassIndex().getClassConstant().getConstant()),
                    put.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

            final BytecodeObjectTypeRef theClass = theEntry.getProvidingClass().getClassName();
            aVisitor.staticFieldAccess(aProgram.getLinkerContext().resolveClass(theClass), theEntry.getValue().getName().stringValue(), theEntry.getValue().getTypeRef());
        } else if (e instanceof GetStaticExpression) {
            final GetStaticExpression get = (GetStaticExpression) e;
            final BytecodeResolvedFields.FieldEntry theEntry = implementingClassForStaticField(aProgram.getLinkerContext(), BytecodeObjectTypeRef.fromUtf8Constant(get.getField().getClassIndex().getClassConstant().getConstant()),
                    get.getField().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());

            final BytecodeObjectTypeRef theClass = theEntry.getProvidingClass().getClassName();
            aVisitor.staticFieldAccess(aProgram.getLinkerContext().resolveClass(theClass), theEntry.getValue().getName().stringValue(), theEntry.getValue().getTypeRef());
        } else if (e instanceof NewArrayExpression) {
            aVisitor.classReference(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
        } else if (e instanceof NewMultiArrayExpression) {
            aVisitor.classReference(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class)));
        } else if (e instanceof ClassReferenceValue) {
            final ClassReferenceValue r = (ClassReferenceValue) e;
            aVisitor.classReference(aProgram.getLinkerContext().resolveClass(r.getType()));
        } else if (e instanceof NewObjectExpression) {
            final NewObjectExpression n = (NewObjectExpression) e;
            aVisitor.classReference(aProgram.getLinkerContext().resolveClass(BytecodeObjectTypeRef.fromUtf8Constant(n.getType().getConstant())));
        } else if (e instanceof NewObjectAndConstructExpression) {
            final NewObjectAndConstructExpression n = (NewObjectAndConstructExpression) e;
            aVisitor.classReference(aProgram.getLinkerContext().resolveClass(n.getClazz()));
        }

        for (final Value v : e.incomingDataFlows()) {
            searchDependencies(aProgram, v, aVisitor);
        }
    }
}
