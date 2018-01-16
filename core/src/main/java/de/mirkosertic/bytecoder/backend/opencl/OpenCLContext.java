package de.mirkosertic.bytecoder.backend.opencl;

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;

import de.mirkosertic.bytecoder.api.opencl.Context;
import de.mirkosertic.bytecoder.api.opencl.Kernel;
import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLoader;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodePackageReplacer;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.optimizer.KnownOptimizer;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

public class OpenCLContext implements Context {

    private static class CachedKernel {

        private final OpenCLInputOutputs inputOutputs;
        private final cl_program program;
        private final cl_kernel kernel;

        CachedKernel(OpenCLInputOutputs aInputOutputs, cl_program aProgram, cl_kernel aKernel) {
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

    OpenCLContext(OpenCLPlatform aPlatform) {
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

        Class theKernelClass = aKernel.getClass();
        CachedKernel theCachedKernel = kernelFor(aKernel);

        // Construct the input and output elements based on object properties
        List<OpenCLInputOutputs.KernelArgument> theArguments = theCachedKernel.inputOutputs.arguments();
        cl_mem theMemObjects[] = new cl_mem[theArguments.size()];
        Map<Integer, Pointer> theOutputs = new HashMap<>();
        try {
            for (int i = 0; i < theArguments.size(); i++) {

                OpenCLInputOutputs.KernelArgument theArgument = theArguments.get(i);

                TypeRef theFieldType = TypeRef.toType(theArgument.getField().getField().getTypeRef());
                Pointer thePointer;
                if (theFieldType.isArray()) {
                    TypeRef.ArrayTypeRef theArrayTypeRef = (TypeRef.ArrayTypeRef) theFieldType;
                    TypeRef theArrayElement = TypeRef.toType(theArrayTypeRef.arrayType().getType());
                    switch (theArrayElement.resolve()) {
                    case BYTE: {
                        Field theField = theKernelClass.getDeclaredField(theArgument.getField().getField().getName().stringValue());
                        theField.setAccessible(true);
                        byte[] theData = (byte[]) theField.get(aKernel);
                        thePointer = Pointer.to(theData);
                        break;
                    }
                    case SHORT: {
                        Field theField = theKernelClass.getDeclaredField(theArgument.getField().getField().getName().stringValue());
                        theField.setAccessible(true);
                        short[] theData = (short[]) theField.get(aKernel);
                        thePointer = Pointer.to(theData);
                        break;
                    }
                    case INT: {
                        Field theField = theKernelClass.getDeclaredField(theArgument.getField().getField().getName().stringValue());
                        theField.setAccessible(true);
                        int[] theData = (int[]) theField.get(aKernel);
                        thePointer = Pointer.to(theData);
                        break;
                    }
                    case FLOAT: {
                        Field theField = theKernelClass.getDeclaredField(theArgument.getField().getField().getName().stringValue());
                        theField.setAccessible(true);
                        float[] theData = (float[]) theField.get(aKernel);
                        thePointer = Pointer.to(theData);
                        break;
                    }
                    case DOUBLE: {
                        Field theField = theKernelClass.getDeclaredField(theArgument.getField().getField().getName().stringValue());
                        theField.setAccessible(true);
                        double[] theData = (double[]) theField.get(aKernel);
                        thePointer = Pointer.to(theData);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Not supported array element type " + theArrayElement.resolve() + " for kernel argument " + theArgument.getField().getField().getName());
                    }
                } else {
                    throw new IllegalArgumentException("Type " + theFieldType + " is not supported for kernel argument " + theArgument.getField().getField().getName().stringValue());
                }

                switch (theArgument.getType()) {
                case INPUT:
                    theMemObjects[i] = clCreateBuffer(context,
                            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                            Sizeof.cl_float * aNumberOfStreams, thePointer, null);

                    break;
                case OUTPUT:
                case INPUTOUTPUT:
                    theMemObjects[i] = clCreateBuffer(context,
                            CL_MEM_READ_WRITE,
                            Sizeof.cl_float * aNumberOfStreams, null, null);

                    theOutputs.put(i, thePointer);

                    break;
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
        for (Map.Entry<Integer, Pointer> theEntry : theOutputs.entrySet()) {
            clEnqueueReadBuffer(commandQueue, theMemObjects[theEntry.getKey()], CL_TRUE, 0,
                    aNumberOfStreams * Sizeof.cl_float, theEntry.getValue(), 0, null, null);
        }

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
