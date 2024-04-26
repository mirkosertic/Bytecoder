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
package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Node;
import de.mirkosertic.bytecoder.core.ir.Variable;
import org.junit.Test;
import org.objectweb.asm.Type;

import static org.junit.Assert.assertTrue;

public class GraphPatternMatcherTest {

    @Test
    public void testEmpty() {
        final Logger logger = new Slf4JLogger();
        final Graph source = new Graph(logger);
        final Graph pattern = new Graph(logger);
        final Node pivot = pattern.newRegion("Test");

        final GraphPatternMatcher matcher = new GraphPatternMatcher(pivot);
        assertTrue(matcher.findMatches(source).isEmpty());
    }

    @Test
    public void testSearchForCopy() {
        final Logger logger = new Slf4JLogger();
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

        final GraphPatternMatcher matcher = new GraphPatternMatcher(pc);
        assertTrue(matcher.findMatches(source).size() == 1);
    }
}
