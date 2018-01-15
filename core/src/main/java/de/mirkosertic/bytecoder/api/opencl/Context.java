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
package de.mirkosertic.bytecoder.api.opencl;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileBackend;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLCompileResult;
import de.mirkosertic.bytecoder.backend.opencl.OpenCLInputOutputs;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodePackageReplacer;
import de.mirkosertic.bytecoder.ssa.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContextFromType;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;

public class Context implements AutoCloseable {

    private static class CachedKernel {

        private final OpenCLInputOutputs inputOutputs;
        private final cl_program program;
        private final cl_kernel kernel;

        public CachedKernel(OpenCLInputOutputs aInputOutputs, cl_program aProgram, cl_kernel aKernel) {
            inputOutputs = aInputOutputs;
            program = aProgram;
            kernel = aKernel;
        }
    }

    private final OpenCLCompileBackend backend;
    private final CompileOptions compileOptions;
    private final cl_context context;
    private final cl_command_queue commandQueue;
    private final Map<Class, CachedKernel> cachedKernels;

    Context(Platform aPlatform) {
        cachedKernels = new HashMap<>();
        backend = new OpenCLCompileBackend();
        compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true);

        context = clCreateContextFromType(
                aPlatform.contextProperties, CL_DEVICE_TYPE_GPU, null, null, null);

        commandQueue =
                clCreateCommandQueue(context, aPlatform.selectedDevice, 0, null);
    }

    private CachedKernel kernelFor(Kernel aKernel) {
        Class theKernelClass = aKernel.getClass();
        CachedKernel theCachedKernel = cachedKernels.get(theKernelClass);
        if (theCachedKernel != null) {
            return theCachedKernel;
        }

        Method[] theMethods = aKernel.getClass().getDeclaredMethods();
        if (theMethods.length != 1) {
            throw new IllegalArgumentException("A kernel must have exactly one declared method!");
        }

        Method theMethod = theMethods[0];

        BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

        BytecodeLoader theLoader = new BytecodeLoader(getClass().getClassLoader(), new BytecodePackageReplacer());
        BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, compileOptions.getLogger());
        OpenCLCompileResult theResult = backend.generateCodeFor(compileOptions, theLinkerContext, aKernel.getClass(), theMethod.getName(), theSignature);

        System.out.println(theResult.getData());

        // Construct the program
        cl_program theCLProgram = clCreateProgramWithSource(context,
                1, new String[]{ theResult.getData() }, null, null);

        clBuildProgram(theCLProgram, 0, null, null, null, null);

        cl_kernel theKernel = clCreateKernel(theCLProgram, "BytecoderKernel", null);

        CachedKernel theCached = new CachedKernel(theResult.getInputOutputs(), theCLProgram, theKernel);
        cachedKernels.put(theKernelClass, theCached);
        return theCached;
    }

    public void compute(int aNumberOfStreams, Kernel aKernel) {

        CachedKernel theCachedKernel = kernelFor(aKernel);

        // Construct the input and output elements based on object properties
        List<OpenCLInputOutputs.KernelArgument> theArguments = theCachedKernel.inputOutputs.arguments();
        cl_mem theMemObjects[] = new cl_mem[theArguments.size()];
        Pointer theTargetPointer = null;
        try {
            for (int i = 0; i < theArguments.size(); i++) {

                OpenCLInputOutputs.KernelArgument theArgument = theArguments.get(i);

                float[] theData = (float[]) theArgument.getField().get(aKernel);
                Pointer thePointer = Pointer.to(theData);

                if (i < theArguments.size() - 1) {
                    theMemObjects[i] = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            Sizeof.cl_float * aNumberOfStreams, thePointer, null);
                } else {
                    theMemObjects[i] = clCreateBuffer(context,
                            CL_MEM_READ_WRITE,
                            Sizeof.cl_float * aNumberOfStreams, null, null);

                    theTargetPointer = thePointer;
                }

                clSetKernelArg(theCachedKernel.kernel, i,
                        Sizeof.cl_mem, Pointer.to(theMemObjects[i]));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error extracting kernel parameter", e);
        }

        // Set the work-item dimensions
        long global_work_size[] = new long[]{aNumberOfStreams};
        long local_work_size[] = new long[]{1};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, theCachedKernel.kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, theMemObjects[2], CL_TRUE, 0,
                aNumberOfStreams * Sizeof.cl_float, theTargetPointer, 0, null, null);

        // Release memory
        for (cl_mem theMem : theMemObjects) {
            clReleaseMemObject(theMem);
        }
    }

    @Override
    public void close() {
        for (CachedKernel theCached : cachedKernels.values()) {
            clReleaseKernel(theCached.kernel);
            clReleaseProgram(theCached.program);
        }
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
}
