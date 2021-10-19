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
    private Throwable cause;

    public TThrowable(final String aMessage) {
        message = aMessage;
        cause = null;
    }

    public TThrowable(final String aMessage, final Throwable aCause) {
        message = aMessage;
        cause = aCause;
    }

    protected TThrowable(final String aMessage, final Throwable aCause, final boolean enableSupression, final boolean writeableStackTrace) {
        message = aMessage;
        cause = aCause;
    }

    public TThrowable(final Throwable aCause) {
        message = null;
        cause = aCause;
    }

    public TThrowable() {
        message = null;
        cause = null;
    }

    private void doNothing() {
    }

    public void printStackTrace() {
    }

    public void printStackTrace(final PrintStream s) {
    }

    public Throwable fillInStackTrace() {
        return (Throwable) (Object) this;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public Throwable initCause(final Throwable aCause) {
        cause = aCause;
        return (Throwable) (Object) this;
    }

    public void addSuppressed(final Throwable aCause) {
    }

    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    public void setStackTrace(final StackTraceElement[] trace) {
    }
}