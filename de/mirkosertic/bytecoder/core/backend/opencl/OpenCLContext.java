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
package de.mirkosertic.bytecoder.core.backend.opencl;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.api.opencl.Context;
import de.mirkosertic.bytecoder.api.opencl.FloatSerializable;
import de.mirkosertic.bytecoder.api.opencl.Kernel;
import de.mirkosertic.bytecoder.api.opencl.OpenCLType;
import de.mirkosertic.bytecoder.core.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.ir.AnalysisStack;
import de.mirkosertic.bytecoder.core.loader.BytecoderLoader;
import de.mirkosertic.bytecoder.core.optimizer.Optimizations;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jocl.CL.*;

class OpenCLContext implements Context {

    private static class CachedKernel {

        private final OpenCLInputOutputs inputOutputs;
        private final cl_program program;
        private final cl_kernel kernel;

        CachedKernel(final OpenCLInputOutputs inputOutputs, final cl_program program, final cl_kernel kernel) {
            this.inputOutputs = inputOutputs;
            this.program = program;
            this.kernel = kernel;
        }

        void close() {
            clReleaseKernel(kernel);
            clReleaseProgram(program);
        }
    }

    private static class DataRef {

        private final Pointer pointer;
        private final int size;

        DataRef(final Pointer pointer, final int size) {
            this.pointer = pointer;
            this.size = size;
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

    OpenCLContext(final OpenCLPlatform platform, final Logger logger) {
        this.logger = logger;
        this.platform = platform;
        this.cachedKernels = new HashMap<>();
        this.backend = new OpenCLCompileBackend();
        this.compileOptions = new CompileOptions(logger, Optimizations.ALL, new String[0], null, true);

        final cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform.selectedPlatform.id);

        this.context = clCreateContextFromType(
                contextProperties, CL_DEVICE_TYPE_ALL, null, null, null);

        this.commandQueue =
                clCreateCommandQueue(context, platform.selectedDevice.id, 0, null);
    }

    private CachedKernel kernelFor(final Kernel kernel, final AnalysisStack analysisStack) throws IOException {

        final Class<? extends Kernel> kernelClass = kernel.getClass();

        final CachedKernel cachedKernel = cachedKernels.get(kernelClass);
        if (null != cachedKernel) {
            return cachedKernel;
        }

        OpenCLCompileResult result = ALREADY_COMPILED.get(kernelClass);
        OpenCLCompileResult.OpenCLContent content = null;
        if (null == result) {
            final Method method;
            try {
                method = kernel.getClass().getDeclaredMethod("processWorkItem");
            } catch (final Exception e) {
                throw new IllegalArgumentException("Error resolving kernel method", e);
            }

            final BytecoderLoader loader = new BytecoderLoader(kernelClass.getClassLoader());
            final CompileUnit compileUnit = new CompileUnit(loader, logger, new OpenCLIntrinsics());

            result = backend.generateCodeFor(compileUnit, kernel.getClass(), method.getName(), Type.getType(method), analysisStack);

            content = (OpenCLCompileResult.OpenCLContent) result.getContent()[0];

            logger.debug("Generated Kernel code : {}", content.asString());

            ALREADY_COMPILED.put(kernelClass, result);
        } else {
            content = (OpenCLCompileResult.OpenCLContent) result.getContent()[0];
        }

        // Construct the program
        final cl_program clProgram = clCreateProgramWithSource(context,
                1, new String[]{ content.asString() }, null, null);

        try {
            clBuildProgram(clProgram, 0, null, null, null, null);
        } catch (final Exception e) {
            throw new RuntimeException("Error compiling : " + content.asString(), e);
        }

        final cl_kernel clKernel = clCreateKernel(clProgram, "BytecoderKernel", null);

        final CachedKernel theCached = new CachedKernel(content.getInputOutputs(), clProgram, clKernel);
        cachedKernels.put(kernelClass, theCached);
        return theCached;
    }

