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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TThread implements Runnable {

    public static Thread MAIN_THREAD;

    public static Thread currentThread() {
        if (MAIN_THREAD == null) {
            final ThreadGroup main = new ThreadGroup((ThreadGroup) (Object) TThreadGroup.SYSTEM, "main");
            MAIN_THREAD = new Thread(main, "main");
        }
        return MAIN_THREAD;
    }

    private final Runnable runnable;
    private final ThreadGroup threadGroup;
    private String name;
    private ClassLoader contextClassLoader;

    public TThread() {
        runnable = null;
        threadGroup = (ThreadGroup) (Object) TThreadGroup.SYSTEM;
        name = "UNKNOWN";
    }

    public TThread(final Runnable aRunnable) {
        threadGroup = (ThreadGroup) (Object) TThreadGroup.SYSTEM;
        runnable = aRunnable;
        name = "UNKNOWN";
    }

    public TThread(final ThreadGroup group, final Runnable aRunnable, final String aName, final long aPriority, final boolean daemon) {
        threadGroup = group;
        runnable = aRunnable;
        name = aName;
    }

    public TThread(final ThreadGroup group, final Runnable aRunnable, final String aName) {
        threadGroup = group;
        runnable = aRunnable;
        name = aName;
    }

    public TThread(final ThreadGroup group, final String aName) {
        threadGroup = group;
        runnable = null;
        name = aName;
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(final String aName) {
        name = aName;
    }

    public void start() {
    }

    public void stop() {
    }

    public void interrupt() {
    }

    public static boolean interrupted() {
        return currentThread().isInterrupted();
    }

    @Override
    public void run() {
        runnable.run();
    }

    public static boolean holdsLock(final Object obj) {
        return true;
    }

    public static void sleep(final long duration) {
    }

    public boolean isInterrupted() {
        return false;
    }

    public static void yield() {
    }

    public static void dumpStack() {
    }

    public void setDaemon(final boolean flag) {
    }

    public void setPriority(final int value) {
    }

    public void join(final long aTimeout) {
    }

    public void join() {
    }

    public boolean isAlive() {
        return true;
    }

    public static Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return null;
    }

    public void setContextClassLoader(final ClassLoader aLoader) {
        contextClassLoader = aLoader;
    }

    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return null;
    }

    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    public boolean isVirtual() {
        return false;
    }
}
