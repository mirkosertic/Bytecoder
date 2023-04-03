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
import java.util.List;
import java.util.Map;

public class Graph {

    public static final String START_REGION_NAME = "Start";

    private final List<Node> nodes;

    private final Map<AbstractInsnNode, InstructionTranslation> translations;

    private final List<Fixup> fixups;

    private final Map<String, Region> labeledRegions;

    private final Logger logger;

    public Graph(final Logger logger) {
        this.nodes = new ArrayList<>();
        this.translations = new HashMap<>();
        this.fixups = new ArrayList<>();
        this.labeledRegions = new HashMap<>();
        this.logger = logger;
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
        return (This) register(new This(type));
    }

    public MethodArgument newMethodArgument(final Type type, final int index) {
        return (MethodArgument) register(new MethodArgument(type, index));
    }

    public NullReference newNullReference() {
        return (NullReference) register(new NullReference());
    }

    public PrimitiveInt newInt(final int value) {
        return (PrimitiveInt) register(new PrimitiveInt(value));
    }

    public PrimitiveShort newShort(final short value) {
        return (PrimitiveShort) register(new PrimitiveShort(value));
    }

    public NewArray newNewArray(final Type arrayType) {
        return (NewArray) register(new NewArray(arrayType));
    }

    public If newIf() {
        return (If) register(new If());
    }

    public USHR newUSHR(final Type type) {
        return (USHR) register(new USHR(type));
    }

    public SHR newSHR(final Type type) {
        return (SHR) register(new SHR(type));
    }

    public SHL newSHL(final Type type) {
        return (SHL) register(new SHL(type));
    }

    public Neg newNEG(final Type type) {
        return (Neg) register(new Neg(type));
    }

    public And newAND(final Type type) {
        return (And) register(new And(type));
    }

    public Or newOR(final Type type) {
        return (Or) register(new Or(type));
    }

    public XOr newXOR(final Type type) {
        return (XOr) register(new XOr(type));
    }

    public ObjectString newObjectString(final StringConstant value) {
        return (ObjectString) register(new ObjectString(value));
    }

    public InstanceMethodInvocation newInstanceMethodInvocation(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (InstanceMethodInvocation) register(new InstanceMethodInvocation(insn, rm));
    }

    public InstanceMethodInvocationExpression newInstanceMethodInvocationExpression(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (InstanceMethodInvocationExpression) register(new InstanceMethodInvocationExpression(insn, rm));
    }

    public VirtualMethodInvocation newVirtualMethodInvocation(final MethodInsnNode insn, final ResolvedMethod resolvedMethod) {
        return (VirtualMethodInvocation) register(new VirtualMethodInvocation(insn, resolvedMethod));
    }

    public InterfaceMethodInvocation newInterfaceMethodInvocation(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (InterfaceMethodInvocation) register(new InterfaceMethodInvocation(insn, rm));
    }

    public InterfaceMethodInvocationExpression newInterfaceMethodInvocationExpression(final MethodInsnNode insn, final ResolvedMethod rm) {
        return (InterfaceMethodInvocationExpression) register(new InterfaceMethodInvocationExpression(insn, rm));
    }

    public StaticMethodInvocation newStaticMethodInvocation(final ResolvedMethod rm) {
        return (StaticMethodInvocation) register(new StaticMethodInvocation(rm));
    }

    public StaticMethodInvocationExpression newStaticMethodInvocationExpression(final ResolvedMethod rm) {
        return (StaticMethodInvocationExpression) register(new StaticMethodInvocationExpression(rm));
    }

