/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.pointsto;

import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PointsToAnalysisResultTest {

    @Test
    public void test1() {
        final PointsToAnalysisResult.GlobalSymbols thisRef = PointsToAnalysisResult.GlobalSymbols.thisScope;
        final PointsToAnalysisResult.VariableSymbol var1 = new PointsToAnalysisResult.VariableSymbol("var1");
        final PointsToAnalysisResult.VariableSymbol var2 = new PointsToAnalysisResult.VariableSymbol("var2");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, var1);

        final Set<PointsToAnalysisResult.Symbol> resolvedPointsTo = result.resolvedPointsToFor(var2);
        assertEquals(1, resolvedPointsTo.size());
        assertTrue(containsOneInstanceOf(resolvedPointsTo, PointsToAnalysisResult.GlobalSymbols.class, t -> t == PointsToAnalysisResult.GlobalSymbols.thisScope));
    }

    @Test
    public void test2() {
        final PointsToAnalysisResult.GlobalSymbols thisRef = PointsToAnalysisResult.GlobalSymbols.thisScope;
        final PointsToAnalysisResult.VariableSymbol var1 = new PointsToAnalysisResult.VariableSymbol("var1");
        final PointsToAnalysisResult.VariableSymbol var2 = new PointsToAnalysisResult.VariableSymbol("var2");
        final PointsToAnalysisResult.VariableSymbol phi = new PointsToAnalysisResult.VariableSymbol("PHI");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, new PointsToAnalysisResult.AllocationSymbol());
        result.assign(phi, var1);
        result.assign(phi, var2);

        final Set<PointsToAnalysisResult.Symbol> resolvedPointsTo = result.resolvedPointsToFor(phi);
        assertEquals(2, resolvedPointsTo.size());
        assertTrue(containsOneInstanceOf(resolvedPointsTo, PointsToAnalysisResult.GlobalSymbols.class, t -> t == PointsToAnalysisResult.GlobalSymbols.thisScope));
        assertTrue(containsOneInstanceOf(resolvedPointsTo, PointsToAnalysisResult.AllocationSymbol.class));
    }

    @Test
    public void test3() {
        final PointsToAnalysisResult.GlobalSymbols thisRef = PointsToAnalysisResult.GlobalSymbols.thisScope;
        final PointsToAnalysisResult.VariableSymbol var1 = new PointsToAnalysisResult.VariableSymbol("var1");
        final PointsToAnalysisResult.VariableSymbol var2 = new PointsToAnalysisResult.VariableSymbol("var2");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, new PointsToAnalysisResult.AllocationSymbol());
        result.writeInto(var1, var2);

        final List<PointsToAnalysisResult.PotentialScopeMergeOperation> writeOps = result.potentialScopeMergeOperations();
        assertEquals(1, writeOps.size());

        final PointsToAnalysisResult.PotentialScopeMergeOperation op = writeOps.get(0);

        final Set<PointsToAnalysisResult.Symbol> sourcePointsTo = result.resolvedPointsToFor(op.source());
        assertEquals(1, sourcePointsTo.size());
        assertTrue(containsOneInstanceOf(sourcePointsTo, PointsToAnalysisResult.AllocationSymbol.class));

        final Set<PointsToAnalysisResult.Symbol> destPointsTo = result.resolvedPointsToFor(op.destination());
        assertEquals(2, destPointsTo.size());
        assertTrue(containsOneInstanceOf(destPointsTo, PointsToAnalysisResult.GlobalSymbols.class, t -> t == PointsToAnalysisResult.GlobalSymbols.thisScope));
        assertTrue(containsOneInstanceOf(destPointsTo, PointsToAnalysisResult.AllocationSymbol.class));
    }

    <T> boolean containsNInstancesOf(final Collection<T> aCollection, final Class<? extends T> aType, final int aNumber) {
        return aCollection.stream().filter(t -> aType.isAssignableFrom(t.getClass())).count() == aNumber;
    }

    <T> boolean containsOneInstanceOf(final Collection<T> aCollection, final Class<? extends T> aType) {
        return containsNInstancesOf(aCollection, aType, 1);
    }

    <T,X> boolean containsNInstancesOf(final Collection<T> aCollection, final Class<X> aType, final Predicate<X> aPred, final int aNumber) {
        return aCollection.stream().filter(t -> aType.isAssignableFrom(t.getClass())).filter(t -> aPred.test((X) t)).count() == aNumber;
    }

    <T,X> boolean containsOneInstanceOf(final Collection<T> aCollection, final Class<X> aType, final Predicate<X> aPred) {
        return containsNInstancesOf(aCollection, aType, aPred, 1);
    }
}