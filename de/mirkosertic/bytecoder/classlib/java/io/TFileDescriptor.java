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

import de.mirkosertic.bytecoder.api.AnyTypeMatches;
import de.mirkosertic.bytecoder.api.Export;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.Closeable;
import java.io.FileDescriptor;

@SubstitutesInClass(completeReplace = true)
public class TFileDescriptor {

    public static final FileDescriptor in = (FileDescriptor) (Object) new TFileDescriptor(0);

    public static final FileDescriptor out = (FileDescriptor) (Object) new TFileDescriptor(1);

    public static final FileDescriptor err = (FileDescriptor) (Object) new TFileDescriptor(2);

    private final int num;
    private long handle;

    public TFileDescriptor() {
        num = -1;
        handle = -1;
    }

    TFileDescriptor(final int aNum) {
        num = aNum;
        handle = -1;
    }

    public long getHandle() {
        return handle;
    }

    public void setHandle(final long handle) {
        this.handle = handle;
    }

    public void attach(final Closeable aClosable) {
    }

    public void close() {
    }

    public void closeAll(final Closeable aClosable) {
    }

    public void registerCleanup(final AnyTypeMatches aCleanable) {
    }

    public boolean valid() {
        return true;
    }

    @Export("initDefaultFileHandles")
    public static void initDefaultFileHandles(final int stdIn, final int stdOut, final int stdErr) {
        ((TFileDescriptor) (Object) FileDescriptor.in).setHandle(stdIn);
        ((TFileDescriptor) (Object) FileDescriptor.out).setHandle(stdOut);
        ((TFileDescriptor) (Object) FileDescriptor.err).setHandle(stdErr);
    }
}
