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
import java.io.OutputStream;

@SubstitutesInClass(completeReplace = true)
public class TFileOutputStream extends OutputStream {

    private final FileDescriptor fd;

    private final String path;

    private volatile boolean closed;

    public TFileOutputStream(final String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null, false);
    }

    public TFileOutputStream(final String name, final boolean append) throws FileNotFoundException {
        this(name != null ? new File(name) : null, append);
    }

    public TFileOutputStream(final File file) throws FileNotFoundException {
        this(file, false);
    }

    public TFileOutputStream(final File file, final boolean append) throws FileNotFoundException {
        final String name = (file != null ? file.getPath() : null);
        if (name == null) {
            throw new NullPointerException();
        }
        this.fd = new FileDescriptor();
        this.path = name;

        open(name, append);
    }

    public TFileOutputStream(final FileDescriptor fdObj) {
        this.fd = fdObj;
        this.path = null;
    }

    private native int open0(String name, boolean append) throws FileNotFoundException;

    private native void close0(int handle);

    private void open(final String name, final boolean append) throws FileNotFoundException {
        ((TFileDescriptor) (Object) fd).setHandle(open0(name, append));
    }

    public void write(final int b) throws IOException {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        writeInt(handle, b);
    }

    public native void writeInt(int handle, int b) throws IOException;

    private native void writeBytes(int handle, byte b[], int off, int len);

    public void write(final byte[] b) {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        writeBytes(handle, b, 0, b.length);
    }

    public void write(final byte[] b, final int off, final int len) {
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        writeBytes(handle, b, off, len);
    }

    public void close() {
        if (closed) {
            return;
        }
        final int handle = (int) ((TFileDescriptor) (Object) fd).getHandle();
        close0(handle);
        closed = true;
    }

    public final FileDescriptor getFD()  throws IOException {
        if (fd != null) {
            return fd;
        }
        throw new IOException();
    }
}
