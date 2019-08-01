/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.stackifier;

import java.io.PrintWriter;

import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFElseExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;

class DebugStructurecControlFlowWriter extends Stackifier.StackifierStructuredControlFlowWriter {

    private final PrintWriter writer;

    public DebugStructurecControlFlowWriter(final Stackifier st, final PrintWriter writer) {
        super(st);
        this.writer = writer;
    }

    private String indent(final int l) {
        final StringBuilder b = new StringBuilder();
        for (int i=0;i<l;i++) {
            b.append("    ");
        }
        return b.toString();
    }

    @Override
    public void beginLoopFor(final Block<RegionNode> block) {
        writer.print(indent(hierarchy.size()));
        writer.print(String.format("LOOP $%s: {", block.getLabel().name()));
        writer.println();
        super.beginLoopFor(block);
    }

    @Override
    public void beginBlockFor(final Block<RegionNode> block) {
        writer.print(indent(hierarchy.size()));
        writer.print(String.format("BLOCK $%s: {", block.getLabel().name()));
        writer.println();
        super.beginBlockFor(block);
    }

    @Override
    public void writeExpression(final RegionNode regionNode, final Expression e) {
        final int indent = hierarchy.size();
        if (e instanceof GotoExpression) {
            final GotoExpression g = (GotoExpression) e;
            writer.print(indent(indent));
            writer.println(String.format("goto %d", g.jumpTarget().getAddress()));
        } else if (e instanceof BreakExpression) {
            final BreakExpression b = (BreakExpression) e;
            writer.print(indent(indent));
            writer.println(String.format("break $%s", b.blockToBreak().name()));
        } else if (e instanceof ContinueExpression) {
            final ContinueExpression c = (ContinueExpression) e;
            writer.print(indent(indent));
            writer.println(String.format("continue $%s", c.labelToReturnTo().name()));
        } else if (e instanceof IFExpression) {
            final IFExpression i = (IFExpression) e;
            writer.print(indent(indent));
            writer.println("if ");
            writeExpressionList(regionNode, i.getExpressions());
        } else if (e instanceof IFElseExpression) {
            final IFElseExpression i = (IFElseExpression) e;
            writeExpression(regionNode, i.getCondition());
            writer.print(indent(indent));
            writer.println("else ");
            writeExpressionList(regionNode, i.getElsePart());
        }
    }

    @Override
    public void write(final RegionNode node) {
        writer.print(indent(hierarchy.size()));
        writer.println(node);
        super.write(node);
    }

    @Override
    public void closeBlock() {
        final Block<RegionNode> currentBlock = hierarchy.peek();
        super.closeBlock();
        writer.print(indent(hierarchy.size()));
        writer.println(String.format("} ; Closing block $%s", currentBlock.getLabel().name()));
    }

    @Override
    public void end() {
        super.end();
        writer.flush();
    }
}
