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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.Permission;

@SubstitutesInClass(completeReplace = true)
public class TSecurityManager {

    public TSecurityManager() {
    }

    private boolean hasAllPermission() {
        return true;
    }

    public boolean getInCheck() {
        return false;
    }

    public Object getSecurityContext() {
        return AccessController.getContext();
    }

    public void checkPermission(final Permission perm) {
        java.security.AccessController.checkPermission(perm);
    }

    public void checkPermission(final Permission perm, final Object context) {
    }

    public void checkCreateClassLoader() {
    }

    private static ThreadGroup getRootGroup() {
        ThreadGroup root =  Thread.currentThread().getThreadGroup();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }

    public void checkAccess(final Thread t) {
    }

    public void checkAccess(final ThreadGroup g) {
    }

    public void checkExit(final int status) {
    }

    public void checkExec(final String cmd) {
    }

    public void checkLink(final String lib) {
    }

    public void checkRead(final FileDescriptor fd) {
    }

    public void checkRead(final String file) {
    }

    public void checkRead(final String file, final Object context) {
    }

    public void checkWrite(final FileDescriptor fd) {
    }

    public void checkWrite(final String file) {
    }

    public void checkDelete(final String file) {
    }

    public void checkConnect(final String host, final int port) {
    }

    public void checkConnect(final String host, final int port, final Object context) {
    }

    public void checkListen(final int port) {
    }

    public void checkAccept(final String host, final int port) {
    }

    public void checkMulticast(final InetAddress maddr) {
    }

    public void checkMulticast(final InetAddress maddr, final byte ttl) {
    }

    public void checkPropertiesAccess() {
    }

    public void checkPropertyAccess(final String key) {
    }

    public boolean checkTopLevelWindow(final Object window) {
        return hasAllPermission();
    }

    public void checkPrintJobAccess() {
    }

    public void checkSystemClipboardAccess() {
    }

    public void checkAwtEventQueueAccess() {
    }

    public void checkPackageAccess(final String pkg) {
    }

    public void checkPackageDefinition(final String pkg) {
    }

    public void checkSetFactory() {
    }

    public void checkMemberAccess(final Class<?> clazz, final int which) {
    }

    public void checkSecurityAccess(final String target) {
    }

    public ThreadGroup getThreadGroup() {
        return Thread.currentThread().getThreadGroup();
    }
}
