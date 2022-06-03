/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.backend.cpp;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.Minifier;
import de.mirkosertic.bytecoder.classlib.Array;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.ssa.TypeRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CPPMinifier implements Minifier {

    private static final String DOUBLE_TYPE = "double";
    private static final String SHORT_TYPE = "short";
    private static final String FLOAT_TYPE = "float";
    private static final String VOID_TYPE = "void";
    private static final String LONG_TYPE = "long";
    private static final String CHAR_TYPE = "unsigned short";
    private static final String BYTE_TYPE = "short";
    private static final String INT_TYPE = "int";
    private static final String BOOLEAN_TYPE = "bool";
    private static final String OBJECT_TYPE = "jlObject*";

    private final CompileOptions options;
    private final Set<String> staticDependencies;

    public CPPMinifier(final CompileOptions options) {
        this.options = options;
        this.staticDependencies = new HashSet<>();
    }

    public List<String> staticDependencies() {
        return new ArrayList<>(staticDependencies).stream().sorted().collect(Collectors.toList());
    }

    public String toSymbol(final String aValue) {
        return aValue;
    }

    @Override
    public String toClassName(final BytecodeObjectTypeRef aTypeRef) {
        if (aTypeRef.name().endsWith(";")) {
            // This seems to be an array
            return toClassName(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
        }
        return toClassNameInternal(aTypeRef.name());
    }

    @Override
    public String toClassName(final BytecodeClassinfoConstant aTypeRef) {
        return toClassNameInternal(aTypeRef.getConstant().stringValue().replace("/", "."));
    }

    private String toClassNameInternal(final String aClassName) {
        final int p = aClassName.lastIndexOf(".");
        final String simpleName = aClassName.substring(p + 1);
        String packageName = aClassName.substring(0, p);
        final StringBuilder result = new StringBuilder();
        while (packageName.length() > 0) {
            result.append(Character.toLowerCase(packageName.charAt(0)));
            final int j = packageName.indexOf(".");
            if (j >= 0) {
                packageName = packageName.substring(j + 1);
            } else {
                packageName = "";
            }
        }

        return result.append(simpleName).toString();
    }

    @Override
    public String toMethodName(final String aMethodName, final BytecodeMethodSignature aSignature) {
        String name = typeRefToString(aSignature.getReturnType()).replace("*", "").replace(" ", "").replace("[", "").replace("]", "");
        name += aMethodName.replace("<", "").replace(">", "");
        for (final BytecodeTypeRef typeRef : aSignature.getArguments()) {
            name += typeRefToString(typeRef).replace("*", "").replace(" ", "").replace("[", "").replace("]", "");
        }
        return name;
    }

    public String typeRefToString(final TypeRef aTypeRef) {
        if (aTypeRef instanceof TypeRef.Native) {
            switch ((TypeRef.Native) aTypeRef) {
                case DOUBLE:
                    return DOUBLE_TYPE;
                case SHORT:
                    return SHORT_TYPE;
                case FLOAT:
                    return FLOAT_TYPE;
                case VOID:
                    return VOID_TYPE;
                case LONG:
                    return LONG_TYPE;
                case CHAR:
                    return CHAR_TYPE;
                case BYTE:
                    return BYTE_TYPE;
                case INT:
                    return INT_TYPE;
                case BOOLEAN:
                    return BOOLEAN_TYPE;
                case REFERENCE:
                    return OBJECT_TYPE;
            }
            throw new IllegalStateException("Not supported datatype : " + aTypeRef);
        } else if (aTypeRef instanceof TypeRef.ObjectTypeRef) {
            final TypeRef.ObjectTypeRef b = (TypeRef.ObjectTypeRef) aTypeRef;
            return typeRefToString(b.objectType());
        } else if (aTypeRef instanceof TypeRef.ArrayTypeRef) {
            final TypeRef.ArrayTypeRef b = (TypeRef.ArrayTypeRef) aTypeRef;
            return "Array<" + typeRefToString(b.arrayType()) + ">";
        }
        throw new IllegalStateException("Not supported datatype : " + aTypeRef);
    }

    @Override
    public String typeRefToString(final BytecodeTypeRef aTypeRef) {
        if (aTypeRef.isPrimitive()) {
            final BytecodePrimitiveTypeRef primitive = (BytecodePrimitiveTypeRef) aTypeRef;
            switch (primitive) {
                case INT:
                    return INT_TYPE;
                case BYTE:
                    return BYTE_TYPE;
                case CHAR:
                    return CHAR_TYPE;
                case LONG:
                    return LONG_TYPE;
                case VOID:
                    return VOID_TYPE;
                case FLOAT:
                    return FLOAT_TYPE;
                case SHORT:
                    return SHORT_TYPE;
                case DOUBLE:
                    return DOUBLE_TYPE;
                case BOOLEAN:
                    return BOOLEAN_TYPE;
            }
            throw new IllegalStateException("Not supported datatype : " + primitive);
        }
        if (aTypeRef.isArray()) {
            final BytecodeArrayTypeRef ref = (BytecodeArrayTypeRef) aTypeRef;
            return typeRefToString(ref.getType());
        }
        final BytecodeObjectTypeRef objectRef = (BytecodeObjectTypeRef) aTypeRef;
        final String className = toClassName(objectRef);
        staticDependencies.add(className);
        return className + "*";
    }

    @Override
    public String toVariableName(final String aVariable) {
        return aVariable;
    }

    public String tab() {
        if (options.isMinify()) {
            return "";
        }
        return "    ";
    }

    public String space() {
        if (options.isMinify()) {
            return "";
        }
        return " ";
    }

    public String newLine() {
        if (options.isMinify()) {
            return "";
        }
        return System.lineSeparator();
    }
}