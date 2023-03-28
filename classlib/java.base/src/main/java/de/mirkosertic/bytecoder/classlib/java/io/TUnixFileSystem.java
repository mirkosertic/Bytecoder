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

@SubstitutesInClass(completeReplace = true)
public class TUnixFileSystem {

    public TUnixFileSystem() {
    }

    public char getSeparator() {
        return '/';
    }

    public char getPathSeparator() {
        return ':';
    }

    private String normalize(final String pathname, final int len, final int off) {
        if (len == 0) {
            return pathname;
        } else {
            int n;
            for(n = len; n > 0 && pathname.charAt(n - 1) == '/'; --n) {
            }

            if (n == 0) {
                return "/";
            } else {
                final StringBuilder sb = new StringBuilder(pathname.length());
                if (off > 0) {
                    sb.append(pathname, 0, off);
                }

                char prevChar = 0;

                for(int i = off; i < n; ++i) {
                    final char c = pathname.charAt(i);
                    if (prevChar != '/' || c != '/') {
                        sb.append(c);
                        prevChar = c;
                    }
                }

                return sb.toString();
            }
        }
    }

    public String normalize(final String pathname) {
        final int n = pathname.length();
        char prevChar = 0;

        for(int i = 0; i < n; ++i) {
            final char c = pathname.charAt(i);
            if (prevChar == '/' && c == '/') {
                return normalize(pathname, n, i - 1);
            }

            prevChar = c;
        }

        if (prevChar == '/') {
            return normalize(pathname, n, n - 1);
        } else {
            return pathname;
        }
    }

    public int prefixLength(final String pathname) {
        if (pathname.isEmpty()) {
            return 0;
        } else {
            return pathname.charAt(0) == '/' ? 1 : 0;
        }
    }

    public String resolve(final String parent, final String child) {
        if (child.isEmpty()) {
            return parent;
        } else if (child.charAt(0) == '/') {
            return parent.equals("/") ? child : parent + child;
        } else {
            return parent.equals("/") ? parent + child : parent + '/' + child;
        }
    }

    public String getDefaultParent() {
        return "/";
    }

    public String fromURIPath(final String path) {
        String p = path;
        if (path.endsWith("/") && path.length() > 1) {
            p = path.substring(0, path.length() - 1);
        }

        return p;
    }

    public boolean isAbsolute(final File f) {
        return f.getName().startsWith("/");
    }

    public String resolve(final File f) {
        if (isAbsolute(f)) {
            return f.getPath();
        }
        return resolve(".", f.getPath());
    }

    public native String canonicalize(String path);

    private native int getBooleanAttributes0(String aFileName);

    public int getBooleanAttributes(final File f) {
        return getBooleanAttributes0(f.getPath());
    }

    public boolean checkAccess(final File f, final int attribute) {
        return (getBooleanAttributes0(f.getPath()) & attribute) > 0;
    }

    private native long getLastModifiedTime0(String aFileName);

    public long getLastModifiedTime(final File f) {
        return getLastModifiedTime0(f.getPath());
    }

    private native long getLength0(String aFileName);

    public long getLength(final File f) {
        return getLength0(f.getPath());
    }

    public boolean setPermission(final File f, final int access, final boolean enable, final boolean owneronly) {
        return false;
    }

    public boolean createFileExclusively(final String path) {
        return false;
    }

    public boolean delete(final File f) {
        return false;
    }

    public String[] list(final File var1) {
        return new String[0];
    }

    public boolean createDirectory(final File f) {
        return false;
    }

    public boolean rename(final File f1, final File f2) {
        return false;
    }

    public boolean setLastModifiedTime(final File var1, final long var2) {
        return false;
    }

    public boolean setReadOnly(final File var1) {
        return false;
    }

    public File[] listRoots() {
        return new File[]{new File("/")};
    }

    public native long getSpace(File var1, int var2);

    private native long getNameMax0(String var1);

    public int getNameMax(final String path) {
        long nameMax = getNameMax0(path);
        if (nameMax > 2147483647L) {
            nameMax = 2147483647L;
        }

        return (int)nameMax;
    }

    public int compare(final File f1, final File f2) {
        return f1.getPath().compareTo(f2.getPath());
    }

    public int hashCode(final File f) {
        return f.getPath().hashCode() ^ 1234321;
    }

    public boolean isInvalid(final File f) {
        return false;
    }
}
