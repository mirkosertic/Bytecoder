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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Graph {

    private final List<Node> nodes;

    public Graph() {
        nodes = new ArrayList<>();
    }

    protected Node register(final Node n) {
        nodes.add(n);
        return n;
    }

    public Node newThisNode(final Type type) {
        return register(new ThisNode(type));
    }

    public Node newMethodArgumentNode(final Type type, final int local) {
        return register(new MethodArgumentNode(type, local));
    }

    public Node newNode(final Type type) {
        return register(new Node(type));
    }

    public Node newNullNode() {
        return register(new NullNode());
    }

    public Node newIntNode(final int value) {
        return register(new IntNode(value));
    }

    public IfNode newIfNode() {
        return (IfNode) register(new IfNode());
    }

    public Node newIntegerNode(final Integer value) {
        return register(new IntegerNode(value));
    }

    public Node newFloatNode(final Float value) {
        return register(new FloatNode(value));
    }

    public Node newLongNode(final Long value) {
        return register(new LongNode(value));
    }

    public Node newDoubleNode(final Double value) {
        return register(new DoubleNode(value));
    }

    public Node newStringNode(final String value) {
        return register(new StringNode(value));
    }

    public ControlTokenConsumerNode newMethodInvocationNode(final MethodInsnNode insn) {
        return (ControlTokenConsumerNode) register(new MethodInvocationNode(insn));
    }

    public ReturnNothingNode newReturnNode() {
        return (ReturnNothingNode) register(new ReturnNothingNode());
    }

    public RegionNode getOrCreateRegionNodeFor(final String label) {
        for (final Node n : nodes) {
            if (n instanceof RegionNode) {
                final RegionNode r = (RegionNode) n;
                if (label.equals(r.label)) {
                    return r;
                }
            }
        }

        return (RegionNode) register(new RegionNode(label));
    }

    public Node newAddInt() {
        return register(new AddIntNode());
    }

    public void writeDebugTo(OutputStream fileOutputStream) {
        final PrintWriter pw = new PrintWriter(fileOutputStream);
        pw.println("digraph debugoutput {");
        for (int i = 0; i < nodes.size(); i++) {
            final Node n = nodes.get(i);
            String label = n.getClass().getSimpleName();
            if (n instanceof RegionNode) {
                label = ((RegionNode) n).label;
            }
            if (n instanceof IntNode) {
                label = "Int : " + ((IntNode) n).value;
            }
            pw.print(" node_" + i + "[label=\"" + label + "\" ");
            if (n instanceof ControlTokenConsumerNode) {
                pw.print("shape=\"box\" style=\"filled\" fillcolor=\"lightgray\"");
            }
            pw.println("];");
            for (final Node incoming : n.incomingDataFlows) {
                pw.print(" node_" + nodes.indexOf(incoming) + " -> node_" + i + "[dir=\"forward\" color=\"blue\" label=\"arg " + n.incomingDataFlows.indexOf(incoming)+ "\"];");
            }
            if (n instanceof ControlTokenConsumerNode) {
                final ControlTokenConsumerNode c = (ControlTokenConsumerNode) n;
                for (final Node outgoing : c.controlFlowsTo) {
                    pw.println(" node_" + i + " -> node_" + nodes.indexOf(outgoing) + "[dir=\"forward\" color=\"red\"];");
                }
            }

        }
        pw.println("}");
        pw.flush();
    }
}
