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

import de.mirkosertic.bytecoder.ssa.ControlFlowEdgeType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class StructuredControlFlowTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testSingleNode() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Collections.singletonList(0));
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).end();
    }

    @Test
    public void testSimpleSequence() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).end();
    }

    @Test
    public void testSimpleSequenceWithoutGotos() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2));
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).end();
    }

    @Test
    public void testDoubleExit() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        builder.add(ControlFlowEdgeType.forward, 0, 3);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock, times(2)).beginBlockFor(any(Block.class));
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).end();

    }

    @Test
    public void testSimpleLoop() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        builder.add(ControlFlowEdgeType.forward, 2, 3);
        builder.add(ControlFlowEdgeType.back, 2, 1);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).end();
    }

    @Test
    public void testGlobalLoop() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        builder.add(ControlFlowEdgeType.forward, 2, 3);
        builder.add(ControlFlowEdgeType.back, 2, 0);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).end();
    }

    @Test
    public void testSingleNodeLoop() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Collections.singletonList(0));
        builder.add(ControlFlowEdgeType.back, 0, 0);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).end();
    }

    @Test
    public void testNestedLoop() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3, 4));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        builder.add(ControlFlowEdgeType.forward, 2, 3);
        builder.add(ControlFlowEdgeType.forward, 3, 4);
        builder.add(ControlFlowEdgeType.back, 3, 1);
        builder.add(ControlFlowEdgeType.back, 4, 0);

        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).beginBlockFor(any(Block.class));
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(4));
        order.verify(writerMock, times(2)).closeBlock();
        order.verify(writerMock).end();
    }

    @Test
    public void testAdvancedLoop() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3, 4));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        builder.add(ControlFlowEdgeType.forward, 2, 3);
        builder.add(ControlFlowEdgeType.back, 3, 1);
        builder.add(ControlFlowEdgeType.forward, 2, 4);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock, times(2)).beginBlockFor(any(Block.class));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(4));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).end();
    }

    @Test
    public void testTwoDominatedLoops() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3, 4, 5));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        builder.add(ControlFlowEdgeType.back, 2, 1);
        builder.add(ControlFlowEdgeType.forward, 0, 3);
        builder.add(ControlFlowEdgeType.forward, 3, 4);
        builder.add(ControlFlowEdgeType.back, 4, 3);
        builder.add(ControlFlowEdgeType.forward, 2, 5);
        builder.add(ControlFlowEdgeType.forward, 4, 5);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);

        order.verify(writerMock).begin();
        order.verify(writerMock, times(3)).beginBlockFor(any(Block.class));
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock, times(2)).closeBlock();
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).write(eq(4));
        order.verify(writerMock, times(2)).closeBlock();
        order.verify(writerMock).write(eq(5));
        order.verify(writerMock).end();
    }

    @Test
    public void testHeadToHead() throws HeadToHeadControlFlowException {
        expectedException.expect(HeadToHeadControlFlowException.class);
        expectedException.expectMessage("{2,3} are head to head");

        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3));
        builder.add(ControlFlowEdgeType.forward,0, 2);
        builder.add(ControlFlowEdgeType.back, 3, 1);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
    }

    @Test
    public void testIfThenElse() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 0, 2);
        builder.add(ControlFlowEdgeType.forward, 1, 3);
        builder.add(ControlFlowEdgeType.forward, 2, 3);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock, times(3)).beginBlockFor(any(Block.class));
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).end();
    }

    @Test
    public void testSimpleLoopMultipleExits() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1, 2, 3));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.forward, 1, 2);
        builder.add(ControlFlowEdgeType.forward, 1, 3);
        builder.add(ControlFlowEdgeType.forward, 2, 3);
        builder.add(ControlFlowEdgeType.back, 2, 1);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock, times(2)).beginBlockFor(any(Block.class));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).beginBlockFor(any(Block.class));
        order.verify(writerMock).write(eq(2));
        order.verify(writerMock, times(2)).closeBlock();
        order.verify(writerMock).write(eq(3));
        order.verify(writerMock).end();
    }

    @Test
    public void testCompleteLoop() throws HeadToHeadControlFlowException {
        final StructuredControlFlowBuilder<Integer> builder = new StructuredControlFlowBuilder<>(Arrays.asList(0, 1));
        builder.add(ControlFlowEdgeType.forward, 0, 1);
        builder.add(ControlFlowEdgeType.back, 1, 0);
        final StructuredControlFlow<Integer> graph = builder.build();
        graph.printDebug(new PrintWriter(System.out));
        graph.writeStructuredControlFlow(new IntegerDebugStructurecControlFlowWriter(System.out));

        final StructuredControlFlowWriter<Integer> writerMock = mock(StructuredControlFlowWriter.class);
        graph.writeStructuredControlFlow(writerMock);
        final InOrder order = inOrder(writerMock);
        order.verify(writerMock).begin();
        order.verify(writerMock).beginLoopFor(any(Block.class));
        order.verify(writerMock).write(eq(0));
        order.verify(writerMock).write(eq(1));
        order.verify(writerMock).closeBlock();
        order.verify(writerMock).end();
    }
}