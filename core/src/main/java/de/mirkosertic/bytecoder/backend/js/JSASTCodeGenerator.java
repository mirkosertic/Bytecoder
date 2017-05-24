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

import java.io.UnsupportedEncodingException;

import de.mirkosertic.bytecoder.ast.ASTArrayLength;
import de.mirkosertic.bytecoder.ast.ASTArrayValue;
import de.mirkosertic.bytecoder.ast.ASTBlock;
import de.mirkosertic.bytecoder.ast.ASTByteValue;
import de.mirkosertic.bytecoder.ast.ASTCheckCast;
import de.mirkosertic.bytecoder.ast.ASTConstant;
import de.mirkosertic.bytecoder.ast.ASTDouble2Generic;
import de.mirkosertic.bytecoder.ast.ASTFloat2Generic;
import de.mirkosertic.bytecoder.ast.ASTFloatCompare;
import de.mirkosertic.bytecoder.ast.ASTFloatValue;
import de.mirkosertic.bytecoder.ast.ASTGetField;
import de.mirkosertic.bytecoder.ast.ASTGetStatic;
import de.mirkosertic.bytecoder.ast.ASTGoto;
import de.mirkosertic.bytecoder.ast.ASTIF;
import de.mirkosertic.bytecoder.ast.ASTInputOfBlock;
import de.mirkosertic.bytecoder.ast.ASTInteger2Generic;
import de.mirkosertic.bytecoder.ast.ASTIntegerValue;
import de.mirkosertic.bytecoder.ast.ASTInvokeSpecial;
import de.mirkosertic.bytecoder.ast.ASTInvokeStatic;
import de.mirkosertic.bytecoder.ast.ASTInvokeVirtual;
import de.mirkosertic.bytecoder.ast.ASTLocalVariable;
import de.mirkosertic.bytecoder.ast.ASTLong2Generic;
import de.mirkosertic.bytecoder.ast.ASTNeg;
import de.mirkosertic.bytecoder.ast.ASTNewArray;
import de.mirkosertic.bytecoder.ast.ASTNewObject;
import de.mirkosertic.bytecoder.ast.ASTNull;
import de.mirkosertic.bytecoder.ast.ASTObjectReturn;
import de.mirkosertic.bytecoder.ast.ASTPutField;
import de.mirkosertic.bytecoder.ast.ASTPutStatic;
import de.mirkosertic.bytecoder.ast.ASTReturn;
import de.mirkosertic.bytecoder.ast.ASTSetArrayValue;
import de.mirkosertic.bytecoder.ast.ASTSetLocalVariable;
import de.mirkosertic.bytecoder.ast.ASTShortValue;
import de.mirkosertic.bytecoder.ast.ASTThrow;
import de.mirkosertic.bytecoder.ast.ASTValue;
import de.mirkosertic.bytecoder.ast.ASTValueReference;
import de.mirkosertic.bytecoder.ast.ASTValuesWithOperator;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeDoubleConstant;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeIntegerConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLongConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class JSASTCodeGenerator {

    private final BytecodeLinkerContext linkerContext;

    public JSASTCodeGenerator(BytecodeLinkerContext aLinkerContext) {
        linkerContext = aLinkerContext;
    }

    public void generateFor(ASTBlock aBlock, JSWriter aWriter) {
        for (ASTValue theValue : aBlock.getValues()) {
            visit(theValue, aWriter);
        }
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

    public String toArray(byte[] aData) {
        StringBuilder theResult = new StringBuilder("[");
        for (int i=0;i<aData.length;i++) {
            if (i>0) {
                theResult.append(",");
            }
            theResult.append(aData[i]);
        }
        theResult.append("]");
        return theResult.toString();
    }

    private String generateJumpCodeFor(BytecodeOpcodeAddress aTarget) {
        return "currentLabel = " + aTarget.getAddress()+";continue controlflowloop;";
    }

    private void visit(ASTValue aValue, JSWriter aWriter) {
        if (aValue instanceof ASTObjectReturn) {
            visit((ASTObjectReturn) aValue, aWriter);
        } else if (aValue instanceof ASTSetLocalVariable) {
            visit((ASTSetLocalVariable) aValue, aWriter);
        } else if (aValue instanceof ASTValuesWithOperator) {
            visit((ASTValuesWithOperator) aValue, aWriter);
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
        } else if (aValue instanceof ASTReturn) {
            visit((ASTReturn) aValue, aWriter);
        } else if (aValue instanceof ASTConstant) {
            visit((ASTConstant) aValue, aWriter);
        } else if (aValue instanceof ASTPutField) {
            visit((ASTPutField) aValue, aWriter);
        } else if (aValue instanceof ASTFloatCompare) {
            visit((ASTFloatCompare) aValue, aWriter);
        } else if (aValue instanceof ASTIntegerValue) {
            visit((ASTIntegerValue) aValue, aWriter);
        } else if (aValue instanceof ASTFloatValue) {
            visit((ASTFloatValue) aValue, aWriter);
        } else if (aValue instanceof ASTNeg) {
            visit((ASTNeg) aValue, aWriter);
        } else if (aValue instanceof ASTArrayLength) {
            visit((ASTArrayLength) aValue, aWriter);
        } else if (aValue instanceof ASTGetField) {
            visit((ASTGetField) aValue, aWriter);
        } else if (aValue instanceof ASTInteger2Generic) {
            visit((ASTInteger2Generic) aValue, aWriter);
        } else if (aValue instanceof ASTNewArray) {
            visit((ASTNewArray) aValue, aWriter);
        } else if (aValue instanceof ASTGoto) {
            visit((ASTGoto) aValue, aWriter);
        } else if (aValue instanceof ASTSetArrayValue) {
            visit((ASTSetArrayValue) aValue, aWriter);
        } else if (aValue instanceof ASTArrayValue) {
            visit((ASTArrayValue) aValue, aWriter);
        } else if (aValue instanceof ASTCheckCast) {
            visit((ASTCheckCast) aValue, aWriter);
        } else if (aValue instanceof ASTByteValue) {
            visit((ASTByteValue) aValue, aWriter);
        } else if (aValue instanceof ASTFloat2Generic) {
            visit((ASTFloat2Generic) aValue, aWriter);
        } else if (aValue instanceof ASTLong2Generic) {
            visit((ASTLong2Generic) aValue, aWriter);
        } else if (aValue instanceof ASTDouble2Generic) {
            visit((ASTDouble2Generic) aValue, aWriter);
        } else if (aValue instanceof ASTShortValue) {
            visit((ASTShortValue) aValue, aWriter);
        } else if (aValue instanceof ASTIF) {
            visit((ASTIF) aValue, aWriter);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue);
        }
    }

    private void visit(ASTIF aValue, JSWriter aWriter) {
        aWriter.print("if (");
        visit(aValue.getValue(), aWriter);
        aWriter.println(") {");
        aWriter.print(" ");
        aWriter.println(generateJumpCodeFor(aValue.getTargetAddress()));
        aWriter.println("}");
    }

    private void visit(ASTValuesWithOperator aValue, JSWriter aWriter) {
        aWriter.print("(");
        aWriter.print("(");
        visit(aValue.getValue1(), aWriter);
        aWriter.print(")");

        switch (aValue.getOperator()) {
            case ADD:
                aWriter.print(" + ");
                break;
            case SUBSTRACT:
                aWriter.print(" - ");
                break;
            case MULTIPLY:
                aWriter.print(" * ");
                break;
            case DIVIDE:
                aWriter.print(" / ");
                break;
            case REMAINDER:
                aWriter.print(" % ");
                break;
            case EQUALS:
                aWriter.print(" == ");
                break;
            case NOTEQUALS:
                aWriter.print(" != ");
                break;
            case LESSTHAN:
                aWriter.print(" > ");
                break;
            case GREATEREQUALS:
                aWriter.print(" >= ");
                break;
            case GREATERTHAN:
                aWriter.print(" > ");
                break;
            case LESSOREQUALS:
                aWriter.print(" <= ");
                break;
        }

        aWriter.print("(");
        visit(aValue.getValue2(), aWriter);
        aWriter.print(")");
        aWriter.print(")");
    }

    private void visit(ASTShortValue aValue, JSWriter aWriter) {
        aWriter.print(aValue.getShortValue());
    }

    private void visit(ASTDouble2Generic aValue, JSWriter aWriter) {
        switch (aValue.getTargetType()) {
        case SHORT:
        case LONG:
        case CHAR:
        case INT:
        case BYTE:
            aWriter.print("Math.round(");
            break;
        default:
            break;
        }

        visit(aValue.getValue(), aWriter);

        switch (aValue.getTargetType()) {
        case SHORT:
        case LONG:
        case CHAR:
        case INT:
        case BYTE:
            aWriter.print(")");
            break;
        default:
            break;
        }
    }

    private void visit(ASTLong2Generic aValue, JSWriter aWriter) {
        visit(aValue.getValue(), aWriter);
    }

    private void visit(ASTFloat2Generic aValue, JSWriter aWriter) {
        switch (aValue.getTargetType()) {
            case SHORT:
            case LONG:
            case CHAR:
            case INT:
            case BYTE:
                aWriter.print("Math.round(");
                break;
            default:
                break;
        }

        visit(aValue.getValue(), aWriter);

        switch (aValue.getTargetType()) {
        case SHORT:
        case LONG:
        case CHAR:
        case INT:
        case BYTE:
            aWriter.print(")");
            break;
        default:
            break;
        }
    }

    private void visit(ASTByteValue aValue, JSWriter aWriter) {
        aWriter.print(aValue.getByteValue());
    }

    private void visit(ASTCheckCast aValue, JSWriter aWriter) {
        visit(aValue.getCurrentValue(), aWriter);
    }

    private void visit(ASTArrayValue aValue, JSWriter aWriter) {
        visit(aValue.getArray(), aWriter);
        aWriter.print(".data[");
        visit(aValue.getIndex(), aWriter);
        aWriter.print("]");
    }

    private void visit(ASTSetArrayValue aValue, JSWriter aWriter) {
        visit(aValue.getArray(), aWriter);
        aWriter.print(".data[");
        visit(aValue.getIndex(), aWriter);
        aWriter.print("] = ");
        visit(aValue.getValue(), aWriter);
        aWriter.println(";");
    }

    private void visit(ASTGoto aValue, JSWriter aWriter) {
        aWriter.print(generateJumpCodeFor(aValue.getTargetAddress()));
        aWriter.println();
    }

    private void visit(ASTNewArray aValue, JSWriter aWriter) {
        aWriter.print("newArray(");
        visit(aValue.getLength(), aWriter);
        aWriter.print(")");
    }

    private void visit(ASTInteger2Generic aValue, JSWriter aWriter) {
        visit(aValue.getValue(), aWriter);
    }

    private void visit(ASTGetField aValue, JSWriter aWriter) {
        visit(aValue.getReference(), aWriter);
        aWriter.print(".data.");
        aWriter.print(aValue.getFieldName());
    }

    private void visit(ASTArrayLength aValue, JSWriter aWriter) {
        visit(aValue.getReference(), aWriter);
        aWriter.print(".data.length");
    }

    private void visit(ASTNeg aValue, JSWriter aWriter) {
        aWriter.print("-(");
        visit(aValue.getValue(), aWriter);
        aWriter.print(")");
    }

    private void visit(ASTFloatValue aValue, JSWriter aWriter) {
        aWriter.print(aValue.getFloatValue());
    }

    private void visit(ASTIntegerValue aValue, JSWriter aWriter) {
        aWriter.print(aValue.getIntValue());
    }

    private void visit(ASTFloatCompare aValue, JSWriter aWriter) {
        aWriter.print("(");
        visit(aValue.getValue1(), aWriter);
        aWriter.print(" == ");
        visit(aValue.getValue2(), aWriter);
        aWriter.print("? 0 : ");
        aWriter.print("(");
        visit(aValue.getValue1(), aWriter);
        aWriter.print(" > ");
        visit(aValue.getValue2(), aWriter);
        aWriter.print("? 1 : ");
        aWriter.print(" - 1))");
    }

    private void visit(ASTPutField aValue, JSWriter aWriter) {
        visit(aValue.getReference(), aWriter);
        aWriter.print(".data.");
        aWriter.print(aValue.getFieldName());
        aWriter.print(" = ");
        visit(aValue.getFieldValue(), aWriter);
        aWriter.println(";");
    }

    private void visit(ASTConstant aValue, JSWriter aWriter) {
        BytecodeConstant theConstant = aValue.getConstant();
        if (theConstant instanceof BytecodeFloatConstant) {
            BytecodeFloatConstant theFloat = (BytecodeFloatConstant) theConstant;
            aWriter.print(theFloat.getFloatValue());
        } else if (theConstant instanceof BytecodeDoubleConstant) {
            BytecodeDoubleConstant theDouble = (BytecodeDoubleConstant) theConstant;
            aWriter.print(theDouble.getDoubleValue());
        } else if (theConstant instanceof BytecodeLongConstant) {
            BytecodeLongConstant theLong = (BytecodeLongConstant) theConstant;
            aWriter.print(theLong.getLongValue());
        } else if (theConstant instanceof BytecodeClassinfoConstant) {
            BytecodeClassinfoConstant theClassInfo = (BytecodeClassinfoConstant) theConstant;
            aWriter.print(toClassName(theClassInfo) + ".runtimeClass);");
        } else if (theConstant instanceof BytecodeIntegerConstant) {
            BytecodeIntegerConstant theInteger = (BytecodeIntegerConstant) theConstant;
            aWriter.print(theInteger.integerValue());
        } else if (theConstant instanceof BytecodeStringConstant) {
            try {
                BytecodeStringConstant theStr = (BytecodeStringConstant) theConstant;
                byte[] theBytes = theStr.getValue().toUTF8Bytes();

                // Construct a String
                aWriter.print("newConstantString(");
                aWriter.print(toArray(theBytes));
                aWriter.print(")");

            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException("Unsupported constant : " + theConstant);
        }
    }

    private void visit(ASTReturn aValue, JSWriter aWriter) {
        aWriter.println("return;");
    }

    private void visit(ASTNewObject aValue, JSWriter aWriter) {
        aWriter.print(toClassName(aValue.getType()));
        aWriter.print(".emptyInstance()");
    }

    private void visit(ASTValueReference aValue, JSWriter aWriter) {
        visit(aValue.getReference(), aWriter);
    }

    private void visit(ASTInvokeSpecial aValue, JSWriter aWriter) {
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

    private void visit(ASTInvokeVirtual aValue, JSWriter aWriter) {
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

    private void visit(ASTInputOfBlock aValue, JSWriter aWriter) {
        // TODO: How to handle this?
        System.out.println("lala");
    }

    private void visit(ASTThrow aValue, JSWriter aWriter) {
        aWriter.print("throw ");

        visit(aValue.getReference(), aWriter);

        aWriter.println(";");
    }

    private void visit(ASTInvokeStatic aValue, JSWriter aWriter) {

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

    private void visit(ASTGetStatic aValue, JSWriter aWriter) {
        // TODO: Check static init here
        aWriter.print(toClassName(aValue.getClassName()));
        aWriter.print(".staticFields.");
        aWriter.print(aValue.getFieldName().stringValue());
    }

    private void visit(ASTPutStatic aValue, JSWriter aWriter) {
        // TODO: Check static init here
        aWriter.print(toClassName(aValue.getClassName()));
        aWriter.print(".staticFields.");
        aWriter.print(aValue.getFieldName().stringValue());
        aWriter.print(" = ");

        visit(aValue.getArgument(), aWriter);

        aWriter.println(";");
    }

    private void visit(ASTLocalVariable aValue, JSWriter aWriter) {
        aWriter.print("local" + aValue.getVariableIndex());
    }

    private void visit(ASTNull aValue, JSWriter aWriter) {
        aWriter.print("null");
    }

    private void visit(ASTSetLocalVariable aValue, JSWriter aWriter) {
        aWriter.print("frame.local" + aValue.getVariableIndex());
        aWriter.print(" = ");
        visit(aValue.getValue(), aWriter);
        aWriter.println(";");
    }

    private void visit(ASTObjectReturn aValue, JSWriter aWriter) {
        aWriter.print("return ");
        visit(aValue.getValue(), aWriter);
        aWriter.println(";");
    }
}