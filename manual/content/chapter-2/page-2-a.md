---
title: "Compiling OpenCL Kernels"
date: 2019-10-25T14:49:24+02:00
draft: false
weight: 1
---

## Compiling JVM Bytecode Kernels to OpenCL

{{% notice warning %}}
OpenCL Integration is highly experimental
{{% /notice %}}

First of all, make sure to include the following dependencies:

```
<dependencies>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder.opencl</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
    <dependency>
        <groupId>de.mirkosertic.bytecoder</groupId>
        <artifactId>bytecoder-core</artifactId>
        <version>{{% siteparam "bytecoderversion" %}}</version>
    </dependency>
</dependency>
```

Before we can use the OpenCL integration, we have to setup the whole OpenCL environment.
An OpenCL environment can be obtained by the following code:

```
import de.mirkosertic.bytecoder.api.opencl.PlatformFactory;
import de.mirkosertic.bytecoder.api.opencl.Platform;

PlatformFactory theFactory = PlatformFactory.resolve();
Platform thePlatform = theFactory.createPlatform(logger,options);
```

The `Platform` instance must only be obtained once and can be cached.

If you have multiple GPUs or a system with NVIDIA Optimus technology, you
have multiple OpenCL platform available. One for the NVIDIA GPU, and another
for the embedded Intel GPU on your CPU. By default Bytecoder logs the available
platforms at startup and selects the OpenCL platform with the highest number. 
If this does not fit, you can always add a `OPENCL_PLATFORM=x` to your JVM properties 
to set the OpenCL platform to number x.

Every OpenCL computation is done within an OpenCL `Context`. This context
keeps references to compiled OpenCL programs and allocated memory per `Kernel`.
It definitely makes sense to cache a created context. Closing and reopening
of context instances forces the OpenCL runtime to recompile Kernels, which will
cause a huge performance impact. `Context` instances are auto-closable, so they
can be used with Java try-with-resource blocks.

OpenCL `Kernel` instances are the workhorse of the system. They can be created
by sub classing `de.mirkosertic.bytecoder.api.opencl.Kernel` and adding a single
method to it. Here is a simple example of the whole workflow:

```
PlatformFactory theFactory = PlatformFactory.resolve();
Platform thePlatform = theFactory.createPlatform(logger,options);

final float[] theA = {10f, 20f, 30f, 40f};
final float[] theB = {100f, 200f, 300f, 400f};
final float[] theResult = new float[4];

try (Context theContext = thePlatform.createContext()) {
    theContext.compute(4, new Kernel() {
        public void processWorkItem() {
            int id = get_global_id(0);
            float a = theA[id];
            float b = theB[id];
            theResult[id] = a + b;
        }
    });
}

for (int i=0; i<theResult.length;i++) {
    System.out.println(theResult[i]);
}
```

This will print the numbers 110,220,330 and 440 at command line.

Here is an example of the OpenCL C-Code generated for the Java Kernel from above:

```
__kernel void BytecoderKernel(__global const float* val$theA, 
                              __global const float* val$theB, 
                              __global float* val$theResult) {
    int var1 = get_global_id(0);
    float var4 = val$theA[var1];
    float var7 = val$theB[var1];
    float var9 = var4 + var7;
    val$theResult[var1] = var9;
    return;
}
```
