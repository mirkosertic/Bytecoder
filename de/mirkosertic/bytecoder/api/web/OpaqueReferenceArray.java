/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.OpaqueIndexed;
import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;

public abstract class OpaqueReferenceArray<T extends OpaqueReferenceType> implements OpaqueReferenceType {

    @OpaqueIndexed
    public abstract T get(int aIndex);

    @OpaqueIndexed
    public abstract void set(int aIndex, T aValue);

    public abstract void push(T aValue);

    public abstract T pop();

    @OpaqueProperty("length")
    public abstract int objectArrayLength();
}