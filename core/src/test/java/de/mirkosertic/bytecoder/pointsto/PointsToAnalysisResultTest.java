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
        /*
                var1 = this
                var2 = var1

                -> var2 points to {this}
         */
        final GlobalSymbols thisRef = GlobalSymbols.thisScope;
        final VariableSymbol var1 = new VariableSymbol("var1");
        final VariableSymbol var2 = new VariableSymbol("var2");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, var1);

        final Set<Symbol> resolvedPointsTo = result.resolvedPointsToFor(var2);
        assertEquals(1, resolvedPointsTo.size());
        assertTrue(containsOneInstanceOf(resolvedPointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.thisScope));
    }

    @Test
    public void test2() {
        /*
               var1 = this
               var2 = allocation
               phi = [var1,var2]

               -> phi points to {this,allocation}

         */
        final GlobalSymbols thisRef = GlobalSymbols.thisScope;
        final VariableSymbol var1 = new VariableSymbol("var1");
        final VariableSymbol var2 = new VariableSymbol("var2");
        final VariableSymbol phi = new VariableSymbol("PHI");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, result.allocation());
        result.assign(phi, var1);
        result.assign(phi, var2);

        final Set<Symbol> resolvedPointsTo = result.resolvedPointsToFor(phi);
        assertEquals(2, resolvedPointsTo.size());
        assertTrue(containsOneInstanceOf(resolvedPointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.thisScope));
        assertTrue(containsOneInstanceOf(resolvedPointsTo, AllocationSymbol.class));
    }

    @Test
    public void test3() {
        /*
                var1 = this
                var2 = allocation
                var1.x = vars

                -> var1 points to {this,allocation}

         */
        final GlobalSymbols thisRef = GlobalSymbols.thisScope;
        final VariableSymbol var1 = new VariableSymbol("var1");
        final VariableSymbol var2 = new VariableSymbol("var2");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, result.allocation());
        result.writeInto(var1, var2);

        final Set<Symbol> resolvedPointsTo = result.resolvedPointsToFor(var1);
        assertEquals(2, resolvedPointsTo.size());
        assertTrue(containsOneInstanceOf(resolvedPointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.thisScope));
        assertTrue(containsOneInstanceOf(resolvedPointsTo, AllocationSymbol.class));

        final List<PointsToAnalysisResult.PotentialScopeMergeOperation> writeOps = result.potentialScopeMergeOperations();
        assertEquals(1, writeOps.size());

        final PointsToAnalysisResult.PotentialScopeMergeOperation op = writeOps.get(0);

        final Set<Symbol> sourcePointsTo = result.resolvedPointsToFor(op.source());
        assertEquals(1, sourcePointsTo.size());
        assertTrue(containsOneInstanceOf(sourcePointsTo, AllocationSymbol.class));

        final Set<Symbol> destPointsTo = result.resolvedPointsToFor(op.destination());
        assertEquals(2, destPointsTo.size());
        assertTrue(containsOneInstanceOf(destPointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.thisScope));
        assertTrue(containsOneInstanceOf(destPointsTo, AllocationSymbol.class));
    }

    @Test
    public void test4() {
        /*
                var1 = this
                var2 = param1
                phi = [var1,var4]
                phi.x = var2
                var4 = param2

                -> var3 points to {this,param1,param2}
         */
        final GlobalSymbols thisRef = GlobalSymbols.thisScope;
        final VariableSymbol var1 = new VariableSymbol("var1");
        final VariableSymbol var2 = new VariableSymbol("var2");
        final VariableSymbol phi = new VariableSymbol("PHI");
        final VariableSymbol var3 = new VariableSymbol("var3");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, new ParamPref(1));
        result.assign(phi, var1);
        result.assign(phi, var3);
        result.writeInto(phi, var2);
        result.alias(var3, new ParamPref(2));

        final Set<Symbol> resolvedPointsTo = result.resolvedPointsToFor(phi);
        assertEquals(3, resolvedPointsTo.size());
        assertTrue(containsOneInstanceOf(resolvedPointsTo, GlobalSymbols.class, t -> t == GlobalSymbols.thisScope));
        assertTrue(containsOneInstanceOf(resolvedPointsTo, ParamPref.class, t -> t.index() == 1));
        assertTrue(containsOneInstanceOf(resolvedPointsTo, ParamPref.class, t -> t.index() == 2));
    }

    @Test
    public void test5() {
        /*
                var1 = this
                var2 = param1
                var3 = var2.x
                var4 = var3

                -> var4 points to {param1}
         */
        final GlobalSymbols thisRef = GlobalSymbols.thisScope;
        final VariableSymbol var1 = new VariableSymbol("var1");
        final VariableSymbol var2 = new VariableSymbol("var2");
        final VariableSymbol var3 = new VariableSymbol("var3");
        final VariableSymbol var4 = new VariableSymbol("var4");

        final PointsToAnalysisResult result = new PointsToAnalysisResult();
        result.alias(var1, thisRef);
        result.alias(var2, new ParamPref(1));
        result.readFrom(var3, var2);
        result.alias(var4, var3);

        final Set<Symbol> resolvedPointsTo = result.resolvedPointsToFor(var4);
        assertEquals(1, resolvedPointsTo.size());
        assertTrue(containsOneInstanceOf(resolvedPointsTo, ParamPref.class, t -> t.index() == 1));
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