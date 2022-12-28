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

import de.mirkosertic.bytecoder.asm.Add;
import de.mirkosertic.bytecoder.asm.ControlTokenConsumer;
import de.mirkosertic.bytecoder.asm.Copy;
import de.mirkosertic.bytecoder.asm.Div;
import de.mirkosertic.bytecoder.asm.Goto;
import de.mirkosertic.bytecoder.asm.Graph;
import de.mirkosertic.bytecoder.asm.If;
import de.mirkosertic.bytecoder.asm.Int;
import de.mirkosertic.bytecoder.asm.MethodInvocation;
import de.mirkosertic.bytecoder.asm.Node;
import de.mirkosertic.bytecoder.asm.Region;
import de.mirkosertic.bytecoder.asm.ReturnNothing;
import de.mirkosertic.bytecoder.asm.Short;
import de.mirkosertic.bytecoder.asm.StandardProjections;
import de.mirkosertic.bytecoder.asm.This;
import de.mirkosertic.bytecoder.asm.Variable;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class Interpreter {

    private final Map<Variable, Object> variables;
    private final Object thisRef;

    public Interpreter(final Object thisRef, final Graph graph) {
        this.variables = new HashMap<>();
        this.thisRef = thisRef;
        interpretInstanceInvocation(graph);
    }

    private ControlTokenConsumer interpret(final Region regionNode) {
        return regionNode.flowForProjection(StandardProjections.DefaultProjection.class);
    }

    private Object interpretValue(final This node) {
        return thisRef;
    }

    private Object interpretValue(final Int node) {
        return node.value;
    }

    private Object interpretValue(final Short node) {
        return node.value;
    }

    private Object interpretValue(final Variable node) {
        return variables.get(node);
    }

    private Object interpretValue(final Add node) {
        final Node[] incoming = node.incomingDataFlows;
        if (incoming.length != 2) {
            throw new IllegalStateException("Wrong number of incoming nodes");
        }
        if (!(node.type == Type.INT_TYPE)) {
            throw new IllegalStateException("Only integer addition supported!");
        }
        final Number a = (Number) interpretValue(incoming[0]);
        final Number b = (Number) interpretValue(incoming[1]);
        return a.intValue() + b.intValue();
    }

    private Object interpretValue(final Div node) {
        final Node[] incoming = node.incomingDataFlows;
        if (incoming.length != 2) {
            throw new IllegalStateException("Wrong number of incoming nodes");
        }
        if (!(node.type == Type.INT_TYPE)) {
            throw new IllegalStateException("Only integer addition supported!");
        }
        final Number a = (Number) interpretValue(incoming[0]);
        final Number b = (Number) interpretValue(incoming[1]);
        return a.intValue() / b.intValue();
    }

    private Object interpretValue(final Node node) {
        if (node instanceof This) {
            return interpretValue((This) node);
        }
        if (node instanceof Int) {
            return interpretValue((Int) node);
        }
        if (node instanceof Short) {
            return interpretValue((Short) node);
        }
        if (node instanceof Variable) {
            return interpretValue((Variable) node);
        }
        if (node instanceof Add) {
            return interpretValue((Add) node);
        }
        if (node instanceof Div) {
            return interpretValue((Div) node);
        }
        throw new IllegalStateException("Not implemented : " + node);
    }

    private ControlTokenConsumer interpret(final Copy copyNode) {
        final Node[] incoming = copyNode.incomingDataFlows;
        final Node[] outgoing = copyNode.outgoingFlows;
        if (incoming.length == 0 && outgoing.length == 0) {
            // nothing to do
            return copyNode.flowForProjection(StandardProjections.DefaultProjection.class);
        }
        if (incoming.length != 1 || outgoing.length != 1) {
            throw new IllegalStateException("Wrong number of incoming and outgoing nodes");
        }
        final Node target = outgoing[0];
        if (!(target instanceof Variable)) {
            throw new IllegalStateException("Can only copy value to variable!");
        }
        variables.put((Variable) target, interpretValue(incoming[0]));
        return copyNode.flowForProjection(StandardProjections.DefaultProjection.class);
    }

    private ControlTokenConsumer interpret(final MethodInvocation node) {
        return node.flowForProjection(StandardProjections.DefaultProjection.class);
    }

    private ControlTokenConsumer interpret(final If node) {
        final Node[] incoming = node.incomingDataFlows;
        if (incoming.length != 2) {
            throw new IllegalStateException("Wrong number of incoming data flows!");
        }
        final Number a = (Number) interpretValue(incoming[0]);
        final Number b = (Number) interpretValue(incoming[1]);
        switch (node.operation) {
            case icmpge:
                if (!(a instanceof Integer)) {
                    throw new IllegalStateException("Only integers supported!");
                }
                if (!(b instanceof Integer)) {
                    throw new IllegalStateException("Only integers supported!");
                }
                if (a.intValue() >= b.intValue()) {
                    return node.flowForProjection(StandardProjections.TrueProjection.class);
                }
                return node.flowForProjection(StandardProjections.FalseProjection.class);
            default:
                throw new IllegalStateException("Not supported operation : " + node.operation);
        }
    }

    private ControlTokenConsumer interpret(final Goto node) {
        return node.flowForProjection(StandardProjections.DefaultProjection.class);
    }

    private ControlTokenConsumer interpret(final ControlTokenConsumer token) {
        System.out.println("Visiting token " + token);
        if (token instanceof Region) {
            return interpret((Region) token);
        }
        if (token instanceof Copy) {
            return interpret((Copy) token);
        }
        if (token instanceof MethodInvocation) {
            return interpret((MethodInvocation) token);
        }
        if (token instanceof If) {
            return interpret((If) token);
        }
        if (token instanceof Goto) {
            return interpret((Goto) token);
        }
        if (token instanceof ReturnNothing) {
            return null;
        }
        throw new IllegalStateException("Not implemented : " + token);
    }

    private void interpretInstanceInvocation(final Graph graph) {

        ControlTokenConsumer currentToken = null;
        for (final Node n : graph.nodes()) {
            if (n instanceof Region) {
                if (Graph.START_REGION_NAME.equals(((Region) n).label)) {
                    currentToken = (Region) n;
                }
            }
        }
        while (currentToken != null) {
            currentToken = interpret(currentToken);
        }
        System.out.println("final state " + variables);
    }
}
