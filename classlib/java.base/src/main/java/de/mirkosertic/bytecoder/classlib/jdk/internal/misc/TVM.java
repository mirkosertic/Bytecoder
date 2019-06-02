package de.mirkosertic.bytecoder.classlib.jdk.internal.misc;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    /*
     * Gets the number of objects pending for finalization.
     *
     * @return the number of objects pending for finalization.
     */
    public static int getFinalRefCount() {
        return finalRefCount;
    }

    /*
     * Gets the peak number of objects pending for finalization.
     *
     * @return the peak number of objects pending for finalization.
     */
    public static int getPeakFinalRefCount() {
        return peakFinalRefCount;
    }

    /*
     * Add {@code n} to the objects pending for finalization count.
     *
     * @param n an integer value to be added to the objects pending
     * for finalization count
     */
    public static void addFinalRefCount(final int n) {
        // The caller must hold lock to synchronize the update.

        finalRefCount += n;
        if (finalRefCount > peakFinalRefCount) {
            peakFinalRefCount = finalRefCount;
        }
    }

    /*
     * Returns the first user-defined class loader up the execution stack,
     * or the platform class loader if only code from the platform or
     * bootstrap class loader is on the stack.
     */
    public static ClassLoader latestUserDefinedLoader() {
        return null;
    }

    /*
     * Returns the first user-defined class loader up the execution stack,
     * or null if only code from the platform or bootstrap class loader is
     * on the stack.  VM does not keep a reference of platform loader and so
     * it returns null.
     *
     * This method should be replaced with StackWalker::walk and then we can
     * remove the logic in the VM.
     */
    private static ClassLoader latestUserDefinedLoader0() {
        return null;
    }

    /**
     * Returns {@code true} if we are in a set UID program.
     */
    public static boolean isSetUID() {
        final long uid = getuid();
        final long euid = geteuid();
        final long gid = getgid();
        final long egid = getegid();
        return uid != euid  || gid != egid;
    }

    /**
     * Returns the real user ID of the calling process,
     * or -1 if the value is not available.
     */
    public static native long getuid();

    /**
     * Returns the effective user ID of the calling process,
     * or -1 if the value is not available.
     */
    public static native long geteuid();

    /**
     * Returns the real group ID of the calling process,
     * or -1 if the value is not available.
     */
    public static native long getgid();

    /**
     * Returns the effective group ID of the calling process,
     * or -1 if the value is not available.
     */
    public static native long getegid();

    /**
     * Get a nanosecond time stamp adjustment in the form of a single long.
     *
     * This value can be used to create an instant using
     * {@link java.time.Instant#ofEpochSecond(long, long)
     *  java.time.Instant.ofEpochSecond(offsetInSeconds,
     *  getNanoTimeAdjustment(offsetInSeconds))}.
     * <p>
     * The value returned has the best resolution available to the JVM on
     * the current system.
     * This is usually down to microseconds - or tenth of microseconds -
     * depending on the OS/Hardware and the JVM implementation.
     *
     * @param offsetInSeconds The offset in seconds from which the nanosecond
     *        time stamp should be computed.
     *
     * @apiNote The offset should be recent enough - so that
     *         {@code offsetInSeconds} is within {@code +/- 2^32} seconds of the
     *         current UTC time. If the offset is too far off, {@code -1} will be
     *         returned. As such, {@code -1} must not be considered as a valid
     *         nano time adjustment, but as an exception value indicating
     *         that an offset closer to the current time should be used.
     *
     * @return A nanosecond time stamp adjustment in the form of a single long.
     *     If the offset is too far off the current time, this method returns -1.
     *     In that case, the caller should call this method again, passing a
     *     more accurate offset.
     */
    public static native long getNanoTimeAdjustment(long offsetInSeconds);

    /**
     * Returns the VM arguments for this runtime environment.
     *
     * @implNote
     * The HotSpot JVM processes the input arguments from multiple sources
     * in the following order:
     * 1. JAVA_TOOL_OPTIONS environment variable
     * 2. Options from JNI Invocation API
     * 3. _JAVA_OPTIONS environment variable
     *
     * If VM options file is specified via -XX:VMOptionsFile, the vm options
     * file is read and expanded in place of -XX:VMOptionFile option.
     */
    public static native String[] getRuntimeArguments();

    static {
        saveProperties(new HashMap<>());
    }

    /**
     * Initialize archived static fields in the given Class using archived
     * values from CDS dump time. Also initialize the classes of objects in
     * the archived graph referenced by those fields.
     *
     * Those static fields remain as uninitialized if there is no mapped CDS
     * java heap data or there is any error during initialization of the
     * object class in the archived graph.
     */
    public static void initializeFromArchive(final Class<?> c) {
    }
}