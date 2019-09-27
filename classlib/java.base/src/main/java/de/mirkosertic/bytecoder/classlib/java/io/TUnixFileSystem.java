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
import java.io.IOException;

@SubstitutesInClass(completeReplace = true)
public class TUnixFileSystem {

    public char getSeparator() {
        return '/';
    }

    public char getPathSeparator() {
        return ':';
    }

    public String normalize(final String path) {
        return path;
    }

    public int prefixLength(final String path) {
        return 0;
    }

    public String resolve(final String parent, final String child) {
        return null;
    }

    public String getDefaultParent() {
        return null;
    }

    public String fromURIPath(final String path) {
        return null;
    }

    public boolean isAbsolute(final File f) {
        return false;
    }

    public String resolve(final File f) {
        return null;
    }

    public String canonicalize(final String path) throws IOException {
        return null;
    }

    public int getBooleanAttributes(final File f) {
        return 0;
    }

    public boolean checkAccess(final File f, final int access) {
        return false;
    }

    public boolean setPermission(final File f, final int access, final boolean enable, final boolean owneronly) {
        return false;
    }

    public long getLastModifiedTime(final File f) {
        return 0;
    }

    public long getLength(final File f) {
        return 0;
    }

    public boolean createFileExclusively(final String pathname) throws IOException {
        return false;
    }

    public boolean delete(final File f) {
        return false;
    }

    public String[] list(final File f) {
        return new String[0];
    }

    public boolean createDirectory(final File f) {
        return false;
    }

    public boolean rename(final File f1, final File f2) {
        return false;
    }

    public boolean setLastModifiedTime(final File f, final long time) {
        return false;
    }

    public boolean setReadOnly(final File f) {
        return false;
    }

    public File[] listRoots() {
        return new File[0];
    }

    public long getSpace(final File f, final int t) {
        return 0;
    }

    public int getNameMax(final String path) {
        return 0;
    }

    public int compare(final File f1, final File f2) {
        return 0;
    }

    public int hashCode(final File f) {
        return 0;
    }
}
