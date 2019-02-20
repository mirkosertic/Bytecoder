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
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_MEM_USE_HOST_PTR;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContextFromType;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clFinish;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.api.opencl.Context;
import de.mirkosertic.bytecoder.api.opencl.FloatSerializable;
import de.mirkosertic.bytecoder.api.opencl.Kernel;
import de.mirkosertic.bytecoder.api.opencl.OpenCLType;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OpenCLContext implements Context {

    private static class CachedKernel {

        private final OpenCLInputOutputs inputOutputs;
        private final cl_program program;
        private final cl_kernel kernel;

        CachedKernel(final OpenCLInputOutputs aInputOutputs, final cl_program aProgram, final cl_kernel aKernel) {
            inputOutputs = aInputOutputs;
            program = aProgram;
            kernel = aKernel;
        }

        void close() {
            clReleaseKernel(kernel);
            clReleaseProgram(program);
        }
    }

    private static class DataRef {

        private final Pointer pointer;
        private final int size;

        DataRef(final Pointer aPointer, final int aSize) {
            pointer = aPointer;
            size = aSize;
        }

        void updateFromBuffer() {
        }
    }

    private static final Map<Class, OpenCLCompileResult> ALREADY_COMPILED = new HashMap<>();

    private final OpenCLCompileBackend backend;
    private final CompileOptions compileOptions;
    private final cl_context context;
    private final cl_command_queue commandQueue;
    private final Map<Class, CachedKernel> cachedKernels;
    private final Logger logger;
    private final OpenCLPlatform platform;

    OpenCLContext(final OpenCLPlatform aPlatform, final Logger aLogger) {
        logger = aLogger;
        platform = aPlatform;
        cachedKernels = new HashMap<>();
        backend = new OpenCLCompileBackend();
        compileOptions = new CompileOptions(new Slf4JLogger(), false, KnownOptimizer.ALL, true, "opencl", 512, 512, false);

        final cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, aPlatform.selectedPlatform.id);

        context = clCreateContextFromType(
                contextProperties, CL_DEVICE_TYPE_ALL, null, null, null);

        commandQueue =
                clCreateCommandQueue(context, aPlatform.selectedDevice.id, 0, null);
    }

    private CachedKernel kernelFor(final Kernel aKernel) throws IOException {

        final Class theKernelClass = aKernel.getClass();

        final CachedKernel theCachedKernel = cachedKernels.get(theKernelClass);
        if (null != theCachedKernel) {
            return theCachedKernel;
        }

        OpenCLCompileResult theResult = ALREADY_COMPILED.get(theKernelClass);
        OpenCLCompileResult.OpenCLContent content = null;
        if (null == theResult) {
            final Method theMethod;
            try {
                theMethod = aKernel.getClass().getDeclaredMethod("processWorkItem");
            } catch (final Exception e) {
                throw new IllegalArgumentException("Error resolving kernel method", e);
            }

            final BytecodeMethodSignature theSignature = backend.signatureFrom(theMethod);

            final BytecodeLoader theLoader = new BytecodeLoader(theKernelClass.getClassLoader());
            final BytecodeLinkerContext theLinkerContext = new BytecodeLinkerContext(theLoader, compileOptions.getLogger());
            theResult = backend.generateCodeFor(compileOptions, theLinkerContext, aKernel.getClass(), theMethod.getName(), theSignature);

            content = theResult.getContent()[0];

            logger.debug("Generated Kernel code : {}", content.asString());

            ALREADY_COMPILED.put(theKernelClass, theResult);
        } else {
            content = theResult.getContent()[0];
        }

        // Construct the program
        final cl_program theCLProgram = clCreateProgramWithSource(context,
                1, new String[]{ content.asString() }, null, null);

        try {
            clBuildProgram(theCLProgram, 0, null, null, null, null);
        } catch (final Exception e) {
            throw new RuntimeException("Error compiling : " + content.asString(), e);
        }

        final cl_kernel theKernel = clCreateKernel(theCLProgram, "BytecoderKernel", null);

        final CachedKernel theCached = new CachedKernel(content.getInputOutputs(), theCLProgram, theKernel);
        cachedKernels.put(theKernelClass, theCached);
        return theCached;
    }

    @Override
    public void compute(final int aNumberOfStreams, final Kernel aKernel) throws IOException {

        final Class theKernelClass = aKernel.getClass();
        final CachedKernel theCachedKernel = kernelFor(aKernel);

        // Construct the input and output elements based on object properties
        final List<OpenCLInputOutputs.KernelArgument> theArguments = theCachedKernel.inputOutputs.arguments();
        final cl_mem[] theMemObjects = new cl_mem[theArguments.size()];
        final Map<Integer, DataRef> theOutputs = new HashMap<>();
        try {
            for (int i = 0; i < theArguments.size(); i++) {

                final OpenCLInputOutputs.KernelArgument theArgument = theArguments.get(i);

                final TypeRef theFieldType = TypeRef.toType(theArgument.getField().getValue().getTypeRef());
                final DataRef theDataRef;
                if (theFieldType.isArray()) {
                    final TypeRef.ArrayTypeRef theArrayTypeRef = (TypeRef.ArrayTypeRef) theFieldType;
                    final TypeRef theArrayElement = TypeRef.toType(theArrayTypeRef.arrayType().getType());
                    switch (theArrayElement.resolve()) {
                    case INT: {
                        final Field theField = theKernelClass.getDeclaredField(theArgument.getField().getValue().getName().stringValue());
                        theField.setAccessible(true);
                        final int[] theData = (int[]) theField.get(aKernel);
                        theDataRef = new DataRef(Pointer.to(theData), Sizeof.cl_int * theData.length);
                        break;
                    }
                    case FLOAT: {
                        final Field theField = theKernelClass.getDeclaredField(theArgument.getField().getValue().getName().stringValue());
                        theField.setAccessible(true);
                        final float[] theData = (float[]) theField.get(aKernel);
                        theDataRef = new DataRef(Pointer.to(theData), Sizeof.cl_float * theData.length);
                        break;
                    }
                    case REFERENCE: {
                        final Field theField = theKernelClass.getDeclaredField(theArgument.getField().getValue().getName().stringValue());
                        theField.setAccessible(true);
                        final Object[] theData = (Object[]) theField.get(aKernel);

                        final Class theObjectType = theData.getClass().getComponentType();

                        theDataRef = toDataRef(theData, theObjectType);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Not supported array element type " + theArrayElement.resolve() + " for kernel argument " + theArgument.getField().getValue().getName());
                    }
                } else {
                    throw new IllegalArgumentException("Type " + theFieldType + " is not supported for kernel argument " + theArgument.getField().getValue().getName().stringValue());
                }

                switch (theArgument.getType()) {
                case INPUT:
/*                    theMemObjects[i] = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            theDataRef.size * aNumberOfStreams, theDataRef.pointer, null);

                    break;*/
                case OUTPUT:
                case INPUTOUTPUT:
                    theMemObjects[i] = clCreateBuffer(context,
                            CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR,
                            theDataRef.size, theDataRef.pointer, null);

                    theOutputs.put(i, theDataRef);

                    break;
                }

                clSetKernelArg(theCachedKernel.kernel, i,
                        Sizeof.cl_mem, Pointer.to(theMemObjects[i]));
            }
        } catch (final Exception e) {
            throw new RuntimeException("Error extracting kernel parameter", e);
        }

        // Set the work-item dimensions
        final long[] global_work_size = {aNumberOfStreams};

        // Let the driver guess the optimal size
        final long[] local_work_size = null; //new long[] {32};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, theCachedKernel.kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Wait till everything is done
        clFinish(commandQueue);

        // Read the output data
        for (final Map.Entry<Integer, DataRef> theEntry : theOutputs.entrySet()) {
            final DataRef theDataRef = theEntry.getValue();
            clEnqueueReadBuffer(commandQueue, theMemObjects[theEntry.getKey()], CL_TRUE, 0,
                    theDataRef.size, theDataRef.pointer, 0, null, null);

            theDataRef.updateFromBuffer();
        }

        // Release memory
        for (final cl_mem theMem : theMemObjects) {
            clReleaseMemObject(theMem);
        }
    }

    private static DataRef toDataRef(final Object[] aArray, final Class aDataType) {
        if (FloatSerializable.class.isAssignableFrom(aDataType)) {
            final OpenCLType theType = (OpenCLType) aDataType.getAnnotation(OpenCLType.class);
            final int theSize = aArray.length * theType.elementCount();
            final FloatBuffer theBuffer = FloatBuffer.allocate(theSize);
            for (final Object anAArray : aArray) {
                final FloatSerializable theVec = (FloatSerializable) anAArray;
                theVec.writeTo(theBuffer);
            }
            return new DataRef(Pointer.to(theBuffer), Sizeof.cl_float * theSize) {
                @Override
                public void updateFromBuffer() {
                    theBuffer.rewind();
                    for (final Object anAArray : aArray) {
                        final FloatSerializable theVec = (FloatSerializable) anAArray;
                        theVec.readFrom(theBuffer);
                    }
                }
            };
        }
        throw new IllegalArgumentException("Not supported datatype : " + aDataType);
    }

    @Override
    public void close() {
        for (final CachedKernel theCached : cachedKernels.values()) {
            theCached.close();
        }
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
}