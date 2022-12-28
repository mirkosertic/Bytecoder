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

public abstract class Projection {

    public static class DefaultProjection extends Projection {

        public DefaultProjection(EdgeType edgeType) {
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

        public TrueProjection(EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public TrueProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new TrueProjection(edgeType);
        }
    }

    public static class FalseProjection extends Projection {

        public static final FalseProjection FALSE = new FalseProjection(EdgeType.FORWARD);

        public FalseProjection(EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public FalseProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new FalseProjection(edgeType);
        }
    }

    public static class ExceptionHandler extends Projection {

        public static final ExceptionHandler INSTANCE = new ExceptionHandler();

        public ExceptionHandler() {
            super(EdgeType.FORWARD);
        }

        @Override
        public ExceptionHandler withEdgeType(final EdgeType edgeType) {
            return this;
        }
    }

    private final EdgeType edgeType;

    protected Projection(EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public EdgeType edgeType() {
        return edgeType;
    }

    public abstract <T extends Projection> T withEdgeType(final EdgeType edgeType);
}
