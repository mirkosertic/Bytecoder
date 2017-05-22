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
import de.mirkosertic.bytecoder.ast.ASTLocalVariableValue;
import de.mirkosertic.bytecoder.ast.ASTObjectReturn;
import de.mirkosertic.bytecoder.ast.ASTSetLocalVariableValue;
import de.mirkosertic.bytecoder.ast.ASTValue;

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

    private void visit(ASTValue aValue, PrintWriter aWriter) {
        if (aValue instanceof ASTObjectReturn) {
            visit((ASTObjectReturn) aValue, aWriter);
        } else if (aValue instanceof ASTSetLocalVariableValue) {
            visit((ASTSetLocalVariableValue) aValue, aWriter);
        } else if (aValue instanceof ASTComputationADD) {
            visit((ASTComputationADD) aValue, aWriter);
        } else if (aValue instanceof ASTComputationSUB) {
            visit((ASTComputationSUB) aValue, aWriter);
        } else if (aValue instanceof ASTComputationMUL) {
            visit((ASTComputationMUL) aValue, aWriter);
        } else if (aValue instanceof ASTComputationDIV) {
            visit((ASTComputationDIV) aValue, aWriter);
        } else if (aValue instanceof ASTLocalVariableValue) {
            visit((ASTLocalVariableValue) aValue, aWriter);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue);
        }
    }

    private void visit(ASTLocalVariableValue aValue, PrintWriter aWriter) {
        aWriter.print("local" + aValue.getVariableIndex());
    }

    private void visit(ASTSetLocalVariableValue aValue, PrintWriter aWriter) {
        aWriter.print("local" + aValue.getVariableIndex());
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