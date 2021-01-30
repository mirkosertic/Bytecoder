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
package de.mirkosertic.bytecoder.escapeanalysis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.pointsto.AllocationSymbol;
import de.mirkosertic.bytecoder.pointsto.GlobalSymbols;
import de.mirkosertic.bytecoder.pointsto.InvocationResultSymbol;
import de.mirkosertic.bytecoder.pointsto.ParamRef;
import de.mirkosertic.bytecoder.pointsto.PointsToAnalysisResult;
import de.mirkosertic.bytecoder.pointsto.Symbol;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;

public class EscapeAnalysis {

    public void analyze(final Logger aLogger, final ProgramDescriptor aProgramDescriptor, final PointsToAnalysisResult aAnalysisResult) {

        if (aAnalysisResult.potentialScopeMergeOperations().size() > 1000) {

            aLogger.info("{}.{} seems to be too complex. Giving up escape analysis", aProgramDescriptor.linkedClass().getClassName().name(), aProgramDescriptor.method().getName().stringValue());

            for (final AllocationSymbol allocation : aAnalysisResult.allocationSymbols()) {
                final Value v = allocation.value();
                if (v instanceof ValueWithEscapeCheck) {
                    ((ValueWithEscapeCheck) v).markAsEscaped();
                }
            }

            return;
        }

        final Map<Symbol, Set<Symbol>> flows = aAnalysisResult.computeMergingFlows();
        for (final AllocationSymbol allocation : aAnalysisResult.allocationSymbols()) {
            final Value v = allocation.value();
            final Set<Symbol> symbolFlow = flows.get(allocation);
            if (symbolFlow != null) {
                final Set<Symbol> merged = new HashSet<>(symbolFlow);
                final Set<Symbol> expaned = new HashSet<>();
                check: for(;;) {
                    for (final Symbol s : merged) {
                        if (((s instanceof AllocationSymbol) || (s instanceof InvocationResultSymbol)) && expaned.add(s)) {
                            final Set<Symbol> replacement = flows.get(s);
                            if (replacement != null) {
                                merged.addAll(flows.get(s));
                                merged.remove(s);
                            }
                            continue check;
                        }
                    }
                    break check;
                }
                boolean escaping = false;
                for (final Symbol s : symbolFlow) {
                    if (s == GlobalSymbols.returnScope ||
                            s == GlobalSymbols.staticScope ||
                            s == GlobalSymbols.thisScope ||
                            s instanceof ParamRef) {
                        // Escaping into either return, static, this or
                        // method argument scope
                        escaping = true;
                        break;
                    }
                }

                if (escaping) {
                    if (v instanceof ValueWithEscapeCheck) {
                        // We just mark the allocation as escaping to be sure here
                        ((ValueWithEscapeCheck) v).markAsEscaped();
                    }
                }
            }
        }
    }
}
