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
package de.mirkosertic.bytecoder.classlib.jdk.internal.misc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TVM {

    // the init level when the VM is fully initialized
    private static final int JAVA_LANG_SYSTEM_INITED     = 1;
    private static final int MODULE_SYSTEM_INITED        = 2;
    private static final int SYSTEM_LOADER_INITIALIZING  = 3;
    private static final int SYSTEM_BOOTED               = 4;
    private static final int SYSTEM_SHUTDOWN             = 5;


    // 0, 1, 2, ...
    private static volatile int initLevel;
    private static final Object lock = new Object();

    public static void initLevel(final int value) {
        synchronized (lock) {
            if (value <= initLevel || value > SYSTEM_SHUTDOWN)
                throw new InternalError("Bad level: " + value);
            initLevel = value;
            lock.notifyAll();
        }
    }

    public static int initLevel() {
        return initLevel;
    }

    public static void awaitInitLevel(final int value) throws InterruptedException {
        synchronized (lock) {
            while (initLevel < value) {
                lock.wait();
            }
        }
    }

    public static boolean isModuleSystemInited() {
        return initLevel() >= MODULE_SYSTEM_INITED;
    }

    public static boolean isBooted() {
        return initLevel >= SYSTEM_BOOTED;
    }

    public static void shutdown() {
        initLevel(SYSTEM_SHUTDOWN);
    }

    public static boolean isShutdown() {
        return initLevel == SYSTEM_SHUTDOWN;
    }

    private static long directMemory = 64 * 1024 * 1024;

    public static long maxDirectMemory() {
        return directMemory;
    }

    private static boolean pageAlignDirectMemory;

    public static boolean isDirectMemoryPageAligned() {
        return pageAlignDirectMemory;
    }

    public static boolean isSystemDomainLoader(final ClassLoader loader) {
        return true;
    }

    public static String getSavedProperty(final String key) {
        if (savedProps == null)
            throw new IllegalStateException("Not yet initialized");

        return savedProps.get(key);
    }

    public static Map<String, String> getSavedProperties() {
        if (savedProps == null)
            throw new IllegalStateException("Not yet initialized");

        return Collections.unmodifiableMap(savedProps);
    }

    private static Map<String, String> savedProps;

    // Save a private copy of the system properties and remove
    // the system properties that are not intended for public access.
    //
    // This method can only be invoked during system initialization.
    public static void saveProperties(final Map<String, String> props) {
        if (initLevel() != 0)
            throw new IllegalStateException("Wrong init level");

        // only main thread is running at this time, so savedProps and
        // its content will be correctly published to threads started later
        if (savedProps == null) {
            savedProps = props;
        }
    }

    // Initialize any miscellaneous operating system settings that need to be
    // set for the class libraries.
    //
    public static void initializeOSEnvironment() {
        if (initLevel() == 0) {
        }
    }

    /* Current count of objects pending for finalization */
    private static volatile int finalRefCount;

    /* Peak count of objects pending for finalization */
    private static volatile int peakFinalRefCount;

    public static int getFinalRefCount() {
        return finalRefCount;
    }

    public static int getPeakFinalRefCount() {
        return peakFinalRefCount;
    }

    public static void addFinalRefCount(final int n) {
        // The caller must hold lock to synchronize the update.

        finalRefCount += n;
        if (finalRefCount > peakFinalRefCount) {
            peakFinalRefCount = finalRefCount;
        }
    }

    public static ClassLoader latestUserDefinedLoader() {
        return null;
    }

    private static ClassLoader latestUserDefinedLoader0() {
        return null;
    }

    public static boolean isSetUID() {
        final long uid = getuid();
        final long euid = geteuid();
        final long gid = getgid();
        final long egid = getegid();
        return uid != euid  || gid != egid;
    }

    public static native long getuid();

    public static native long geteuid();

    public static native long getgid();

    public static native long getegid();

    public static native long getNanoTimeAdjustment(long offsetInSeconds);

    public static native String[] getRuntimeArguments();

    static {
        saveProperties(new HashMap<>());
    }

    public static void initializeFromArchive(final Class<?> c) {
    }

    public static long getRandomSeedForCDSDump() {
        return 0L;
    }

    public static int classFileVersion() {
        return 64;
    }
}
