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
package de.mirkosertic.bytecoder.classlib.java.io;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.IOException;
import java.io.OutputStream;

@SubstitutesInClass(completeReplace = true)
public class TObjectOutputStream extends OutputStream {

    private final OutputStream delegate;

    public TObjectOutputStream(final OutputStream os) {
        delegate = os;
    }

    @Override
    public void write(final byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void write(final int b) throws IOException {
        delegate.write(b);
    }
}
