/*
 * Copyright 2024 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.patternmatcher;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.StandardProjections;
import de.mirkosertic.bytecoder.core.ir.Variable;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PatternMatcherTest {

    private static final Logger logger = new Slf4JLogger();

    @Test
    public void testEmpty() {
        final Graph source = new Graph(logger);
        final Graph pattern = new Graph(logger);
        final Node pivot = pattern.newRegion("Test");

        final PatternMatcher matcher = new PatternMatcher(logger, pivot);
        assertTrue(matcher.findMatches(source).isEmpty());
    }

    @Test
    public void testSearchForCopy() {
        final Graph source = new Graph(logger);
        final Variable st = source.newVariable(Type.INT_TYPE);
        final Node sc = source.newCopy();
        final Node sq = source.newInt(10);
        st.addIncomingData(sc);
        sc.addIncomingData(sq);

        final Graph pattern = new Graph(logger);
        final Variable pt = pattern.newVariable(Type.INT_TYPE);
        final Node pc = pattern.newCopy();
        final Node pq = pattern.newInt(10);
        pt.addIncomingData(pc);
        pc.addIncomingData(pq);

        final PatternMatcher matcher = new PatternMatcher(logger, pc);
        matcher.debugOutput();
        final List<Match> matches = matcher.findMatches(source);
        assertEquals(1, matches.size());
        final Match m = matches.get(0);
        assertSame(sc, m.root());
        assertSame(st, m.mappingFor(pt));
        assertSame(sq, m.mappingFor(pq));
        assertSame(sc, m.mappingFor(pc));
    }

    @Test
    public void testRedundantCopy() {
        final Graph source = new Graph(logger);
        final Node sourceConstant1 = source.newInt(10);
        final Node sourceVariable1 = source.newVariable(Type.INT_TYPE);
        final Copy sourceCopy1 = source.newCopy();
        sourceCopy1.addIncomingData(sourceConstant1);
        sourceVariable1.addIncomingData(sourceCopy1);

        final Copy sourceCopy2 = source.newCopy();
        sourceCopy2.addIncomingData(sourceVariable1);
        final Node sourceVariable2 = source.newVariable(Type.INT_TYPE);
        sourceVariable2.addIncomingData(sourceCopy2);

        sourceCopy1.addControlFlowTo(StandardProjections.DEFAULT, sourceCopy2);

        final Graph pattern = new Graph(logger);
        final Node patternConstant1 = pattern.newInt(10);
        final Node patternVariable1 = pattern.newVariable(Type.INT_TYPE);
        final Copy patternCopy1 = pattern.newCopy();
        patternCopy1.addIncomingData(patternConstant1);
        patternVariable1.addIncomingData(patternCopy1);

        final Copy patternCopy2 = pattern.newCopy();
        patternCopy2.addIncomingData(patternVariable1);
        final Node patternVariable2 = pattern.newVariable(Type.INT_TYPE);
        patternVariable2.addIncomingData(patternCopy2);

        patternCopy1.addControlFlowTo(StandardProjections.DEFAULT, patternCopy2);

        final PatternMatcher matcher = new PatternMatcher(logger, patternCopy1);
        matcher.debugOutput();
        final List<Match> matches = matcher.findMatches(source);
        assertEquals(1, matches.size());
        final Match m = matches.get(0);

        assertSame(sourceCopy1, m.root());
        assertSame(sourceCopy1, m.mappingFor(patternCopy1));
        assertSame(sourceCopy2, m.mappingFor(patternCopy2));
        assertSame(sourceConstant1, m.mappingFor(patternConstant1));
        assertSame(sourceVariable1, m.mappingFor(patternVariable1));
        assertSame(sourceVariable2, m.mappingFor(patternVariable2));
    }
}
