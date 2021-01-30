/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.ssa;

public class Variable extends Value {

    public static final String THISREF_NAME = "__tr";

    public static Variable createThisRef(final TypeRef aThisType) {
        final Variable theVariable = new Variable(aThisType, THISREF_NAME, true, 0);
        theVariable.initializeWith(new SelfReferenceParameterValue(), 0);
        return theVariable;
    }

    public static Variable createMethodParameter(final int aIndex, final TypeRef aTypeRef) {
        final Variable theVariable = new Variable(aTypeRef, "p" + aIndex, true, 0);
        theVariable.initializeWith(new MethodParameterValue(aIndex, aTypeRef), 0);
        return theVariable;
    }

    public static Variable createMethodParameter(final int aIndex, final String aName, final TypeRef aTypeRef) {
        final Variable theVariable = new Variable(aTypeRef, aName, true, 0);
        theVariable.initializeWith(new MethodParameterValue(aIndex, aTypeRef), 0);
        return theVariable;
    }

    private final TypeRef type;
    private final String name;
    private final boolean synthetic;
    private final LiveRange liveRange;

    private Variable(final TypeRef aType, final String aName, final boolean aSynthetic, final long aDefinedAt) {
        type = aType;
        name = aName;
        synthetic = aSynthetic;
        liveRange = new LiveRange(aDefinedAt, aDefinedAt);
    }

    public Variable(final TypeRef aType, final String aName, final long definedAt) {
        this(aType, aName, false, definedAt);
    }

    public LiveRange liveRange() {
        return liveRange;
    }

    public void initializeWith(final Value aValue, final long analysisTime) {
        // Test there is a widest type available
        type.resolve().eventuallyPromoteTo(aValue.resolveType().resolve());
        aValue.addEdgeTo(DataFlowEdgeType.instance, this);
        liveRange.usedAt(analysisTime);

        markUsageIn(aValue, analysisTime);
    }

    private void markUsageIn(final Value aValue, final long analysisTime) {
        if (aValue instanceof Variable) {
            ((Variable) aValue).liveRange().usedAt(analysisTime);
        } else {
            for (final Value theValue : aValue.incomingDataFlows()) {
                markUsageIn(theValue, analysisTime);
            }
        }
    }

    @Override
    public TypeRef resolveType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " of type " + resolveType();
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}