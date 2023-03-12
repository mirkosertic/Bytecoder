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

import org.objectweb.asm.Type;

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
}
