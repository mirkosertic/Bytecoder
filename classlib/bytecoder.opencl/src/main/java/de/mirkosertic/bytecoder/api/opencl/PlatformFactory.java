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

import de.mirkosertic.bytecoder.api.Logger;

import java.util.ServiceLoader;

public abstract class PlatformFactory {

    public static PlatformFactory resolve() {
        final ServiceLoader<PlatformFactory> theLoader = ServiceLoader.load(PlatformFactory.class);
        return theLoader.iterator().next();
    }

    /**
     * Creates a {@link Platform} with default {@link OpenCLOptions}
     * @param aLogger
     * @return
     */
    public Platform createPlatform(final Logger aLogger) {
        return createPlatform(aLogger, OpenCLOptions.defaults());
    }
    
    public abstract Platform createPlatform(Logger aLogger, OpenCLOptions aOptions);
    
    
}
