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

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.set_global_id;
import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.set_global_size;

import java.util.stream.IntStream;

import de.mirkosertic.bytecoder.api.Logger;
import de.mirkosertic.bytecoder.api.opencl.Context;
import de.mirkosertic.bytecoder.api.opencl.Kernel;

public class CPUContext implements Context {

    private final Logger logger;

    public CPUContext(Logger aLogger) {
        logger = aLogger;
    }

    @Override
    public void compute(int aNumberOfStreams, Kernel aKernel) {
        
        IntStream.range(0, aNumberOfStreams)
        .parallel()
        .forEach(workItemId->{
            try {
                set_global_size(0, aNumberOfStreams);
                set_global_id(0, workItemId);
                aKernel.processWorkItem();
            } catch (Exception e) {
                throw new IllegalStateException("Kernel execution (single work item) failed.", e);
            }
        }); // blocks until all work-items are complete

    }

    @Override
    public void close() {
        // no-op
    }
}
