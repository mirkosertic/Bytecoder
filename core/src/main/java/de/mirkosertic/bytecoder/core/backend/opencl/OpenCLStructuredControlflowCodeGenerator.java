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
package de.mirkosertic.bytecoder.core.backend.opencl;

import de.mirkosertic.bytecoder.core.backend.sequencer.Sequencer;
import de.mirkosertic.bytecoder.core.backend.sequencer.StructuredControlflowCodeGenerator;
import de.mirkosertic.bytecoder.core.ir.AbstractVar;
import de.mirkosertic.bytecoder.core.ir.Add;
import de.mirkosertic.bytecoder.core.ir.And;
import de.mirkosertic.bytecoder.core.ir.AnnotationUtils;
import de.mirkosertic.bytecoder.core.ir.ArrayLength;
import de.mirkosertic.bytecoder.core.ir.ArrayLoad;
import de.mirkosertic.bytecoder.core.ir.ArrayStore;
import de.mirkosertic.bytecoder.core.ir.CMP;
import de.mirkosertic.bytecoder.core.ir.Cast;
import de.mirkosertic.bytecoder.core.ir.ClassInitialization;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Div;
import de.mirkosertic.bytecoder.core.ir.FrameDebugInfo;
import de.mirkosertic.bytecoder.core.ir.Goto;
import de.mirkosertic.bytecoder.core.ir.If;
import de.mirkosertic.bytecoder.core.ir.LineNumberDebugInfo;
import de.mirkosertic.bytecoder.core.ir.LookupSwitch;
import de.mirkosertic.bytecoder.core.ir.MethodArgument;
import de.mirkosertic.bytecoder.core.ir.MethodInvocation;
import de.mirkosertic.bytecoder.core.ir.MethodInvocationExpression;
import de.mirkosertic.bytecoder.core.ir.MonitorEnter;
import de.mirkosertic.bytecoder.core.ir.MonitorExit;
import de.mirkosertic.bytecoder.core.ir.Mul;
import de.mirkosertic.bytecoder.core.ir.Neg;
import de.mirkosertic.bytecoder.core.ir.New;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.NullReference;
import de.mirkosertic.bytecoder.core.ir.NullTest;
import de.mirkosertic.bytecoder.core.ir.NumericalTest;
import de.mirkosertic.bytecoder.core.ir.Or;
import de.mirkosertic.bytecoder.core.ir.PHI;
import de.mirkosertic.bytecoder.core.ir.PrimitiveDouble;
import de.mirkosertic.bytecoder.core.ir.PrimitiveFloat;
import de.mirkosertic.bytecoder.core.ir.PrimitiveInt;
import de.mirkosertic.bytecoder.core.ir.PrimitiveLong;
import de.mirkosertic.bytecoder.core.ir.PrimitiveShort;
import de.mirkosertic.bytecoder.core.ir.ReadClassField;
import de.mirkosertic.bytecoder.core.ir.ReadInstanceField;
import de.mirkosertic.bytecoder.core.ir.ReferenceTest;
import de.mirkosertic.bytecoder.core.ir.Rem;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.ReturnValue;
import de.mirkosertic.bytecoder.core.ir.SHL;
import de.mirkosertic.bytecoder.core.ir.SHR;
import de.mirkosertic.bytecoder.core.ir.SetClassField;
import de.mirkosertic.bytecoder.core.ir.SetInstanceField;
import de.mirkosertic.bytecoder.core.ir.Sub;
import de.mirkosertic.bytecoder.core.ir.TableSwitch;
import de.mirkosertic.bytecoder.core.ir.This;
import de.mirkosertic.bytecoder.core.ir.TypeConversion;
import de.mirkosertic.bytecoder.core.ir.USHR;
import de.mirkosertic.bytecoder.core.ir.Unwind;
import de.mirkosertic.bytecoder.core.ir.XOr;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenCLStructuredControlflowCodeGenerator implements StructuredControlflowCodeGenerator {

    int level = 4;

    private final Map<AbstractVar, String> variableToName;

    private final PrintWriter pw;

    private final ResolvedClass cl;

    private final CompileUnit compileUnit;

    private final OpenCLInputOutputs inputOutputs;

    public OpenCLStructuredControlflowCodeGenerator(final CompileUnit compileUnit, final ResolvedClass cl, final PrintWriter pw, final OpenCLInputOutputs inputOutputs) {
        this.compileUnit = compileUnit;
        this.cl = cl;
        this.pw = pw;
        this.variableToName = new HashMap<>();
        this.inputOutputs = inputOutputs;
    }

    @Override
    public void registerVariables(final List<AbstractVar> variables) {
        for (int i = 0; i < variables.size(); i++) {
            final AbstractVar v = variables.get(i);
            final String varName;
            if (v instanceof PHI) {
                varName = "phi" + i;
            } else {
                varName = "var" + i;
            }
            variableToName.put(variables.get(i), varName);

            writeIndent();
            switch (v.type.getSort()) {
                case Type.ARRAY: {
                    pw.print("__global ");
                    pw.print(OpenCLHelpers.toType(v.type, compileUnit));
                    pw.print(" ");
                    pw.print(varName);
                    pw.println(";");
                    break;
                }
                default: {
                    pw.print(OpenCLHelpers.toType(v.type, compileUnit));
                    pw.print(" ");
                    pw.print(varName);
                    pw.println(";");
                    break;
                }
            }
        }
    }

    private void writeIndent() {
        for (int i = 0; i < level; i++) {
            pw.print(" ");
        }
    }

    @Override
    public void write(final LineNumberDebugInfo node) {
        writeIndent();
        pw.print("// line number ");
        pw.println(node.lineNumber);
    }

    @Override
    public void write(final Goto node) {
        writeIndent();
        pw.println("// Here was a goto statement");
    }

    @Override
    public void write(final FrameDebugInfo node) {
    }

    @Override
    public void write(final MonitorEnter node) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void write(final MonitorExit node) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void write(final Unwind node) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void write(final MethodInvocation invocation) {
        switch (invocation.invocationType) {
            case DIRECT: {
                writeDirect(invocation);
                break;
            }
            case STATIC: {
                writeStatic(invocation);
                break;
            }
            case INTERFACE: {
                writeInterface(invocation);
                break;
            }
            case VIRTUAL: {
                writeVirtual(invocation);
                break;
            }
        }
    }

    private void writeDirect(final MethodInvocation node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);
        if (!invocationTarget.getClassName().equals(cl.type.getClassName())) {
            throw new IllegalArgumentException("Not supported by OpenCL! Target = " + invocationTarget);
        }

        pw.print(OpenCLHelpers.generateMethodName(node.insnNode.name, node.method.methodType));
        pw.print("(");

        writeDelegateInputOutputs();

        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1 || !inputOutputs.arguments().isEmpty()) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpression(final MethodInvocationExpression node) {
        switch (node.invocationType) {
            case STATIC: {
                writeExpressionStaticInvocation(node);
                break;
            }
            case DIRECT: {
                writeExpressionDirectInvocation(node);
                break;
            }
            case VIRTUAL: {
                writeExpressionVirtualInvocation(node);
                break;
            }
            default: {
                throw new IllegalArgumentException("Not implemented : " + node.invocationType);
            }
        }
    }

    private void writeExpressionDirectInvocation(final MethodInvocationExpression node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);
        if (!invocationTarget.getClassName().equals(cl.type.getClassName())) {
            throw new IllegalArgumentException("Not supported by OpenCL! Target = " + invocationTarget);
        }

        pw.print(OpenCLHelpers.generateMethodName(node.insnNode.name, node.method.methodType));
        pw.print("(");

        writeDelegateInputOutputs();

        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1 || !inputOutputs.arguments().isEmpty()) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print(")");
    }

    private void writeExpression(final ReadInstanceField node) {

        if (node.resolvedField.owner == cl) {
            pw.print(node.resolvedField.name);
        } else {
            pw.print("(");
            writeExpression(node.incomingDataFlows[0]);
            pw.print(".");
            pw.print(OpenCLHelpers.generateFieldName(node.resolvedField.name));
            pw.print(")");
        }
    }

    @Override
    public void write(final ClassInitialization node) {
    }

    private void writeExpression(final ReadClassField node) {

        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(".");
        pw.print(OpenCLHelpers.generateFieldName(node.resolvedField.name));
        pw.print(")");
    }

    private void writeExpression(final ArrayLoad node) {

        writeExpression(node.incomingDataFlows[0]);
        pw.print("[");
        writeExpression(node.incomingDataFlows[1]);
        pw.print("]");
    }

    private void writeExpression(final MethodArgument node) {
        pw.print("arg");
        pw.print(node.index);
    }

    private void writeExpression(final NullReference node) {
        pw.print("null");
    }

    private void writeExpression(final ReferenceTest node) {
        writeExpression(node.incomingDataFlows[0]);
        switch (node.operation) {
            case EQ:
                pw.print(" == ");
                break;
            case NE:
                pw.print(" != ");
                break;
            default:
                throw new IllegalStateException("Not implemented operation : " + node.operation);
        }
        writeExpression(node.incomingDataFlows[1]);
    }

    private void writeExpression(final NullTest node) {
        writeExpression(node.incomingDataFlows[0]);
        switch (node.operation) {
            case NOTNULL:
                pw.print(" != null");
                break;
            case NULL:
                pw.print(" == null");
                break;
            default:
                throw new IllegalStateException("Not implemented operation : " + node.operation);
        }
    }

    private void writeExpression(final And node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" & ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final TypeConversion node) {
        if (node.type.getSort() == Type.INT || node.type.getSort() == Type.LONG) {
            pw.print("(");
            writeExpression(node.incomingDataFlows[0]);
            pw.print(" | 0");
            pw.print(")");
        } else {
            writeExpression(node.incomingDataFlows[0]);
        }
    }

    private void writeExpression(final ArrayLength node) {
        writeExpression(node.incomingDataFlows[0]);
        pw.print(".data.length");
    }

    private void writeExpression(final SHR node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" >> ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final SHL node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" << ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Or node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" | ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Neg node) {
        pw.print("(0 - ");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(")");
    }

    private void writeExpression(final Mul node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" * ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final CMP node) {
        final Node theVariable1 = node.incomingDataFlows[0];
        final Node theVariable2 = node.incomingDataFlows[1];
        pw.print("(");
        writeExpression(theVariable1);
        pw.print(" > ");
        writeExpression(theVariable2);
        pw.print(" ? 1 ");
        pw.print(" : (");
        writeExpression(theVariable1);
        pw.print(" < ");
        writeExpression(theVariable2);
        pw.print(" ? -1 : 0))");
    }

    private void writeExpression(final Cast node) {
        writeExpression(node.incomingDataFlows[0]);
    }

    private void writeExpression(final PrimitiveLong node) {
        pw.print(node.value);
    }

    private void writeExpression(final PrimitiveDouble node) {
        pw.print(node.value);
    }

    private void writeExpression(final PrimitiveFloat node) {
        pw.print(node.value);
    }

    private void writeExpression(final XOr node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" ^ ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final USHR node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" >>> ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Rem node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" % ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final NumericalTest node) {
        writeExpression(node.incomingDataFlows[0]);

        switch (node.operation) {
            case EQ:
                pw.print(" == ");
                break;
            case GE:
                pw.print(" >= ");
                break;
            case GT:
                pw.print(" > ");
                break;
            case LE:
                pw.print(" <= ");
                break;
            case LT:
                pw.print(" < ");
                break;
            case NE:
                pw.print(" != ");
                break;
            default:
                throw new IllegalStateException("Not implemented : " + node.operation);
        }

        writeExpression(node.incomingDataFlows[1]);
    }

    @Override
    public void write(final SetInstanceField node) {

        writeIndent();
        if (node.field.owner != cl) {
            writeExpression(node.outgoingFlows[0]);
            pw.print(".");
        }
        pw.print(OpenCLHelpers.generateFieldName(node.field.name));
        pw.print(" = ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(";");
    }

    @Override
    public void write(final SetClassField node) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void write(final ArrayStore node) {
        writeIndent();
        writeExpression(node.incomingDataFlows[0]);
        pw.print("[");
        writeExpression(node.incomingDataFlows[1]);
        pw.print("] = ");
        writeExpression(node.incomingDataFlows[2]);
        pw.println(";");
    }

    private void writeDelegateInputOutputs() {
        for (int i = 0; i < inputOutputs.arguments().size(); i++) {
            if (i > 0) {
                pw.print(", ");
            }
            final OpenCLInputOutputs.KernelArgument theArgument = inputOutputs.arguments().get(i);
            pw.print(theArgument.getField().name);
        }
    }

    private void writeVirtual(final MethodInvocation node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);
        if (!invocationTarget.getClassName().equals(cl.type.getClassName())) {
            throw new IllegalArgumentException("Not supported by OpenCL! Target = " + invocationTarget);
        }

        writeIndent();

        pw.print(OpenCLHelpers.generateMethodName(node.insnNode.name, node.method.methodType));
        pw.print("(");

        writeDelegateInputOutputs();

        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1 || !inputOutputs.arguments().isEmpty()) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpressionVirtualInvocation(final MethodInvocationExpression node) {

        final Type invocationTarget = Type.getObjectType(node.insnNode.owner);
        if (!invocationTarget.getClassName().equals(cl.type.getClassName())) {
            throw new IllegalArgumentException("Not supported by OpenCL! Target = " + invocationTarget);
        }

        pw.print(OpenCLHelpers.generateMethodName(node.insnNode.name, Type.getMethodType(node.insnNode.desc)));
        pw.print("(");

        writeDelegateInputOutputs();

        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1 || !inputOutputs.arguments().isEmpty()) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print(")");
    }

    private void writeInterface(final MethodInvocation node) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    private void writeStatic(final MethodInvocation node) {

        writeIndent();

        if (!AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/opencl/OpenCLFunction;", node.method.methodNode.visibleAnnotations)) {
            throw new IllegalArgumentException("Static invocation target must have @OpenCLFunction annotation!");
        }
        final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/opencl/OpenCLFunction;", node.method.methodNode.visibleAnnotations);

        if (Boolean.TRUE.equals(values.get("literal"))) {
            pw.print("(");
            pw.print(values.get("value"));
            pw.print(")");
        } else {
            pw.print(values.get("value"));
        }
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.println(");");
    }

    private void writeExpressionStaticInvocation(final MethodInvocationExpression node) {

        if (!AnnotationUtils.hasAnnotation("Lde/mirkosertic/bytecoder/api/opencl/OpenCLFunction;", node.method.methodNode.visibleAnnotations)) {
            throw new IllegalArgumentException("Static invocation target must have @OpenCLFunction annotation!");
        }
        final Map<String, Object> values = AnnotationUtils.parseAnnotation("Lde/mirkosertic/bytecoder/api/opencl/OpenCLFunction;", node.method.methodNode.visibleAnnotations);

        if (Boolean.TRUE.equals(values.get("literal"))) {
            pw.print("(");
            pw.print(values.get("value"));
            pw.print(")");
        } else {
            pw.print(values.get("value"));
        }
        pw.print("(");
        for (int i = 1; i < node.incomingDataFlows.length; i++) {
            if (i > 1) {
                pw.print(",");
            }
            writeExpression(node.incomingDataFlows[i]);
        }
        pw.print(")");
    }

    @Override
    public void write(final Copy node) {
        writeIndent();
        final Node target = node.outgoingFlows[0];
        final Node value = node.incomingDataFlows[0];
        if (target instanceof AbstractVar) {
            pw.print(variableToName.get(target));
        } else if (target instanceof MethodArgument) {
            writeExpression(target);
        } else {
            throw new IllegalStateException("Invalid copy target : " + target);
        }
        pw.print(" = ");
        writeExpression(value);
        pw.println(";");
    }

    private void writeExpression(final Node node) {
        if (node instanceof AbstractVar) {
            writeExpression((AbstractVar) node);
        } else if (node instanceof PrimitiveShort) {
            writeExpression((PrimitiveShort) node);
        } else if (node instanceof Sub) {
            writeExpression((Sub) node);
        } else if (node instanceof Add) {
            writeExpression((Add) node);
        } else if (node instanceof Div) {
            writeExpression((Div) node);
        } else if (node instanceof PrimitiveInt) {
            writeExpression((PrimitiveInt) node);
        } else if (node instanceof New) {
            writeExpression((New) node);
        } else if (node instanceof This) {
            writeExpression((This) node);
        } else if (node instanceof MethodInvocationExpression) {
            writeExpression((MethodInvocationExpression) node);
        } else if (node instanceof ReadInstanceField) {
            writeExpression((ReadInstanceField) node);
        } else if (node instanceof ReadClassField) {
            writeExpression((ReadClassField) node);
        } else if (node instanceof ArrayLoad) {
            writeExpression((ArrayLoad) node);
        } else if (node instanceof MethodArgument) {
            writeExpression((MethodArgument) node);
        } else if (node instanceof NumericalTest) {
            writeExpression((NumericalTest) node);
        } else if (node instanceof NullReference) {
            writeExpression((NullReference) node);
        } else if (node instanceof ReferenceTest) {
            writeExpression((ReferenceTest) node);
        } else if (node instanceof NullTest) {
            writeExpression((NullTest) node);
        } else if (node instanceof And) {
            writeExpression((And) node);
        } else if (node instanceof TypeConversion) {
            writeExpression((TypeConversion) node);
        } else if (node instanceof ArrayLength) {
            writeExpression((ArrayLength) node);
        } else if (node instanceof SHR) {
            writeExpression((SHR) node);
        } else if (node instanceof SHL) {
            writeExpression((SHL) node);
        } else if (node instanceof Or) {
            writeExpression((Or) node);
        } else if (node instanceof Neg) {
            writeExpression((Neg) node);
        } else if (node instanceof Mul) {
            writeExpression((Mul) node);
        } else if (node instanceof CMP) {
            writeExpression((CMP) node);
        } else if (node instanceof PrimitiveLong) {
            writeExpression((PrimitiveLong) node);
        } else if (node instanceof PrimitiveDouble) {
            writeExpression((PrimitiveDouble) node);
        } else if (node instanceof PrimitiveFloat) {
            writeExpression((PrimitiveFloat) node);
        } else if (node instanceof XOr) {
            writeExpression((XOr) node);
        } else if (node instanceof USHR) {
            writeExpression((USHR) node);
        } else if (node instanceof Rem) {
            writeExpression((Rem) node);
        } else if (node instanceof Cast) {
            writeExpression((Cast) node);
        } else {
            throw new IllegalArgumentException("Not implemented : " + node);
        }
    }

    private void writeExpression(final This node) {
        pw.print("0");
    }

    private void writeExpression(final New node) {
        pw.print("new ");
        writeExpression(node.incomingDataFlows[0]);
        pw.print("()");
    }

    private void writeExpression(final Sub node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" - ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Add node) {
        pw.print("(");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(" + ");
        writeExpression(node.incomingDataFlows[1]);
        pw.print(")");
    }

    private void writeExpression(final Div node) {
        if (node.type == Type.DOUBLE_TYPE || node.type == Type.FLOAT_TYPE) {
            pw.print("(");
            writeExpression(node.incomingDataFlows[0]);
            pw.print(" / ");
            writeExpression(node.incomingDataFlows[1]);
            pw.print(")");
        } else {
            pw.print("(");
            pw.print(OpenCLHelpers.toType(node.type, compileUnit));
            pw.print(")(");
            writeExpression(node.incomingDataFlows[0]);
            pw.print(" / ");
            writeExpression(node.incomingDataFlows[1]);
            pw.print(")");
        }
    }

    private void writeExpression(final AbstractVar node) {
        pw.print(variableToName.get(node));
    }

    private void writeExpression(final PrimitiveShort node) {
        pw.print(node.value);
    }

    private void writeExpression(final PrimitiveInt node) {
        pw.print(node.value);
    }

    @Override
    public void startIfWithTrueBlock(final If node) {
        writeIndent();
        pw.print("if (");

        writeExpression(node.incomingDataFlows[0]);

        pw.println(") {");
        level++;
    }

    @Override
    public void startIfElseBlock(final If node) {
        level--;
        writeIndent();
        pw.println("} else {");
        level++;
    }

    @Override
    public void finishIfBlock() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void startBlock(final Sequencer.Block block) {
        writeIndent();
        pw.print(block.label);
        pw.print(": ");
        if (block.type == Sequencer.Block.Type.LOOP) {
            pw.print("while(true) ");
        }
        pw.println("{");
        level++;
    }

    @Override
    public void finishBlock(final Sequencer.Block block, final boolean stackEmpty) {
        level--;
        writeIndent();
        pw.println("}");
        writeIndent();
        if (!stackEmpty) {
            pw.print(block.label);
            pw.println("_exit:");
        }
    }

    @Override
    public void startTryCatch(final String label) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void startCatchBlock() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void startCatchHandler(final Type type) {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void finishCatchHandler() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void writeRethrowException() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void finishTryCatch() {
        throw new IllegalArgumentException("Not supported by OpenCL!");
    }

    @Override
    public void write(final Return node) {
        writeIndent();
        pw.println("return;");
    }

    @Override
    public void write(final ReturnValue node) {
        writeIndent();
        pw.print("return ");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(";");
    }

    @Override
    public void writeBreakTo(final String label) {
        writeIndent();
        pw.print("goto ");
        pw.print(label);
        pw.println("_exit;");
    }

    @Override
    public void writeContinueTo(final String label) {
        writeIndent();
        pw.print("goto ");
        pw.print(label);
        pw.println(";");
    }

    @Override
    public void startTableSwitch(final TableSwitch node) {
        writeIndent();
        pw.print("if ((");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(") >= ");
        pw.print(node.min);
        pw.print(" && (");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(") <= ");
        pw.print(node.max);
        pw.print(") switch ((");
        writeExpression(node.incomingDataFlows[0]);
        pw.print(") - ");
        pw.print(node.min);
        pw.println(") {");
        level++;
    }

    @Override
    public void startTableSwitchDefaultBlock() {
        level--;
        writeIndent();
        pw.println("} else {");
        level++;
    }

    @Override
    public void finishTableSwitchDefaultBlock() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void startLookupSwitch(final LookupSwitch node) {
        writeIndent();
        pw.print("switch (");
        writeExpression(node.incomingDataFlows[0]);
        pw.println(") {");
        level++;
    }

    @Override
    public void writeSwitchCase(final int index) {
        writeIndent();
        pw.print("case ");
        pw.print(index);
        pw.println(": {");
        level++;
    }

    @Override
    public void writeSwitchDefaultCase() {
        writeIndent();
        pw.println("default: {");
        level++;
    }

    @Override
    public void finishSwitchDefault() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void finishSwitchCase() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void finishLookupSwitch() {
        level--;
        writeIndent();
        pw.println("}");
    }

    @Override
    public void finishTableSwitch() {
    }
}
