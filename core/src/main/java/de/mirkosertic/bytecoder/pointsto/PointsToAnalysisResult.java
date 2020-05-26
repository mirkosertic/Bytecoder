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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PointsToAnalysisResult {

    interface Symbol {
        default boolean origin() {
            return true;
        }
    }

    enum GlobalSymbols implements Symbol {
        thisScope,staticScope
    }

    public static class ParamPref implements Symbol {

        private final int number;

        public ParamPref(final int number) {
            this.number = number;
        }
    }

    public static class VariableSymbol implements Symbol {

        private final String name;

        public VariableSymbol(final String name) {
            this.name = name;
        }

        @Override
        public boolean origin() {
            return false;
        }
    }

    public static class AllocationSymbol implements Symbol {

        public AllocationSymbol() {
        }
    }

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

    public PointsToAnalysisResult() {
        pts = new HashMap<>();
        potentialScopeMergeOperations = new ArrayList<>();
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

    public Set<Symbol> resolvedPointsToFor(final Symbol aSymbol) {
        final Set<Symbol> initialSet = pts.get(aSymbol);
        if (initialSet == null) {
            return Collections.emptySet();
        }
        final Set<Symbol> workingSet = new HashSet<>(initialSet);
        repeat: while (true) {
            for (final Symbol entry : workingSet) {
                if (!entry.origin()) {
                    final Set<Symbol> result = pts.get(entry);
                    if (result != null) {
                        workingSet.remove(entry);
                        workingSet.addAll(result);
                        continue repeat;
                    }
                }
            }
            break;
        }
        return workingSet;
    }
}
