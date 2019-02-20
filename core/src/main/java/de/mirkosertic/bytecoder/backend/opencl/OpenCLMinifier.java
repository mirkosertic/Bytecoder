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
package de.mirkosertic.bytecoder.backend.opencl;

import de.mirkosertic.bytecoder.backend.Minifier;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;

public class OpenCLMinifier implements Minifier {

    @Override
    public String toClassName(final BytecodeObjectTypeRef aTypeRef) {
        throw new IllegalArgumentException("Not implemented!");
    }

    @Override
    public String toClassName(final BytecodeClassinfoConstant aTypeRef) {
        throw new IllegalArgumentException("Not implemented!");
    }

    @Override
    public String toMethodName(final String aMethodName, final BytecodeMethodSignature aSignature) {
        throw new IllegalArgumentException("Not implemented!");
    }

    @Override
    public String typeRefToString(final BytecodeTypeRef aTypeRef) {
        throw new IllegalArgumentException("Not implemented!");
    }

    @Override
    public String toVariableName(final String aVariable) {
        throw new IllegalArgumentException("Not implemented!");
    }
}
