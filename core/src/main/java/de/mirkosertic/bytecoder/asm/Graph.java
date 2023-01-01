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

    private final Map<String, Region> labeledRegions;

    public Graph() {
        nodes = new ArrayList<>();
        translations = new HashMap<>();
        fixups = new ArrayList<>();
        labeledRegions = new HashMap<>();
    }

    public void addFixup(final Fixup fixup) {
        fixups.add(fixup);
    }

    public void applyFixups(final Map<AbstractInsnNode, Map<AbstractInsnNode, EdgeType>> incomingEdgesPerInstruction) {
        for (final Fixup f : fixups) {
            f.applyTo(this, incomingEdgesPerInstruction);
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
        if (n instanceof Region) {
            final Region r = (Region) n;
            labeledRegions.put(r.label, r);
        }
        return n;
    }

    public List<Node> nodes() {
        return new ArrayList<>(nodes);
    }

    public Region regionByLabel(final String label) {
        return labeledRegions.get(label);
    }

    public This newThis(final Type type) {
        return (This) register(new This(type));
    }

    public MethodArgument newMethodArgument(final Type type, final int local) {
        return (MethodArgument) register(new MethodArgument(type, local));
    }

    public Node newNull() {
        return register(new NullReference());
    }

    public Node newIntNode(final int value) {
        return register(new Int(value));
    }

    public Node newShortNode(final short value) {
        return register(new Short(value));
    }

    public If newIf(final If.Operation operation) {
        return (If) register(new If(operation));
    }

    public Node newObjectInteger(final Integer value) {
        return register(new ObjectInteger(value));
    }

    public Node newObjectFloat(final Float value) {
        return register(new ObjectFloat(value));
    }

    public Node newObjectLong(final Long value) {
        return register(new ObjectLong(value));
    }

    public Node newObjectDouble(final Double value) {
        return register(new ObjectDouble(value));
    }

    public Node newObjectString(final String value) {
        return register(new ObjectString(value));
    }

    public MethodInvocation newMethodInvocation(final MethodInsnNode insn) {
        return (MethodInvocation) register(new MethodInvocation(insn));
    }

    public ReturnNothing newReturnNothing() {
        return (ReturnNothing) register(new ReturnNothing());
    }

    public Node newAdd(final Type type) {
        return register(new Add(type));
    }

    public Node newDiv(final Type type) {
        return register(new Div(type));
    }

    public void writeDebugTo(final OutputStream fileOutputStream) {
        final PrintWriter pw = new PrintWriter(fileOutputStream);
        pw.println("digraph debugoutput {");
        for (int i = 0; i < nodes.size(); i++) {
            final Node n = nodes.get(i);
            final String label = nodes.indexOf(n) + " " + n.getClass().getSimpleName() + " " + n.additionalDebugInfo();
            pw.print(" node_" + i + "[label=\"" + label + "\" ");
            if (n instanceof ControlTokenConsumer) {
                pw.print("shape=\"box\" fillcolor=\"orangered\" style=\"filled\"");
            } else {
                if (n instanceof Constant) {
                    pw.print("shape=\"diamong\" fillcolor=\"darkgoldenrod1\" style=\"filled\"");
                } else if (n instanceof Variable) {
                    pw.print("shape=\"oval\" fillcolor=\"cyan2\" style=\"filled\"");
                } else {
                    pw.print("shape=\"hexagon\" fillcolor=\"cyan2\" style=\"filled\"");
                }
            }
            if (n.error) {
                pw.print(" color=\"red\"");
            }
            pw.println("];");
            for (int inidx = 0; inidx < n.incomingDataFlows.length; inidx++) {
                final Node incoming = n.incomingDataFlows[inidx];
                pw.print(" node_" + nodes.indexOf(incoming) + " -> node_" + i + "[dir=\"forward\" color=\"cyan2\" label=\"arg " + inidx + "\"];");
            }
            if (n instanceof ControlTokenConsumer) {
                final ControlTokenConsumer c = (ControlTokenConsumer) n;
                for (final Map.Entry<Projection, List<ControlTokenConsumer>> entry : c.controlFlowsTo.entrySet()) {
                    for (final ControlTokenConsumer no : entry.getValue()) {
                        pw.print(" node_" + i + " -> node_" + nodes.indexOf(no) + "[dir=\"forward\"");
                        if (entry.getKey().isControlFlow()) {
                            pw.print(" color=\"red\" penwidth=\"2\"");
                        } else {
                            pw.print(" color=\"azure4\" penwidth=\"1\"");
                        }
                        pw.print(" label=\"");
                        pw.print(entry.getKey().additionalDebugInfo());
                        pw.print("\"");
                        if (entry.getKey().edgeType() == EdgeType.BACK) {
                            pw.print(" style=\"dashed\"");
                        }
                        pw.println("];");
                    }
                }

            }

        }
        pw.println("}");
        pw.flush();
    }

    public PHI newPHI(final Type type) {
        return (PHI) register(new PHI(type));
    }

    public Variable newVariable(final Type type) {
        return (Variable) register(new Variable(type));
    }

    public Copy newCopy(final Type type) {
        return (Copy) register(new Copy(type));
    }

    public CaughtException newCaughtException(final Type type) {
        return (CaughtException) register(new CaughtException(type));
    }

    public Goto newGoto() {
        return (Goto) register(new Goto());
    }

    public Region newRegion(final String label) {
        return (Region) register(new Region(label));
    }

    public TryCatch newTryCatch(final String label) {
        return (TryCatch) register(new TryCatch(label));
    }

    public Unwind newUnwind(final Type type) {
        return (Unwind) register(new Unwind(type));
    }

    public void deleteNode(final Node node) {
        nodes.remove(node);
    }
}
