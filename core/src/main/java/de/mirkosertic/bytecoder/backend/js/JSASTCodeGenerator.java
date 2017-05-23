/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.js;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.mirkosertic.bytecoder.ast.ASTBlock;
import de.mirkosertic.bytecoder.ast.ASTComputationADD;
import de.mirkosertic.bytecoder.ast.ASTComputationDIV;
import de.mirkosertic.bytecoder.ast.ASTComputationMUL;
import de.mirkosertic.bytecoder.ast.ASTComputationSUB;
import de.mirkosertic.bytecoder.ast.ASTGetStatic;
import de.mirkosertic.bytecoder.ast.ASTInputOfBlock;
import de.mirkosertic.bytecoder.ast.ASTInvokeSpecial;
import de.mirkosertic.bytecoder.ast.ASTInvokeStatic;
import de.mirkosertic.bytecoder.ast.ASTInvokeVirtual;
import de.mirkosertic.bytecoder.ast.ASTLocalVariable;
import de.mirkosertic.bytecoder.ast.ASTNewObject;
import de.mirkosertic.bytecoder.ast.ASTNull;
import de.mirkosertic.bytecoder.ast.ASTObjectReturn;
import de.mirkosertic.bytecoder.ast.ASTPutStatic;
import de.mirkosertic.bytecoder.ast.ASTSetLocalVariable;
import de.mirkosertic.bytecoder.ast.ASTThrow;
import de.mirkosertic.bytecoder.ast.ASTValue;
import de.mirkosertic.bytecoder.ast.ASTValueReference;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class JSASTCodeGenerator {

    public String generateFor(ASTBlock aBlock) {
        StringWriter theResult = new StringWriter();
        final PrintWriter theWriter = new PrintWriter(theResult);

        for (ASTValue theValue : aBlock.getValues()) {
            visit(theValue, theWriter);
        }

        theWriter.flush();;
        return theResult.toString();
    }

    private String toClassName(BytecodeClassinfoConstant aTypeRef) {
        return aTypeRef.getConstant().stringValue().replace("/","_");
    }

    private String typeRefToString(BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            BytecodePrimitiveTypeRef thePrimitive = (BytecodePrimitiveTypeRef) aTypeRef;
            return thePrimitive.toString();
        }
        if (aTypeRef.isArray()) {
            BytecodeArrayTypeRef theRef = (BytecodeArrayTypeRef) aTypeRef;
            return "A" + theRef.getDepth() + typeRefToString(theRef.getType());
        }
        BytecodeObjectTypeRef theObjectRef = (BytecodeObjectTypeRef) aTypeRef;
        return theObjectRef.name().replace(".", "");
    }

    private String toMethodName(String aMethodName, BytecodeMethodSignature aSignature) {
        String theName = aSignature.getReturnType().name().replace(".","_");
        theName += aMethodName.replace("<", "").replace(">", "");

        for (BytecodeTypeRef theTypeRef : aSignature.getArguments()) {
            theName += typeRefToString(theTypeRef);
        }
        return theName;
    }

    private void visit(ASTValue aValue, PrintWriter aWriter) {
        if (aValue instanceof ASTObjectReturn) {
            visit((ASTObjectReturn) aValue, aWriter);
        } else if (aValue instanceof ASTSetLocalVariable) {
            visit((ASTSetLocalVariable) aValue, aWriter);
        } else if (aValue instanceof ASTComputationADD) {
            visit((ASTComputationADD) aValue, aWriter);
        } else if (aValue instanceof ASTComputationSUB) {
            visit((ASTComputationSUB) aValue, aWriter);
        } else if (aValue instanceof ASTComputationMUL) {
            visit((ASTComputationMUL) aValue, aWriter);
        } else if (aValue instanceof ASTComputationDIV) {
            visit((ASTComputationDIV) aValue, aWriter);
        } else if (aValue instanceof ASTLocalVariable) {
            visit((ASTLocalVariable) aValue, aWriter);
        } else if (aValue instanceof ASTGetStatic) {
            visit((ASTGetStatic) aValue, aWriter);
        } else if (aValue instanceof ASTPutStatic) {
            visit((ASTPutStatic) aValue, aWriter);
        } else if (aValue instanceof ASTNull) {
            visit((ASTNull) aValue, aWriter);
        } else if (aValue instanceof ASTInputOfBlock) {
            visit((ASTInputOfBlock) aValue, aWriter);
        } else if (aValue instanceof ASTThrow) {
            visit((ASTThrow) aValue, aWriter);
        } else if (aValue instanceof ASTInvokeStatic) {
            visit((ASTInvokeStatic) aValue, aWriter);
        } else if (aValue instanceof ASTInvokeSpecial) {
            visit((ASTInvokeSpecial) aValue, aWriter);
        } else if (aValue instanceof ASTValueReference) {
            visit((ASTValueReference) aValue, aWriter);
        } else if (aValue instanceof ASTNewObject) {
            visit((ASTNewObject) aValue, aWriter);
        } else if (aValue instanceof ASTInvokeVirtual) {
            visit((ASTInvokeVirtual) aValue, aWriter);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue);
        }
    }

    private void visit(ASTNewObject aValue, PrintWriter aWriter) {
        aWriter.print(toClassName(aValue.getType()));
        aWriter.print(".emptyInstance()");
    }

    private void visit(ASTValueReference aValue, PrintWriter aWriter) {
        visit(aValue.getReference(), aWriter);
    }

    private void visit(ASTInvokeSpecial aValue, PrintWriter aWriter) {
        aWriter.print(toClassName(aValue.getClassname()));
        aWriter.print(".");
        aWriter.print(toMethodName(aValue.getMethodName(), aValue.getSignature()));
        aWriter.print("(");
        visit(aValue.getReference(), aWriter);
        BytecodeMethodSignature theSignature = aValue.getSignature();
        for (int i=0;i<theSignature.getArguments().length;i++) {
            aWriter.print(",");
            visit(aValue.getArguments().get(i), aWriter);
        }
        aWriter.println(");");
    }

    private void visit(ASTInvokeVirtual aValue, PrintWriter aWriter) {
        aWriter.print(toClassName(aValue.getClassname()));
        aWriter.print(".");
        aWriter.print(toMethodName(aValue.getMethodName(), aValue.getSignature()));
        aWriter.print("(");
        visit(aValue.getReference(), aWriter);
        BytecodeMethodSignature theSignature = aValue.getSignature();
        for (int i=0;i<theSignature.getArguments().length;i++) {
            aWriter.print(",");
            visit(aValue.getArguments().get(i), aWriter);
        }
        aWriter.println(");");
    }

    private void visit(ASTInputOfBlock aValue, PrintWriter aWriter) {
        // TODO: How to handle this?
        System.out.println("lala");
    }

    private void visit(ASTThrow aValue, PrintWriter aWriter) {
        aWriter.print("throw ");

        visit(aValue.getReference(), aWriter);

        aWriter.println(";");
    }

    private void visit(ASTInvokeStatic aValue, PrintWriter aWriter) {

        aWriter.print(toClassName(aValue.getClassname()));
        aWriter.print(".");
        aWriter.print(toMethodName(aValue.getMethodName().stringValue(), aValue.getSignature()));
        aWriter.print("(");
        for (int i=0;i<aValue.getArguments().size();i++) {
            if (i>0) {
                aWriter.print(",");
            }
            visit(aValue.getArguments().get(i), aWriter);
        }
        aWriter.print(")");
    }

    private void visit(ASTGetStatic aValue, PrintWriter aWriter) {
        // TODO: Check static init here
        aWriter.print(toClassName(aValue.getClassName()));
        aWriter.print(".staticFields.");
        aWriter.print(aValue.getFieldName().stringValue());
    }

    private void visit(ASTPutStatic aValue, PrintWriter aWriter) {
        // TODO: Check static init here
        aWriter.print(toClassName(aValue.getClassName()));
        aWriter.print(".staticFields.");
        aWriter.print(aValue.getFieldName().stringValue());
        aWriter.print(" = ");

        visit(aValue.getArgument(), aWriter);

        aWriter.println(";");
    }

    private void visit(ASTLocalVariable aValue, PrintWriter aWriter) {
        aWriter.print("local" + aValue.getVariableIndex());
    }

    private void visit(ASTNull aValue, PrintWriter aWriter) {
        aWriter.print("null");
    }

    private void visit(ASTSetLocalVariable aValue, PrintWriter aWriter) {
        aWriter.print("frame.local" + aValue.getVariableIndex());
        aWriter.print(" = ");
        visit(aValue.getValue(), aWriter);
        aWriter.println(";");
    }

    private void visit(ASTComputationADD aValue, PrintWriter aWriter) {
        aWriter.print("(");
        visit(aValue.getValue2(), aWriter);
        aWriter.print(" + ");
        visit(aValue.getValue1(), aWriter);
        aWriter.print(")");
    }

    private void visit(ASTComputationSUB aValue, PrintWriter aWriter) {
        aWriter.print("(");
        visit(aValue.getValue2(), aWriter);
        aWriter.print(" - ");
        visit(aValue.getValue1(), aWriter);
        aWriter.print(")");
    }

    private void visit(ASTComputationMUL aValue, PrintWriter aWriter) {
        aWriter.print("(");
        visit(aValue.getValue2(), aWriter);
        aWriter.print(" * ");
        visit(aValue.getValue1(), aWriter);
        aWriter.print(")");
    }

    private void visit(ASTComputationDIV aValue, PrintWriter aWriter) {
        aWriter.print("(");
        visit(aValue.getValue2(), aWriter);
        aWriter.print(" / ");
        visit(aValue.getValue1(), aWriter);
        aWriter.print(")");
    }

    private void visit(ASTObjectReturn aValue, PrintWriter aWriter) {
        aWriter.print("return ");
        visit(aValue.getValue(), aWriter);
        aWriter.println(";");
    }
}