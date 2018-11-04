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
package de.mirkosertic.bytecoder.backend;

import org.apache.commons.io.output.WriterOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

public interface CompileResult<T> {

    interface Content {
        String getFileName();

        void writeTo(OutputStream stream) throws IOException;

        default String asString() throws IOException {
            final StringWriter strData = new StringWriter();
            try (final WriterOutputStream wos = new WriterOutputStream(strData, Charset.defaultCharset())) {
                writeTo(wos);
            }
            return strData.toString();
        }
    }

    Content[] getContent();
}

