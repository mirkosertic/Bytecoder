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
package de.mirkosertic.bytecoder.asm.backend;

public class OpaqueReferenceTypeHelpers {

    public static String derivePropertyNameFromMethodName(String methodName) {
        if (methodName.startsWith("get")) {
            methodName = methodName.substring(3);
            methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
        } else if (methodName.startsWith("is")) {
            methodName = methodName.substring(2);
            methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
        } else if (methodName.startsWith("set")) {
            methodName = methodName.substring(3);
            methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
        }

        return methodName;
    }
}
