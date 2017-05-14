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

import java.io.UnsupportedEncodingException;

public class BytecodeUtf8Constant implements BytecodeConstant {

    private final String value;

    public BytecodeUtf8Constant(String aValue) {
        value = aValue;
    }

    public String stringValue() {
        return value;
    }

    public byte[] toUTF8Bytes() throws UnsupportedEncodingException {
        return value.getBytes("UTF-8");
    }
}