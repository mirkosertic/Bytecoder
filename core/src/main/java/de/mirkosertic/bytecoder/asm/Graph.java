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

    public MethodArgument newMethodArgument(final Type type, final int index) {
        return (MethodArgument) register(new MethodArgument(type, index));
    }

    public NullReference newNullReference() {
        return (NullReference) register(new NullReference());
    }

    public Int newIntNode(final int value) {
        return (Int) register(new Int(value));
    }

    public Short newShortNode(final short value) {
        return (Short) register(new Short(value));
    }

    public NewArray newNewArray(final Type elementType) {
        return (NewArray) register(new NewArray(elementType));
    }

    public If newIf(final If.Operation operation) {
        return (If) register(new If(operation));
    }

    public ObjectIf newObjectIf(final ObjectIf.Operation operation) {
        return (ObjectIf) register(new ObjectIf(operation));
    }

    public USHR newUSHR(final Type type) {
        return (USHR) register(new USHR(type));
    }

    public And newAND(final Type type) {
        return (And) register(new And(type));
    }

    public ObjectInteger newObjectInteger(final Integer value) {
        return (ObjectInteger)register(new ObjectInteger(value));
    }

    public ObjectFloat newObjectFloat(final Float value) {
        return (ObjectFloat) register(new ObjectFloat(value));
    }

    public ObjectLong newObjectLong(final Long value) {
        return (ObjectLong) register(new ObjectLong(value));
    }

    public ObjectDouble newObjectDouble(final Double value) {
        return (ObjectDouble) register(new ObjectDouble(value));
    }

    public ObjectString newObjectString(final String value) {
        return (ObjectString) register(new ObjectString(value));
    }

    public InstanceMethodInvocation newInstanceMethodInvocation(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (InstanceMethodInvocation) register(new InstanceMethodInvocation(insn, rm));
    }

    public InstanceMethodInvocationExpression newInstanceMethodInvocationExpression(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (InstanceMethodInvocationExpression) register(new InstanceMethodInvocationExpression(insn, rm));
    }

    public VirtualMethodInvocation newVirtualMethodInvocation(final MethodInsnNode insn) {
        return (VirtualMethodInvocation) register(new VirtualMethodInvocation(insn));
    }

    public VirtualMethodInvocationExpression newVirtualMethodInvocationExpression(final MethodInsnNode insn) {
        return (VirtualMethodInvocationExpression) register(new VirtualMethodInvocationExpression(insn));
    }

    public StaticMethodInvocation newStaticMethodInvocation(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (StaticMethodInvocation) register(new StaticMethodInvocation(insn, rm));
    }

    public StaticMethodInvocationExpression newStaticMethodInvocationExpression(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (StaticMethodInvocationExpression) register(new StaticMethodInvocationExpression(insn, rm));
    }

    public ReturnNothing newReturnNothing() {
        return (ReturnNothing) register(new ReturnNothing());
    }

    public ReturnValue newReturnValue() {
        return (ReturnValue) register(new ReturnValue());
    }

    public Add newAdd(final Type type) {
        return (Add) register(new Add(type));
    }

    public ArrayStore newArrayStore() {
        return (ArrayStore) register(new ArrayStore());
    }

    public ArrayLoad newArrayLoad(final Type type) {
        return (ArrayLoad) register(new ArrayLoad(type));
    }

    public Sub newSub(final Type type) {
        return (Sub) register(new Sub(type));
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

    public Copy newCopy() {
        return (Copy) register(new Copy());
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

    public Unwind newUnwind() {
        return (Unwind) register(new Unwind());
    }

    public TypeReference newTypeReference(final Type type) {
        return (TypeReference) register(new TypeReference(type));
    }

    public New newNew(final Type type) {
        return (New) register(new New(type));
    }

    public InstanceFieldExpression newInstanceFieldExpression(final Type type, final ResolvedField resolvedField) {
        return (InstanceFieldExpression) register(new InstanceFieldExpression(type, resolvedField));
    }

    public SetInstanceField newSetInstanceField(final Type type, final ResolvedField resolvedField) {
        return (SetInstanceField) register(new SetInstanceField(resolvedField));
    }

    public void deleteNode(final Node node) {
        nodes.remove(node);
    }
}
