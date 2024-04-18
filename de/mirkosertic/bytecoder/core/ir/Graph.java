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
package de.mirkosertic.bytecoder.core.ir;

import de.mirkosertic.bytecoder.api.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph {

    public static final String START_REGION_NAME = "Start";

    private final List<Node> nodes;

    private final Map<AbstractInsnNode, InstructionTranslation> translations;

    private final List<Fixup> fixups;

    private final Map<String, Region> labeledRegions;

    private final Logger logger;

    private int importLabelCounter = 0;

    public Graph(final Logger logger) {
        this.nodes = new ArrayList<>();
        this.translations = new HashMap<>();
        this.fixups = new ArrayList<>();
        this.labeledRegions = new HashMap<>();
        this.logger = logger;
    }

    public Region newStartRegion() {
        return (Region) register(new Region(this, START_REGION_NAME));
    }

    public Node[] outgoingDataFlowsFor(final Node n) {
        final Set<Node> result = new HashSet<>();
        for (final Node t : nodes) {
            final Node[] incoming = t.incomingDataFlows;
            for (int i = 0; i <incoming.length; i++) {
                if (incoming[i] == n) {
                    result.add(t);
                }
            }
        }
        return result.toArray(new Node[0]);
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
            logger.warn("Already mapped translation for {}", instruction);
        }
    }

    public InstructionTranslation translationFor(final AbstractInsnNode instruction) {
        return translations.get(instruction);
    }

    public Node register(final Node n) {
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
        return (This) register(new This(this, type));
    }

    public MethodArgument newMethodArgument(final Type type, final int index) {
        return (MethodArgument) register(new MethodArgument(this, type, index));
    }

    public NullReference newNullReference() {
        return (NullReference) register(new NullReference(this));
    }

    public PrimitiveInt newInt(final int value) {
        return (PrimitiveInt) register(new PrimitiveInt(this, value));
    }

    public PrimitiveShort newShort(final short value) {
        return (PrimitiveShort) register(new PrimitiveShort(this, value));
    }

    public NewArray newNewArray(final Type arrayType) {
        return (NewArray) register(new NewArray(this, arrayType));
    }

    public If newIf() {
        return (If) register(new If(this));
    }

    public USHR newUSHR(final Type type) {
        return (USHR) register(new USHR(this, type));
    }

    public SHR newSHR(final Type type) {
        return (SHR) register(new SHR(this, type));
    }

    public SHL newSHL(final Type type) {
        return (SHL) register(new SHL(this, type));
    }

    public Neg newNEG(final Type type) {
        return (Neg) register(new Neg(this, type));
    }

    public And newAND(final Type type) {
        return (And) register(new And(this, type));
    }

    public Or newOR(final Type type) {
        return (Or) register(new Or(this, type));
    }

    public XOr newXOR(final Type type) {
        return (XOr) register(new XOr(this, type));
    }

    public ObjectString newObjectString(final StringConstant value) {
        return (ObjectString) register(new ObjectString(this, value));
    }

    public MethodInvocation newMethodInvocation(final InvocationType invocationType, final MethodInsnNode insn, final ResolvedMethod rm) {
        return (MethodInvocation) register(new MethodInvocation(this, insn, rm, invocationType));
    }

    public MethodInvocationExpression newMethodInvocationExpression(final InvocationType invocationType, final MethodInsnNode insn, final ResolvedMethod rm) {
        return (MethodInvocationExpression) register(new MethodInvocationExpression(this, insn, rm, invocationType));
    }

    public Return newReturnNothing() {
        return (Return) register(new Return(this));
    }

    public ReturnValue newReturnValue() {
        return (ReturnValue) register(new ReturnValue(this));
    }

    public Add newAdd(final Type type) {
        return (Add) register(new Add(this, type));
    }

    public ArrayStore newArrayStore() {
        return (ArrayStore) register(new ArrayStore(this));
    }

    public ArrayLoad newArrayLoad(final Type type) {
        return (ArrayLoad) register(new ArrayLoad(this, type));
    }

    public Sub newSub(final Type type) {
        return (Sub) register(new Sub(this, type));
    }

    public Div newDiv(final Type type) {
        return (Div) register(new Div(this, type));
    }

    public Mul newMul(final Type type) {
        return (Mul) register(new Mul(this, type));
    }

    public TypeConversion newTypeConversion(final Type type) {
        return (TypeConversion) register(new TypeConversion(this, type));
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
                if (n.isConstant()) {
                    pw.print("shape=\"diamond\" fillcolor=\"darkgoldenrod1\" style=\"filled\"");
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
                pw.println(" node_" + nodes.indexOf(incoming) + " -> node_" + i + "[dir=\"forward\" color=\"cyan2\" label=\"arg " + inidx + "\"];");
            }
            if (n instanceof ControlTokenConsumer) {
                final ControlTokenConsumer c = (ControlTokenConsumer) n;
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : c.controlFlowsTo.entrySet()) {
                    pw.print(" node_" + i + " -> node_" + nodes.indexOf(entry.getValue()) + "[dir=\"forward\"");
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
        pw.println("}");
        pw.flush();
    }

    public PHI newPHI(final Type type) {
        return (PHI) register(new PHI(this, type));
    }

    public Variable newVariable(final Type type) {
        return (Variable) register(new Variable(this, type));
    }

    public Copy newCopy() {
        return (Copy) register(new Copy(this));
    }

    public Cast newCast(final Type type) {
        return (Cast) register(new Cast(this, type));
    }

    public Nop newNop() {
        return (Nop) register(new Nop(this));
    }
    public CaughtException newCaughtException(final Type type) {
        return (CaughtException) register(new CaughtException(this, type));
    }

    public Goto newGoto() {
        return (Goto) register(new Goto(this));
    }

    public CMP newCMP() {
        return (CMP) register(new CMP(this));
    }

    public Rem newRem(final Type type) {
        return (Rem) register(new Rem(this, type));
    }

    public Region newRegion(final String label) {
        return (Region) register(new Region(this, label));
    }

    public TryCatch newTryCatch(final String label) {
        return (TryCatch) register(new TryCatch(this, label));
    }

    public Unwind newUnwind() {
        return (Unwind) register(new Unwind(this));
    }

    public MonitorEnter newMonitorEnter() {
        return (MonitorEnter) register(new MonitorEnter(this));
    }

    public MonitorExit newMonitorExit() {
        return (MonitorExit) register(new MonitorExit(this));
    }

    public ArrayLength newArrayLength() {
        return (ArrayLength) register(new ArrayLength(this));
    }

    public TypeReference newTypeReference(final Type type) {
        for (final Node n : nodes) {
            if (n instanceof TypeReference) {
                final TypeReference t = (TypeReference) n;
                if (t.type.equals(type)) {
                    return t;
                }
            }
        }
        return (TypeReference) register(new TypeReference(this, type));
    }

    public New newNew(final Type type) {
        return (New) register(new New(this, type));
    }

    public InstanceOf newInstanceOf() {
        return (InstanceOf) register(new InstanceOf(this));
    }

    public TableSwitch newTableSwitch(final int min, final int max) {
        return (TableSwitch) register(new TableSwitch(this, min, max));
    }

    public LookupSwitch newLookupSwitch() {
        return (LookupSwitch) register(new LookupSwitch(this));
    }

    public ReadInstanceField newInstanceFieldExpression(final Type type, final ResolvedField resolvedField) {
        return (ReadInstanceField) register(new ReadInstanceField(this, type, resolvedField));
    }

    public ReadClassField newClassFieldExpression(final Type type, final ResolvedField resolvedField) {
        return (ReadClassField) register(new ReadClassField(this, type, resolvedField));
    }

    public SetInstanceField newSetInstanceField(final ResolvedField resolvedField) {
        return (SetInstanceField) register(new SetInstanceField(this, resolvedField));
    }

    public SetClassField newSetClassField(final ResolvedField resolvedField) {
        return (SetClassField) register(new SetClassField(this, resolvedField));
    }

    public void deleteNode(final Node node) {
        nodes.remove(node);
    }

    public PrimitiveFloat newFloat(final float constant) {
        return (PrimitiveFloat) register(new PrimitiveFloat(this, constant));
    }

    public PrimitiveDouble newDouble(final double constant) {
        return (PrimitiveDouble) register(new PrimitiveDouble(this, constant));
    }

    public PrimitiveLong newLong(final long constant) {
        return (PrimitiveLong) register(new PrimitiveLong(this, constant));
    }

    public LineNumberDebugInfo newLineNumberDebugInfo(final String sourceFile, final int lineNumber) {
        return (LineNumberDebugInfo) register(new LineNumberDebugInfo(this, sourceFile, lineNumber));
    }

    public FrameDebugInfo newFrameDebugInfo(final Frame frame) {
        return (FrameDebugInfo) register(new FrameDebugInfo(this, frame));
    }

    public MethodReference newMethodReference(final ResolvedMethod method, final Reference.Kind kind) {
        return (MethodReference) register(new MethodReference(this, method, kind));
    }

    public FieldReference newFieldReference(final ResolvedField field, final Reference.Kind kind) {
        return (FieldReference) register(new FieldReference(this, field, kind));
    }

    public ResolveCallsite newResolveCallsite() {
        return (ResolveCallsite) register(new ResolveCallsite(this));
    }

    public MethodType newMethodType(final Type type) {
        return (MethodType) register(new MethodType(this, type));
    }

    public InvokeDynamicExpression newInvokeDynamicExpression(final Type type) {
        return (InvokeDynamicExpression) register(new InvokeDynamicExpression(this, type));
    }

    public NumericalTest newNumericalTest(final NumericalTest.Operation operation) {
        return (NumericalTest) register(new NumericalTest(this, operation));
    }

    public NullTest newNullTest(final NullTest.Operation operation) {
        return (NullTest) register(new NullTest(this, operation));
    }

    public ReferenceTest newReferenceTest(final ReferenceTest.Operation operation) {
        return (ReferenceTest) register(new ReferenceTest(this, operation));
    }

    public RuntimeClass newRuntimeClass() {
        return (RuntimeClass) register(new RuntimeClass(this));
    }

    public PrimitiveClassReference newPrimitiveClassReference(final Type type) {
        return (PrimitiveClassReference) register(new PrimitiveClassReference(this, type));
    }

    public RuntimeClassOf newRuntimeTypeOf() {
        return (RuntimeClassOf) register(new RuntimeClassOf(this));
    }

    public EnumValuesOf newEnumValuesOf(final Type type) {
        return (EnumValuesOf) register(new EnumValuesOf(this, type));
    }

    public Reinterpret newReinterpret(final Type type) {
        return (Reinterpret) register(new Reinterpret(this, type));
    }

    public BootstrapMethod newBootstrapMethod(final Type methodType, final Type className, final String methodName, final Reference.Kind kind) {
        return (BootstrapMethod) register(new BootstrapMethod(this, methodType, className, methodName, kind));
    }

    public ClassInitialization newClassInitialization(final Type type) {
        return (ClassInitialization) register(new ClassInitialization(this, type));
    }

    public void remapDataFlow(final Node original, final Node newValue) {
        for (final Node n : nodes) {
            n.remapDataFlow(original, newValue);
        }
    }

    void deleteFromControlFlowInternally(final ControlTokenConsumer consumer) {
        if (consumer.hasIncomingBackEdges()) {
            throw new IllegalStateException("Cannot delete node with incoming back edges! Node Type is " + consumer.nodeType + " #" + consumer.owner.nodes.indexOf(consumer));
        }
        if (consumer.controlComingFrom.size() != 1) {
//            throw new IllegalStateException("Can only delete nodes with exactly one incoming edge! Node Type is " + consumer.nodeType + " #" + consumer.owner.nodes.indexOf(consumer));
        }
        if (consumer.controlFlowsTo.size() != 1) {
            throw new IllegalStateException("Can only delete nodes with exactly one outgoing edge! Node Type is " + consumer.nodeType + " #" + consumer.owner.nodes.indexOf(consumer));
        }
        if (consumer.controlFlowsTo.keySet().stream().anyMatch(t -> t.edgeType() == EdgeType.BACK)) {
            throw new IllegalStateException("Can only delete nodes without outgoing back edges! Node Type is " + consumer.nodeType + " #" + consumer.owner.nodes.indexOf(consumer));
        }

        for (final ControlTokenConsumer pred : consumer.controlComingFrom) {
            for (final Map.Entry<Projection, ControlTokenConsumer> entry : new HashSet<>(pred.controlFlowsTo.entrySet())) {
                if (entry.getValue() == consumer) {
                    for (final Map.Entry<Projection, ControlTokenConsumer> to : consumer.controlFlowsTo.entrySet()) {
                        pred.controlFlowsTo.put(entry.getKey(), to.getValue());
                        to.getValue().controlComingFrom.remove(consumer);
                        to.getValue().controlComingFrom.add(pred);
                    }
                }
            }
        }

        consumer.controlComingFrom.clear();
        consumer.controlFlowsTo.clear();
        nodes.remove(consumer);
    }

    public void replaceInControlFlow(final ControlTokenConsumer source, final ControlTokenConsumer target) {
        final Set<ControlTokenConsumer> sources = new HashSet<>(source.controlComingFrom);
        for (final ControlTokenConsumer s : sources) {
            s.replaceInControlFlow(source, target);
        }
        nodes.remove(source);
    }

    public void sanityCheck() {
        for (final Node n : nodes) {
            n.sanityCheck();
        }
    }

    public Map<Node, Node> stampFrom(final Graph source, final Node thisRef, final Node[] arguments) {
        final Map<Node, Node> clones = new HashMap<>();
        for (final Node n : source.nodes()) {
            final Node clone;
            switch (n.nodeType) {
                case Region:
                    final Region r = (Region) n;
                    if (Graph.START_REGION_NAME.equals(r.label)) {
                        clone = newRegion("InlineStartProxy_" + importLabelCounter++);
                    } else {
                        clone = newRegion(r.label + "_" + importLabelCounter++);
                    }
                    break;
                case TryCatch:
                    final TryCatch t = (TryCatch) n;
                    clone = newTryCatch(t.label + "_" + importLabelCounter++);
                    break;
                case Return:
                    clone = newRegion("InlineReturnProxy_" + importLabelCounter++);
                    break;
                case This:
                    clone = thisRef;
                    break;
                case MethodArgument:
                    clone = arguments[((MethodArgument) n).index];
                    break;
                case ReturnValue:
                    clone = newCopy();
                    break;
                default:
                    clone = n.stampInto(this);
                    break;
            }
            clones.put(n, clone);
        }
        for (final Node n : source.nodes()) {
            final Node t = clones.get(n);
            for (final Node inc : n.incomingDataFlows) {
                final Node mapped = clones.get(inc);
                t.addIncomingData(mapped);
            }

            if (n instanceof ControlTokenConsumer) {
                final ControlTokenConsumer s1 = (ControlTokenConsumer) n;
                final ControlTokenConsumer t1 = (ControlTokenConsumer) t;
                for (final Map.Entry<Projection, ControlTokenConsumer> entry : s1.controlFlowsTo.entrySet()) {
                    final ControlTokenConsumer controlFlowTarget = (ControlTokenConsumer) clones.get(entry.getValue());
                    t1.addControlFlowTo(entry.getKey(), controlFlowTarget);
                }
            }
        }
        return clones;
    }
}
