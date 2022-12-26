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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {

    public static final String START_REGION_NAME = "Start";

    private final List<Node> nodes;

    private final Map<AbstractInsnNode, InstructionTranslation> translations;

    private final List<Fixup> fixups;

    public Graph() {
        nodes = new ArrayList<>();
        translations = new HashMap<>();
        fixups = new ArrayList<>();
    }

    public void addFixup(final Fixup fixup) {
        fixups.add(fixup);
    }

    public void applyFixups() {
        for (final Fixup f : fixups) {
            f.applyTo(this);
        }
        fixups.clear();
    }

    public void registerTranslation(final AbstractInsnNode instruction, final InstructionTranslation translation) {
        if (!translations.containsKey(instruction)) {
            translations.put(instruction, translation);
        } else {
            System.out.println("Already mapped translation for " + instruction);
        }
    }

    public InstructionTranslation translationFor(final AbstractInsnNode instruction) {
        return translations.get(instruction);
    }

    protected Node register(final Node n) {
        nodes.add(n);
        return n;
    }

    public List<Node> nodes() {
        return nodes;
    }

    public VarNode newThisNode(final Type type) {
        return (VarNode) register(new ThisNode(type));
    }

    public MethodArgumentNode newMethodArgumentNode(final Type type, final int local) {
        return (MethodArgumentNode) register(new MethodArgumentNode(type, local));
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

    public Node newShortNode(final short value) {
        return register(new ShortNode(value));
    }

    public IfNode newIfNode(final IfNode.Operation operation) {
        return (IfNode) register(new IfNode(operation));
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

    public Node newAddNode(final Type type) {
        return register(new AddNode(type));
    }

    public Node newDivNode(final Type type) {
        return register(new DivNode(type));
    }

    public void writeDebugTo(final OutputStream fileOutputStream) {
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
            if (n instanceof ShortNode) {
                label = "Short : " + ((ShortNode) n).value;
            }
            pw.print(" node_" + i + "[label=\"" + label + "\" ");
            if (n instanceof ControlTokenConsumerNode) {
                pw.print("style=\"filled\" fillcolor=\"lightgray\"");
            }
            if (n.error) {
                pw.print(" color=\"red\"");
            }
            pw.println("];");
            for (final Node incoming : n.incomingDataFlows) {
                pw.print(" node_" + nodes.indexOf(incoming) + " -> node_" + i + "[dir=\"forward\" color=\"blue\" label=\"arg " + n.incomingDataFlows.indexOf(incoming)+ "\"];");
            }
            if (n instanceof ControlTokenConsumerNode) {
                final ControlTokenConsumerNode c = (ControlTokenConsumerNode) n;
                for (final Map.Entry<Projection, List<ControlTokenConsumerNode>> entry : c.controlFlowsTo.entrySet()) {
                    for (final ControlTokenConsumerNode no : entry.getValue()) {
                        pw.print(" node_" + i + " -> node_" + nodes.indexOf(no) + "[dir=\"forward\" color=\"red\"");
                        if (!(entry.getKey() instanceof StandardProjections.DefaultProjection)) {
                            pw.print(" label=\"" + entry.getKey().getClass().getSimpleName() + "\"");
                        }
                        pw.println("];");
                    }
                }
            }

        }
        pw.println("}");
        pw.flush();
    }

    public PHINode newPHINode(final Type type) {
        return (PHINode) register(new PHINode(type));
    }

    public VarNode newVarNode(final Type type) {
        return (VarNode) register(new VarNode(type));
    }

    public CopyNode newCopyNode(final Type type) {
        return (CopyNode) register(new CopyNode(type));
    }

    public VarNode newCaughtException(final Type type) {
        return (VarNode) register(new CaughtException(type));
    }

    public GotoNode newGotoNode() {
        return (GotoNode) register(new GotoNode());
    }
}
