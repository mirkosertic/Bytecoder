/*
 * Copyright 2022 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm;

import org.objectweb.asm.Type;

public abstract class Projection {

    public static class DefaultProjection extends Projection {

        public DefaultProjection(final EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public DefaultProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new DefaultProjection(edgeType);
        }
    }

    public static class TrueProjection extends Projection {

        public TrueProjection(final EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public TrueProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new TrueProjection(edgeType);
        }

        @Override
        public String additionalDebugInfo() {
            return "TRUE";
        }
    }

    public static class FalseProjection extends Projection {

        public FalseProjection(final EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public FalseProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new FalseProjection(edgeType);
        }

        @Override
        public String additionalDebugInfo() {
            return "FALSE";
        }
    }

    public static class ExceptionHandler extends Projection {

        public final Type type;

        public ExceptionHandler(final Type type) {
            super(EdgeType.FORWARD);
            this.type = type;
        }

        @Override
        public ExceptionHandler withEdgeType(final EdgeType edgeType) {
            return this;
        }

        @Override
        public String additionalDebugInfo() {
            return "EXCEPTIONHANDLER : " + type;
        }
    }

    public static class FinallyProjection extends Projection {

        public FinallyProjection(final EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public FinallyProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new FinallyProjection(edgeType);
        }

        @Override
        public String additionalDebugInfo() {
            return "FINALLY";
        }
    }

    private final EdgeType edgeType;

    protected Projection(final EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public EdgeType edgeType() {
        return edgeType;
    }

    public abstract <T extends Projection> T withEdgeType(final EdgeType edgeType);

    public String additionalDebugInfo() {
        return "";
    }
}
