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
package de.mirkosertic.bytecoder.core.ir;

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

    public static class IndexedProjection extends Projection {

        public final int index;

        public IndexedProjection(final EdgeType edgeType, final int index) {
            super(edgeType);
            this.index = index;
        }

        @Override
        public IndexedProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new IndexedProjection(edgeType, index);
        }

        @Override
        public String additionalDebugInfo() {
            return "INDEX : " + index;
        }
    }

    public static class KeyedProjection extends Projection {

        public final int key;

        public KeyedProjection(final EdgeType edgeType, final int key) {
            super(edgeType);
            this.key = key;
        }

        @Override
        public KeyedProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new KeyedProjection(edgeType, key);
        }

        @Override
        public String additionalDebugInfo() {
            return "KEY : " + key;
        }
    }

    public static class TryCatchGuardedProjection extends Projection {

        public TryCatchGuardedProjection(final EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public TryCatchGuardedProjection withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new TryCatchGuardedProjection(edgeType);
        }

        @Override
        public String additionalDebugInfo() {
            return "EXCEPTIONGUARDED";
        }
    }

    public static class TryCatchGuardedExit extends Projection {

        public TryCatchGuardedExit(final EdgeType edgeType) {
            super(edgeType);
        }

        @Override
        public TryCatchGuardedExit withEdgeType(final EdgeType edgeType) {
            if (edgeType == edgeType()) {
                return this;
            }
            return new TryCatchGuardedExit(edgeType);
        }

        @Override
        public String additionalDebugInfo() {
            return "EXITS TO";
        }

        @Override
        public boolean isControlFlow() {
            return false;
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
        public final int position;

        public ExceptionHandler(final Type type, final int position) {
            super(EdgeType.FORWARD);
            this.type = type;
            this.position = position;
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

    public boolean isControlFlow() {
        return true;
    }
}
