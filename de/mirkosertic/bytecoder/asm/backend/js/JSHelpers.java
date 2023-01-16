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
package de.mirkosertic.bytecoder.asm.backend.js;

import org.objectweb.asm.Type;

public class JSHelpers {

    public static String generateClassName(final Type type) {
        return type.getClassName().replace('.', '$');
    }

    public static String generateFieldName(final String name) {
        return name;
    }

    public static String generateMethodName(final String name, final Type[] argumentTypes) {
        final StringBuilder builder = new StringBuilder(name);
        for (final Type arg : argumentTypes) {
            builder.append("$").append(arg);
        }
        if (argumentTypes.length == 0) {
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
