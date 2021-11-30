/*
 * Copyright 2021 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;

public class AnalysisStack {

    public abstract class Frame {

        protected final int index;

        public Frame(final int index) {
            this.index = index;
        }

        public void close() {
            if (!(frames.peek() == this)) {
                throw new IllegalStateException("Frame is no longer at the top of stack.");
            }
            frames.pop();
        }

        public abstract void toDebugOutput(final PrintWriter pw);
    }

    public class ClassLoadingFrame extends Frame {

        private final BytecodeObjectTypeRef activeClass;

        public ClassLoadingFrame(final int index, final BytecodeObjectTypeRef activeClass) {
            super(index);
            this.activeClass = activeClass;
        }

        @Override
        public void toDebugOutput(final PrintWriter pw) {
            pw.print(" #");
            pw.print(index);
            pw.print(" : class loading for ");
            pw.println(activeClass.name());
        }
    }

    public class MethodInvocationFrame extends Frame {

        private final BytecodeObjectTypeRef activeClass;
        private final String methodName;
        private final BytecodeMethodSignature signature;

        public MethodInvocationFrame(final int index, final BytecodeObjectTypeRef activeClass, final String methodName, final BytecodeMethodSignature signature) {
            super(index);
            this.activeClass = activeClass;
            this.methodName = methodName;
            this.signature = signature;
        }

        @Override
        public void toDebugOutput(final PrintWriter pw) {
            pw.print(" #");
            pw.print(index);
            pw.print(" : call ");
            pw.print(activeClass.name());
            pw.print(".");
            pw.print(methodName);
            pw.print("(");
            pw.print(signature.toString());
            pw.println(")");
        }
    }

    public class FieldAccessFrame extends Frame {

        private final BytecodeObjectTypeRef activeClass;
        private final String fieldName;
        private final boolean staticAccess;

        public FieldAccessFrame(final int index, final BytecodeObjectTypeRef activeClass, final String fieldName, final boolean staticAccess) {
            super(index);
            this.activeClass = activeClass;
            this.fieldName = fieldName;
            this.staticAccess = staticAccess;
        }

        @Override
        public void toDebugOutput(final PrintWriter pw) {
            pw.print(" #");
            pw.print(index);
            if (staticAccess) {
                pw.print(" : static field access ");
            } else {
                pw.print(" : field access ");
            }
            pw.print(activeClass.name());
            pw.print(".");
            pw.print(fieldName);
        }
    }

    private final Deque<Frame> frames;

    public AnalysisStack() {
        frames = new ArrayDeque<>();
    }

    public Frame startClassInitialization(final BytecodeObjectTypeRef className) {
        final ClassLoadingFrame frame = new ClassLoadingFrame(frames.size(), className);
        frames.push(frame);
        return frame;
    }

    public Frame startMethodInvocation(final BytecodeObjectTypeRef className, final String name, final BytecodeMethodSignature signature) {
        final MethodInvocationFrame frame = new MethodInvocationFrame(frames.size(), className, name, signature);
        frames.push(frame);
        return frame;
    }

    public Frame fieldAccess(final BytecodeObjectTypeRef className, final String name) {
        final FieldAccessFrame frame = new FieldAccessFrame(frames.size(), className, name, false);
        frames.push(frame);
        return frame;
    }

    public Frame staticFieldAccess(final BytecodeObjectTypeRef className, final String name) {
        final FieldAccessFrame frame = new FieldAccessFrame(frames.size(), className, name, true);
        frames.push(frame);
        return frame;
    }

    public String toDebugOutput() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        frames.stream().forEach(fr -> fr.toDebugOutput(pw));
        pw.flush();
        return sw.toString();
    }
}
