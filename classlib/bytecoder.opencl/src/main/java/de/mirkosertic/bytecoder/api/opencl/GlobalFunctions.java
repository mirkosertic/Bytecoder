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

public class GlobalFunctions {

    private static class Context {
        int currentWorkItekId;
        int size;
    }

    private final static ThreadLocal<Context> currentContext = new ThreadLocal<>();

    private static Context current() {
        Context theCurrent = currentContext.get();
        if (theCurrent == null) {
            theCurrent = new Context();
            currentContext.set(theCurrent);
        }
        return theCurrent;
    }

    @OpenCLFunction("get_global_id")
    public static int get_global_id(int aDimension) {
        return current().currentWorkItekId;
    }

    @OpenCLFunction("get_global_size")
    public static int get_global_size(int aDimension) {
        return current().size;
    }

    public static void set_global_id(int aDimension, int aId) {
        current().currentWorkItekId = aId;
    }

    public static void set_global_size(int aDimension, int aSize) {
        current().size = aSize;
    }
}
