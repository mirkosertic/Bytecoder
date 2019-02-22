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
package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.OpaqueIndexed;
import de.mirkosertic.bytecoder.api.OpaqueProperty;

public abstract class Int8Array implements TypedArray {

    @OpaqueIndexed
    public abstract byte get(int aIndex);

    @OpaqueIndexed
    public abstract void set(int aIndex, byte aValue);

    @OpaqueProperty("length")
    public abstract int byteArrayLength();
}