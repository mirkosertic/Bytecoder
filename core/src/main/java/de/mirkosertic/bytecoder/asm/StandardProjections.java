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

public class StandardProjections implements Projection {

    public static class DefaultProjection extends StandardProjections {
        public DefaultProjection(EdgeType edgeType) {
            super(edgeType);
        }
    }

    public static class TrueProjection extends StandardProjections {
        public TrueProjection(EdgeType edgeType) {
            super(edgeType);
        }
    }

    public static class FalseProjection extends StandardProjections {
        public FalseProjection(EdgeType edgeType) {
            super(edgeType);
        }
    }

    public static class ExceptionHandler extends StandardProjections {
        public ExceptionHandler() {
            super(EdgeType.FORWARD);
        }
    }

    public static final StandardProjections DEFAULT_FORWARD = new DefaultProjection(EdgeType.FORWARD);

    public static final StandardProjections DEFAULT_BACKWARD = new DefaultProjection(EdgeType.BACK);

    public static final StandardProjections EXCEPTION_FORWARD = new ExceptionHandler();
    private final EdgeType edgeType;

    protected StandardProjections(EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    @Override
    public EdgeType edgeType() {
        return null;
    }
}
