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

import de.mirkosertic.bytecoder.api.opencl.OpenCLOptions;
import de.mirkosertic.bytecoder.api.opencl.Platform;
import de.mirkosertic.bytecoder.api.opencl.PlatformFactory;
import de.mirkosertic.bytecoder.api.Logger;

public class PlatformFactoryImpl extends PlatformFactory {

    @Override
    public Platform createPlatform(final Logger aLogger, final OpenCLOptions aOptions) {
        try {
            return new OpenCLPlatform(aLogger, aOptions);
        } catch (final Exception e) {
            aLogger.warn("Problem while detecting OpenCL device. Using CPU emulation layer", e);
            return new CPUPlatform(aLogger);
        }
    }
}
