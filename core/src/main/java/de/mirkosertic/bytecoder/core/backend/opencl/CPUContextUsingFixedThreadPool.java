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
import de.mirkosertic.bytecoder.api.opencl.Kernel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.set_global_id;
import static de.mirkosertic.bytecoder.api.opencl.GlobalFunctions.set_global_size;

public class CPUContextUsingFixedThreadPool implements Context {

    private final ExecutorService executorService;
    private final Logger logger;

    public CPUContextUsingFixedThreadPool(final Logger logger) {
        this.logger = logger;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {

            int counter = 0;

            @Override
            public Thread newThread(final Runnable aRunnable) {
                return new Thread(aRunnable, "OpenCL-CPU#" + (counter ++));
            }
        });
    }

    @Override
    public void compute(final int aNumberOfStreams, final Kernel aKernel) {
        final CountDownLatch theLatch = new CountDownLatch(aNumberOfStreams);
        for (int i=0;i<aNumberOfStreams;i++) {
            final int theWorkItemId = i;
            executorService.submit(() -> {
                try {
                    set_global_id(0, theWorkItemId);
                    set_global_size(0, aNumberOfStreams);
                    aKernel.processWorkItem();
                } finally {
                    theLatch.countDown();
                }
            });
        }
        try {
            theLatch.await();
        } catch (final InterruptedException e) {
            throw new IllegalStateException("Something went wrong", e);
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
