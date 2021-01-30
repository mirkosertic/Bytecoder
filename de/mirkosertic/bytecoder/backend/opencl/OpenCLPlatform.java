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
package de.mirkosertic.bytecoder.backend.opencl;

import static org.jocl.CL.CL_DEVICE_MAX_CLOCK_FREQUENCY;
import static org.jocl.CL.CL_DEVICE_MAX_COMPUTE_UNITS;
import static org.jocl.CL.CL_DEVICE_MAX_WORK_GROUP_SIZE;
import static org.jocl.CL.CL_DEVICE_MAX_WORK_ITEM_SIZES;
import static org.jocl.CL.CL_DEVICE_NAME;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_PLATFORM_NAME;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clGetPlatformInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.api.opencl.OpenCLOptions;
import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import de.mirkosertic.bytecoder.api.opencl.Context;
import de.mirkosertic.bytecoder.api.opencl.DeviceProperties;
import de.mirkosertic.bytecoder.api.opencl.Platform;
import de.mirkosertic.bytecoder.api.opencl.PlatformProperties;

public class OpenCLPlatform implements Platform {

    final Platform selectedPlatform;
    final Device selectedDevice;
    private final Logger logger;
    private final OpenCLOptions openCLOptions;

    static class Device {
        final int index;
        final cl_device_id id;
        private final DeviceProperties deviceProperties;
        private final Platform parent;

        Device(final int index, final Platform parent, final cl_device_id aId, final DeviceProperties aDeviceProperties) {
            this.index = index;
            this.parent = parent;
            id = aId;
            deviceProperties = aDeviceProperties;
        }
    }

    static class Platform {

        final int index;
        final cl_platform_id id;
        private final PlatformProperties platformProperties;
        private final List<Device> deviceList;

        Platform(final int index, final cl_platform_id aId, final PlatformProperties aPlatformProperties) {
            this.index = index;
            id = aId;
            platformProperties = aPlatformProperties;
            deviceList = new ArrayList<>();
        }
    }

    public OpenCLPlatform(final Logger aLogger, final OpenCLOptions aOptions) {

        logger = aLogger;
        openCLOptions = aOptions;

        final List<Platform> thePlatforms = new ArrayList<>();

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        final int[] theNumPlatforms = new int[1];
        clGetPlatformIDs(theNumPlatforms.length, null, theNumPlatforms);

        if (theNumPlatforms[0] == 0) {
            throw new IllegalArgumentException("No OpenCL Platform found!");
        }

        // Find all plattforms and devices

        final cl_platform_id[] thePlattforms = new cl_platform_id[theNumPlatforms[0]];
        clGetPlatformIDs(thePlattforms.length, thePlattforms, null);

        for (int i=0;i<theNumPlatforms[0];i++) {
            final cl_platform_id theSelectedPlatform = thePlattforms[i];

            try {
                final Platform thePlatform = new Platform(
                        i,
                        theSelectedPlatform, 
                        () -> getString(theSelectedPlatform, CL_PLATFORM_NAME));

                logger.info("Platform with id {} is {}", i, thePlatform.platformProperties.getName());

                // Obtain the number of devices for the platform
                final int[] numDevicesArray = new int[1];
                clGetDeviceIDs(theSelectedPlatform, CL_DEVICE_TYPE_ALL, 0, null, numDevicesArray);
                final int numDevices = numDevicesArray[0];

                final cl_device_id[] devices = new cl_device_id[numDevices];
                clGetDeviceIDs(theSelectedPlatform, CL_DEVICE_TYPE_ALL, numDevices, devices, null);

                int devideIndex = 0;
                
                for (final cl_device_id theDevice : devices) {

                    final DeviceProperties theDeviceProperties = new DeviceProperties() {
                        @Override
                        public String getName() {
                            return getString(theDevice, CL_DEVICE_NAME);
                        }

                        @Override
                        public int getNumberOfComputeUnits() {
                            return getInt(theDevice, CL_DEVICE_MAX_COMPUTE_UNITS);
                        }

                        @Override
                        public long[] getMaxWorkItemSizes() {
                            return getSizes(theDevice, CL_DEVICE_MAX_WORK_ITEM_SIZES, 3);
                        }

                        @Override
                        public long getMaxWorkGroupSize() {
                            return getSize(theDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);
                        }

                        @Override
                        public long getClockFrequency() {
                            return getLong(theDevice, CL_DEVICE_MAX_CLOCK_FREQUENCY);
                        }
                    };

                    logger.info("Found device {} with #CU {} and max workgroup size {} ", 
                            theDeviceProperties.getName(),
                            theDeviceProperties.getNumberOfComputeUnits(),
                            theDeviceProperties.getMaxWorkGroupSize());

                    thePlatform.deviceList.add(new Device(devideIndex++, thePlatform, theDevice, theDeviceProperties));
                }

                thePlatforms.add(thePlatform);
            } catch (final Exception e) {
                logger.warn("Error processing device {}", i);
            }
        }

        if (thePlatforms.isEmpty()) {
            throw new IllegalStateException("No OpenCL platform detected");
        }
        
        final Device bestDevice = findBestDevice(thePlatforms, aOptions);

        selectedDevice = bestDevice;
        selectedPlatform = bestDevice.parent;

        logger.info("Device detection done, platform {} and device {} selected", 
                selectedPlatform.index, 
                selectedDevice.index);
    }

