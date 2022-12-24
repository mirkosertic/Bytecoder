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
package de.mirkosertic.bytecoder.asm.interpreter;

import de.mirkosertic.bytecoder.asm.*;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {

    private final Map<VarNode, Object> variables;
    private final Object thisRef;

    public Interpreter(final Object thisRef, final Graph graph) {
        this.variables = new HashMap<>();
        this.thisRef = thisRef;
        interpretInstanceInvocation(graph);
    }

    private ControlTokenConsumerNode interpret(final RegionNode regionNode) {
        return regionNode.flowForProjection(StandardProjections.DEFAULT);
    }

    private Object interpretValue(final ThisNode node) {
        return thisRef;
    }

    private Object interpretValue(final IntNode node) {
        return node.value;
    }

    private Object interpretValue(final VarNode node) {
        return variables.get(node);
    }

    private Object interpretValue(final AddNode node) {
        final List<Node> incoming = node.incomingDataFlows;
        if (incoming.size() != 2) {
            throw new IllegalStateException("Wrong number of incoming nodes");
        }
        if (!(node.type == Type.INT_TYPE)) {
            throw new IllegalStateException("Only integer addition supported!");
        }
        final Number a = (Number) interpretValue(incoming.get(0));
        final Number b = (Number) interpretValue(incoming.get(1));
        if (!(a instanceof Integer)) {
            throw new IllegalStateException("Only integers supported!");
        }
        if (!(b instanceof Integer)) {
            throw new IllegalStateException("Only integers supported!");
        }
        return a.intValue() + b.intValue();
    }

    private Object interpretValue(final Node node) {
        if (node instanceof ThisNode) {
            return interpretValue((ThisNode) node);
        }
        if (node instanceof IntNode) {
            return interpretValue((IntNode) node);
        }
        if (node instanceof VarNode) {
            return interpretValue((VarNode) node);
        }
        if (node instanceof AddNode) {
            return interpretValue((AddNode) node);
        }
        throw new IllegalStateException("Not implemented : " + node);
    }

    private ControlTokenConsumerNode interpret(final CopyNode copyNode) {
        final List<Node> incoming = copyNode.incomingDataFlows;
        final List<Node> outgoing = copyNode.outgoingFlows;
        if (incoming.isEmpty() && outgoing.isEmpty()) {
            // nothing to do
            return copyNode.flowForProjection(StandardProjections.DEFAULT);
        }
        if (incoming.size() != 1 || outgoing.size() != 1) {
            throw new IllegalStateException("Wrong number of incoming and outgoing nodes");
        }
        final Node target = outgoing.get(0);
        if (!(target instanceof VarNode)) {
            throw new IllegalStateException("Can only copy value to variable!");
        }
        variables.put((VarNode) target, interpretValue(incoming.get(0)));
        return copyNode.flowForProjection(StandardProjections.DEFAULT);
    }

    private ControlTokenConsumerNode interpret(final MethodInvocationNode node) {
        return node.flowForProjection(StandardProjections.DEFAULT);
    }

    private ControlTokenConsumerNode interpret(final IfNode node) {
        final List<Node> incoming = node.incomingDataFlows;
        if (incoming.size() != 2) {
            throw new IllegalStateException("Wrong number of incoming data flows!");
        }
        final Number a = (Number) interpretValue(incoming.get(0));
        final Number b = (Number) interpretValue(incoming.get(1));
        switch (node.operation) {
            case icmpge:
                if (!(a instanceof Integer)) {
                    throw new IllegalStateException("Only integers supported!");
                }
                if (!(b instanceof Integer)) {
                    throw new IllegalStateException("Only integers supported!");
                }
                if (a.intValue() >= b.intValue()) {
                    return node.flowForProjection(StandardProjections.TRUE);
                }
                return node.flowForProjection(StandardProjections.FALSE);
            default:
                throw new IllegalStateException("Not suppted operation : " + node.operation);
        }
    }

    private ControlTokenConsumerNode interpret(final ControlTokenConsumerNode token) {
        System.out.println("Visiting token " + token);
        if (token instanceof RegionNode) {
            return interpret((RegionNode) token);
        }
        if (token instanceof CopyNode) {
            return interpret((CopyNode) token);
        }
        if (token instanceof MethodInvocationNode) {
            return interpret((MethodInvocationNode) token);
        }
        if (token instanceof IfNode) {
            return interpret((IfNode) token);
        }
        if (token instanceof ReturnNothingNode) {
            return null;
        }
        throw new IllegalStateException("Not implemented : " + token);
    }

    private void interpretInstanceInvocation(final Graph graph) {
        ControlTokenConsumerNode currentToken = graph.getOrCreateRegionNodeFor(Graph.START_REGION_NAME);
        while (currentToken != null) {
            currentToken = interpret(currentToken);
        }
        System.out.println("final state " + variables);
    }
}
