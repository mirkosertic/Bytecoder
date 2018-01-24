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

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
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

import de.mirkosertic.bytecoder.core.Logger;
import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import de.mirkosertic.bytecoder.api.opencl.Context;
import de.mirkosertic.bytecoder.api.opencl.DeviceProperties;
import de.mirkosertic.bytecoder.api.opencl.Platform;
import de.mirkosertic.bytecoder.api.opencl.PlatformProperties;

public class OpenCLPlatform implements Platform {

    final cl_context_properties contextProperties;
    final cl_platform_id selectedPlatform;
    final cl_device_id selectedDevice;
    final PlatformProperties platformProperties;
    final DeviceProperties deviceProperties;
    private final Logger logger;

    public OpenCLPlatform(Logger aLogger) {

        logger = aLogger;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        cl_platform_id platforms[] = new cl_platform_id[1];
        clGetPlatformIDs(platforms.length, platforms, null);

        selectedPlatform = platforms[0];

        platformProperties = () -> getString(selectedPlatform, CL_PLATFORM_NAME);

        contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, selectedPlatform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(selectedPlatform, CL_DEVICE_TYPE_ALL, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(selectedPlatform, CL_DEVICE_TYPE_ALL, numDevices, devices, null);
        selectedDevice = devices[0];

        deviceProperties = new DeviceProperties() {
            @Override
            public String getName() {
                return getString(selectedDevice, CL_DEVICE_NAME);
            }

            @Override
            public int getNumberOfComputeUnits() {
                return getInt(selectedDevice, CL_DEVICE_MAX_COMPUTE_UNITS);
            }

            @Override
            public long[] getMaxWorkItemSizes() {
                return getSizes(selectedDevice, CL_DEVICE_MAX_WORK_ITEM_SIZES, 3);
            }

            @Override
            public long getMaxWorkGroupSize() {
                return getSize(selectedDevice, CL_DEVICE_MAX_WORK_GROUP_SIZE);
            }

            @Override
            public long getClockFrequency() {
                return getLong(selectedDevice, CL_DEVICE_MAX_CLOCK_FREQUENCY);
            }
        };
    }

    @Override
    public PlatformProperties getPlatformProperties() {
        return platformProperties;
    }

    @Override
    public DeviceProperties getDeviceProperties() {
        return deviceProperties;
    }

    public Context createContext() {
        return new OpenCLContext(this, logger);
    }

    private static String getString(cl_platform_id platform, int paramName) {
        long size[] = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);
        byte buffer[] = new byte[(int) size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, buffer.length - 1);
    }

    private static String getString(cl_device_id device, int paramName) {
        // Obtain the length of the string that will be queried
        long size[] = new long[1];
        clGetDeviceInfo(device, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte buffer[] = new byte[(int) size[0]];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

    private static long getSize(cl_device_id device, int paramName) {
        return getSizes(device, paramName, 1)[0];
    }

    static long[] getSizes(cl_device_id device, int paramName, int numValues) {
        // The size of the returned data has to depend on
        // the size of a size_t, which is handled here
        ByteBuffer buffer = ByteBuffer.allocate(numValues * Sizeof.size_t).order(ByteOrder.nativeOrder());
        clGetDeviceInfo(device, paramName, Sizeof.size_t * numValues, Pointer.to(buffer), null);
        long values[] = new long[numValues];
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

    private static long getLong(cl_device_id device, int paramName) {
        return getLongs(device, paramName, 1)[0];
    }

    private static long[] getLongs(cl_device_id device, int paramName, int numValues) {
        long values[] = new long[numValues];
        clGetDeviceInfo(device, paramName, Sizeof.cl_long * numValues, Pointer.to(values), null);
        return values;
    }

    private static int getInt(cl_device_id device, int paramName) {
        return getInts(device, paramName, 1)[0];
    }

    private static int[] getInts(cl_device_id device, int paramName, int numValues) {
        int values[] = new int[numValues];
        clGetDeviceInfo(device, paramName, Sizeof.cl_int * numValues, Pointer.to(values), null);
        return values;
    }
}
