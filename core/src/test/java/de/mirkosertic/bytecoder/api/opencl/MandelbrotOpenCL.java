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

import de.mirkosertic.bytecoder.backend.opencl.CPUPlatform;
import de.mirkosertic.bytecoder.unittest.Slf4JLogger;

import java.io.IOException;

public class MandelbrotOpenCL {

    private final Platform platform;
    private final Context context;
    private final MandelbrotKernel kernel;
    private long computingTime;

    public MandelbrotOpenCL() {
        platform = PlatformFactory.resolve().createPlatform(new Slf4JLogger(), new OpenCLOptions(true));
        //platform = new CPUPlatform(new Slf4JLogger());
        context = platform.createContext();
        kernel = new MandelbrotKernel(1024, 768, 512);
    }

    public MandelbrotKernel compute() throws IOException {
        final long start = System.currentTimeMillis();
        context.compute(kernel.getWidth() * kernel.getHeight(), kernel);
        computingTime = System.currentTimeMillis() - start;
        return kernel;
    }

    public long getComputingTime() {
        return computingTime;
    }
}
