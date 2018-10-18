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
package de.mirkosertic.bytecoder.backend.wasm.ast;

import java.io.IOException;

public class SImport implements SValue {

    private final String moduleName;
    private final String objectName;
    private final SImportable importable;

    public SImport(final String moduleName, final String objectName, final SImportable importable) {
        this.moduleName = moduleName;
        this.objectName = objectName;
        this.importable = importable;
    }

    @Override
    public void writeTo(final STextWriter textWriter) throws IOException {
        textWriter.opening();
        textWriter.write("import");
        textWriter.space();
        textWriter.writeText(moduleName);
        textWriter.space();
        textWriter.writeText(objectName);
        textWriter.space();
        importable.writeTo(textWriter);
        textWriter.closing();
    }
}
