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

    private static final TThread MAIN = new TThread(null);

    public static TThread currentThread() {
        return MAIN;
    }

    private final Runnable runnable;

    public TThread() {
        runnable = null;
    }

    public TThread(final Runnable aRunnable) {
        runnable = aRunnable;
    }

    public TThread(final ThreadGroup group, final Runnable aRunnable, final String name, final long aPriority, final boolean daemon) {
        runnable = aRunnable;
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

    public static boolean interrupted() {
        return false;
    }

    public static void yield() {
    }

    public static void dumpStack() {
    }

    public static Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return null;
    }
}