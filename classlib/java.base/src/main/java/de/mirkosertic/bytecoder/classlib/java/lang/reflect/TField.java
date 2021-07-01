/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang.reflect;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

@SubstitutesInClass(completeReplace = true)
public class TField {

    private final Class declaredClass;
    private final String name;
    private final int modifiers;
    private final Class type;
    @SuppressWarnings("FieldCanBeLocal")
    private final Object accessorMethod;
    @SuppressWarnings("FieldCanBeLocal")
    private final Object mutationMethod;
    private final int offset;

    public TField(final Class declaredClass, final String name, final int modifiers, final Class type, final Object accessorMethod, final Object mutationMethod, final int offset) {
        this.declaredClass = declaredClass;
        this.name = name;
        this.modifiers = modifiers;
        this.type = type;
        this.accessorMethod = accessorMethod;
        this.mutationMethod = mutationMethod;
        this.offset = offset;
    }

    public String getName() {
        return name;
    }

    public Class getDeclaringClass() {
        return declaredClass;
    }

    public int getModifiers() {
        return modifiers;
    }

    public Object get(final Object o) {
        if (Modifier.isStatic(modifiers)) {
            return VM.getObjectFromStaticField(declaredClass, (Field) (Object) this);
        }
        return VM.getObjectFromInstanceField(o, (Field) (Object) this);
    }

    public void set(final Object o, final Object value) {
        if (Modifier.isStatic(modifiers)) {
            VM.putObjectToStaticField(declaredClass, (Field) (Object) this, value);
        } else {
            VM.putObjectToInstanceField(o, (Field) (Object) this, value);
        }
    }

    public Class getType() {
        return type;
    }

    public boolean isAccessible() {
        return true;
    }

    public void setAccessible(final boolean a){
    }

    public Type getGenericType() {
        return null;
    }
}
