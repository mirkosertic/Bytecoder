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
package de.mirkosertic.bytecoder.api.web;

public class HTMLCanvasElement extends Element {

    private final String id;
    private final CanvasRenderingContext2D renderingContext2D;

    public HTMLCanvasElement(final String aId) {
        id = aId;
        renderingContext2D = new CanvasRenderingContext2D(this);
    }

    public CanvasRenderingContext2D getContext(final String contextName) {
        return renderingContext2D;
    }
}
