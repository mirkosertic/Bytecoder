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

public abstract class Kernel {

    private final ThreadLocal<Integer> currentWorkItemId = new ThreadLocal<>();

    @OpenCLFunction("get_global_id")
    protected int get_global_id(int aDimension) {
        return currentWorkItemId.get();
    }

    public void set_global_id(int aDimension, int aId) {
        currentWorkItemId.set(aId);
    }

    public abstract void processWorkItem();
}