    @Override
    public void compute(final int numberOfStreams, final Kernel kernel) throws IOException {

        final Class<? extends Kernel> kernelClass = kernel.getClass();
        final AnalysisStack analysisStack = new AnalysisStack();

        final CachedKernel cachedKernel = kernelFor(kernel, analysisStack);

        // Construct the input and output elements based on object properties
        final List<OpenCLInputOutputs.KernelArgument> arguments = cachedKernel.inputOutputs.arguments();
        final cl_mem[] memObjects = new cl_mem[arguments.size()];
        final Map<Integer, DataRef> outputs = new HashMap<>();
        try {
            for (int i = 0; i < arguments.size(); i++) {

                final OpenCLInputOutputs.KernelArgument theArgument = arguments.get(i);

                final DataRef dataRef;
                if (theArgument.getField().type.getSort() == Type.ARRAY) {
                    final Type arrayType = theArgument.getField().type.getElementType();
                    switch (arrayType.getSort()) {
                        case Type.FLOAT: {
                            final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                            field.setAccessible(true);
                            final float[] data = (float[]) field.get(kernel);
                            dataRef = new DataRef(Pointer.to(data), Sizeof.cl_float * data.length);
                            break;
                        }
                        case Type.INT: {
                            final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                            field.setAccessible(true);
                            final int[] data = (int[]) field.get(kernel);
                            dataRef = new DataRef(Pointer.to(data), Sizeof.cl_int * data.length);
                            break;
                        }
                        case Type.LONG: {
                            final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                            field.setAccessible(true);
                            final long[] data = (long[]) field.get(kernel);
                            dataRef = new DataRef(Pointer.to(data), Sizeof.cl_long * data.length);
                            break;
                        }
                        case Type.DOUBLE: {
                            final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                            field.setAccessible(true);
                            final double[] data = (double[]) field.get(kernel);
                            dataRef = new DataRef(Pointer.to(data), Sizeof.cl_double * data.length);
                            break;
                        }
                        case Type.SHORT: {
                            final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                            field.setAccessible(true);
                            final short[] data = (short[]) field.get(kernel);
                            dataRef = new DataRef(Pointer.to(data), Sizeof.cl_short * data.length);
                            break;
                        }
                        case Type.BYTE: {
                            final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                            field.setAccessible(true);
                            final byte[] data = (byte[]) field.get(kernel);
                            dataRef = new DataRef(Pointer.to(data), Sizeof.cl_char * data.length);
                            break;
                        }
                        case Type.OBJECT: {
                            final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                            field.setAccessible(true);
                            final Object[] data = (Object[]) field.get(kernel);

                            final Class<?> theObjectType = data.getClass().getComponentType();

                            dataRef = toDataRef(data, theObjectType);
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Not supported array element type " + arrayType + " for kernel argument " + theArgument.getField().name);
                    }

                    switch (theArgument.getType()) {
                        case INPUT:
/*                    theMemObjects[i] = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            theDataRef.size * aNumberOfStreams, theDataRef.pointer, null);

                    break;*/
                        case OUTPUT:
                        case INPUTOUTPUT:
                            memObjects[i] = clCreateBuffer(context,
                                    CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR,
                                    dataRef.size, dataRef.pointer, null);

                            outputs.put(i, dataRef);

                            break;
                    }

                    clSetKernelArg(cachedKernel.kernel, i,
                            Sizeof.cl_mem, Pointer.to(memObjects[i]));

                } else {
                    final Field field = kernelClass.getDeclaredField(theArgument.getField().name);
                    field.setAccessible(true);

                    switch (theArgument.getField().type.getSort()) {
                        case Type.FLOAT: {
                            final float data = (float) field.get(kernel);
                            clSetKernelArg(cachedKernel.kernel, i, Sizeof.cl_float, Pointer.to(new float[]{data}));
                            break;
                        }
                        case Type.INT: {
                            final int data = (int) field.get(kernel);
                            clSetKernelArg(cachedKernel.kernel, i, Sizeof.cl_int, Pointer.to(new int[]{data}));
                            break;
                        }
                        case Type.LONG: {
                            final long data = (long) field.get(kernel);
                            clSetKernelArg(cachedKernel.kernel, i, Sizeof.cl_long, Pointer.to(new long[]{data}));
                            break;
                        }
                        case Type.DOUBLE: {
                            final double data = (double) field.get(kernel);
                            clSetKernelArg(cachedKernel.kernel, i, Sizeof.cl_double, Pointer.to(new double[]{data}));
                            break;
                        }
                        case Type.SHORT: {
                            final short data = (short) field.get(kernel);
                            clSetKernelArg(cachedKernel.kernel, i, Sizeof.cl_short, Pointer.to(new short[]{data}));
                            break;
                        }
                        case Type.BYTE: {
                            final byte data = (byte) field.get(kernel);
                            clSetKernelArg(cachedKernel.kernel, i, Sizeof.cl_char, Pointer.to(new byte[]{data}));
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Type " + theArgument.getField().type + " is not supported for kernel argument " + theArgument.getField().name);
                    }
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException("Error extracting kernel parameter", e);
        }

        // Set the work-item dimensions
        final long[] global_work_size = {numberOfStreams};

        // Let the driver guess the optimal size
        final long[] local_work_size = null; //new long[] {32};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, cachedKernel.kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);

        // Wait till everything is done
        clFinish(commandQueue);

        // Read the output data
        for (final Map.Entry<Integer, DataRef> entry : outputs.entrySet()) {
            final DataRef theDataRef = entry.getValue();
            clEnqueueReadBuffer(commandQueue, memObjects[entry.getKey()], CL_TRUE, 0,
                    theDataRef.size, theDataRef.pointer, 0, null, null);

            theDataRef.updateFromBuffer();
        }

        // Release memory
        for (final cl_mem mem : memObjects) {
            if (mem != null) {
                clReleaseMemObject(mem);
            }
        }
    }

    private static DataRef toDataRef(final Object[] array, final Class dataType) {
        if (FloatSerializable.class.isAssignableFrom(dataType)) {
            final OpenCLType type = (OpenCLType) dataType.getAnnotation(OpenCLType.class);
            final int theSize = array.length * type.elementCount();
            final FloatBuffer buffer = FloatBuffer.allocate(theSize);
            for (final Object anAArray : array) {
                final FloatSerializable vec = (FloatSerializable) anAArray;
                vec.writeTo(buffer);
            }
            return new DataRef(Pointer.to(buffer), Sizeof.cl_float * theSize) {
                @Override
                public void updateFromBuffer() {
                    buffer.rewind();
                    for (final Object anAArray : array) {
                        final FloatSerializable vec = (FloatSerializable) anAArray;
                        vec.readFrom(buffer);
                    }
                }
            };
        }
        throw new IllegalArgumentException("Not supported datatype : " + dataType);
    }

    @Override
    public void close() {
        for (final CachedKernel cached : cachedKernels.values()) {
            cached.close();
        }
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
}
