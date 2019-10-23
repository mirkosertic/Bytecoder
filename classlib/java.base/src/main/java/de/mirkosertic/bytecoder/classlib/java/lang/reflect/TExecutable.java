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

import java.lang.reflect.Type;

@SubstitutesInClass(completeReplace = true)
public class TExecutable {

    public String toGenericString() {
        return null;
    }

    public Class[] getParameterTypes() {
        return new Class[0];
    }

    public Type[] getGenericParameterTypes() {
        return new Type[0];
    }

    public Type getGenericReturnType() {
        return null;
    }

    public Class getReturnType() {
        return null;
    }

    public int getParameterCount() {
        return 0;
    }

    public Class[] getExceptionTypes() {
        return new Class[0];
    }
}
