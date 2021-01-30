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

import java.util.Objects;

public class ImportReference {

    private final String moduleName;
    private final String objectName;

    public ImportReference(final String moduleName, final String objectName) {
        this.moduleName = moduleName;
        this.objectName = objectName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        final ImportReference that = (ImportReference) o;
        return Objects.equals(moduleName, that.moduleName) &&
                Objects.equals(objectName, that.objectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, objectName);
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getObjectName() {
        return objectName;
    }
}
