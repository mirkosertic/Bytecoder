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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.PrintStream;

@SubstitutesInClass(completeReplace = true)
public class TThrowable {

    private final String message;

    public TThrowable(String aMessage) {
        message = aMessage;
    }

    public TThrowable(String aMessage, Throwable aCause) {
        message = aMessage;
    }

    public TThrowable(Throwable aCause) {
        message = null;
    }

    public TThrowable() {
        message = null;
    }

    private void doNothing() {
    }

    public void printStackTrace() {
    }

    public void printStackTrace(PrintStream s) {
    }

    public Throwable fillInStackTrace() {
        return (Throwable) (Object) this;
    }

    public String getMessage() {
        return message;
    }
}