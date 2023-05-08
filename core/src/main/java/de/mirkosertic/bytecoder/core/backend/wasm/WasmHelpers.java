/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.backend.wasm;

import de.mirkosertic.bytecoder.core.backend.VTable;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ConstExpressions;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.ExportableFunction;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Iff;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Module;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.Param;
import de.mirkosertic.bytecoder.core.backend.wasm.ast.PrimitiveType;
import de.mirkosertic.bytecoder.core.ir.ResolvedClass;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WasmHelpers {

    public final static int TYPE_ID_RUNTIMECLASS = -1;

    public final static int TYPE_ID_BOOLEAN = -10;
    public static final int TYPE_ID_BYTE = -20;
    public static final int TYPE_ID_CHAR = -21;
    public static final int TYPE_ID_SHORT = -22;
    public static final int TYPE_ID_INT = -23;
    public static final int TYPE_ID_LONG = -24;
    public static final int TYPE_ID_FLOAT = -25;
    public static final int TYPE_ID_DOUBLE = -26;
    public static final int TYPE_ID_VOID = -27;

    public static String generateClassName(final Type type) {
        return type.getClassName().replace('.', '$');
    }

    public static String generateFieldName(final String name) {
        return name;
    }

    public static String generateMethodName(final String name, final Type methodType) {
        final StringBuilder builder = new StringBuilder(methodType.getReturnType().toString());
        builder.append("$").append(name);
        for (final Type arg : methodType.getArgumentTypes()) {
            builder.append("$").append(arg);
        }
        if (methodType.getArgumentTypes().length == 0) {
            builder.append("$$");
        }
        return builder.toString()
                .replace('<', '$')
                .replace('>', '$')
                .replace('/', '$')
                .replace(';', '$')
                .replace('[', '$');
    }

    public static ExportableFunction createVTableResolver(final Module module, final String methodName, final Map<Integer, String> methods) {
        final List<Param> params = new ArrayList<>();
        params.add(ConstExpressions.param("methodid", PrimitiveType.i32));
        final ExportableFunction vtFunction = module.getFunctions().newFunction(methodName, params, PrimitiveType.i32);

        for (final Map.Entry<Integer, String> entry : methods.entrySet()) {
            final int methodId = entry.getKey();

            final String implMethodName = entry.getValue();

            final Iff iff = vtFunction.flow.iff("check_" + methodId, ConstExpressions.i32.eq(
                    ConstExpressions.i32.c(methodId),
                    ConstExpressions.getLocal(vtFunction.localByLabel("methodid"))
            ));
            iff.flow.ret(ConstExpressions.weakFunctionTableReference(implMethodName));
        }

        vtFunction.flow.unreachable();
        // Forward declaration by putting it to the table
        vtFunction.toTable();

        return vtFunction;
    }

    public static ExportableFunction createVTableResolver(final Module module, final ResolvedClass resolvedClass, final VTable vTable) {
        final Map<Integer, String> implMethods = new HashMap<>();
        for (final Map.Entry<Integer, ResolvedMethod> entry : vTable.getMethods().entrySet()) {
            final ResolvedMethod rm = entry.getValue();
            final int methodId = entry.getKey();

            final String ownerClassName = WasmHelpers.generateClassName(rm.owner.type);
            final String methodName = WasmHelpers.generateMethodName(rm.methodNode.name, rm.methodType);
            implMethods.put(methodId, ownerClassName + "$" + methodName);
        }

        final String className = generateClassName(resolvedClass.type);

        return createVTableResolver(module,
                className + "_vt",
                implMethods
        );
    }
}
