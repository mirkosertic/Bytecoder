/*
 * Copyright 2018 Mirko Sertic
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

import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.objectweb.asm.Type;

import java.io.PrintWriter;
import java.util.List;

public class OpenCLWriter {

    private final OpenCLInputOutputs inputOutputs;
    private final ResolvedClass kernelClass;

    private final CompileUnit compileUnit;

    private final PrintWriter pw;


    public OpenCLWriter(
            final ResolvedClass kernelClass,
            final PrintWriter writer,
            final CompileUnit compileUnit,
            final OpenCLInputOutputs inputOutputs) {

        this.compileUnit = compileUnit;
        this.pw = writer;
        this.inputOutputs = inputOutputs;
        this.kernelClass = kernelClass;
    }

    private String toType(final Type type) {
        /*if (aType.isArray()) {
            final TypeRef.ArrayTypeRef theArray = (TypeRef.ArrayTypeRef) aType;
            return toType(TypeRef.toType(theArray.arrayType().getType()));
        }
        if (aType instanceof TypeRef.ObjectTypeRef) {
            final TypeRef.ObjectTypeRef theObject = (TypeRef.ObjectTypeRef) aType;
            return toStructName(theObject.objectType());
        }*/
        switch (type.getSort()) {
            case Type.ARRAY:
                return toType(type.getElementType());
            case Type.INT:
                return "int";
            case Type.FLOAT:
                return "float";
            case Type.LONG:
                return "long";
            case Type.DOUBLE:
                return "double";
            case Type.SHORT:
                return "short";
            case Type.BYTE:
                return "byte";
            case Type.OBJECT:
                return "void*";
            default:
                throw new IllegalArgumentException("Not supported : " + type);
        }
    }

    private void printInputOutputArgs(final List<OpenCLInputOutputs.KernelArgument> arguments) {
        for (int i = 0; i<arguments.size(); i++) {
            if (i>0) {
                pw.print(", ");
            }
            final OpenCLInputOutputs.KernelArgument theArgument = arguments.get(i);
            final Type theTypeRef = theArgument.getField().type;
            if (theTypeRef.getSort() == Type.OBJECT || theTypeRef.getSort() == Type.ARRAY) {
                pw.print("__global ");
            }
            switch (theArgument.getType()) {
                case INPUT:
                    pw.print("const ");
                    pw.print(toType(theTypeRef));
                    if (theArgument.getField().type.getSort() == Type.ARRAY) {
                        pw.print("*");
                    }
                    pw.print(" ");
                    pw.print(theArgument.getField().name);
                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    pw.print(toType(theTypeRef));
                    if (theArgument.getField().type.getSort() == Type.ARRAY) {
                        pw.print("*");
                    }
                    pw.print(" ");
                    pw.print(theArgument.getField().name);
                    break;
            }
        }
    }

/*    private void printProgramVariablesDeclaration(final ResolvedMethod resolvedMethod) {
        final List<Variable> variable = resolvedMethod.methodBody.;
        for (final Register r : theRegisters) {
            final TypeRef theVarType = r.getType();
            if (theVarType.isArray()) {
                print("__global ");
                print(toType(theVarType));
                print("* ");
                print(registerName(r));
                println(";");
            } else {
                print(toType(theVarType));
                print(" ");
                print(registerName(r));
                println(";");
            }
        }
    }*/

    public void writeKernel(final ResolvedMethod method) {

        pw.print("__kernel void BytecoderKernel(");

        printInputOutputArgs(inputOutputs.arguments());

        pw.println(") {");

//        printProgramVariablesDeclaration(method);

        pw.println("}");
    }

    public void writeInline(final ResolvedMethod method) {

        pw.print("__inline ");
        pw.print(toType(method.methodType.getReturnType()));
        pw.print(" ");
        pw.print(method.methodNode.name);
        pw.print("(");

        printInputOutputArgs(inputOutputs.arguments());

        final boolean theFirst = inputOutputs.arguments().isEmpty();

        for (int i = 0; i < method.methodType.getArgumentTypes().length; i++)  {
            if (i > 0) {
                pw.print(", ");
            }
            pw.print(toType(method.methodType.getArgumentTypes()[i]));
            pw.print(" arg");
            pw.print(i);

        }

        pw.println(") {");

//        printProgramVariablesDeclaration(resolvedMethod);

        pw.println("}");
    }
}
