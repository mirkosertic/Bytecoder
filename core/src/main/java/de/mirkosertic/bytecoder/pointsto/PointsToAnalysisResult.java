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

import de.mirkosertic.bytecoder.ssa.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PointsToAnalysisResult {

    static class PotentialScopeMergeOperation {
        private final Symbol destination;
        private final Symbol source;

        public PotentialScopeMergeOperation(final Symbol destination, final Symbol source) {
            this.destination = destination;
            this.source = source;
        }

        public Symbol destination() {
            return destination;
        }

        public Symbol source() {
            return source;
        }
    }

    private final Map<Symbol, Set<Symbol>> pts;
    private final List<PotentialScopeMergeOperation> potentialScopeMergeOperations;
    private final Set<Symbol> returningSymbols;
    private final Set<AllocationSymbol> allocationSymbols;

    public PointsToAnalysisResult() {
        pts = new HashMap<>();
        potentialScopeMergeOperations = new ArrayList<>();
        returningSymbols = new HashSet<>();
        allocationSymbols = new HashSet<>();
    }

    public AllocationSymbol allocation(final Value v) {
        final AllocationSymbol alloc = new AllocationSymbol(v);
        allocationSymbols.add(alloc);
        return alloc;
    }

    public List<PotentialScopeMergeOperation> potentialScopeMergeOperations() {
        return potentialScopeMergeOperations;
    }

    public void assign(final Symbol aLHS, final Symbol aRHS) {
        pts.computeIfAbsent(aLHS, t -> new HashSet<>()).add(aRHS);
    }

    public void alias(final Symbol aLHS, final Symbol aRHS) {
        final Set<Symbol> known = pts.get(aRHS);
        if (known == null) {
            pts.computeIfAbsent(aLHS, t -> new HashSet<>()).add(aRHS);
        } else {
            pts.put(aLHS, known);
        }
    }

    public void writeInto(final Symbol aDestination, final Symbol aSource) {
        pts.computeIfAbsent(aDestination, t -> new HashSet<>()).add(aSource);
        potentialScopeMergeOperations.add(new PotentialScopeMergeOperation(aDestination, aSource));
    }

    public void readFrom(final Symbol aDestination, final Symbol aSource) {
        pts.computeIfAbsent(aDestination, t -> new HashSet<>()).add(aSource);
    }

    public void returns(final Symbol aSymbol) {
        returningSymbols.add(aSymbol);
    }

    public Set<Symbol> resolvedPointsToFor(final Symbol aSymbol) {
        final Set<Symbol> initialSet = pts.get(aSymbol);
        if (initialSet == null) {
            return Collections.emptySet();
        }
        final Set<Symbol> alreadyExpanded = new HashSet<>();
        final Set<Symbol> workingSet = new HashSet<>(initialSet);
        repeat: while (true) {
            for (final Symbol entry : workingSet) {
                if (!entry.origin() && pts.containsKey(entry)) {
                    final Set<Symbol> result = new HashSet<>(pts.get(entry));
                    workingSet.remove(entry);
                    if (alreadyExpanded.add(entry)) {
                        result.removeAll(alreadyExpanded);
                        workingSet.addAll(result);
                    }
                    continue repeat;
                }
            }
            break;
        }
        return workingSet;
    }

    public Set<Symbol> returningSymbols() {
        return returningSymbols;
    }

    public Set<AllocationSymbol> allocationSymbols() {
        return allocationSymbols;
    }

    public Map<Symbol, Set<Symbol>> computeMergingFlows() {
        final Map<Symbol, Set<Symbol>> result = new HashMap<>();
        for (final Symbol ret : returningSymbols) {
            if (ret.origin()) {
                result.computeIfAbsent(ret, t -> new HashSet<>()).add(GlobalSymbols.returnScope);
            } else {
                for (final Symbol s : resolvedPointsToFor(ret)){
                    result.computeIfAbsent(s, t -> new HashSet<>()).add(GlobalSymbols.returnScope);
                }
            }
        }
        final Map<Symbol, Set<Symbol>> resolvedPointsCache = new HashMap<>();

        for (final PotentialScopeMergeOperation merge : potentialScopeMergeOperations) {
            final Set<Symbol> destinations;
            if (merge.destination.origin()) {
                destinations = Collections.singleton(merge.destination);
            } else {
                destinations = resolvedPointsCache.computeIfAbsent(merge.destination, symbol -> resolvedPointsToFor(symbol));
            }
            final Set<Symbol> sources;
            if (merge.source.origin()) {
                sources = Collections.singleton(merge.source);
            } else {
                sources = resolvedPointsCache.computeIfAbsent(merge.source, symbol -> resolvedPointsToFor(symbol));
            }

            for (final Symbol s : sources) {
                result.computeIfAbsent(s, t -> new HashSet<>()).addAll(destinations);
            }
        }
        for (final Map.Entry<Symbol, Set<Symbol>> entry : result.entrySet()) {
            final Set<Symbol> expaned = new HashSet<>();
            check: for(;;) {
                final Set<Symbol> values = entry.getValue();
                for (final Symbol s : values) {
                    if (((s instanceof AllocationSymbol) || (s instanceof InvocationResultSymbol)) && expaned.add(s)) {
                        final Set<Symbol> replacement = result.get(s);
                        if (replacement != null) {
                            values.addAll(result.get(s));
                            values.remove(s);
                        }
                        continue check;
                    }
                }
                break check;
            }
        }
        return result;
    }
}