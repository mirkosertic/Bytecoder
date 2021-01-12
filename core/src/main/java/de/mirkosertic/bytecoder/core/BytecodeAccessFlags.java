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
package de.mirkosertic.bytecoder.core;

public class BytecodeAccessFlags {

    private final int modifiers;

    public BytecodeAccessFlags(final int aValue) {
        modifiers = aValue;
    }

    public boolean isStatic() {
        return (modifiers & 0x0008) > 0;
    }

    public boolean isInterface() {
        return (modifiers & 0x0200) > 0;
    }

    public boolean isPublic() {
        return (modifiers & 0x0001) > 0;
    }

    public boolean isPrivate() {
        return (modifiers & 0x0002) > 0;
    }

    public boolean isProtected() {
        return (modifiers & 0x0004) > 0;
    }

    public boolean isFinal() {
        return (modifiers & 0x0010) > 0;
    }

    public boolean isAbstract() {
        return (modifiers & 0x0400) > 0;
    }

    public boolean isSyntetic() {
        return (modifiers & 0x1000) > 0;
    }

    public boolean isAnnotation() {
        return (modifiers & 0x2000) > 0;
    }

    public boolean isEnum() {
        return (modifiers & 0x4000) > 0;
    }

    public boolean isNative() {
        return (modifiers & 0x00000100) > 0;
    }

    public int getModifiers() {
        return modifiers;
    }
}