    @Override
    public PlatformProperties getPlatformProperties() {
        return selectedPlatform.platformProperties;
    }

    @Override
    public DeviceProperties getDeviceProperties() {
        return selectedDevice.deviceProperties;
    }

    @Override
    public Context createContext() {
        return new OpenCLContext(this, logger, openCLOptions);
    }

    private Device findBestDevice(final List<Platform> platforms, final OpenCLOptions clOptions) {
        final Optional<Integer> preferredPlatformIndex = getSystemPropertyAsInt("OPENCL_PLATFORM");
        final Optional<Integer> preferredDeviceIndex = getSystemPropertyAsInt("OPENCL_DEVICE");

        return platforms.stream()
        .filter(p->preferredPlatformIndex.map(index->index==p.index).orElse(true))
        .filter(p->clOptions.getPlatformFilter().test(p.platformProperties))
        .flatMap(p->p.deviceList
                .stream()
                .filter(d->preferredDeviceIndex.map(index->index==d.index).orElse(true)))
        .sorted((a, b)->clOptions.getPreferredDeviceComparator().compare(b.deviceProperties, a.deviceProperties))
        .findFirst()
        .orElseThrow(()->new IllegalStateException("Cannot find usable OpenCL device"));
    }
    
    private Optional<Integer> getSystemPropertyAsInt(final String name) {
        final String propertyLiteral = System.getProperty(name);
        if (propertyLiteral != null && propertyLiteral.length() > 0) {
            try {
                return Optional.of(Integer.parseInt(propertyLiteral));
            } catch (Exception e) {
                logger.warn("cannot parse system property '{}' to an interer value '{}'", name);
            }
        }
        return Optional.empty();
    }
    
    private static String getString(final cl_platform_id platform, final int paramName) {
        final long[] size = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);
        final byte[] buffer = new byte[(int) size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, buffer.length - 1);
    }

    private static String getString(final cl_device_id device, final int paramName) {
        // Obtain the length of the string that will be queried
        final long[] size = new long[1];
        clGetDeviceInfo(device, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        final byte[] buffer = new byte[(int) size[0]];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

    private static long getSize(final cl_device_id device, final int paramName) {
        return getSizes(device, paramName, 1)[0];
    }

    private static long[] getSizes(final cl_device_id device, final int paramName, final int numValues) {
        // The size of the returned data has to depend on
        // the size of a size_t, which is handled here
        final ByteBuffer buffer = ByteBuffer.allocate(numValues * Sizeof.size_t).order(ByteOrder.nativeOrder());
        clGetDeviceInfo(device, paramName, Sizeof.size_t * numValues, Pointer.to(buffer), null);
        final long[] values = new long[numValues];
        if (Sizeof.size_t == 4) {
            for (int i = 0; i < numValues; i++) {
                values[i] = buffer.getInt(i * Sizeof.size_t);
            }
        } else {
            for (int i = 0; i < numValues; i++) {
                values[i] = buffer.getLong(i * Sizeof.size_t);
            }
        }
        return values;
    }

    private static long getLong(final cl_device_id device, final int paramName) {
        return getLongs(device, paramName, 1)[0];
    }

    private static long[] getLongs(final cl_device_id device, final int paramName, final int numValues) {
        final long[] values = new long[numValues];
        clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues, Pointer.to(values), null);
        return values;
    }

    private static int getInt(final cl_device_id device, final int paramName) {
        return getInts(device, paramName, 1)[0];
    }

    private static int[] getInts(final cl_device_id device, final int paramName, final int numValues) {
        final int[] values = new int[numValues];
        clGetDeviceInfo(device, paramName, Sizeof.cl_int * numValues, Pointer.to(values), null);
        return values;
    }
}
