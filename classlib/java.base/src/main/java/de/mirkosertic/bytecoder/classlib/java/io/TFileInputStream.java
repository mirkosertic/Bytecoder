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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@SubstitutesInClass(completeReplace = true)
public class TFileInputStream extends InputStream {

    /* File Descriptor - handle to the open file */
    private final FileDescriptor fd;

    /**
     * The path of the referenced file
     * (null if the stream is created with a file descriptor)
     */
    private final String path;

    private volatile boolean closed;

    public TFileInputStream(final String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null);
    }

    public TFileInputStream(final File file) throws FileNotFoundException {
        final String name = (file != null ? file.getPath() : null);
        if (name == null) {
            throw new NullPointerException();
        }
        fd = new FileDescriptor();
        path = name;
        open(name);
    }

    public TFileInputStream(final FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        fd = fdObj;
        path = null;
    }

    /**
     * Opens the specified file for reading.
     * @param name the name of the file
     */
    private native int open0(String name) throws FileNotFoundException;

    // wrap native call to allow instrumentation
    /**
     * Opens the specified file for reading.
     * @param name the name of the file
     */
    private void open(final String name) throws FileNotFoundException {
        final long handle = open0(name);
        if (handle < 0) {
            throw new FileNotFoundException(name);
        }
        ((TFileDescriptor) (Object) fd).setHandle(handle);
    }

    public int read() throws IOException {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        return read0(handle);
    }

    private native int read0(int handle) throws IOException;

    private native int readBytes(int handle, byte b[], int off, int len) throws IOException;

    public int read(final byte[] b) throws IOException {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        return readBytes(handle, b, 0, b.length);
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        return readBytes(handle, b, off, len);
    }

    public long skip(final long n) throws IOException {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        return skip0(handle, (int) n);
    }

    private native long skip0(int handle, int n) throws IOException;

    public int available() throws IOException {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        return available0(handle);
    }

    private native int available0(int handle) throws IOException;

    public void close() {
        if (closed) {
            return;
        }
        if (closed) {
            return;
        }
        closed = true;
    }

    public final FileDescriptor getFD() throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
    }
}
