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
package de.mirkosertic.bytecoder.classlib.java.security;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.security.AccessControlContext;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

@SubstitutesInClass(completeReplace = true)
public class TAccessController {

    public static <T> T doPrivileged(final PrivilegedAction<T> action) {
        return action.run();
    }

    public static <T> T doPrivileged(final PrivilegedAction<T> action, final AccessControlContext context, final Permission[] permissions) {
        return action.run();
    }

    public static <T> T doPrivileged(final PrivilegedAction<T> action, final AccessControlContext context) {
        return action.run();
    }

    public static <T> T doPrivileged(final PrivilegedExceptionAction<T> action) throws Exception {
        return action.run();
    }

    public static void checkPermission(final Permission p) {
    }

    public static AccessControlContext getContext() {
        return null;
    }
}
