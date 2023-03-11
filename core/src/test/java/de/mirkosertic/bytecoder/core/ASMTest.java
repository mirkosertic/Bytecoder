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
package de.mirkosertic.bytecoder.core;

import org.junit.Test;
import org.objectweb.asm.Type;

public class ASMTest {

    @Test
    public void testArrayType() {
        final Type t = Type.getType(byte[].class);
        System.out.println(t);
        System.out.println(t.getDescriptor());
        System.out.println(t.getDimensions());

        final Type t2 = Type.getType("[Ljava/lang/String;");
        System.out.println(t2);
        System.out.println(t2.getElementType());
        System.out.println(t2.getElementType().getClassName());
        System.out.println(t2.getDimensions());

        System.out.println("T3");
        final Type t3 = Type.getType("[[Ljava/lang/String;");
        System.out.println(t3);
        System.out.println(t3.getElementType());
        System.out.println(t3.getElementType().getClassName());
        System.out.println(t3.getDimensions());

        System.out.println(Type.getType(String[].class).getDescriptor());
    }
}
