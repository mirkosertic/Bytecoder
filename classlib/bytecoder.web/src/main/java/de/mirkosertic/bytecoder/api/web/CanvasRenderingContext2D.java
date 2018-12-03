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

import de.mirkosertic.bytecoder.api.Import;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;

public class CanvasRenderingContext2D implements OpaqueReferenceType {

    private final HTMLCanvasElement owningCanvas;

    public CanvasRenderingContext2D(HTMLCanvasElement aOwningCanvas) {
        owningCanvas = aOwningCanvas;
    }

    @Import(module = "canvas", name = "canvasClear")
    public native void clear();

    @Import(module = "canvas", name = "contextSave")
    public native void save();

    @Import(module = "canvas", name = "contextRestore")
    public native void restore();

    @Import(module = "canvas", name = "contextTranslate")
    public native void translate(float aX, float aY);

    @Import(module = "canvas", name = "contextScale")
    public native void scale(float aX, float aY);

    @Import(module = "canvas", name = "contextLineWidth")
    public native void lineWidth(float aWidth);

    @Import(module = "canvas", name = "contextRotate")
    public native void rotate(float aAngleInRadians);

    @Import(module = "canvas", name = "contextBeginPath")
    public native void beginPath();

    @Import(module = "canvas", name = "contextClosePath")
    public native void closePath();

    @Import(module = "canvas", name = "contextMoveTo")
    public native void moveTo(float aX, float aY);

    @Import(module = "canvas", name = "contextLineTo")
    public native void lineTo(float aX, float aY);

    @Import(module = "canvas", name = "contextArc")
    public native void arc(double x, double y, double radius, double startAngle, double endAngle, boolean anticlockwise);

    @Import(module = "canvas", name = "contextStroke")
    public native void stroke();
}