    public Return newReturnNothing() {
        return (Return) register(new Return());
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

    public Div newDiv(final Type type) {
        return (Div) register(new Div(type));
    }

    public Mul newMul(final Type type) {
        return (Mul) register(new Mul(type));
    }

    public TypeConversion newTypeConversion(final Type type) {
        return (TypeConversion) register(new TypeConversion(type));
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
        return (PHI) register(new PHI(type));
    }

    public Variable newVariable(final Type type) {
        return (Variable) register(new Variable(type));
    }

    public Copy newCopy() {
        return (Copy) register(new Copy());
    }

    public Cast newCast(final Type type) {
        return (Cast) register(new Cast(type));
    }

    public Nop newNop() {
        return (Nop) register(new Nop());
    }
    public CaughtException newCaughtException(final Type type) {
        return (CaughtException) register(new CaughtException(type));
    }

    public Goto newGoto() {
        return (Goto) register(new Goto());
    }

    public CMP newCMP() {
        return (CMP) register(new CMP());
    }

    public Rem newRem(final Type type) {
        return (Rem) register(new Rem(type));
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

    public MonitorEnter newMonitorEnter() {
        return (MonitorEnter) register(new MonitorEnter());
    }

    public MonitorExit newMonitorExit() {
        return (MonitorExit) register(new MonitorExit());
    }

    public ArrayLength newArrayLength() {
        return (ArrayLength) register(new ArrayLength());
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
        return (TypeReference) register(new TypeReference(type));
    }

    public New newNew(final Type type) {
        return (New) register(new New(type));
    }

    public InstanceOf newInstanceOf() {
        return (InstanceOf) register(new InstanceOf());
    }

    public TableSwitch newTableSwitch(final int min, final int max) {
        return (TableSwitch) register(new TableSwitch(min, max));
    }

    public LookupSwitch newLookupSwitch() {
        return (LookupSwitch) register(new LookupSwitch());
    }

    public ReadInstanceField newInstanceFieldExpression(final Type type, final ResolvedField resolvedField) {
        return (ReadInstanceField) register(new ReadInstanceField(type, resolvedField));
    }

    public ReadClassField newClassFieldExpression(final Type type, final ResolvedField resolvedField) {
        return (ReadClassField) register(new ReadClassField(type, resolvedField));
    }

    public SetInstanceField newSetInstanceField(final ResolvedField resolvedField) {
        return (SetInstanceField) register(new SetInstanceField(resolvedField));
    }

    public SetClassField newSetClassField(final ResolvedField resolvedField) {
        return (SetClassField) register(new SetClassField(resolvedField));
    }

    public void deleteNode(final Node node) {
        nodes.remove(node);
    }

    public PrimitiveFloat newFloat(final float constant) {
        return (PrimitiveFloat) register(new PrimitiveFloat(constant));
    }

    public PrimitiveDouble newDouble(final double constant) {
        return (PrimitiveDouble) register(new PrimitiveDouble(constant));
    }

    public PrimitiveLong newLong(final long constant) {
        return (PrimitiveLong) register(new PrimitiveLong(constant));
    }

    public VirtualMethodInvocationExpression newVirtualMethodInvocationExpression(final MethodInsnNode node, final ResolvedMethod resolvedMethod) {
        return (VirtualMethodInvocationExpression) register(new VirtualMethodInvocationExpression(node, resolvedMethod));
    }

    public LineNumberDebugInfo newLineNumberDebugInfo(final int lineNumber) {
        return (LineNumberDebugInfo) register(new LineNumberDebugInfo(lineNumber));
    }

    public FrameDebugInfo newFrameDebugInfo(final Frame frame) {
        return (FrameDebugInfo) register(new FrameDebugInfo(frame));
    }

    public MethodReference newMethodReference(final ResolvedMethod method, final Reference.Kind kind) {
        return (MethodReference) register(new MethodReference(method, kind));
    }

    public FieldReference newFieldReference(final ResolvedField field, final Reference.Kind kind) {
        return (FieldReference) register(new FieldReference(field, kind));
    }

    public ResolveCallsite newResolveCallsite() {
        return (ResolveCallsite) register(new ResolveCallsite());
    }

    public MethodType newMethodType(final Type type) {
        return (MethodType) register(new MethodType(type));
    }

    public InvokeDynamicExpression newInvokeDynamicExpression(final Type type) {
        return (InvokeDynamicExpression) register(new InvokeDynamicExpression(type));
    }

    public NumericalTest newNumericalTest(final NumericalTest.Operation operation) {
        return (NumericalTest) register(new NumericalTest(operation));
    }

    public NullTest newNullTest(final NullTest.Operation operation) {
        return (NullTest) register(new NullTest(operation));
    }

    public ReferenceTest newReferenceTest(final ReferenceTest.Operation operation) {
        return (ReferenceTest) register(new ReferenceTest(operation));
    }

    public RuntimeClass newRuntimeClass() {
        return (RuntimeClass) register(new RuntimeClass());
    }

    public PrimitiveClassReference newPrimitiveClassReference(final Type type) {
        return (PrimitiveClassReference) register(new PrimitiveClassReference(type));
    }

    public RuntimeClassOf newRuntimeTypeOf() {
        return (RuntimeClassOf) register(new RuntimeClassOf());
    }

    public EnumValuesOf newEnumValuesOf(final Type type) {
        return (EnumValuesOf) register(new EnumValuesOf(type));
    }

    public Reinterpret newReinterpret(final Type type) {
        return (Reinterpret) register(new Reinterpret(type));
    }

    public BootstrapMethod newBootstrapMethod(final Type methodType, final Type className, final String methodName, final Reference.Kind kind) {
        return (BootstrapMethod) register(new BootstrapMethod(methodType, className, methodName, kind));
    }
}
