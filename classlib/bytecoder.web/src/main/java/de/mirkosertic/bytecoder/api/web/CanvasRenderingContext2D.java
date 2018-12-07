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

import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;

public interface CanvasRenderingContext2D extends OpaqueReferenceType {

    @OpaqueProperty
    void setFillStyle(String aStyle);

    @OpaqueProperty
    void setStrokeStyle(String aStyle);

    void fillRect(float x, float y, float width, float height);

    void save();

    void restore();

    void translate(float aX, float aY);

    void scale(float aX, float aY);

    @OpaqueProperty
    void setLineWidth(float aWidth);

    void rotate(float aAngleInRadians);

    void beginPath();

    void closePath();

    void moveTo(float aX, float aY);

    void lineTo(float aX, float aY);

    void arc(double x, double y, double radius, double startAngle, double endAngle, boolean anticlockwise);

    void stroke();
}
