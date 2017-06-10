/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.annotations.DelegatesTo;
import de.mirkosertic.bytecoder.annotations.OverrideParentClass;
import de.mirkosertic.bytecoder.classlib.java.io.TPrintStream;

public class TThrowable {

    private String message;

    @DelegatesTo(methodName = "doNothingAgain")
    public TThrowable() {
    }

    public TThrowable(String aMessage) {
        message = aMessage;
    }

    public String getMessage() {
        return message;
    }

    public void printStackTrace(TPrintStream aStream) {
    }

    public void addSuppressed(TThrowable aThrowable) {
    }

    private void doNothingAgain() {

    }
}