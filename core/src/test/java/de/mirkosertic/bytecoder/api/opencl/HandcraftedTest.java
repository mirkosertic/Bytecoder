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
package de.mirkosertic.bytecoder.api.opencl;

import org.jocl.CL;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.junit.Ignore;
import org.junit.Test;

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContextFromType;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;

public class HandcraftedTest {

    @Test
    @Ignore
    public void testHandcrafttedCode() {

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

        final cl_platform_id theSelectedPlatform = thePlattforms[0];

        // Obtain the number of devices for the platform
        final int[] numDevicesArray = new int[1];
        clGetDeviceIDs(theSelectedPlatform, CL_DEVICE_TYPE_ALL, 0, null, numDevicesArray);
        final int numDevices = numDevicesArray[0];

        final cl_device_id[] devices = new cl_device_id[numDevices];
        clGetDeviceIDs(theSelectedPlatform, CL_DEVICE_TYPE_ALL, numDevices, devices, null);

        final cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, theSelectedPlatform);

        final cl_context context = clCreateContextFromType(
                contextProperties, CL_DEVICE_TYPE_ALL, null, null, null);

        final cl_command_queue commandQueue =
                clCreateCommandQueue(context, devices[0], 0, null);

        final String content = "__kernel void BytecoderKernel(__global float2* val$theA, __global float2* val$theResult) {\n" +
                "    int local_1_INT = get_global_id(0);\n" +
                "    float2 local_2_REFERENCE = normalize(*&val$theA[local_1_INT]);\n" +
                "    val$theResult[local_1_INT].x = local_2_REFERENCE.s1;\n" +
                "}";

        final cl_program theCLProgram = clCreateProgramWithSource(context,
                1, new String[]{ content }, null, null);

        try {
            clBuildProgram(theCLProgram, 0, null, null, null, null);
        } catch (final Exception e) {
            throw new RuntimeException("Error compiling : " + content, e);
        }

        final cl_kernel theKernel = clCreateKernel(theCLProgram, "BytecoderKernel", null);
    }
}
