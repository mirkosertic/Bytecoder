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
package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.api.OpaqueProperty;

public abstract class Window implements EventTarget, WindowOrWorkerGlobalScope {

    @Import(module = "runtime", name = "nativewindow")
    public native static Window window();

    @OpaqueProperty("document")
    public abstract HTMLDocument document();

    public abstract void requestAnimationFrame(AnimationFrameCallback aCallback);

    @OpaqueProperty
    public abstract float innerWidth();

    @OpaqueProperty
    public abstract float innerHeight();
}
