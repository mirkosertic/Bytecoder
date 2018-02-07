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

    public static Variable createThisRef() {
        Variable theVariable = new Variable(TypeRef.Native.REFERENCE, "thisRef", true);
        theVariable.initializeWith(new SelfReferenceParameterValue());
        return theVariable;
    }

    public static Variable createMethodParameter(int aIndex, TypeRef aTypeRef) {
        Variable theVariable = new Variable(aTypeRef, "p" + aIndex, true);
        theVariable.initializeWith(new MethodParameterValue(aIndex, aTypeRef));
        return theVariable;
    }

    public static Variable createMethodParameter(int aIndex, String aName, TypeRef aTypeRef) {
        Variable theVariable = new Variable(aTypeRef, aName, true);
        theVariable.initializeWith(new MethodParameterValue(aIndex, aTypeRef));
        return theVariable;
    }

    private final TypeRef type;
    private final String name;
    private final boolean synthetic;

    private Variable(TypeRef aType, String aName, boolean aSsynthetic) {
        type = aType;
        name = aName;
        synthetic = aSsynthetic;
    }

    public Variable(TypeRef aType, String aName) {
        this(aType, aName, false);
    }

    public void initializeWith(Value aValue) {
        // Test there is a videst type available
        type.resolve().eventuallyPromoteTo(aValue.resolveType().resolve());
        aValue.addEdgeTo(new DataFlowEdgeType(), this);
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