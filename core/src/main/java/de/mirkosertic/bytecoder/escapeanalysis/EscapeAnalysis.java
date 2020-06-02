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

import de.mirkosertic.bytecoder.pointsto.AllocationSymbol;
import de.mirkosertic.bytecoder.pointsto.PointsToAnalysisResult;
import de.mirkosertic.bytecoder.ssa.ProgramDescriptor;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.ValueWithEscapeCheck;

public class EscapeAnalysis {
    public void analyze(final ProgramDescriptor aProgramDescriptor, final PointsToAnalysisResult aAnalysisResult) {
        for (final AllocationSymbol allocation : aAnalysisResult.allocationSymbols()) {
            final Value v = allocation.value();

            // TODO: Implement EA here

            if (v instanceof ValueWithEscapeCheck) {
                // We just mark the allocation as escaping to be sure here
                ((ValueWithEscapeCheck) v).markAsEscaped();
            }
        }
    }
}
