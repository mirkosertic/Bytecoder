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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public abstract class CompileResult<T> {

    public interface Content {
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

    public static class URLContent implements Content {

        private final String fileName;
        private final URL url;

        public URLContent(final String fileName, final URL url) {
            this.fileName = fileName;
            this.url = url;
        }

        @Override
        public String getFileName() {
            return fileName;
        }

        @Override
        public void writeTo(OutputStream stream) throws IOException {
            try (InputStream is = url.openStream()) {
                IOUtils.copy(is, stream);
            }
        }

        @Override
        public String asString() {
            throw new IllegalStateException("Not implemented!");
        }
    }

    private final List<Content> content;

    public CompileResult() {
        this.content = new ArrayList<>();
    }

    public void add(final Content aContent) {
        content.add(aContent);
    }

    public Content[] getContent() {
        return content.toArray(new Content[0]);
    }
}

