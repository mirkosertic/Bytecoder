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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.AbstractInvocation;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.InvocationType;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.Value;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class VirtualToDirectInvocation implements Optimizer {

    private final NodePatternMatcher patternMatcher;

    public VirtualToDirectInvocation() {
        patternMatcher = new NodePatternMatcher(
                NodePredicates.ofType(AbstractInvocation.class)
        );
    }

    private Set<ResolvedClass> subclassesOf(final ResolvedClass rc) {
        final Set<ResolvedClass> result = new HashSet<>();
        for (final ResolvedClass subClass : rc.directSubclasses) {
            if (result.add(subClass)) {
                result.addAll(subclassesOf(subClass));
            }
        }
        return result;
    }

    private boolean isMethodOverriddenInSubclass(final ResolvedClass rc, final ResolvedMethod method) {

        for (final ResolvedClass sub : subclassesOf(rc)) {
            for (final ResolvedMethod o : sub.resolvedMethods) {
                if (!Modifier.isAbstract(o.methodNode.access) && o.owner == sub && o.methodNode.name.equals(method.methodNode.name) && o.methodType.equals(method.methodType)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean optimize(final CompileUnit compileUnit, final ResolvedMethod method) {
        final Graph g = method.methodBody;

        for (final Node node : g.nodes()) {
            if (patternMatcher.test(g, node)) {
                final AbstractInvocation invocation = (AbstractInvocation) node;
                switch (invocation.invocationType()) {
                    case VIRTUAL: {
                        final Value target = (Value) node.incomingDataFlows[0];
                        final ResolvedMethod invokedMethod = invocation.method();
                        final ResolvedClass targetClass = compileUnit.findClass(target.type);

                        if (targetClass == invokedMethod.owner && !isMethodOverriddenInSubclass(targetClass, invokedMethod) && !Modifier.isAbstract(invokedMethod.methodNode.access)) {
                            // Can be a direct invocation
                            invocation.changeInvocationTypeTo(InvocationType.DIRECT);
                        }
                        break;
                    }
                }
            }
        }

        return false;
    }
}
